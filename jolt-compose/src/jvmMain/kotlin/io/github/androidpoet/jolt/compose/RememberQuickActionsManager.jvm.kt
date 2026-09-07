package io.github.androidpoet.jolt.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.androidpoet.jolt.QuickActionsManager
import io.github.androidpoet.jolt.UnsupportedQuickActionsManager

@Composable
public actual fun rememberQuickActionsManager(): QuickActionsManager = remember { UnsupportedQuickActionsManager() }
