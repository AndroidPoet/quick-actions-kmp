package io.github.androidpoet.quickactions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Manager for platforms without a long-press icon menu (JVM desktop, macOS,
 * browser). Lets shared code compile and run everywhere; every mutation fails
 * with [QuickActionsException.Unsupported] and [launches] never emits.
 */
public class UnsupportedQuickActionsManager : QuickActionsManager {
    override val isSupported: Boolean = false

    override val maxActions: Int = 0

    override val actions: StateFlow<List<QuickAction>> = MutableStateFlow(emptyList())

    override val launches: Flow<QuickActionLaunch> = emptyFlow()

    override suspend fun set(actions: List<QuickAction>): QuickActionsResult<Unit> = unsupported()

    override suspend fun add(action: QuickAction): QuickActionsResult<Unit> = unsupported()

    override suspend fun remove(vararg ids: String): QuickActionsResult<Unit> = unsupported()

    override suspend fun clear(): QuickActionsResult<Unit> = unsupported()

    override fun reportUsed(id: String): Unit = Unit

    private fun unsupported(): QuickActionsResult.Failure = QuickActionsResult.Failure(QuickActionsException.Unsupported())
}
