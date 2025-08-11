plugins {
    alias(libs.plugins.ehviewer.multiplatform.library)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project.dependencies.platform(libs.okio.bom))
                api(libs.androidx.datastore)
                api(libs.kotlinx.coroutines.core)
            }
        }
        androidMain {
            dependencies {
                api(libs.kotlinx.coroutines.android)
                api(libs.splitties.appctx)
            }
        }
    }
}
