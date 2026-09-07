package io.github.androidpoet.quickactions.internal

import android.content.Intent
import io.github.androidpoet.quickactions.QuickAction
import io.github.androidpoet.quickactions.QuickActionCodec

/**
 * Decides whether an incoming intent is a quick-action launch worth dispatching.
 * Pure so the rules are unit-testable without a device.
 */
internal object LaunchIntentPolicy {
    /**
     * @param extraJson the `QuickActions.EXTRA_ACTION` string, or null when absent.
     * @param flags `Intent.flags`.
     * @param restored true when the Activity is being recreated (saved state present).
     * @return the action to dispatch, or null to ignore the intent.
     */
    fun actionFor(
        extraJson: String?,
        flags: Int,
        restored: Boolean,
    ): QuickAction? {
        if (restored) return null
        if (flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) return null
        return QuickActionCodec.decodeOrNull(extraJson)
    }
}
