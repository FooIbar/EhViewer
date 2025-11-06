plugins {
    alias(libs.plugins.ehviewer.multiplatform.library.compose)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.core.common)
                api(libs.compose.material3)
                api(libs.compose.material.icons.extended)
                api(libs.compose.material3.adaptive)
                api(libs.androidx.lifecycle.compose)
                api(libs.androidx.lifecycle.viewmodel.compose)
                implementation(libs.compose.ui.backhandler)
                implementation(libs.compose.ui.tooling.preview)
            }
        }

        androidMain {
            dependencies {
                api(project.dependencies.platform(libs.compose.bom))
                api(libs.compose.foundation)
                implementation(libs.androidx.activity.compose)
            }
        }
    }
}
