// ====================================================================
// File: build.gradle.kts (Root)
// Version: 1.0.0
// Purpose: Top-level build file. Applies safe initialization flags and 
//          declares Android/Kotlin versions without bloating classpath.
// ====================================================================

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
