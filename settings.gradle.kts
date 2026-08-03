// ====================================================================
// File: settings.gradle.kts
// Version: 1.0.0
// Purpose: Configures Gradle project settings, plugin management, and 
//          centralizes dependency versions via Version Catalogs.
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
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "lias-android-remote"
include(":app")
