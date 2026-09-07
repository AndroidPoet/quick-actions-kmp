package io.github.androidpoet.quickactions.compose

import androidx.compose.runtime.Composable
import io.github.androidpoet.quickactions.QuickActionsManager

/**
 * Returns the process-wide [QuickActionsManager] for the current platform:
 *
 * - **Android** — `QuickActions.manager(context)` with `QuickActions.androidConfig`, and wires launch
 *   delivery to the hosting Activity through `QuickActions.attach`.
 * - **iOS** — `QuickActions.manager`. Launch delivery still needs the Swift glue.
 * - **JVM desktop / browser** — `UnsupportedQuickActionsManager`.
 *
 * ```kotlin
 * val quickActions = rememberQuickActionsManager()
 * PublishQuickActions(listOf(QuickAction("start", "Start timer", icon = "timer")), quickActions)
 * OnQuickActionLaunch(quickActions) { launch -> navigate(launch.action.id) }
 * ```
 */
@Composable
public expect fun rememberQuickActionsManager(): QuickActionsManager
