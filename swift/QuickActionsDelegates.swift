// Copy this file into your iOS app target and replace `ComposeApp` with the
// name of the Kotlin framework that contains quick-actions-kmp. It is the only source you
// copy. Export the library from that framework so Swift sees `QuickActions` by name:
//
//     binaries.framework { export(project(":quick-actions")) }   // plus api(...) in commonMain
//
// SwiftUI lifecycle:
//     @main struct MyApp: App {
//         @UIApplicationDelegateAdaptor(QuickActionsAppDelegate.self) var delegate
//         ...
//     }
//
// UIKit apps that already have an AppDelegate / SceneDelegate: copy the three
// forwarding calls below into your own delegates instead of adopting these classes.

import ComposeApp
import UIKit

/// Routes scene connections to `QuickActionsSceneDelegate`. If you already return a scene
/// configuration, set `delegateClass = QuickActionsSceneDelegate.self` on it (or forward the
/// two calls from your own scene delegate).
final class QuickActionsAppDelegate: NSObject, UIApplicationDelegate {
    override init() {
        super.init()
        QuickActions.shared.installDelivery()
    }

    func application(
        _ application: UIApplication,
        configurationForConnecting connectingSceneSession: UISceneSession,
        options: UIScene.ConnectionOptions
    ) -> UISceneConfiguration {
        let configuration = UISceneConfiguration(name: nil, sessionRole: connectingSceneSession.role)
        configuration.delegateClass = QuickActionsSceneDelegate.self
        return configuration
    }

    // Apps WITHOUT a scene manifest receive quick actions here instead. Returning
    // `false` when a shortcut item started the app stops UIKit from also calling
    // `performActionFor`, which would dispatch the same tap twice.
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        guard application.connectedScenes.isEmpty || application.supportsMultipleScenes == false,
              let item = launchOptions?[.shortcutItem] as? UIApplicationShortcutItem,
              Bundle.main.object(forInfoDictionaryKey: "UIApplicationSceneManifest") == nil
        else { return true }
        QuickActions.shared.handle(item: item, createdScreen: true)
        return false
    }

    func application(
        _ application: UIApplication,
        performActionFor shortcutItem: UIApplicationShortcutItem,
        completionHandler: @escaping (Bool) -> Void
    ) {
        QuickActions.shared.handle(item: shortcutItem, createdScreen: false)
        completionHandler(true)
    }
}

/// Receives quick actions for scene-based apps (every SwiftUI app).
final class QuickActionsSceneDelegate: NSObject, UIWindowSceneDelegate {
    var window: UIWindow?

    // A tap that created the scene arrives here, not in `performActionFor`.
    func scene(_ scene: UIScene, willConnectTo session: UISceneSession, options connectionOptions: UIScene.ConnectionOptions) {
        if let item = connectionOptions.shortcutItem {
            QuickActions.shared.handle(item: item, createdScreen: true)
        }
    }

    func windowScene(
        _ windowScene: UIWindowScene,
        performActionFor shortcutItem: UIApplicationShortcutItem,
        completionHandler: @escaping (Bool) -> Void
    ) {
        QuickActions.shared.handle(item: shortcutItem, createdScreen: false)
        completionHandler(true)
    }
}
