package io.github.androidpoet.jolt.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.androidpoet.jolt.Jolt
import io.github.androidpoet.jolt.QuickActionsManager

@Composable
public actual fun rememberQuickActionsManager(): QuickActionsManager = remember { Jolt.manager }
