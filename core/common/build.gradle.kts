plugins {
    alias(libs.plugins.ehviewer.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(libs.kotlinx.coroutines.core)
                api(libs.kotlinx.datetime)
                api(libs.kotlinx.io)
                api(libs.okio)
                api(libs.serialization.cbor)
                api(libs.logcat)
                api(project.dependencies.platform(libs.arrow.stack))
                api(libs.bundles.arrow)
            }
        }
        androidMain {
            dependencies {
                api(libs.kotlinx.coroutines.android)
                api(libs.splitties.appctx)
                implementation(libs.androidx.core)
            }
        }
    }
}
