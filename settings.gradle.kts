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
    }
}

plugins {
    id("com.android.settings") version "8.9.1"
}

android {
    compileSdk = 36
    minSdk = 26
    targetSdk = 36
    ndkVersion = "28.0.13004108"
    buildToolsVersion = "36.0.0"
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
rootProject.name = "EhViewer"
include(":app")
include(":benchmark")
