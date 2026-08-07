// ====================================================================
// File: app/build.gradle.kts
// Version: 1.5.0
// Audit Fixes:
//   1. Updated compileSdk to 36 to satisfy Cupertino 3.3.1 AAR metadata requirement.
//   2. Retained targetSdk 35 and minSdk 26 for 100% runtime parity.
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
        versionCode = 4
        versionName = "1.5.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            val keystoreFile = file("release.keystore")
            if (keystoreFile.exists()) {
                signingConfig = signingConfigs.getByName("release").apply {
                    storeFile = keystoreFile
                    storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                    keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                    keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
                }
            }
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    
    implementation(libs.okhttp)
    implementation(libs.okhttp.sse)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    
    implementation(libs.androidx.datastore.preferences)
    
    // Cupertino library (RobinPcrd fork - active maintenance)
    implementation(libs.cupertino)
    implementation(libs.cupertino.icons.extended)
    
    testImplementation(libs.junit)
    debugImplementation(libs.androidx.ui.tooling)
}
