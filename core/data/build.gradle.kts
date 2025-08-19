plugins {
    alias(libs.plugins.ehviewer.multiplatform.library)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.core.common)
                api(project.dependencies.platform(libs.okio.bom))
                api(libs.androidx.datastore)
            }
        }
        androidMain {
            dependencies {
                api(libs.splitties.appctx)
            }
        }
    }
}
