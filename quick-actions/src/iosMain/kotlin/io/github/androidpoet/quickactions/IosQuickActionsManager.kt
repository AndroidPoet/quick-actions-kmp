package io.github.androidpoet.quickactions

import io.github.androidpoet.quickactions.internal.LaunchDispatcher
import io.github.androidpoet.quickactions.internal.ShortcutItemsStore
import io.github.androidpoet.quickactions.internal.UIKitShortcutItemsStore
import io.github.androidpoet.quickactions.internal.toQuickActionOrNull
import io.github.androidpoet.quickactions.internal.toRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import platform.Foundation.NSThread

/**
 * Quick actions over `UIApplication.shared.shortcutItems`. iOS 13 and up.
 *
 * Items are written on the main thread. The Home Screen shows four in total,
 * static `Info.plist` items first, so [maxActions] is four minus the static
 * count. Published actions are restored from the platform the first time they
 * are needed on the main thread (immediately when constructed there). Launches
 * arrive through [QuickActions.handle], called from the Swift glue. Use [QuickActions.manager]
 * for one shared instance.
 */
public class IosQuickActionsManager internal constructor(
    private val store: ShortcutItemsStore,
    dispatcher: LaunchDispatcher,
    seedNow: Boolean,
) : QuickActionsManager {
    /** Uses the real Home Screen list and the process-wide launch stream. */
    public constructor() : this(UIKitShortcutItemsStore(), QuickActions.dispatcher, seedNow = NSThread.isMainThread)

    private val state = MutableStateFlow<List<QuickAction>>(emptyList())
    private val mutex = Mutex()
    private var seeded = false

    init {
        if (seedNow) seedIfNeeded()
    }

    override val isSupported: Boolean = true

    override val maxActions: Int
        get() = (QUICK_ACTIONS_RECOMMENDED_MAX - store.staticItemCount).coerceAtLeast(0)

    override val actions: StateFlow<List<QuickAction>>
        get() {
            seedIfNeeded()
            return state
        }

    override val launches: Flow<QuickActionLaunch> = dispatcher.launches

    override suspend fun set(actions: List<QuickAction>): QuickActionsResult<Unit> {
        actions.validate(maxActions)?.let { return QuickActionsResult.Failure(it) }
        return mutex.withLock {
            seedIfNeeded()
            if (actions == state.value) QuickActionsResult.Success(Unit) else publish(actions)
        }
    }

    override suspend fun add(action: QuickAction): QuickActionsResult<Unit> {
        action.validate()?.let { return QuickActionsResult.Failure(it) }
        return mutex.withLock {
            seedIfNeeded()
            val current = state.value
            val next = if (current.any { it.id == action.id }) current.map { if (it.id == action.id) action else it } else current + action
            next.validate(maxActions)?.let { return QuickActionsResult.Failure(it) }
            publish(next)
        }
    }

    override suspend fun remove(vararg ids: String): QuickActionsResult<Unit> =
        mutex.withLock {
            seedIfNeeded()
            val next = state.value.filterNot { it.id in ids }
            if (next.size == state.value.size) QuickActionsResult.Success(Unit) else publish(next)
        }

    override suspend fun clear(): QuickActionsResult<Unit> =
        mutex.withLock {
            seedIfNeeded()
            if (state.value.isEmpty()) QuickActionsResult.Success(Unit) else publish(emptyList())
        }

    override fun reportUsed(id: String): Unit = Unit

    private fun seedIfNeeded() {
        if (seeded) return
        seeded = true
        state.value = store.read().mapNotNull { it.toQuickActionOrNull() }
    }

    private suspend fun publish(actions: List<QuickAction>): QuickActionsResult<Unit> {
        try {
            store.write(actions.map { it.toRecord() })
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            return QuickActionsResult.Failure(QuickActionsException.PlatformError("Could not update shortcut items: ${e.message}", e))
        }
        state.value = actions
        return QuickActionsResult.Success(Unit)
    }
}
