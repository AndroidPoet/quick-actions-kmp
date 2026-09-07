import ComposeApp
import SwiftUI

@main
struct iOSApp: App {
    // Wires quick-action delivery (scene connect + performActionFor) to QuickActions.
    @UIApplicationDelegateAdaptor(QuickActionsAppDelegate.self) var delegate

    var body: some Scene {
        WindowGroup {
            ComposeView().ignoresSafeArea()
        }
    }
}
