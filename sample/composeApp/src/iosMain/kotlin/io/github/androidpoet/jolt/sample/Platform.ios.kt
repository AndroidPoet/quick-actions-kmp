package io.github.androidpoet.jolt.sample

import platform.UIKit.UIDevice

actual fun platformName(): String = "${UIDevice.currentDevice.systemName} ${UIDevice.currentDevice.systemVersion}"
