package io.github.androidpoet.quickactions

import io.github.androidpoet.quickactions.internal.LaunchDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LaunchDispatcherTest {
    private val action = QuickAction("a", "A")

    @Test
    fun buffersLaunchesUntilCollected() =
        runTest {
            val dispatcher = LaunchDispatcher(clock = { 42 })
            dispatcher.dispatch(action, createdScreen = true)
            dispatcher.dispatch(action.copy(id = "b"), createdScreen = false)

            val received = dispatcher.launches.take(2).toList()
            assertEquals(listOf("a", "b"), received.map { it.action.id })
            assertEquals(listOf(true, false), received.map { it.createdScreen })
            assertTrue(received.all { it.atEpochMillis == 42L })
        }

    @Test
    fun eachLaunchIsDeliveredOnce() =
        runTest {
            val dispatcher = LaunchDispatcher()
            dispatcher.dispatch(action, createdScreen = true)
            assertEquals(
                "a",
                dispatcher.launches
                    .first()
                    .action.id,
            )

            dispatcher.dispatch(action.copy(id = "b"), createdScreen = false)
            assertEquals(
                "b",
                dispatcher.launches
                    .first()
                    .action.id,
            )
        }

    @Test
    fun returnsTheDispatchedLaunch() {
        val dispatcher = LaunchDispatcher(clock = { 7 })
        val launch = dispatcher.dispatch(action, createdScreen = false)
        assertEquals(QuickActionLaunch(action, createdScreen = false, atEpochMillis = 7), launch)
    }
}
