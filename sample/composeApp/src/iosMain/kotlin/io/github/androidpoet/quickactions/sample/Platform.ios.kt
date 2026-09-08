package io.github.androidpoet.quickactions.sample

import io.github.androidpoet.quickactions.QuickActions
import platform.UIKit.UIDevice

actual fun platformName(): String = "${UIDevice.currentDevice.systemName} ${UIDevice.currentDevice.systemVersion}"

actual fun deliveryInstalled(): Boolean = QuickActions.isDeliveryInstalled
