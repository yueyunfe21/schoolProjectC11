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

## Retired Background Yellow OCR Probe

The old no-UI `YellowOcrProbe` script and its checked-in `.class` outputs were removed during the
CR140 release cleanup. Keep this document for the local OCR sidecar and provider-mode notes above.
Future OCR tuning should use saved testcase images or a new explicit CR-scoped replay tool instead
of reintroducing default-package probe code into the release tree.
