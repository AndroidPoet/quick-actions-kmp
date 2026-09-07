package io.github.androidpoet.jolt

import android.content.Intent
import io.github.androidpoet.jolt.internal.LaunchIntentPolicy
import io.github.androidpoet.jolt.internal.ShortcutMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LaunchIntentPolicyTest {
    private val action = QuickAction("start", "Start", data = mapOf("route" to "timer"))
    private val json = QuickActionCodec.encode(action)

    @Test
    fun validExtraOnFreshActivityDispatches() {
        assertEquals(action, LaunchIntentPolicy.actionFor(json, flags = Intent.FLAG_ACTIVITY_NEW_TASK, restored = false))
    }

    @Test
    fun missingOrMalformedExtraIsIgnored() {
        assertNull(LaunchIntentPolicy.actionFor(null, flags = 0, restored = false))
        assertNull(LaunchIntentPolicy.actionFor("{broken", flags = 0, restored = false))
    }

    @Test
    fun recreatedActivityDoesNotRedispatch() {
        assertNull(LaunchIntentPolicy.actionFor(json, flags = 0, restored = true))
    }

    @Test
    fun relaunchFromRecentsDoesNotRedispatch() {
        assertNull(LaunchIntentPolicy.actionFor(json, flags = Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY, restored = false))
        assertNull(
            LaunchIntentPolicy.actionFor(
                json,
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY,
                restored = false,
            ),
        )
    }

    @Test
    fun longLabelKeepsTheTitle() {
        assertEquals("Start timer · 25 minutes", ShortcutMapper.longLabel(QuickAction("a", "Start timer", subtitle = "25 minutes")))
        assertEquals("Search", ShortcutMapper.longLabel(QuickAction("b", "Search")))
    }

    @Test
    fun iconResolutionPrecedence() {
        val resolver = AndroidQuickActionsConfig(defaultIconRes = 7, iconResolver = { key -> if (key == "timer") 11 else 0 })
        assertEquals(11, ShortcutMapper.resolveIconRes(resolver, iconKey = "timer", appIconRes = 3))
        assertEquals(7, ShortcutMapper.resolveIconRes(resolver, iconKey = "unknown", appIconRes = 3))
        assertEquals(7, ShortcutMapper.resolveIconRes(resolver, iconKey = null, appIconRes = 3))
        assertEquals(3, ShortcutMapper.resolveIconRes(AndroidQuickActionsConfig(), iconKey = "timer", appIconRes = 3))
    }
}
