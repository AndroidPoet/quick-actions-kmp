package io.github.androidpoet.quickactions.sample

import android.os.Build
import io.github.androidpoet.quickactions.QuickActions

actual fun platformName(): String = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

actual fun deliveryInstalled(): Boolean = QuickActions.isDeliveryInstalled
