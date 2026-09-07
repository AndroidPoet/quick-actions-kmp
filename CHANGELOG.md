# Changelog

All notable changes to this project are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 0.1.0 - 2026-09-07

### Added

- **`quick-actions`** — `QuickActionsManager` with `set`, `add`, `remove`, `clear`, `reportUsed`, an
  `actions` state flow restored from the platform and a `launches` flow that buffers the tap
  that started the app; typed `QuickActionsResult` / `QuickActionsException` (codes 3001–3006);
  `QuickAction` with title, subtitle, icon and free-form data; JSON wire codec shared with the
  iOS `userInfo` and Android intent extras.
- **Android** — `AndroidQuickActionsManager` over `ShortcutManagerCompat`; `QuickActions.attach(activity)`
  wires delivery for the Activity lifetime, dispatches the launch intent once (saved-state flag),
  ignores relaunches from Recents and drops forged intents whose id no shortcut carries;
  `isRateLimited`, `canPin`, `requestPin`.
- **iOS** — `IosQuickActionsManager` over `UIApplication.shortcutItems`; `QuickActions.handle(item:)`
  for the delegate glue `swift/QuickActionsDelegates.swift` (scene and non-scene paths); static
  `Info.plist` items surface as synthesised actions.
- **`quick-actions-compose`** — `rememberQuickActionsManager()`, `PublishQuickActions(actions)` and
  `OnQuickActionLaunch { }` for Compose Multiplatform.
- JVM, macOS and Wasm stubs so shared code compiles everywhere.
