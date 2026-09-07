package io.github.androidpoet.quickactions.sample

import android.os.Build

actual fun platformName(): String = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
