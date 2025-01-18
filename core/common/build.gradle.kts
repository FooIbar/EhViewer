plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvmToolchain(21)

    androidLibrary {
        namespace = "com.ehviewer.core.common"
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(compose.runtime)
                api(compose.components.resources)
            }
        }
    }
}

compose {
    resources {
        publicResClass = true
        packageOfResClass = "com.ehviewer.core.common"
    }
}
