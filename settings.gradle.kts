pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots")
    }
}

plugins {
    id("com.android.settings") version "8.8.0-alpha08"
}

android {
    compileSdk = 35
    minSdk = 26
    targetSdk = 35
    ndkVersion = "27.2.12479018"
    buildToolsVersion = "35.0.0"
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
rootProject.name = "EhViewer"
include(":app")
include(":benchmark")
