plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.aboutlibrariesPlugin) apply false
    alias(libs.plugins.rustAndroidPlugin) apply false
    alias(libs.plugins.composeCompilerReportGenerator) apply false
}

tasks.register("Delete", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}

buildscript {
    dependencies {
        classpath(libs.r8)
    }
}
