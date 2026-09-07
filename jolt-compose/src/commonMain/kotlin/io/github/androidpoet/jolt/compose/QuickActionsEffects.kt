package io.github.androidpoet.jolt.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import io.github.androidpoet.jolt.QuickAction
import io.github.androidpoet.jolt.QuickActionLaunch
import io.github.androidpoet.jolt.QuickActionsManager
import io.github.androidpoet.jolt.QuickActionsResult

/**
 * Publishes [actions] as the app's quick actions and re-publishes whenever the list
 * changes. Publishing a list equal to what the platform already holds is free, so
 * this is safe to keep at the root of the UI.
 *
 * @param onResult receives the outcome of each publish; failures carry a typed
 *   `QuickActionsException`, for example `TooManyActions` or Android's `RateLimited`.
 */
@Composable
public fun QuickActions(
    actions: List<QuickAction>,
    manager: QuickActionsManager = rememberQuickActionsManager(),
    onResult: (QuickActionsResult<Unit>) -> Unit = {},
) {
    val currentOnResult by rememberUpdatedState(onResult)
    LaunchedEffect(manager, actions) { currentOnResult(manager.set(actions)) }
}

/**
 * Calls [onLaunch] for every quick action tap, including the one that started the
 * app. Each launch reaches one collector, so place this once, at the root of the UI.
 */
@Composable
public fun OnQuickActionLaunch(
    manager: QuickActionsManager = rememberQuickActionsManager(),
    onLaunch: (QuickActionLaunch) -> Unit,
) {
    val currentOnLaunch by rememberUpdatedState(onLaunch)
    LaunchedEffect(manager) { manager.launches.collect { currentOnLaunch(it) } }
}
