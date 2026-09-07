package io.github.androidpoet.jolt.internal

import io.github.androidpoet.jolt.QuickAction
import io.github.androidpoet.jolt.QuickActionLaunch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Hands launches from the platform entry point to whoever collects
 * `QuickActionsManager.launches`. Unbounded so a cold-start tap that arrives
 * before any collector is kept, and channel-backed so each launch reaches
 * exactly one collector.
 */
internal class LaunchDispatcher(
    private val clock: () -> Long = ::nowEpochMillis,
) {
    private val channel = Channel<QuickActionLaunch>(Channel.UNLIMITED)

    val launches: Flow<QuickActionLaunch> = channel.receiveAsFlow()

    fun dispatch(
        action: QuickAction,
        createdScreen: Boolean,
    ): QuickActionLaunch {
        val launch = QuickActionLaunch(action, createdScreen, clock())
        channel.trySend(launch)
        return launch
    }
}
