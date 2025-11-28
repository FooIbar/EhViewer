plugins {
    alias(libs.plugins.ehviewer.multiplatform.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.core.common)
                api(libs.androidx.datastore)
                api(libs.androidx.room.paging)
                implementation(libs.ktor.client.core)
            }
        }
        androidMain {
            dependencies {
                implementation(project.dependencies.platform(libs.okhttp.bom))
                implementation(libs.ktor.client.okhttp)
            }
        }
    }
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
}

room {
    schemaDirectory("$projectDir/schemas")
}
