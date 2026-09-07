package io.github.androidpoet.quickactions.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.androidpoet.quickactions.QuickActionsManager
import io.github.androidpoet.quickactions.UnsupportedQuickActionsManager

@Composable
public actual fun rememberQuickActionsManager(): QuickActionsManager = remember { UnsupportedQuickActionsManager() }
