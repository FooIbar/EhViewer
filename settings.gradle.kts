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
        maven("https://central.sonatype.com/repository/maven-snapshots/")
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
        maven("https://central.sonatype.com/repository/maven-snapshots/")
    }
}

plugins {
    id("com.android.settings") version "9.0.0-beta05"
}

android {
    compileSdk = 36
    minSdk = 23
    targetSdk = 36
    ndkVersion = "29.0.14206865"
    buildToolsVersion = "36.0.0"
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
