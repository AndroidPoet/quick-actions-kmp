@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.github.androidpoet.quickactions

import io.github.androidpoet.quickactions.internal.LaunchDispatcher
import io.github.androidpoet.quickactions.internal.hook.qa_hook_is_armed
import io.github.androidpoet.quickactions.internal.hook.qa_hook_set_handler
import platform.UIKit.UIApplicationShortcutItem

/** Key under which the encoded [QuickAction] travels in `UIApplicationShortcutItem.userInfo`. */
public const val QUICK_ACTIONS_USER_INFO_KEY: String = "quickActions"

/**
 * iOS entry point for launches. Nothing to wire: the library hooks the app and
 * scene delegates when the binary loads, so `connectionOptions.shortcutItem`,
 * `performActionFor` and the launch options all reach `launches` on every
 * [IosQuickActionsManager]. A tap that arrives before the first manager exists
 * is held and delivered when one is created.
 */
public object QuickActions {
    internal val dispatcher = LaunchDispatcher()
    private var handlerInstalled = false

    /** True once the load-time delivery hooks are in place, which is always in a normal app process. */
    public val isDeliveryInstalled: Boolean
        get() = qa_hook_is_armed()

    /** The process-wide manager `rememberQuickActionsManager()` hands out. */
    public val manager: IosQuickActionsManager by lazy { IosQuickActionsManager() }

    internal fun installDelivery() {
        if (handlerInstalled) return
        handlerInstalled = true
        qa_hook_set_handler { item, createdScreen -> if (item != null) handle(item, createdScreen) }
    }

    /**
     * Dispatches a launch for [item]. Items published by this library decode back
     * to the original [QuickAction]; a static item from `Info.plist` becomes an
     * action with `id = type`, the localized labels and its string `userInfo`.
     */
    internal fun handle(
        item: UIApplicationShortcutItem,
        createdScreen: Boolean,
    ): QuickActionLaunch = dispatcher.dispatch(item.toQuickAction(), createdScreen)
}

internal fun UIApplicationShortcutItem.toQuickAction(): QuickAction {
    val info = userInfo.orEmpty()
    QuickActionCodec.decodeOrNull(info[QUICK_ACTIONS_USER_INFO_KEY] as? String)?.let { return it }
    return QuickAction(
        id = type,
        title = localizedTitle,
        subtitle = localizedSubtitle,
        data = info.entries.mapNotNull { (key, value) -> if (key is String && value is String) key to value else null }.toMap(),
    )
}
