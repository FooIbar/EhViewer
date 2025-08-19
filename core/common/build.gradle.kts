plugins {
    alias(libs.plugins.ehviewer.multiplatform.library)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(libs.kotlinx.coroutines.core)
                api(libs.logcat)
                api(project.dependencies.platform(libs.arrow.stack))
                api(libs.bundles.arrow)
            }
        }
        androidMain {
            dependencies {
                api(libs.kotlinx.coroutines.android)
            }
        }
    }
}
