package io.github.androidpoet.jolt.internal

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal fun nowEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()
