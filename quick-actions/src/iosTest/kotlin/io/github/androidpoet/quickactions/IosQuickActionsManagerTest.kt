package io.github.androidpoet.quickactions

import io.github.androidpoet.quickactions.internal.LaunchDispatcher
import io.github.androidpoet.quickactions.internal.ShortcutItemsStore
import io.github.androidpoet.quickactions.internal.ShortcutRecord
import io.github.androidpoet.quickactions.internal.toItem
import io.github.androidpoet.quickactions.internal.toRecord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSDictionary
import platform.Foundation.NSString
import platform.Foundation.create
import platform.Foundation.dictionaryWithObjects
import platform.UIKit.UIApplicationShortcutItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private class FakeStore(
    initial: List<ShortcutRecord> = emptyList(),
    override val staticItemCount: Int = 0,
    var failWrites: Boolean = false,
) : ShortcutItemsStore {
    var records = initial
    var writes = 0
    var reads = 0

    override fun read(): List<ShortcutRecord> {
        reads++
        return records
    }

    override suspend fun write(records: List<ShortcutRecord>) {
        if (failWrites) error("UIKit said no")
        writes++
        this.records = records
    }
}

class IosQuickActionsManagerTest {
    private val a = QuickAction("a", "A", subtitle = "sub", icon = "timer", data = mapOf("k" to "v"))
    private val b = QuickAction("b", "B")

    private fun manager(
        store: FakeStore,
        dispatcher: LaunchDispatcher = LaunchDispatcher(),
    ) = IosQuickActionsManager(store, dispatcher, seedNow = true)

    @Test
    fun setWritesRecordsWithPayloadInOrder() =
        runTest {
            val store = FakeStore()
            val manager = manager(store)
            assertIs<QuickActionsResult.Success<Unit>>(manager.set(listOf(a, b)))
            assertEquals(listOf("a", "b"), store.records.map { it.type })
            assertEquals("A", store.records[0].title)
            assertEquals("sub", store.records[0].subtitle)
            assertEquals("timer", store.records[0].iconSystemName)
            assertEquals(a, QuickActionCodec.decode(store.records[0].payload!!))
            assertEquals(listOf(a, b), manager.actions.value)
        }

    @Test
    fun restoreDecodesPublishedItemsAndSkipsForeignOnes() {
        val store = FakeStore(listOf(a.toRecord(), ShortcutRecord("static", "Static", null, null, payload = null)))
        val manager = manager(store)
        assertEquals(listOf(a), manager.actions.value)
    }

    @Test
    fun lazySeedReadsOnFirstUseOnly() {
        val store = FakeStore(listOf(a.toRecord()))
        val manager = IosQuickActionsManager(store, LaunchDispatcher(), seedNow = false)
        assertEquals(0, store.reads)
        assertEquals(listOf(a), manager.actions.value)
        manager.actions
        assertEquals(1, store.reads)
    }

    @Test
    fun maxActionsRespectsStaticCount() =
        runTest {
            val manager = manager(FakeStore(staticItemCount = 3))
            assertEquals(1, manager.maxActions)
            val error = manager.set(listOf(a, b)).errorOrNull()
            assertIs<QuickActionsException.TooManyActions>(error)
            assertEquals(1, error.max)
            assertEquals(0, manager(FakeStore(staticItemCount = 9)).maxActions)
        }

    @Test
    fun equalListIsANoOpWrite() =
        runTest {
            val store = FakeStore(listOf(a.toRecord()))
            val manager = manager(store)
            assertIs<QuickActionsResult.Success<Unit>>(manager.set(listOf(a)))
            assertEquals(0, store.writes)
        }

    @Test
    fun addReplacesInPlaceAndAppends() =
        runTest {
            val store = FakeStore()
            val manager = manager(store)
            manager.set(listOf(a, b))
            manager.add(a.copy(title = "A2"))
            assertEquals(listOf("A2", "B"), manager.actions.value.map { it.title })
            manager.add(QuickAction("c", "C"))
            assertEquals(listOf("a", "b", "c"), manager.actions.value.map { it.id })
            assertIs<QuickActionsResult.Success<Unit>>(manager.add(QuickAction("d", "D")))
            assertIs<QuickActionsException.TooManyActions>(manager.add(QuickAction("e", "E")).errorOrNull())
            assertEquals(4, manager.actions.value.size)
        }

    @Test
    fun removeAndClear() =
        runTest {
            val store = FakeStore()
            val manager = manager(store)
            manager.set(listOf(a, b))
            manager.remove("a", "nope")
            assertEquals(listOf(b), manager.actions.value)
            val writesBefore = store.writes
            manager.remove("nope")
            assertEquals(writesBefore, store.writes)
            manager.clear()
            assertTrue(manager.actions.value.isEmpty())
            assertTrue(store.records.isEmpty())
        }

    @Test
    fun writeFailureIsPlatformErrorAndKeepsState() =
        runTest {
            val store = FakeStore(listOf(a.toRecord()), failWrites = true)
            val manager = manager(store)
            val error = manager.set(listOf(b)).errorOrNull()
            assertIs<QuickActionsException.PlatformError>(error)
            assertEquals(3006, error.code)
            assertEquals(listOf(a), manager.actions.value)
        }

    @Test
    fun handleDecodesPublishedItemAndSynthesisesStaticOne() =
        runTest {
            val published = a.toRecord().toItem()
            assertEquals(QuickActionCodec.encode(a), published.userInfo?.get(QUICK_ACTIONS_USER_INFO_KEY))
            val launch = QuickActions.handle(published, createdScreen = true)
            assertEquals(a, launch.action)
            assertTrue(launch.createdScreen)

            @Suppress("UNCHECKED_CAST")
            val info = NSDictionary.dictionaryWithObjects(listOf("v", 1), forKeys = listOf(NSString.create(string = "k"), NSString.create(string = "n"))) as Map<Any?, *>
            val static = UIApplicationShortcutItem(type = "static", localizedTitle = "Static", localizedSubtitle = "S", icon = null, userInfo = info)
            val synthesised = QuickActions.handle(static, createdScreen = false).action
            assertEquals(QuickAction("static", "Static", subtitle = "S", data = mapOf("k" to "v")), synthesised)

            val received = QuickActions.manager.launches.first()
            assertEquals("a", received.action.id)
        }
}
