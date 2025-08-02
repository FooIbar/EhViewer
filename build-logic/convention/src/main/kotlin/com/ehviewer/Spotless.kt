package com.ehviewer

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

internal fun Project.configureSpotless() {
    apply(plugin = libs.findPlugin("spotless").get().get().pluginId)

    configure<SpotlessExtension> {
        val ktlintVersion = libs.findLibrary("ktlint").get().get().version
        kotlin {
            // https://github.com/diffplug/spotless/issues/111
            target("src/**/*.kt")
            ktlint(ktlintVersion)
        }
        kotlinGradle {
            ktlint(ktlintVersion)
        }
    }
}
