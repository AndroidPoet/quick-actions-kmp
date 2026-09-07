package io.github.androidpoet.jolt

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * One common API to publish home-screen quick actions, the menu shown when the
 * user long-presses the app icon, and to receive the launch when one is tapped.
 * Backed by `UIApplicationShortcutItem` on iOS and `ShortcutManagerCompat`
 * dynamic shortcuts on Android.
 *
 * The library never invents UI or routing: titles arrive already localized and
 * the app decides what a [QuickActionLaunch] means. Each action travels inside
 * the platform payload, so a tap decodes back to the same [QuickAction] that was
 * published.
 *
 * Obtain an instance with `rememberQuickActionsManager()` from the Compose
 * module, or construct the platform class (`AndroidQuickActionsManager`,
 * `IosQuickActionsManager`) directly.
 */
public interface QuickActionsManager {
    /** Whether this platform can show quick actions at all. */
    public val isSupported: Boolean

    /**
     * How many dynamic actions the platform accepts right now: the platform
     * ceiling minus the static items declared in the app manifest or Info.plist.
     * Both home screens display about [QUICK_ACTIONS_RECOMMENDED_MAX] in total.
     */
    public val maxActions: Int

    /**
     * Dynamic actions this app has published, in display order. Restored from
     * the platform when the manager is created, so the list survives process death.
     */
    public val actions: StateFlow<List<QuickAction>>

    /**
     * Taps on any action, dynamic or static. Launches that happen before a
     * collector exists (the tap started the app) are buffered, not dropped. Each
     * launch is delivered to exactly one collector, so collect from one place,
     * typically the root of the UI. Delivery needs the one-time platform wiring
     * described in the README; `Jolt.isDeliveryInstalled` tells you it is in place.
     */
    public val launches: Flow<QuickActionLaunch>

    /** Replaces every dynamic action. List order is display order. A list equal to [actions] is a no-op. */
    public suspend fun set(actions: List<QuickAction>): QuickActionsResult<Unit>

    /** Adds [action], or replaces the published action with the same id in place. */
    public suspend fun add(action: QuickAction): QuickActionsResult<Unit>

    /** Removes the actions with these ids. Unknown ids are ignored. */
    public suspend fun remove(vararg ids: String): QuickActionsResult<Unit>

    /** Removes every dynamic action. Static items are untouched. */
    public suspend fun clear(): QuickActionsResult<Unit>

    /** Tells the launcher the action was used so it can rank it. Android only; a no-op elsewhere. */
    public fun reportUsed(id: String)
}
