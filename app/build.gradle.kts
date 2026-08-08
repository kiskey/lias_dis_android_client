// ====================================================================
// File: app/build.gradle.kts
// Version: 23.0.0
//
// Batch 23:
//   - Adds deterministic REST/SSE contract testing.
//   - Adds coroutine-test support.
//   - Adds Robolectric for Android URI/deep-link unit tests.
//   - Keeps production dependency footprint unchanged.
// ====================================================================

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.lias.remote"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.lias.remote"
        minSdk = 26
        targetSdk = 35

        versionCode = 6
        versionName = "2.0.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {

        release {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }

        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility =
            JavaVersion.VERSION_17

        targetCompatibility =
            JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes +=
                "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    /*
     * Required by Robolectric tests that exercise Android URI parsing
     * and other framework-level behavior without an emulator.
     */
    testOptions {
        unitTests {
            isIncludeAndroidResources =
                true
        }
    }
}

dependencies {

    // ---------------------------------------------------------------
    // Android / Lifecycle
    // ---------------------------------------------------------------

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

    // ---------------------------------------------------------------
    // Compose
    // ---------------------------------------------------------------

    implementation(
        platform(
            libs.androidx.compose.bom
        )
    )

    implementation(
        "androidx.compose.ui:ui"
    )

    implementation(
        "androidx.compose.ui:ui-graphics"
    )

    implementation(
        "androidx.compose.ui:ui-tooling-preview"
    )

    implementation(
        "androidx.compose.foundation:foundation"
    )

    implementation(
        "androidx.compose.foundation:foundation-layout"
    )

    // ---------------------------------------------------------------
    // Navigation
    // ---------------------------------------------------------------

    implementation(
        libs.androidx.navigation.compose
    )

    // ---------------------------------------------------------------
    // Networking
    // ---------------------------------------------------------------

    implementation(
        libs.okhttp
    )

    implementation(
        libs.okhttp.sse
    )

    implementation(
        libs.kotlinx.serialization.json
    )

    implementation(
        libs.kotlinx.coroutines.android
    )

    // ---------------------------------------------------------------
    // Persistence
    // ---------------------------------------------------------------

    implementation(
        libs.androidx.datastore.preferences
    )

    // ---------------------------------------------------------------
    // Cupertino
    // ---------------------------------------------------------------

    implementation(
        libs.cupertino
    )

    implementation(
        libs.cupertino.icons.extended
    )

    // ---------------------------------------------------------------
    // Unit / contract tests
    // ---------------------------------------------------------------

    testImplementation(
        libs.junit
    )

    /*
     * Same OkHttp generation as production.
     *
     * Used for exact request/response contract testing without touching
     * a real LIAS installation.
     */
    testImplementation(
        "com.squareup.okhttp3:mockwebserver:4.12.0"
    )

    testImplementation(
        "org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1"
    )

    /*
     * Needed for NavigationRoutes/LiasDeepLinks because android.net.Uri
     * is an Android framework type.
     */
    testImplementation(
        "org.robolectric:robolectric:4.13.2"
    )

    testImplementation(
        "androidx.arch.core:core-testing:2.2.0"
    )

    // ---------------------------------------------------------------
    // Debug-only
    // ---------------------------------------------------------------

    debugImplementation(
        libs.androidx.ui.tooling
    )
}
