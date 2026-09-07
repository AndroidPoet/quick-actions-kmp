package io.github.androidpoet.jolt

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class QuickActionValidationTest {
    private val valid = QuickAction("a", "A")

    @Test
    fun validActionPasses() {
        assertNull(valid.validate())
        assertNull(listOf(valid, QuickAction("b", "B")).validate(max = 4))
    }

    @Test
    fun blankIdOrTitleIsInvalid() {
        assertIs<QuickActionsException.InvalidAction>(QuickAction(" ", "A").validate())
        assertIs<QuickActionsException.InvalidAction>(QuickAction("a", "").validate())
        assertIs<QuickActionsException.InvalidAction>(QuickAction("a", "A", data = mapOf("" to "v")).validate())
    }

    @Test
    fun oversizedPayloadIsTooLarge() {
        val big = QuickAction("a", "A", data = mapOf("blob" to "x".repeat(QUICK_ACTION_MAX_PAYLOAD_BYTES)))
        assertIs<QuickActionsException.PayloadTooLarge>(big.validate())
        assertIs<QuickActionsException.PayloadTooLarge>(listOf(big).validate(max = 4))
    }

    @Test
    fun duplicateIdsAreInvalid() {
        val error = listOf(valid, QuickAction("a", "Other")).validate(max = 4)
        assertIs<QuickActionsException.InvalidAction>(error)
    }

    @Test
    fun overTheMaxIsTooMany() {
        val list = (1..5).map { QuickAction("id$it", "T$it") }
        val error = list.validate(max = 4)
        assertIs<QuickActionsException.TooManyActions>(error)
        assertEquals(5, error.requested)
        assertEquals(4, error.max)
        assertEquals(3003, error.code)
    }

    @Test
    fun perActionProblemsWinOverListProblems() {
        val list = listOf(QuickAction("", "A"), QuickAction("", "B"), QuickAction("c", "C"), QuickAction("d", "D"), QuickAction("e", "E"))
        assertIs<QuickActionsException.InvalidAction>(list.validate(max = 1))
    }

    @Test
    fun emptyListIsValidEvenAtZeroMax() {
        assertNull(emptyList<QuickAction>().validate(max = 0))
    }
}
