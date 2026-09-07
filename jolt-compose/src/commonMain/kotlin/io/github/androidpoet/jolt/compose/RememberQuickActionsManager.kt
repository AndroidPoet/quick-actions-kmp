package io.github.androidpoet.jolt.compose

import androidx.compose.runtime.Composable
import io.github.androidpoet.jolt.QuickActionsManager

/**
 * Returns the process-wide [QuickActionsManager] for the current platform:
 *
 * - **Android** — `Jolt.manager(context)` with `Jolt.androidConfig`, and wires launch
 *   delivery to the hosting Activity through `Jolt.attach`.
 * - **iOS** — `Jolt.manager`. Launch delivery still needs the Swift glue.
 * - **JVM desktop / browser** — `UnsupportedQuickActionsManager`.
 *
 * ```kotlin
 * val quickActions = rememberQuickActionsManager()
 * QuickActions(listOf(QuickAction("start", "Start timer", icon = "timer")), quickActions)
 * OnQuickActionLaunch(quickActions) { launch -> navigate(launch.action.id) }
 * ```
 */
@Composable
public expect fun rememberQuickActionsManager(): QuickActionsManager
