package io.github.androidpoet.jolt.compose

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import io.github.androidpoet.jolt.Jolt
import io.github.androidpoet.jolt.QuickActionsManager

@Composable
public actual fun rememberQuickActionsManager(): QuickActionsManager {
    val context = LocalContext.current.applicationContext
    val activity = LocalActivity.current as? ComponentActivity
    return remember(context, activity) {
        activity?.let(Jolt::attach)
        Jolt.manager(context)
    }
}
