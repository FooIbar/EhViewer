plugins {
    alias(libs.plugins.ehviewer.multiplatform.library)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.core.common)
                api(libs.androidx.datastore)
            }
        }
        androidMain {
            dependencies {
            }
        }
    }
}
