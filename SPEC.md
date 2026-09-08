# quick-actions-kmp — Specification (v0.1.0)

One common Kotlin Multiplatform API to **publish home-screen quick actions** — the menu that
appears when the user long-presses the app icon — and to **receive the launch** when one is
tapped. Backed by `UIApplicationShortcutItem` on iOS and `ShortcutManagerCompat` dynamic
shortcuts on Android.

Repo `AndroidPoet/quick-actions-kmp`. Artifacts `io.github.androidpoet:quick-actions`, `io.github.androidpoet:quick-actions-compose`.
Package `io.github.androidpoet.quickactions`. Built on the halo-kmp / passkeys-kmp house standard
(explicitApi, BCV dumps, detekt, spotless/ktlint, kover, dokka, KDoc everywhere, CoC, Nextra docs,
vanniktech publish).

## 1. Platform facts the design is built on

| | iOS `UIApplicationShortcutItem` | Android dynamic shortcuts |
|---|---|---|
| Publish | `UIApplication.shared.shortcutItems = [...]` (main thread) | `ShortcutManagerCompat.setDynamicShortcuts(context, list)` |
| Persistence | System stores items across launches; read back from `shortcutItems` | System stores; read back with `getShortcuts(FLAG_MATCH_DYNAMIC)` |
| Static items | `UIApplicationShortcutItems` in Info.plist; listed first | `shortcuts.xml` via `<meta-data android:name="android.app.shortcuts">`; listed first |
| Visible limit | 4 total (static + dynamic); extra dynamic items are hidden, no error | `getMaxShortcutCountPerActivity()` (usually 15); launchers show ~4–5; exceeding the max throws `IllegalArgumentException` |
| Title | `localizedTitle` (+ `localizedSubtitle`) | `shortLabel` = title; `longLabel` = "title · subtitle" (launchers show the long label when it fits, so it must keep the title) |
| Icon | `UIApplicationShortcutIcon(systemImageName:)` (SF Symbol, iOS 13+) | `IconCompat.createWithResource(context, @DrawableRes)`; adaptive-icon-shaped by the launcher |
| Payload | `userInfo: [String: NSSecureCoding]` | Intent extras (Binder; keep small) |
| Delivery | `windowScene(_:performActionFor:)` (warm) / `connectionOptions.shortcutItem` (cold) — scene delegate only; SwiftUI lifecycle needs `UIApplicationDelegateAdaptor` + `configurationForConnecting` | The shortcut `Intent` arrives in the target Activity: `onCreate` (cold) or `onNewIntent` (warm) |
| Rate limit | none | `isRateLimitingActive()` while the app is in the background; `setDynamicShortcuts` returns `false` |
| Report usage | none | `reportShortcutUsed(id)` improves launcher ranking |
| Floor | iOS 13 (SF Symbol icons) | API 26 (androidx.core 1.17) |

Design constraints: the library never invents UI; titles are passed already localized; the app
decides what a launch means (there is no routing inside the library); an action carries its own
record in the platform payload so a tap decodes back to the same `QuickAction` on both platforms.

## 2. Public API (commonMain)

```kotlin
public interface QuickActionsManager {
    /** Platform can show quick actions at all. */
    public val isSupported: Boolean
    /** Dynamic actions the platform will accept right now: platform ceiling minus static items. */
    public val maxActions: Int
    /** Dynamic actions currently published by this app, restored from the platform on creation. */
    public val actions: StateFlow<List<QuickAction>>
    /** Taps on any action (dynamic or static), buffered until collected. Single collector. */
    public val launches: Flow<QuickActionLaunch>

    /** Replaces every dynamic action. Order is display order. */
    public suspend fun set(actions: List<QuickAction>): QuickActionsResult<Unit>
    /** Adds or replaces (by id) one action, keeping the others. */
    public suspend fun add(action: QuickAction): QuickActionsResult<Unit>
    /** Removes the actions with these ids; unknown ids are ignored. */
    public suspend fun remove(vararg ids: String): QuickActionsResult<Unit>
    /** Removes every dynamic action. Static items are untouched. */
    public suspend fun clear(): QuickActionsResult<Unit>
    /** Tells the launcher the action was used (Android ranking). No-op elsewhere. */
    public fun reportUsed(id: String)
}

public data class QuickAction(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    /** iOS: SF Symbol name. Android: key passed to AndroidQuickActionsConfig.iconResolver (never getIdentifier). */
    val icon: String? = null,
    /** Free-form payload handed back on launch. */
    val data: Map<String, String> = emptyMap(),
)

public data class QuickActionLaunch(
    val action: QuickAction,
    /** True when the tap created the screen (Android onCreate, iOS scene connect); false when it reached a screen already showing. */
    val createdScreen: Boolean,
    val atEpochMillis: Long,
)

public sealed class QuickActionsResult<out T> { Success(value) ; Failure(error: QuickActionsException) }
public fun <T> QuickActionsResult<T>.getOrNull(): T?
public fun <T> QuickActionsResult<T>.errorOrNull(): QuickActionsException?

public sealed class QuickActionsException(code, message, cause) : Exception {
    Unsupported(3001), InvalidAction(3002, message), TooManyActions(3003, requested, max),
    RateLimited(3004), PayloadTooLarge(3005, message), PlatformError(3006, message)
}
```

Also public in `commonMain`:
- `QuickActionCodec` — `encode(action)` / `decode(json)`: flat JSON `{id,title,subtitle?,icon?,data}`,
  unknown keys ignored, malformed input throws `InvalidAction`. This is the wire contract into iOS
  `userInfo["quickActions"]` and Android intent extra `QuickActions.EXTRA_ACTION`.
- `QuickAction.validate(): QuickActionsException?` — blank `id` or `title` → `InvalidAction`;
  encoded action over `QUICK_ACTION_MAX_PAYLOAD_BYTES` (1024) → `PayloadTooLarge`.
- `List<QuickAction>.validate(max: Int)` — first per-item failure, then duplicate ids → `InvalidAction`,
  then `size > max` → `TooManyActions`.
- `QUICK_ACTIONS_RECOMMENDED_MAX = 4` — what both home screens actually show.
- `UnsupportedQuickActionsManager` — `isSupported=false`, `maxActions=0`, mutators fail `Unsupported`,
  `launches` never emits.

### Launch dispatch (internal, shared)
`LaunchDispatcher` — one process-wide `Channel<QuickActionLaunch>(UNLIMITED)` exposed as `receiveAsFlow()`.
Launches that arrive before any collector (cold start) are buffered, not dropped. A launch is consumed by
exactly one collector; the Compose helper is the intended one.

### Compose module (`quick-actions-compose`)
- `@Composable expect fun rememberQuickActionsManager(): QuickActionsManager` — remembered, closed in
  `DisposableEffect`. Android → `AndroidQuickActionsManager(applicationContext, QuickActions.androidConfig)` **plus
  automatic delivery** (see 3.1); iOS → `IosQuickActionsManager()`; jvm/wasmJs → `UnsupportedQuickActionsManager`.
- `@Composable fun PublishQuickActions(actions: List<QuickAction>, manager = rememberQuickActionsManager(), onResult: (QuickActionsResult<Unit>) -> Unit = {})`
  — declarative publish: `LaunchedEffect(actions) { onResult(manager.set(actions)) }`.
- `@Composable fun OnQuickActionLaunch(manager = rememberQuickActionsManager(), onLaunch: (QuickActionLaunch) -> Unit)`
  — collects `manager.launches` in a `LaunchedEffect`, latest `onLaunch` via `rememberUpdatedState`.

## 3. Platform behaviour

### 3.1 Android (minSdk 26, compileSdk 36, androidx.core 1.17)

```kotlin
public class AndroidQuickActionsConfig(
    @DrawableRes val defaultIconRes: Int = 0,                 // 0 → application icon
    val iconResolver: ((String) -> Int)? = null,              // action.icon → @DrawableRes; null/0 → defaultIconRes
    val targetActivity: ComponentName? = null,                // null → launcher activity of this package
    val intentFlags: Int = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_SINGLE_TOP,
    val intentBuilder: ((Context, QuickAction) -> Intent)? = null,  // full override; library still adds action + extra
)
public object QuickActions {
    public var androidConfig: AndroidQuickActionsConfig
    public const val ACTION: String = "io.github.androidpoet.quickactions.action.QUICK_ACTION"
    public const val EXTRA_ACTION: String = "io.github.androidpoet.quickactions.extra.ACTION"
    /** True once delivery is wired ([attach] or a handle* call). Lets apps detect missing glue. */
    public val isDeliveryInstalled: Boolean
    /** One-line wiring for ComponentActivity: dispatches the launch intent once per Activity lifetime (a
     *  SavedStateRegistry flag survives recreation and process restore), skips relaunches from Recents
     *  (FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY), listens for onNewIntent until ON_DESTROY, and drops launches whose
     *  id is not a shortcut the platform knows (dynamic, pinned or manifest). Idempotent per Activity. */
    public fun attach(activity: ComponentActivity)
    /** Raw entry points for apps not on ComponentActivity. No id verification. */
    public fun handleLaunchIntent(intent: Intent?, savedInstanceState: Bundle?): QuickActionLaunch?
    public fun handleNewIntent(intent: Intent?): QuickActionLaunch?
}
public class AndroidQuickActionsManager(context: Context, config: AndroidQuickActionsConfig = QuickActions.androidConfig) : QuickActionsManager {
    public val isRateLimited: Boolean                      // ShortcutManagerCompat.isRateLimitingActive
    public val canPin: Boolean                             // isRequestPinShortcutSupported
    public suspend fun requestPin(id: String): QuickActionsResult<Unit>   // pins a published dynamic action
}
```
- Mapping: `ShortcutInfoCompat.Builder(context, id).setShortLabel(title).setLongLabel(subtitle ?: title)
  .setIcon(resolved).setIntent(intent).setRank(index)`. Intent = `intentBuilder?.invoke(...) ?: Intent().setComponent(target)`,
  then always `setAction(QuickActions.ACTION)`, `addFlags(intentFlags)`, `putExtra(EXTRA_ACTION, QuickActionCodec.encode(action))`.
- `maxActions = getMaxShortcutCountPerActivity(context) - getShortcuts(FLAG_MATCH_MANIFEST).size` (floor 0).
- `set`: validate(list, maxActions) → if `isRateLimitingActive` → `RateLimited` → `setDynamicShortcuts`;
  `false` → `RateLimited`; `IllegalArgumentException` → `TooManyActions`/`InvalidAction`; any other → `PlatformError`.
  Success updates `actions`.
- `clear` → `removeAllDynamicShortcuts`. `remove(ids)` → `removeDynamicShortcuts(ids)`; `add` → set(current ∖ id + action).
- Restore: on construction, `getShortcuts(FLAG_MATCH_DYNAMIC)` → decode `EXTRA_ACTION` from each intent →
  `actions`. Shortcuts without our extra (foreign dynamic shortcuts) are ignored.
- **Delivery.** `QuickActions.attach(activity)` is the sanctioned entry; `quick-actions-compose`'s Android
  `rememberQuickActionsManager()` calls it with `LocalActivity`. The listener is Activity-scoped, not
  composition-scoped, so a warm tap while another screen is showing is still delivered. The decision logic
  (`LaunchIntentPolicy`: flags, saved state, extra) is a pure function so it is unit-testable without a device.
- `set` returns `Success` without touching the platform when the requested list equals `actions.value`.
- `rememberQuickActionsManager()` returns one process-wide instance, so every call site shares `actions`.
- Static `shortcuts.xml` entries are delivered too when their intent uses `QuickActions.ACTION` and carries `EXTRA_ACTION`.
- `reportUsed` → `ShortcutManagerCompat.reportShortcutUsed`.

### 3.2 iOS (iOS 13+)

```kotlin
public object QuickActions {
    /** True once the load-time delivery hooks are installed (always, in an app process). */
    public val isDeliveryInstalled: Boolean
    public val manager: IosQuickActionsManager
}
public class IosQuickActionsManager() : QuickActionsManager
```
- No app-side wiring. A cinterop Objective-C unit (`src/nativeInterop/cinterop/quickActionsHook.def`)
  runs a load-time constructor that wraps `-[UIApplication setDelegate:]`, `-[UIScene setDelegate:]` and
  `-[UISceneConfiguration delegateClass]`; through them it adds or wraps
  `application:performActionForShortcutItem:completionHandler:` on the app delegate class and
  `scene:willConnectToSession:options:` + `windowScene:performActionForShortcutItem:completionHandler:` on the
  scene delegate class before UIKit computes its responds-to flags. `UIApplicationDidFinishLaunchingNotification`
  supplies the launch-options item for apps without scenes. Existing implementations are kept under
  `qa_original_<selector>` and called. Items are handed to Kotlin through a block set by the first
  `IosQuickActionsManager`; earlier items queue. The launching item is delivered once (same `type` within 3 s).
- The platform list is read lazily on the main thread (`dispatch_sync` when constructed elsewhere), never eagerly
  off-main.
- Mapping: `UIApplicationShortcutItem(type = id, localizedTitle = title, localizedSubtitle = subtitle,
  icon = icon?.let { UIApplicationShortcutIcon.iconWithSystemImageName(it) }, userInfo = mapOf("quickActions" to encode(action)))`.
  Set on `Dispatchers.Main`.
- `maxActions = 4 - (Bundle.main "UIApplicationShortcutItems").count` (floor 0).
- `set` validates then assigns; `clear` assigns `emptyList()`; `remove`/`add` derive from `actions.value`.
  Never throws; `PlatformError` only if the main-thread hop fails.
- Restore: on construction read `UIApplication.shared.shortcutItems` → decode `userInfo["quickActions"]`; items without
  it (foreign/static) are ignored for `actions`.
- **Delivery.** The hook hands every item to internal `QuickActions.handle(item, createdScreen)`, which decodes
  `userInfo["quickActions"]`; a static item without it becomes
  `QuickAction(id = type, title = localizedTitle, subtitle = localizedSubtitle, data = userInfo strings)`.
  `createdScreen` is true from `connectionOptions.shortcutItem` and the launch options, false from `performActionFor`.
- `reportUsed` no-op.

### 3.3 JVM desktop, macOS, Wasm
`UnsupportedQuickActionsManager`. `quick-actions-compose` has no macOS target (Compose Multiplatform has none); the
jvm and wasmJs actuals return it.

## 4. Tests (written from this spec, independent of the implementation)

commonTest: codec round trip incl. empty/unicode data and unknown keys; malformed JSON → `InvalidAction`;
validation matrix (blank id/title, >1024 bytes, duplicate ids, `size > max`, order of precedence);
`UnsupportedQuickActionsManager` contract; `LaunchDispatcher` buffers before a collector and delivers once.
androidUnitTest (pure seams only, no Robolectric): icon resolution precedence (resolver > default > app icon);
`LaunchIntentPolicy` returns no launch for a null extra, malformed JSON, `savedInstanceState != null`, or
`FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY`, and dispatches once for a valid extra. Framework-bound behaviour
(`ShortcutManagerCompat`, `attach`) is verified on the emulator.
iosTest: `IosQuickActionsManager` over an injected `ShortcutItemsStore` fake: set → store receives items with
`userInfo["quickActions"]`; restore decodes; `TooManyActions` respects static count; `remove`/`add` keep order;
`QuickActions.handle` for a static item synthesises the action.

## 5. Acceptance (device)

Android (API 36 AVD): install sample → long-press icon → the published actions appear with icons → tap one
while the app is dead → app opens, log shows `createdScreen=true` with the action id → tap another while the
app is open → `createdScreen=false`, no duplicate → rotate → no re-dispatch → back out, reopen from Recents →
no re-dispatch → Clear → menu shows only static items.
iOS (iPhone 17 sim): same sequence; cold tap from a killed app is handled via `connectionOptions.shortcutItem`.

Verified 2026-09-07: Android on the API 36 AVD (all steps, from the real launcher menu); iOS on the iPhone 17
simulator, iOS 26.5 (cold tap after `simctl terminate`, warm tap, static `Info.plist` item synthesised, four
items rejected with `TooManyActions` while one static item is declared).

Verified 2026-09-08 (0.2.0, no Swift glue, no `export`, stock SwiftUI `App`): iOS cold tap after
`simctl terminate` (`createdScreen=true`), warm tap and static item (`createdScreen=false`), all from the
Home Screen menu on the iPhone 17 simulator.

## 6. Out of scope for 0.1.0
Pinned-shortcut UI beyond `requestPin`; Android launcher-specific icon shaping; App Intents / App Shortcuts
(Siri); macOS Dock menus; localisation helpers.

## 7. Critique disposition
Critic: skeptical staff mobile engineer lens, 12 findings, all dispositioned.

| # | Finding | Disposition |
|---|---|---|
| 1 | Recents relaunch re-delivers the root intent's extra | **Fixed**: `LaunchIntentPolicy` drops intents with `FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY`; §5 covers Recents |
| 2 | Warm tap lost when the Compose helper is not composed | **Fixed**: `QuickActions.attach(activity)` owns an Activity-scoped `OnNewIntentListener`; Compose helper only calls attach |
| 3 | `handleIntent` cannot express cold vs warm; "cold start" misdefined | **Fixed**: split into `handleLaunchIntent` / `handleNewIntent`; field renamed `createdScreen` with a precise definition |
| 4 | Payload trusted through an exported Activity | **Fixed**: `attach` drops launches whose id no shortcut (dynamic, pinned, manifest) carries; `data` documented as untrusted; raw handle* documented as unverified |
| 5 | iOS restore off the main thread | **Fixed**: lazy seed; the store hops to main with `dispatch_sync` when needed |
| 6 | Non-scene UIKit path, double dispatch, missing export docs | **Superseded in 0.2.0**: the Swift glue, `export` and adaptor are gone; load-time hooks cover scene and non-scene apps and deduplicate the launching item |
| 7 | Multiple managers, one channel | **Fixed** (partly): one process-wide instance per platform behind `rememberQuickActionsManager()` so `actions` is shared; `launches` stays single-collector by design and the docs say collect once at the root. Rejected: replay/consume semantics, which duplicate handling instead |
| 8 | Declarative helper re-publishes and can surface `RateLimited` for a no-op | **Fixed**: `set` short-circuits on structural equality before the rate-limit check |
| 9 | Pinned/static launches carry ids absent from `actions` | **Fixed** in docs; restore sorted by rank. Rejected: `pinned` flow, out of scope for 0.1.0 |
| 10 | Android unit tests framework-bound or vacuous | **Fixed**: pure `LaunchIntentPolicy` seam with the four negative cases; framework behaviour on the emulator |
| 11 | `close()` is a no-op; missing glue is silent | **Fixed**: `close()` removed from the interface; `QuickActions.isDeliveryInstalled` on both platforms; troubleshooting table |
| 12 | macOS compose actual promised; activity dependency missing; placeholders | **Fixed**: §3.3 corrected, `androidx.activity.compose` + `LocalActivity`, placeholders deleted before `apiDump` |
