import ComposeApp
import SwiftUI
import UIKit

/// Hosts the shared Compose UI (`MainViewController()` from commonMain) in SwiftUI.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
