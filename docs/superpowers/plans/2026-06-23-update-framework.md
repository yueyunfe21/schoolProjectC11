# Update Framework Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a disabled-by-default mock software update framework and a manual JavaFX check entry.

**Architecture:** Update logic is isolated in `com.bot.dhxy.update`. The UI calls a small service that compares configured current version with a mock manifest, leaving real download/replacement for later.

**Tech Stack:** Java 21, Spring Boot configuration properties, JavaFX, repo-local main-style tests.

---

### Task 1: Version Comparator

**Files:**
- Create: `src/test/java/com/bot/dhxy/update/UpdateVersionComparatorTest.java`
- Create: `src/main/java/com/bot/dhxy/update/UpdateVersionComparator.java`

- [ ] Write main-style tests for newer/equal/older version comparison.
- [ ] Run the test and confirm it fails before implementation.
- [ ] Implement numeric comparison with optional leading `v` and suffix stripping.
- [ ] Re-run the test and confirm it passes.

### Task 2: Update Check Service

**Files:**
- Create: `src/test/java/com/bot/dhxy/update/UpdateCheckServiceTest.java`
- Create: `src/main/java/com/bot/dhxy/update/UpdateStatus.java`
- Create: `src/main/java/com/bot/dhxy/update/UpdateManifest.java`
- Create: `src/main/java/com/bot/dhxy/update/UpdateCheckResult.java`
- Create: `src/main/java/com/bot/dhxy/update/UpdateManifestProvider.java`
- Create: `src/main/java/com/bot/dhxy/update/UpdateProperties.java`
- Create: `src/main/java/com/bot/dhxy/update/AppVersionService.java`
- Create: `src/main/java/com/bot/dhxy/update/MockUpdateManifestProvider.java`
- Create: `src/main/java/com/bot/dhxy/update/UpdateCheckService.java`

- [ ] Write tests for disabled, no update, update available, and provider failure.
- [ ] Run the test and confirm it fails before implementation.
- [ ] Implement the result model and service.
- [ ] Re-run the test and confirm it passes.

### Task 3: UI Entry And Config

**Files:**
- Modify: `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- Modify: `src/main/resources/application.properties`

- [ ] Inject `UpdateCheckService` and `AppVersionService`.
- [ ] Add a manual `检查更新` button to the explanation page.
- [ ] Show the result in logs and a JavaFX dialog.
- [ ] Add disabled mock update properties.
- [ ] Run compile and focused tests.
