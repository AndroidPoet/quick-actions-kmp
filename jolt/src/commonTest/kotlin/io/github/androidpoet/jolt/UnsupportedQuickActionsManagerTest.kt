package io.github.androidpoet.jolt

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class UnsupportedQuickActionsManagerTest {
    private val manager = UnsupportedQuickActionsManager()

    @Test
    fun reportsNoSupport() {
        assertFalse(manager.isSupported)
        assertEquals(0, manager.maxActions)
        assertTrue(manager.actions.value.isEmpty())
    }

    @Test
    fun everyMutationFailsUnsupported() =
        runTest {
            val action = QuickAction("a", "A")
            listOf(
                manager.set(listOf(action)),
                manager.add(action),
                manager.remove("a"),
                manager.clear(),
            ).forEach { result ->
                val error = assertIs<QuickActionsResult.Failure>(result).error
                assertIs<QuickActionsException.Unsupported>(error)
                assertEquals(3001, error.code)
            }
        }

    @Test
    fun launchesNeverEmitAndNoOpsDoNotThrow() =
        runTest {
            assertTrue(manager.launches.toList().isEmpty())
            manager.reportUsed("a")
        }
}
