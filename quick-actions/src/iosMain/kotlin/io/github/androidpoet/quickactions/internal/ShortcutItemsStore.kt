package io.github.androidpoet.quickactions.internal

import io.github.androidpoet.quickactions.QUICK_ACTIONS_USER_INFO_KEY
import io.github.androidpoet.quickactions.QuickAction
import io.github.androidpoet.quickactions.QuickActionCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSBundle
import platform.Foundation.NSDictionary
import platform.Foundation.NSString
import platform.Foundation.NSThread
import platform.Foundation.create
import platform.Foundation.dictionaryWithObject
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationShortcutIcon
import platform.UIKit.UIApplicationShortcutItem
import platform.UIKit.shortcutItems
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_sync

/** One published item as the platform holds it; `payload` is the encoded action, absent for foreign items. */
internal data class ShortcutRecord(
    val type: String,
    val title: String,
    val subtitle: String?,
    val iconSystemName: String?,
    val payload: String?,
)

/** The platform's dynamic shortcut list, abstracted so the manager can be tested without UIKit. */
internal interface ShortcutItemsStore {
    val staticItemCount: Int

    fun read(): List<ShortcutRecord>

    suspend fun write(records: List<ShortcutRecord>)
}

internal fun QuickAction.toRecord(): ShortcutRecord = ShortcutRecord(id, title, subtitle, icon, QuickActionCodec.encode(this))

internal fun ShortcutRecord.toQuickActionOrNull(): QuickAction? = QuickActionCodec.decodeOrNull(payload)

/** `UIApplication.shared.shortcutItems`, always touched on the main thread. */
internal class UIKitShortcutItemsStore : ShortcutItemsStore {
    override val staticItemCount: Int
        get() = (NSBundle.mainBundle.objectForInfoDictionaryKey(STATIC_ITEMS_KEY) as? List<*>)?.size ?: 0

    override fun read(): List<ShortcutRecord> =
        onMain {
            UIApplication.sharedApplication.shortcutItems
                .orEmpty()
                .mapNotNull { (it as? UIApplicationShortcutItem)?.toRecord() }
        }

    override suspend fun write(records: List<ShortcutRecord>) {
        withContext(Dispatchers.Main) {
            UIApplication.sharedApplication.shortcutItems = records.map { it.toItem() }
        }
    }

    private fun <T> onMain(block: () -> T): T {
        if (NSThread.isMainThread) return block()
        var result: T? = null
        dispatch_sync(dispatch_get_main_queue()) { result = block() }
        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    private companion object {
        const val STATIC_ITEMS_KEY = "UIApplicationShortcutItems"
    }
}

/**
 * UIKit copies `userInfo` and validates it against `NSSecureCoding`; a Kotlin map
 * crossing the bridge fails that check and is dropped silently, so the payload
 * travels in a real `NSDictionary`.
 */
internal fun ShortcutRecord.toItem(): UIApplicationShortcutItem =
    UIApplicationShortcutItem(
        type = type,
        localizedTitle = title,
        localizedSubtitle = subtitle,
        icon = iconSystemName?.let { UIApplicationShortcutIcon.iconWithSystemImageName(it) },
        userInfo = payload?.let { userInfoDictionary(it) },
    )

@Suppress("UNCHECKED_CAST")
private fun userInfoDictionary(payload: String): Map<Any?, *> =
    NSDictionary.dictionaryWithObject(payload, forKey = NSString.create(string = QUICK_ACTIONS_USER_INFO_KEY)) as Map<Any?, *>

internal fun UIApplicationShortcutItem.toRecord(): ShortcutRecord =
    ShortcutRecord(
        type = type,
        title = localizedTitle,
        subtitle = localizedSubtitle,
        iconSystemName = null,
        payload = userInfo?.get(QUICK_ACTIONS_USER_INFO_KEY) as? String,
    )
