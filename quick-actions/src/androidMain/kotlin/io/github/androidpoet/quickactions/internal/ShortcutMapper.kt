package io.github.androidpoet.quickactions.internal

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.graphics.drawable.IconCompat
import io.github.androidpoet.quickactions.AndroidQuickActionsConfig
import io.github.androidpoet.quickactions.QuickAction
import io.github.androidpoet.quickactions.QuickActionCodec
import io.github.androidpoet.quickactions.QuickActions

/** Builds `ShortcutInfoCompat` from a [QuickAction] and reads it back. */
internal object ShortcutMapper {
    fun build(
        context: Context,
        config: AndroidQuickActionsConfig,
        action: QuickAction,
        rank: Int,
        target: ComponentName,
    ): ShortcutInfoCompat {
        val builder =
            ShortcutInfoCompat
                .Builder(context, action.id)
                .setShortLabel(action.title)
                .setLongLabel(longLabel(action))
                .setIntent(intentFor(context, config, action, target))
                .setRank(rank)
        val iconRes = resolveIconRes(config, action.icon, context.applicationInfo.icon)
        if (iconRes != 0) builder.setIcon(IconCompat.createWithResource(context, iconRes))
        return builder.build()
    }

    fun intentFor(
        context: Context,
        config: AndroidQuickActionsConfig,
        action: QuickAction,
        target: ComponentName,
    ): Intent {
        val intent = config.intentBuilder?.invoke(context, action) ?: Intent().setComponent(target)
        return intent
            .setAction(QuickActions.ACTION)
            .addFlags(config.intentFlags)
            .putExtra(QuickActions.EXTRA_ACTION, QuickActionCodec.encode(action))
    }

    /**
     * Launchers show the long label instead of the short one when it fits, so the
     * subtitle is appended to the title rather than replacing it.
     */
    fun longLabel(action: QuickAction): String = action.subtitle?.let { "${action.title} · $it" } ?: action.title

    /** Resolver result first, then [AndroidQuickActionsConfig.defaultIconRes], then the app icon. */
    fun resolveIconRes(
        config: AndroidQuickActionsConfig,
        iconKey: String?,
        appIconRes: Int,
    ): Int {
        val resolved = iconKey?.let { key -> config.iconResolver?.invoke(key) } ?: 0
        if (resolved != 0) return resolved
        return if (config.defaultIconRes != 0) config.defaultIconRes else appIconRes
    }

    /** The action a published shortcut carries, or null for shortcuts this library did not publish. */
    fun decode(info: ShortcutInfoCompat): QuickAction? = QuickActionCodec.decodeOrNull(info.intent.getStringExtra(QuickActions.EXTRA_ACTION))

    fun targetComponent(
        context: Context,
        config: AndroidQuickActionsConfig,
    ): ComponentName? = config.targetActivity ?: context.packageManager.getLaunchIntentForPackage(context.packageName)?.component
}
