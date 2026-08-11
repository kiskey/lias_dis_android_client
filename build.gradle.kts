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
