pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven("https://storage.googleapis.com/r8-releases/raw")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jcenter.bintray.com")
        maven("https://jitpack.io")
        maven("https://androidx.dev/storage/compose-compiler/repository/")
        // Test if IME not hiding after performing search is fixed before updating.
        maven("https://androidx.dev/snapshots/builds/11157558/artifacts/repository")
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
rootProject.name = "EhViewer"
include(":app")
