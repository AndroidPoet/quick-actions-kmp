package io.github.androidpoet.jolt

import android.content.Context
import androidx.core.content.pm.ShortcutManagerCompat
import io.github.androidpoet.jolt.internal.ShortcutMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Quick actions as dynamic shortcuts through `ShortcutManagerCompat`. API 26 and up.
 *
 * Every shortcut opens the configured target Activity with [Jolt.ACTION] and the
 * encoded action in [Jolt.EXTRA_ACTION]; wire that Activity with [Jolt.attach]
 * (the Compose helper does this for you). Published actions are restored from the
 * platform on construction. Shortcut changes are rate-limited while the app is in
 * the background; see [isRateLimited]. Use [Jolt.manager] for one shared instance.
 */
public class AndroidQuickActionsManager(
    context: Context,
    private val config: AndroidQuickActionsConfig = Jolt.androidConfig,
) : QuickActionsManager {
    private val appContext: Context = context.applicationContext
    private val state = MutableStateFlow(restore())
    private val mutex = Mutex()

    override val isSupported: Boolean = true

    override val maxActions: Int
        get() {
            val ceiling = ShortcutManagerCompat.getMaxShortcutCountPerActivity(appContext)
            val static = ShortcutManagerCompat.getShortcuts(appContext, ShortcutManagerCompat.FLAG_MATCH_MANIFEST).size
            return (ceiling - static).coerceAtLeast(0)
        }

    override val actions: StateFlow<List<QuickAction>> get() = state

    override val launches: Flow<QuickActionLaunch> = Jolt.dispatcher.launches

    /** Whether the system is currently refusing shortcut changes (app in the background too long). */
    public val isRateLimited: Boolean
        get() = ShortcutManagerCompat.isRateLimitingActive(appContext)

    /** Whether the launcher lets this app request pinned shortcuts. */
    public val canPin: Boolean
        get() = ShortcutManagerCompat.isRequestPinShortcutSupported(appContext)

    /** Asks the launcher to pin the published action with [id] to the home screen. */
    public suspend fun requestPin(id: String): QuickActionsResult<Unit> {
        val action =
            state.value.firstOrNull { it.id == id }
                ?: return QuickActionsResult.Failure(QuickActionsException.InvalidAction("No published quick action with id $id"))
        if (!canPin) return QuickActionsResult.Failure(QuickActionsException.Unsupported("This launcher does not support pinned shortcuts"))
        val target = target() ?: return QuickActionsResult.Failure(noLauncher())
        val info = ShortcutMapper.build(appContext, config, action, rank = 0, target = target)
        return platformCall { ShortcutManagerCompat.requestPinShortcut(appContext, info, null) }
    }

    override suspend fun set(actions: List<QuickAction>): QuickActionsResult<Unit> {
        actions.validate(maxActions)?.let { return QuickActionsResult.Failure(it) }
        return mutex.withLock { if (actions == state.value) QuickActionsResult.Success(Unit) else publish(actions) }
    }

    override suspend fun add(action: QuickAction): QuickActionsResult<Unit> {
        action.validate()?.let { return QuickActionsResult.Failure(it) }
        return mutex.withLock {
            val current = state.value
            val next = if (current.any { it.id == action.id }) current.map { if (it.id == action.id) action else it } else current + action
            next.validate(maxActions)?.let { return QuickActionsResult.Failure(it) }
            publish(next)
        }
    }

    override suspend fun remove(vararg ids: String): QuickActionsResult<Unit> =
        mutex.withLock {
            val next = state.value.filterNot { it.id in ids }
            if (next.size == state.value.size) return QuickActionsResult.Success(Unit)
            val result =
                platformCall {
                    ShortcutManagerCompat.removeDynamicShortcuts(appContext, ids.toList())
                    true
                }
            if (result is QuickActionsResult.Success) state.value = next
            result
        }

    override suspend fun clear(): QuickActionsResult<Unit> =
        mutex.withLock {
            if (state.value.isEmpty()) return QuickActionsResult.Success(Unit)
            val result =
                platformCall {
                    ShortcutManagerCompat.removeAllDynamicShortcuts(appContext)
                    true
                }
            if (result is QuickActionsResult.Success) state.value = emptyList()
            result
        }

    override fun reportUsed(id: String) {
        runCatching { ShortcutManagerCompat.reportShortcutUsed(appContext, id) }
    }

    private fun publish(actions: List<QuickAction>): QuickActionsResult<Unit> {
        val target = target() ?: return QuickActionsResult.Failure(noLauncher())
        if (isRateLimited) return QuickActionsResult.Failure(QuickActionsException.RateLimited())
        val infos = actions.mapIndexed { index, action -> ShortcutMapper.build(appContext, config, action, index, target) }
        val result = platformCall { ShortcutManagerCompat.setDynamicShortcuts(appContext, infos) }
        if (result is QuickActionsResult.Success) state.value = actions
        return result
    }

    /** Runs a `ShortcutManagerCompat` call, mapping `false` and the exceptions it documents to typed failures. */
    private inline fun platformCall(block: () -> Boolean): QuickActionsResult<Unit> =
        try {
            if (block()) QuickActionsResult.Success(Unit) else QuickActionsResult.Failure(QuickActionsException.RateLimited())
        } catch (e: IllegalArgumentException) {
            QuickActionsResult.Failure(QuickActionsException.InvalidAction(e.message ?: "The platform rejected the shortcuts", e))
        } catch (e: IllegalStateException) {
            val message = e.message ?: "The user is locked or the shortcut service is unavailable"
            QuickActionsResult.Failure(QuickActionsException.PlatformError(message, e))
        }

    private fun restore(): List<QuickAction> =
        runCatching { ShortcutManagerCompat.getShortcuts(appContext, ShortcutManagerCompat.FLAG_MATCH_DYNAMIC) }
            .getOrDefault(emptyList())
            .sortedBy { it.rank }
            .mapNotNull(ShortcutMapper::decode)

    private fun target() = ShortcutMapper.targetComponent(appContext, config)

    private fun noLauncher() = QuickActionsException.PlatformError("No launcher Activity found; set AndroidQuickActionsConfig.targetActivity")
}
