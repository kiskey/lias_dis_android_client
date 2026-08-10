#!/usr/bin/env bash
set -euo pipefail

# =====================================================================
# Batch: 001
# Name: Cupertino toolchain + maintained fork baseline
# Run from: repository root
#
# Scope:
#   - Toolchain/dependency baseline only.
#   - No LIAS API behavior changes.
#   - No repository ownership changes.
#   - No navigation, settings, REST, SSE, PDID, policy, or persistence changes.
# =====================================================================

if [[ ! -f "settings.gradle.kts" || ! -d "app/src/main" ]]; then
  echo "ERROR: run this script from the repository root." >&2
  exit 1
fi

mkdir -p gradle/wrapper

cat > gradle/libs.versions.toml <<'CATALOG_EOF'
# ====================================================================
# File: gradle/libs.versions.toml
# Version: 30.0.0
#
# Purpose:
#   Central version catalog for LIAS Remote.
#
# Compose Cupertino migration Plan 3.0 / Batch 1:
#   - Moves the build baseline from Kotlin 2.0.20 to Kotlin 2.2.0.
#   - Moves AGP from 8.9.3 to 8.10.1.
#   - Keeps Gradle 8.11.1, Java/JVM 17, minSdk 26, compileSdk 36,
#     and targetSdk 35 unchanged.
#   - Aligns Compose BOM to a Compose 1.8.x compatible set.
#   - Aligns kotlinx.serialization JSON to 1.7.3.
#   - Replaces original Compose Cupertino coordinates with the
#     maintained fork pinned to io.github.schott12521:*:2.3.1.
#
# Contract:
#   This file changes build/dependency inputs only. It does not change
#   LIAS REST routes, JSON contracts, SSE behavior, repository
#   ownership, navigation routes, persisted settings, or public Kotlin
#   application contracts.
# ====================================================================

[versions]

agp = "8.10.1"
kotlin = "2.2.0"

coreKtx = "1.13.1"
lifecycle = "2.8.3"
activityCompose = "1.9.0"
biometric = "1.1.0"

composeBom = "2025.06.00"
navigationCompose = "2.7.7"

okhttp = "4.12.0"

kotlinxSerialization = "1.7.3"
kotlinxCoroutines = "1.8.1"

datastore = "1.1.1"

junit = "4.13.2"

mockWebServer = "4.12.0"
robolectric = "4.16.1"
archCoreTesting = "2.2.0"

cupertino = "2.3.1"


[libraries]

# --------------------------------------------------------------------
# AndroidX core / lifecycle
# --------------------------------------------------------------------

androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }


# --------------------------------------------------------------------
# Compose / Activity
# --------------------------------------------------------------------

androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-biometric = { group = "androidx.biometric", name = "biometric", version.ref = "biometric" }

androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }

androidx-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }


# --------------------------------------------------------------------
# Navigation
# --------------------------------------------------------------------

androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }


# --------------------------------------------------------------------
# Networking
# --------------------------------------------------------------------

okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-sse = { group = "com.squareup.okhttp3", name = "okhttp-sse", version.ref = "okhttp" }


# --------------------------------------------------------------------
# Kotlin
# --------------------------------------------------------------------

kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "kotlinxCoroutines" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "kotlinxCoroutines" }


# --------------------------------------------------------------------
# Persistence
# --------------------------------------------------------------------

androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }


# --------------------------------------------------------------------
# Unit / contract testing
# --------------------------------------------------------------------

junit = { group = "junit", name = "junit", version.ref = "junit" }
mockwebserver = { group = "com.squareup.okhttp3", name = "mockwebserver", version.ref = "mockWebServer" }
robolectric = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }
androidx-arch-core-testing = { group = "androidx.arch.core", name = "core-testing", version.ref = "archCoreTesting" }


# --------------------------------------------------------------------
# Compose Cupertino maintained fork
# --------------------------------------------------------------------

cupertino = { group = "io.github.schott12521", name = "cupertino", version.ref = "cupertino" }
cupertino-icons-extended = { group = "io.github.schott12521", name = "cupertino-icons-extended", version.ref = "cupertino" }


[plugins]

android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
CATALOG_EOF

cat > app/build.gradle.kts <<'APP_BUILD_EOF'
// ====================================================================
// File: app/build.gradle.kts
// Version: 30.0.0
//
// Purpose:
//   Final application build configuration.
//
// Compose Cupertino migration Plan 3.0 / Batch 1:
//   - Consumes the upgraded version catalog.
//   - Keeps namespace, app id, minSdk, compileSdk, targetSdk,
//     Java/JVM 17, build features, release shrinking, resource
//     packaging, and unit-test Android resources unchanged.
//   - Does not introduce new UI behavior, new endpoints, new
//     repositories, new navigation routes, or new persisted settings.
// ====================================================================

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
}

android {

    namespace =
        "com.lias.remote"

    compileSdk =
        36

    defaultConfig {

        applicationId =
            "com.lias.remote"

        minSdk =
            26

        targetSdk =
            35

        versionCode =
            6

        versionName =
            "2.0.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary =
                true
        }
    }

    buildTypes {

        release {

            isMinifyEnabled =
                true

            isShrinkResources =
                true

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }

        debug {

            isMinifyEnabled =
                false
        }
    }

    compileOptions {

        sourceCompatibility =
            JavaVersion.VERSION_17

        targetCompatibility =
            JavaVersion.VERSION_17
    }

    kotlinOptions {

        jvmTarget =
            "17"
    }

    buildFeatures {

        compose =
            true

        buildConfig =
            true
    }

    packaging {

        resources {

            excludes +=
                "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    /*
     * NavigationRoutesTest and other Android-framework unit tests use
     * Robolectric.
     */
    testOptions {

        unitTests {

            isIncludeAndroidResources =
                true
        }
    }
}

dependencies {

    // ----------------------------------------------------------------
    // Android / lifecycle
    // ----------------------------------------------------------------

    implementation(
        libs.androidx.core.ktx
    )

    implementation(
        libs.androidx.lifecycle.runtime.ktx
    )

    implementation(
        libs.androidx.lifecycle.viewmodel.compose
    )

    implementation(
        libs.androidx.lifecycle.runtime.compose
    )

    implementation(
        libs.androidx.activity.compose
    )

    implementation(
        libs.androidx.biometric
    )


    // ----------------------------------------------------------------
    // Compose
    // ----------------------------------------------------------------

    implementation(
        platform(
            libs.androidx.compose.bom
        )
    )

    implementation(
        libs.androidx.ui
    )

    implementation(
        libs.androidx.ui.graphics
    )

    implementation(
        libs.androidx.ui.tooling.preview
    )

    implementation(
        "androidx.compose.foundation:foundation"
    )

    implementation(
        "androidx.compose.foundation:foundation-layout"
    )


    // ----------------------------------------------------------------
    // Navigation
    // ----------------------------------------------------------------

    implementation(
        libs.androidx.navigation.compose
    )


    // ----------------------------------------------------------------
    // Networking
    // ----------------------------------------------------------------

    implementation(
        libs.okhttp
    )

    implementation(
        libs.okhttp.sse
    )


    // ----------------------------------------------------------------
    // Kotlin
    // ----------------------------------------------------------------

    implementation(
        libs.kotlinx.serialization.json
    )

    implementation(
        libs.kotlinx.coroutines.android
    )


    // ----------------------------------------------------------------
    // Persistence
    // ----------------------------------------------------------------

    implementation(
        libs.androidx.datastore.preferences
    )


    // ----------------------------------------------------------------
    // Cupertino maintained fork
    // ----------------------------------------------------------------

    implementation(
        libs.cupertino
    )

    implementation(
        libs.cupertino.icons.extended
    )


    // ----------------------------------------------------------------
    // Local unit / contract tests
    // ----------------------------------------------------------------

    testImplementation(
        libs.junit
    )

    testImplementation(
        libs.mockwebserver
    )

    testImplementation(
        libs.kotlinx.coroutines.test
    )

    testImplementation(
        libs.robolectric
    )

    testImplementation(
        libs.androidx.arch.core.testing
    )


    // ----------------------------------------------------------------
    // Debug
    // ----------------------------------------------------------------

    debugImplementation(
        libs.androidx.ui.tooling
    )
}
APP_BUILD_EOF

cat > build.gradle.kts <<'ROOT_BUILD_EOF'
// ====================================================================
// File: build.gradle.kts
// Version: 30.0.0
//
// Purpose:
//   Root Android build configuration.
//
// Compose Cupertino migration Plan 3.0 / Batch 1:
//   - Keeps root plugin resolution centralized through the version
//     catalog.
//   - No repository, task, module, or behavior changes.
// ====================================================================

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
}
ROOT_BUILD_EOF

cat > settings.gradle.kts <<'SETTINGS_EOF'
// ====================================================================
// File: settings.gradle.kts
// Version: 30.0.0
//
// Purpose:
//   Configures Gradle project settings, plugin management, and
//   dependency repository ownership.
//
// Compose Cupertino migration Plan 3.0 / Batch 1:
//   - Keeps repository policy unchanged.
//   - Keeps root project and module structure unchanged.
// ====================================================================

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(
        RepositoriesMode.FAIL_ON_PROJECT_REPOS
    )
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name =
    "lias-android-remote"

include(
    ":app"
)
SETTINGS_EOF

cat > gradle/wrapper/gradle-wrapper.properties <<'WRAPPER_EOF'
# ====================================================================
# File: gradle/wrapper/gradle-wrapper.properties
# Version: 30.0.0
#
# Compose Cupertino migration Plan 3.0 / Batch 1:
#   AGP 8.10.x requires Gradle 8.11.1 as the minimum/default Gradle
#   baseline, so the existing wrapper remains unchanged.
# ====================================================================

distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.11.1-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
WRAPPER_EOF

cat > gradle.properties <<'PROPERTIES_EOF'
# ====================================================================
# File: gradle.properties
# Version: 30.0.0
#
# Purpose:
#   Stable project-wide Gradle and Android build properties.
#
# Compose Cupertino migration Plan 3.0 / Batch 1:
#   - Keeps AndroidX, Jetifier, Kotlin style, and non-transitive R
#     behavior unchanged.
# ====================================================================

org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8

android.useAndroidX=true
android.enableJetifier=true

kotlin.code.style=official

android.nonTransitiveRClass=true
PROPERTIES_EOF

mkdir -p app/src/main/java/com/lias/remote/ui/theme

cat > app/src/main/java/com/lias/remote/ui/theme/Theme.kt <<'THEME_EOF'
// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/theme/Theme.kt
// Version: 30.0.0
//
// Purpose:
//   LIAS-owned theme adapter.
//
// Compose Cupertino migration Plan 3.0 / Batch 1:
//   - Replaces the old Cupertino package namespace with the maintained
//     fork namespace.
//   - Preserves LIAS color tokens, dark/light behavior, system-bar
//     behavior, and the public LiasTheme API.
// ====================================================================

package com.lias.remote.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.slapps.cupertino.theme.CupertinoTheme
import com.slapps.cupertino.theme.darkColorScheme
import com.slapps.cupertino.theme.lightColorScheme

@Composable
fun LiasTheme(
    themeMode: String = "system",
    content: @Composable () -> Unit
) {
    val systemDark =
        isSystemInDarkTheme()

    val darkTheme =
        when (themeMode) {
            "light" ->
                false

            "dark" ->
                true

            else ->
                systemDark
        }

    val cupertinoColorScheme =
        if (darkTheme) {
            darkColorScheme(
                accent =
                    SystemBlueDark,
                systemBackground =
                    SystemBackgroundDark,
                secondarySystemBackground =
                    SystemSecondaryBackgroundDark,
                tertiarySystemBackground =
                    SystemTertiaryBackgroundDark,
                label =
                    SystemLabelDark,
                secondaryLabel =
                    SystemSecondaryLabelDark,
                tertiaryLabel =
                    SystemTertiaryLabelDark
            )
        } else {
            lightColorScheme(
                accent =
                    SystemBlueLight,
                systemBackground =
                    SystemBackgroundLight,
                secondarySystemBackground =
                    SystemSecondaryBackgroundLight,
                tertiarySystemBackground =
                    SystemTertiaryBackgroundLight,
                label =
                    SystemLabelLight,
                secondaryLabel =
                    SystemSecondaryLabelLight,
                tertiaryLabel =
                    SystemTertiaryLabelLight
            )
        }

    val view =
        LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window =
                (view.context as Activity)
                    .window

            WindowCompat
                .getInsetsController(
                    window,
                    view
                )
                .isAppearanceLightStatusBars =
                !darkTheme

            val bgColor =
                if (darkTheme) {
                    SystemBackgroundDark
                } else {
                    SystemBackgroundLight
                }

            window.statusBarColor =
                bgColor.toArgb()

            window.navigationBarColor =
                bgColor.toArgb()
        }
    }

    CompositionLocalProvider(
        LocalLiasDarkTheme provides darkTheme
    ) {
        CupertinoTheme(
            colorScheme =
                cupertinoColorScheme,
            content =
                content
        )
    }
}
THEME_EOF

echo "Batch 001 applied. Suggested checks:"
echo "  ./gradlew --version"
echo "  ./gradlew :app:dependencies --configuration debugRuntimeClasspath"
echo "  ./gradlew :app:compileDebugKotlin :app:testDebugUnitTest"
