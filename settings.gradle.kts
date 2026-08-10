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
