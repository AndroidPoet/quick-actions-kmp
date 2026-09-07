import ComposeApp
import SwiftUI

@main
struct iOSApp: App {
    // Wires quick-action delivery (scene connect + performActionFor) to Jolt.
    @UIApplicationDelegateAdaptor(JoltAppDelegate.self) var delegate

    var body: some Scene {
        WindowGroup {
            ComposeView().ignoresSafeArea()
        }
    }
}
