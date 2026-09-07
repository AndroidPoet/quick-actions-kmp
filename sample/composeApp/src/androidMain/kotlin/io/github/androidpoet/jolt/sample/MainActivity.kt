package io.github.androidpoet.jolt.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.github.androidpoet.jolt.AndroidQuickActionsConfig
import io.github.androidpoet.jolt.Jolt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Jolt.androidConfig =
            AndroidQuickActionsConfig(
                defaultIconRes = R.drawable.ic_bolt,
                iconResolver = { key ->
                    when (key) {
                        "timer" -> R.drawable.ic_timer
                        "drop.fill" -> R.drawable.ic_drop
                        "square.and.pencil" -> R.drawable.ic_note
                        "magnifyingglass" -> R.drawable.ic_search
                        else -> 0
                    }
                },
            )
        setContent { App() }
    }
}
