plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.vanniktech.publish) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.dokka)
    alias(libs.plugins.kover)
    alias(libs.plugins.binary.compatibility.validator)
}

// Published modules whose public API and coverage we track.
val publishedModules = listOf("quick-actions", "quick-actions-compose")

// Validate the binary (ABI) compatibility of every published module so an
// accidental public-API break is caught in review instead of by consumers.
apiValidation {
    ignoredProjects.addAll(
        subprojects.map { it.name }.filterNot { it in publishedModules },
    )
    nonPublicMarkers.add("kotlin.PublishedApi")

    // Also track the native/JS klib ABI, not just JVM + Android.
    @OptIn(kotlinx.validation.ExperimentalBCVApi::class)
    klib {
        enabled = true
    }
}

// ktlint engine version shared by every Spotless format below.
val ktlintVersion = libs.versions.ktlint.get()

// ktlint reads these from .editorconfig in the IDE; pass them to Spotless's
// bundled engine too so the Gradle check matches editor behaviour exactly.
//   - line length is owned by detekt (MaxLineLength); ktlint's own reporting
//     rule is off to avoid double-reporting.
//   - the function-signature rule is off because it collapses author-wrapped
//     expression bodies onto a single line that then exceeds detekt's limit.
val ktlintOverrides =
    mapOf(
        "ktlint_standard_max-line-length" to "disabled",
        "ktlint_standard_function-signature" to "disabled",
        "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
    )

// Format the root build script and shared config files too, not just modules.
apply(plugin = "com.diffplug.spotless")
extensions.configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    kotlinGradle {
        target("*.gradle.kts")
        ktlint(ktlintVersion)
            .editorConfigOverride(ktlintOverrides)
    }
    format("misc") {
        target("*.md", ".gitignore", "config/**/*.yml")
        trimTrailingWhitespace()
        leadingTabsToSpaces()
        endWithNewline()
    }
}

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")

    // Auto-format + lint Kotlin via ktlint (ktlint_official style, see .editorconfig).
    apply(plugin = "com.diffplug.spotless")
    extensions.configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        kotlin {
            target("src/**/*.kt")
            targetExclude("**/build/**")
            ktlint(ktlintVersion)
                .editorConfigOverride(ktlintOverrides)
            trimTrailingWhitespace()
            endWithNewline()
        }
        kotlinGradle {
            target("*.gradle.kts")
            ktlint(ktlintVersion)
                .editorConfigOverride(ktlintOverrides)
        }
    }

    extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        parallel = true
        buildUponDefaultConfig = true
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        baseline = file("detekt-baseline.xml")
        // Analyze every Kotlin source set, not just JVM main.
        source.setFrom(
            files(
                "src/commonMain/kotlin",
                "src/commonTest/kotlin",
                "src/jvmMain/kotlin",
                "src/androidMain/kotlin",
                "src/appleMain/kotlin",
                "src/iosMain/kotlin",
                "src/macosMain/kotlin",
                "src/wasmJsMain/kotlin",
            ).filter { it.exists() },
        )
    }

    // Generate API reference docs from KDoc for every published module.
    if (name in publishedModules) {
        apply(plugin = "org.jetbrains.dokka")
    }
}

// Aggregate coverage across all published modules.
dependencies {
    publishedModules.forEach { kover(project(":$it")) }
}

// Coverage gate: fail the build if aggregate line coverage regresses below the
// floor. Native/desktop ceremony code can't be unit-tested without a device, so
// the floor tracks the common + Android logic that can. Run `./gradlew koverVerify`.
kover {
    reports {
        verify {
            rule("Aggregate line coverage") {
                minBound(30)
            }
        }
    }
}
