# Update Framework Design

## Scope

This is a safe scaffold for future software updates. It does not download installers, replace the running app, or auto-update on startup.

## Direction

- Use a public GitHub release repository later for installer packages and `latest.json`.
- Keep the first implementation mock-based and disabled by default.
- Add a manual UI entry so the operator can test update checks without affecting game tasks.
- Keep update code isolated under `com.bot.dhxy.update`.

## Manifest Shape

The future public manifest should contain:

- `latestVersion`
- `downloadUrl`
- `sha256`
- `mandatory`
- `releaseNotes`
- `publishedAt`

## First Implementation

- `UpdateProperties` owns config such as current version and mock release data.
- `UpdateManifestProvider` abstracts where update metadata comes from.
- `MockUpdateManifestProvider` returns configured mock data.
- `UpdateCheckService` compares current version with the manifest and returns a typed result.
- The JavaFX explanation page gets a `检查更新` button that logs and displays the result.

## Later Work

- Add an HTTP/GitHub manifest provider.
- Verify SHA-256 after download.
- Add a separate updater launcher or external installer flow.
- Add startup prompt only after manual checks are proven safe.
