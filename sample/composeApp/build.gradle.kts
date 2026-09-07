import io.github.androidpoet.jolt.Configuration

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(17)

    androidTarget()

    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            // Exposes Jolt to Swift under its own name for the delegate glue.
            export(project(":jolt"))
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":jolt"))
            implementation(project(":jolt-compose"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.kotlinx.coroutines.android)
        }
    }
}

android {
    namespace = "io.github.androidpoet.jolt.sample"
    compileSdk = Configuration.COMPILE_SDK

    defaultConfig {
        applicationId = "io.github.androidpoet.jolt.sample"
        minSdk = Configuration.MIN_SDK
        targetSdk = Configuration.COMPILE_SDK
        versionCode = 1
        versionName = "0.1.0"
    }
}
