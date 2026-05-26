# Local OCR Experiment

This is the lightweight local OCR path for testing RapidOCR accuracy before replacing Baidu OCR in task flows.

## Install

```powershell
python -m pip install -r scripts/requirements-local-ocr.txt
```

## Start Sidecar

```powershell
python scripts/local_ocr_server.py --host 127.0.0.1 --port 18761
```

Health check:

```powershell
Invoke-RestMethod http://127.0.0.1:18761/health
```

## Java Modes

Configure `src/main/resources/application.yml`:

```yaml
bot:
  dhxy:
    ocr:
      provider: baidu
      local-endpoint: http://127.0.0.1:18761
      local-timeout-ms: 10000
```

Provider modes:

- `baidu`: current behavior. Local OCR is not used.
- `local`: use only the local OCR sidecar.
- `compare`: return Baidu OCR to the business flow, but also call local OCR and log a comparison.
- `hybrid`: try local OCR first, fall back to Baidu when local OCR returns empty or is unavailable.

Recommended first test:

1. Start `local_ocr_server.py`.
2. Set `provider: compare`.
3. Run existing OCR-heavy debug actions.
4. Compare `[ocr-compare]` lines in `logs/dhxy-console.log`.

Use `compare` first so local OCR cannot affect task behavior while we measure accuracy.

## Background Yellow OCR Probe

This is the no-UI test path we used while tuning yellow NPC/monster-name OCR. It does not require
clicking the JavaFX UI button. It captures the first detected DHXY game window through HWND capture,
crops either the center area or the full client image, runs several yellow-text washing/segmentation
variants, calls the local OCR sidecar, and writes all debug images to disk.

Important behavior:

- Script: `scripts/YellowOcrProbe.java`
- It is a standalone debug probe in the default Java package, not a Spring bean and not part of
  normal task flow.
- It does not click, move the mouse, focus the game, or send input.
- The game client only needs to be open. HWND capture can still work when the game is covered by
  other windows, as long as the native window binding is still valid.
- Current probe selects the first game window returned by `WindowsNativeWindowScanner`. If multiple
  clients are open, keep the target window first/obvious or adjust the script before relying on the
  result.
- The local OCR sidecar must already be running on `http://127.0.0.1:18761`.
- Output folder: `images/temp/yellow_probe/<yyyyMMdd-HHmmss>/`
- Useful output files include `full_window.png`, `raw_center.png`, `line_match_*`,
  `shadow_*`, `loose_*`, and the console line `best=<variant> text=<ocr text> path=<image>`.

Recommended PowerShell run from repo root:

```powershell
mvn -q -DskipTests compile dependency:build-classpath -Dmdep.outputFile=target\tools-classpath.txt
$cp = Get-Content target\tools-classpath.txt -Raw
New-Item -ItemType Directory -Force target\tools-classes | Out-Null
javac -encoding UTF-8 -cp "target/classes;$cp" -d target\tools-classes scripts\YellowOcrProbe.java
java -cp "target/tools-classes;target/classes;$cp" YellowOcrProbe 无名小妖
```

Full-window scan:

```powershell
java -cp "target/tools-classes;target/classes;$cp" YellowOcrProbe --full 无名小妖
```

If the `.class` files already exist under `scripts/`, this shorter command may also work, but it can
run stale compiled code after edits, so prefer the `javac -d target\tools-classes` flow above:

```powershell
java -cp "scripts;target/classes;$cp" YellowOcrProbe 无名小妖
```

Use this background probe when an agent needs to tune yellow-name image washing or OCR matching
quickly without restarting the JavaFX app or adding a temporary UI button.
