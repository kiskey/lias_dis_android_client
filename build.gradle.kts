// ====================================================================
// File: build.gradle.kts
// Version: 27.0.0
//
// Purpose:
//   Root Android build configuration.
//
// Batch 27:
//   - Declares every version-catalog plugin used by :app.
//   - Adds Compose compiler plugin to root plugin resolution.
// ====================================================================

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
}
