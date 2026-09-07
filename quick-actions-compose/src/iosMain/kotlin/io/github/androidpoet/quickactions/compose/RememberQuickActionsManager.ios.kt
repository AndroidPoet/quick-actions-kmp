package io.github.androidpoet.quickactions.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.androidpoet.quickactions.QuickActions
import io.github.androidpoet.quickactions.QuickActionsManager

@Composable
public actual fun rememberQuickActionsManager(): QuickActionsManager = remember { QuickActions.manager }
