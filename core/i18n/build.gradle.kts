plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.moko.resources)
}

kotlin {
    // TODO: Migrate to AGP KMP
    // https://github.com/icerockdev/moko-resources/issues/820
    androidTarget()

    sourceSets {
        commonMain {
            dependencies {
                api(libs.moko.resources)
            }
        }
    }

    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

android {
    namespace = "com.ehviewer.core.i18n"
}

multiplatformResources {
    resourcesPackage = "com.ehviewer.core.i18n"
}
