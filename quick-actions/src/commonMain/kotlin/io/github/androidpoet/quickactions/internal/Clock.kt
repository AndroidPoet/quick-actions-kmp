package io.github.androidpoet.quickactions.internal

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal fun nowEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()
