<h1 align="center">quick-actions-kmp</h1>

<p align="center"><b>One API. Home-screen quick actions on iOS and Android.</b></p>

<p align="center">
  <a href="https://central.sonatype.com/artifact/io.github.androidpoet/quick-actions"><img alt="Maven Central" src="https://img.shields.io/maven-central/v/io.github.androidpoet/quick-actions?color=blue&label=Maven%20Central"/></a>
  <a href="https://kotlinlang.org"><img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.2.21-7F52FF?logo=kotlin&logoColor=white"/></a>
  <a href="LICENSE"><img alt="License" src="https://img.shields.io/badge/License-MIT-green.svg"/></a>
</p>

<p align="center">
  <img alt="badge-android" src="http://img.shields.io/badge/-android-6EDB8D.svg?style=flat"/>
  <img alt="badge-ios" src="http://img.shields.io/badge/-ios-CDCDCD.svg?style=flat"/>
  <img alt="badge-macos" src="http://img.shields.io/badge/-macos%20(stub)-111111.svg?style=flat"/>
  <img alt="badge-jvm" src="http://img.shields.io/badge/-jvm%20(stub)-DB413D.svg?style=flat"/>
  <img alt="badge-wasm" src="https://img.shields.io/badge/-wasm%20(stub)-624FE8.svg?style=flat"/>
</p>

<p align="center">
Publish the menu that appears when the user long-presses your app icon, and receive the tap,
from <code>commonMain</code>. iOS uses <b>UIApplicationShortcutItem</b>; Android uses
<b>ShortcutManagerCompat</b> dynamic shortcuts. The same <code>QuickAction</code> you published
comes back in the launch, on both platforms, whether the tap started the app or reached it running.
</p>

## Screenshots

<table align="center">
  <tr>
    <th>iOS Home Screen</th>
    <th>iOS launch received</th>
    <th>Android launcher</th>
    <th>Android launch received</th>
  </tr>
  <tr>
    <td><img src="art/ios-menu.png" alt="iOS quick action menu with a static item and three dynamic actions" width="200"/></td>
    <td><img src="art/ios-launch.png" alt="Sample app on iOS showing the action that opened it after a cold start" width="200"/></td>
    <td><img src="art/android-menu.png" alt="Android app drawer shortcut menu with a static item and three dynamic actions" width="200"/></td>
    <td><img src="art/android-launch.png" alt="Sample app on Android showing the action that opened it after a cold start" width="200"/></td>
  </tr>
</table>

## Install

```kotlin
commonMain.dependencies {
    implementation("io.github.androidpoet:quick-actions:0.2.0")
    implementation("io.github.androidpoet:quick-actions-compose:0.2.0")   // Compose helpers
}
```

That is the whole setup. No Swift, no `export`, no manifest edits.

## Use

```kotlin
@Composable
fun App() {
    val quickActions = rememberQuickActionsManager()

    // 1. Publish the menu. Re-publishing an equal list is free, so keep this at the root.
    PublishQuickActions(
        listOf(
            QuickAction("start-timer", "Start timer", subtitle = "25 minutes", icon = "timer", data = mapOf("route" to "timer")),
            QuickAction("log-water", "Log water", icon = "drop.fill", data = mapOf("route" to "water")),
        ),
        quickActions,
    )

    // 2. Receive the tap. The tap that started the app is held until this runs.
    OnQuickActionLaunch(quickActions) { launch ->
        navigate(launch.action.data["route"])
        quickActions.reportUsed(launch.action.id)
    }
}
```

`icon` is an SF Symbol name on iOS. On Android, map it to a drawable once at start-up (skip this and every
action shows the app icon):

```kotlin
QuickActions.androidConfig = AndroidQuickActionsConfig(
    iconResolver = { key -> when (key) { "timer" -> R.drawable.ic_timer; "drop.fill" -> R.drawable.ic_drop; else -> 0 } },
)
```

Without Compose: `IosQuickActionsManager()` / `AndroidQuickActionsManager(context)`, collect `launches`, and on
Android call `QuickActions.attach(activity)` once in `onCreate`.

## What you get

| | |
| --- | --- |
| `set` / `add` / `remove` / `clear` | Replace or edit the dynamic items; results are typed, never exceptions |
| `actions` | `StateFlow` of what is published, restored from the platform |
| `launches` | Every tap, as the `QuickAction` you published, with `createdScreen` and a timestamp |
| `maxActions` | Free slots: the platform ceiling minus static items |
| `reportUsed(id)` | Feeds Android launcher ranking; no-op on iOS |

`launch.createdScreen` is `true` when the tap started the screen and `false` when it reached one already showing.

## Platforms

| Platform | Surface | Floor | App-side setup |
| --- | --- | --- | --- |
| Android | Launcher long-press menu (dynamic shortcuts) | API 26 | None with Compose, else `QuickActions.attach(activity)` |
| iOS | Home Screen quick actions | iOS 13 | None |
| JVM, macOS, Wasm | `UnsupportedQuickActionsManager`, so shared code compiles | — | — |

On iOS the library hooks the app and scene delegates when the binary loads, so SwiftUI apps, UIKit apps
with their own delegates and apps without a scene manifest all work as they are; your own delegate methods
keep running. On Android, `attach` delivers the launch intent once per Activity lifetime, ignores relaunches
from Recents and drops intents whose id is not a shortcut the platform knows for your app. Static items
(`Info.plist`, `shortcuts.xml` with `QuickActions.ACTION`) are delivered too.

## Things to know

- **Four visible slots.** Both home screens show about four items including static ones. iOS rejects the
  fifth with `TooManyActions`; Android accepts more (`maxActions` is usually 14) and hides the rest.
- **Android long label** is `title · subtitle`, because launchers show the long label when it fits.
- **`data` is untrusted.** It travels through an exported Activity. Use it to pick a route; never run it.
- **One collector.** Each launch reaches exactly one collector of `launches`. Collect at the root.
- **Rate limiting.** Android refuses shortcut changes from a backgrounded app; you get `RateLimited`.

Full reference, Android options and the iOS hook details: https://androidpoet.github.io/quick-actions-kmp/

## Sample

`sample/composeApp` is one shared screen: pick actions, publish, and watch launches arrive. Android:
`./gradlew :sample:composeApp:installDebug`. iOS: `cd sample/composeApp/iosApp && xcodegen && open iosApp.xcodeproj`.

## License

MIT © Ranbir Singh
