#!/usr/bin/env python
"""
Small local OCR sidecar for DHXY.

Install:
  python -m pip install -r scripts/requirements-local-ocr.txt

Run:
  python scripts/local_ocr_server.py --host 127.0.0.1 --port 18761

API:
  GET  /health
  POST /ocr/text   {"imagePath": "D:/path/to/image.png"}
  POST /ocr/words  {"imagePath": "D:/path/to/image.png"}
"""

from __future__ import annotations

import argparse
import json
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Any


OCR_ENGINE = None


def load_engine():
    global OCR_ENGINE
    if OCR_ENGINE is None:
        try:
            from rapidocr import RapidOCR
        except Exception as exc:  # pragma: no cover - startup diagnostics
            raise RuntimeError(
                "RapidOCR is not installed. Run: python -m pip install -r scripts/requirements-local-ocr.txt"
            ) from exc
        OCR_ENGINE = RapidOCR()
    return OCR_ENGINE


def run_ocr(image_path: str) -> dict[str, Any]:
    path = Path(image_path)
    if not path.exists() or not path.is_file():
        raise FileNotFoundError(f"imagePath not found: {image_path}")

    started = time.perf_counter()
    raw = load_engine()(str(path))
    words = normalize_result(raw)
    elapsed_ms = int((time.perf_counter() - started) * 1000)
    return {
        "text": "".join(item["text"] for item in words),
        "words": words,
        "elapsedMs": elapsed_ms,
    }


def normalize_result(raw: Any) -> list[dict[str, Any]]:
    # RapidOCR 3.x returns an object with boxes/txts/scores.
    if hasattr(raw, "boxes") and hasattr(raw, "txts"):
        boxes = to_list(getattr(raw, "boxes", None))
        txts = to_list(getattr(raw, "txts", None))
        scores = to_list(getattr(raw, "scores", None))
        return [
            word_from_box_text_score(boxes[i], txts[i], scores[i] if i < len(scores) else 0.0)
            for i in range(min(len(boxes), len(txts)))
        ]

    # Older RapidOCR variants often return (result, elapsed) or a plain result list.
    result = raw[0] if isinstance(raw, tuple) and raw else raw
    if hasattr(result, "boxes") and hasattr(result, "txts"):
        boxes = to_list(getattr(result, "boxes", None))
        txts = to_list(getattr(result, "txts", None))
        scores = to_list(getattr(result, "scores", None))
        return [
            word_from_box_text_score(boxes[i], txts[i], scores[i] if i < len(scores) else 0.0)
            for i in range(min(len(boxes), len(txts)))
        ]

    words: list[dict[str, Any]] = []
    if isinstance(result, list):
        for item in result:
            # Common shape: [box, text, score]
            if isinstance(item, (list, tuple)) and len(item) >= 2:
                box = item[0]
                text = item[1]
                score = item[2] if len(item) >= 3 else 0.0
                words.append(word_from_box_text_score(box, text, score))
    return words


def to_list(value: Any) -> list[Any]:
    if value is None:
        return []
    if hasattr(value, "tolist"):
        value = value.tolist()
    if isinstance(value, list):
        return value
    if isinstance(value, tuple):
        return list(value)
    return [value]


def word_from_box_text_score(box: Any, text: Any, score: Any) -> dict[str, Any]:
    points = normalize_box_points(box)
    xs = [point[0] for point in points]
    ys = [point[1] for point in points]
    left = int(round(min(xs))) if xs else 0
    top = int(round(min(ys))) if ys else 0
    right = int(round(max(xs))) if xs else left
    bottom = int(round(max(ys))) if ys else top
    width = max(0, right - left)
    height = max(0, bottom - top)
    return {
        "text": "" if text is None else str(text),
        "x": left + width // 2,
        "y": top + height // 2,
        "left": left,
        "top": top,
        "width": width,
        "height": height,
        "score": safe_float(score),
    }


def normalize_box_points(box: Any) -> list[tuple[float, float]]:
    if box is None:
        return []
    if hasattr(box, "tolist"):
        box = box.tolist()
    if isinstance(box, (list, tuple)) and len(box) == 4 and all(is_number(v) for v in box):
        left, top, right, bottom = [float(v) for v in box]
        return [(left, top), (right, top), (right, bottom), (left, bottom)]
    points: list[tuple[float, float]] = []
    if isinstance(box, (list, tuple)):
        for point in box:
            if hasattr(point, "tolist"):
                point = point.tolist()
            if isinstance(point, (list, tuple)) and len(point) >= 2:
                points.append((float(point[0]), float(point[1])))
    return points


def is_number(value: Any) -> bool:
    return isinstance(value, (int, float))


def safe_float(value: Any) -> float:
    try:
        return float(value)
    except Exception:
        return 0.0


class Handler(BaseHTTPRequestHandler):
    server_version = "DHXYLocalOCR/0.1"

    def do_GET(self):  # noqa: N802
        if self.path == "/health":
            self.send_json({"ok": True})
            return
        self.send_error(404, "Not found")

    def do_POST(self):  # noqa: N802
        if self.path not in {"/ocr/text", "/ocr/words"}:
            self.send_error(404, "Not found")
            return
        try:
            payload = self.read_json()
            image_path = payload.get("imagePath") or payload.get("image_path")
            if not image_path:
                self.send_json({"ok": False, "error": "imagePath is required"}, status=400)
                return
            result = run_ocr(str(image_path))
            if self.path == "/ocr/text":
                self.send_json({"ok": True, "text": result["text"], "elapsedMs": result["elapsedMs"]})
            else:
                self.send_json({"ok": True, **result})
        except Exception as exc:  # pragma: no cover - runtime diagnostics
            self.send_json({"ok": False, "error": str(exc)}, status=500)

    def read_json(self) -> dict[str, Any]:
        length = int(self.headers.get("Content-Length", "0"))
        data = self.rfile.read(length)
        if not data:
            return {}
        return json.loads(data.decode("utf-8"))

    def send_json(self, payload: dict[str, Any], status: int = 200):
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, fmt: str, *args):  # noqa: A003
        print("[local-ocr]", fmt % args)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=18761)
    args = parser.parse_args()

    # Load once at startup so model download/import errors are visible immediately.
    load_engine()
    server = ThreadingHTTPServer((args.host, args.port), Handler)
    print(f"[local-ocr] listening on http://{args.host}:{args.port}")
    server.serve_forever()


if __name__ == "__main__":
    main()
