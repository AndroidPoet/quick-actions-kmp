package io.github.androidpoet.quickactions

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QuickActionCodecTest {
    @Test
    fun roundTripsEveryField() {
        val action =
            QuickAction(
                id = "start-timer",
                title = "Start timer",
                subtitle = "25 minutes",
                icon = "timer",
                data = mapOf("route" to "timer/start", "minutes" to "25"),
            )
        assertEquals(action, QuickActionCodec.decode(QuickActionCodec.encode(action)))
    }

    @Test
    fun roundTripsMinimalActionAndUnicode() {
        val minimal = QuickAction(id = "a", title = "Añadir 記録 🚀")
        val decoded = QuickActionCodec.decode(QuickActionCodec.encode(minimal))
        assertEquals(minimal, decoded)
        assertNull(decoded.subtitle)
        assertNull(decoded.icon)
        assertTrue(decoded.data.isEmpty())
    }

    @Test
    fun omitsNullsAndEmptyDefaults() {
        val json = QuickActionCodec.encode(QuickAction(id = "a", title = "A"))
        assertEquals("""{"id":"a","title":"A"}""", json)
    }

    @Test
    fun ignoresUnknownKeys() {
        val decoded = QuickActionCodec.decode("""{"id":"a","title":"A","future":true,"data":{"k":"v"}}""")
        assertEquals(QuickAction("a", "A", data = mapOf("k" to "v")), decoded)
    }

    @Test
    fun malformedInputIsInvalidAction() {
        assertFailsWith<QuickActionsException.InvalidAction> { QuickActionCodec.decode("{broken") }
        assertFailsWith<QuickActionsException.InvalidAction> { QuickActionCodec.decode("""{"title":"no id"}""") }
        assertNull(QuickActionCodec.decodeOrNull("{broken"))
        assertNull(QuickActionCodec.decodeOrNull(null))
    }
}
