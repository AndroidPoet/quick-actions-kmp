package io.github.androidpoet.quickactions.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.androidpoet.quickactions.QuickAction
import io.github.androidpoet.quickactions.QuickActionLaunch
import io.github.androidpoet.quickactions.QuickActionsResult
import io.github.androidpoet.quickactions.compose.OnQuickActionLaunch
import io.github.androidpoet.quickactions.compose.rememberQuickActionsManager
import kotlinx.coroutines.launch

private const val LOG_LINES = 8

/** Every action the sample can publish. Icons are SF Symbol names; Android maps them in MainActivity. */
private val catalog =
    listOf(
        QuickAction("start-timer", "Start timer", subtitle = "25 minutes", icon = "timer", data = mapOf("route" to "timer", "minutes" to "25")),
        QuickAction("log-water", "Log water", subtitle = "One glass", icon = "drop.fill", data = mapOf("route" to "water")),
        QuickAction("new-note", "New note", icon = "square.and.pencil", data = mapOf("route" to "note/new")),
        QuickAction("search", "Search", icon = "magnifyingglass", data = mapOf("route" to "search")),
    )

/** One shared screen: pick actions, publish them, watch launches arrive. */
@Composable
fun App() {
    MaterialTheme {
        val manager = rememberQuickActionsManager()
        val scope = rememberCoroutineScope()
        val published by manager.actions.collectAsState()
        var selected by remember { mutableStateOf(catalog.map { it.id }.toSet()) }
        var log by remember { mutableStateOf(listOf<String>()) }
        var lastLaunch by remember { mutableStateOf<QuickActionLaunch?>(null) }

        fun note(line: String) {
            log = (listOf(line) + log).take(LOG_LINES)
        }

        fun report(
            what: String,
            result: QuickActionsResult<Unit>,
        ) {
            when (result) {
                is QuickActionsResult.Success -> note("$what: ok")
                is QuickActionsResult.Failure -> note("$what: ${result.error.code} ${result.error.message}")
            }
        }

        OnQuickActionLaunch(manager) { launch ->
            lastLaunch = launch
            manager.reportUsed(launch.action.id)
            note("launch ${launch.action.id} createdScreen=${launch.createdScreen} route=${launch.action.data["route"]}")
        }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Quick Actions", style = MaterialTheme.typography.headlineMedium)
            Text("Platform: ${platformName()}", style = MaterialTheme.typography.bodyMedium)
            Text("Supported: ${manager.isSupported} · Max dynamic: ${manager.maxActions}", style = MaterialTheme.typography.bodyMedium)

            LaunchCard(lastLaunch)

            Spacer(Modifier.height(8.dp))
            Text("Choose actions", style = MaterialTheme.typography.titleMedium)
            catalog.forEach { action ->
                val checked = action.id in selected
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .toggleable(value = checked, role = Role.Checkbox) { selected = if (it) selected + action.id else selected - action.id },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = checked, onCheckedChange = null)
                    Text("${action.title}${action.subtitle?.let { " · $it" } ?: ""}", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { scope.launch { report("set", manager.set(catalog.filter { it.id in selected })) } }) { Text("Publish") }
                OutlinedButton(onClick = { scope.launch { report("clear", manager.clear()) } }) { Text("Clear") }
            }

            Spacer(Modifier.height(8.dp))
            Text("Published (${published.size})", style = MaterialTheme.typography.titleMedium)
            if (published.isEmpty()) Text("none", style = MaterialTheme.typography.bodySmall)
            published.forEach { action ->
                Text("• ${action.title} (${action.id})", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(8.dp))
            Text("Log", style = MaterialTheme.typography.titleMedium)
            log.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun LaunchCard(launch: QuickActionLaunch?) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (launch == null) {
                Text("Long-press the app icon and pick an action", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text("Opened via: ${launch.action.title}", style = MaterialTheme.typography.titleMedium)
                Text("id=${launch.action.id} createdScreen=${launch.createdScreen}", style = MaterialTheme.typography.bodySmall)
                Text("data=${launch.action.data}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

expect fun platformName(): String
