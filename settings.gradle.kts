pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        // https://issuetracker.google.com/477453079
        maven("https://androidx.dev/snapshots/builds/14690944/artifacts/repository")
    }
}

plugins {
    id("com.android.settings") version "9.0.0"
}

android {
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }
    minSdk = 23
    targetSdk = 36
    ndkVersion = "29.0.14206865"
    buildToolsVersion = "36.1.0"
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
rootProject.name = "EhViewer"
include(":app")
include(":benchmark")
include(":core:common")
include(":core:data")
include(":core:i18n")
include(":core:ui")
