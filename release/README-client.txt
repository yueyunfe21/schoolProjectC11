DHXY client candidate (Windows x64)

This package contains the Java UI client, Java runtime, templates and WGC capture helper.
It does NOT contain Cloud Brain, developer credentials or a game installation.

1. Extract the complete zip to a writable NEW directory. Do not overwrite a running client.
2. Before running game tasks, install the operator-approved signed FakerInput driver separately.
   Update-only UI does not need the driver. Configured game mode refuses to run without it.
   This package does not silently install a kernel driver or change the input backend.
3. You may start without Cloud configuration to use the update UI (no game tasks/capture/input).
   To run game tasks later, obtain the operator's client-connection.properties and copy it to
   config/application.properties, then restart. It contains your dedicated endpoint, UUID and token.
4. Start using Start-DHXY.cmd, not the inner DHXY.exe (the wrapper starts the capture helper).
5. Once configured, verify your license and start game tasks explicitly. No tasks start automatically.

Update entry: About -> Check for updates, or the notice after a successful license verification.
Packaged builds can download and verify a signed release. A second confirmation stops tasks and
exits for installation, then restarts the UI without starting game tasks. Keep using the ROOT
Start-DHXY.cmd so it follows the current-version pointer. Old versions and config/data are retained.
If an update fails, inspect .updates/<id>/installer-status.json and installer.log.
Current G184 stage: update implementation compiled but NOT behavior-tested or production-accepted.
Server task/session authorization is implemented but NOT runtime-accepted; no production Brain is deployed.
Do not distribute this candidate as an accepted production installer.
Keep the whole old directory and its config/data if migrating manually; do not mix binary versions.
