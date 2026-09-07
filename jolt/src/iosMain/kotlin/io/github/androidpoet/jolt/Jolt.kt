package io.github.androidpoet.jolt

import io.github.androidpoet.jolt.internal.LaunchDispatcher
import platform.UIKit.UIApplicationShortcutItem

/** Key under which the encoded [QuickAction] travels in `UIApplicationShortcutItem.userInfo`. */
public const val JOLT_USER_INFO_KEY: String = "jolt"

/**
 * iOS entry point for launches. The Swift glue (`swift/JoltDelegates.swift`)
 * calls [installDelivery] at start-up and forwards every tapped item to [handle];
 * every [IosQuickActionsManager] in the process then sees it on `launches`.
 */
public object Jolt {
    internal val dispatcher = LaunchDispatcher()

    /** True once the Swift glue has called [installDelivery] or [handle]. */
    public var isDeliveryInstalled: Boolean = false
        private set

    /** The process-wide manager `rememberQuickActionsManager()` hands out. */
    public val manager: IosQuickActionsManager by lazy { IosQuickActionsManager() }

    /** Marks delivery as wired. Call from the delegate glue's initializer. */
    public fun installDelivery() {
        isDeliveryInstalled = true
    }

    /**
     * Dispatches a launch for [item]. Items published by this library decode back
     * to the original [QuickAction]; a static item from `Info.plist` becomes an
     * action with `id = type`, the localized labels and its string `userInfo`.
     *
     * @param createdScreen `true` from `scene(_:willConnectTo:)` or
     *   `didFinishLaunchingWithOptions` (the tap created the scene), `false` from
     *   `performActionFor`.
     * @return the dispatched launch.
     */
    public fun handle(
        item: UIApplicationShortcutItem,
        createdScreen: Boolean,
    ): QuickActionLaunch {
        isDeliveryInstalled = true
        return dispatcher.dispatch(item.toQuickAction(), createdScreen)
    }
}

internal fun UIApplicationShortcutItem.toQuickAction(): QuickAction {
    val info = userInfo.orEmpty()
    QuickActionCodec.decodeOrNull(info[JOLT_USER_INFO_KEY] as? String)?.let { return it }
    return QuickAction(
        id = type,
        title = localizedTitle,
        subtitle = localizedSubtitle,
        data = info.entries.mapNotNull { (key, value) -> if (key is String && value is String) key to value else null }.toMap(),
    )
}
