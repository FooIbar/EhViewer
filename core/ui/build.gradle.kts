import org.jetbrains.compose.compose

plugins {
    alias(libs.plugins.ehviewer.multiplatform.library.compose)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.core.common)
                api(compose.material3)
                api(compose.materialIconsExtended)
                api(libs.compose.material3.adaptive)
                api(libs.androidx.lifecycle.compose)
                api(libs.androidx.lifecycle.viewmodel.compose)
                implementation(compose("org.jetbrains.compose.ui:ui-backhandler"))
                implementation(compose.preview)
            }
        }

        androidMain {
            dependencies {
                api(project.dependencies.platform(libs.compose.bom))
                implementation(libs.androidx.activity.compose)
            }
        }
    }
}
