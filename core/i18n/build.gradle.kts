plugins {
    alias(libs.plugins.ehviewer.multiplatform.library)
    alias(libs.plugins.moko.resources)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(libs.moko.resources)
            }
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    multiplatformResources {
        resourcesPackage = android.namespace
    }
}
