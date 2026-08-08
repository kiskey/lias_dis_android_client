// ====================================================================
// File: app/build.gradle.kts
// Version: 27.0.0
//
// Purpose:
//   Final application build configuration.
//
// Batch 27:
//   - compileSdk 36 now paired with AGP 8.9.3.
//   - JDK/JVM 17.
//   - Batch-23 MockWebServer tests restored.
//   - Coroutine tests restored.
//   - Robolectric navigation/deep-link tests restored.
//   - Android resources enabled for local Robolectric tests.
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
    // Cupertino
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
