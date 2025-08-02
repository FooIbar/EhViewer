import com.ehviewer.configureKotlinCompose
import com.ehviewer.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

@Suppress("unused")
class MultiplatformLibraryComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        apply<MultiplatformLibraryConventionPlugin>()
        apply(plugin = libs.findPlugin("compose-compiler").get().get().pluginId)
        apply(plugin = libs.findPlugin("compose-multiplatform").get().get().pluginId)
        apply(plugin = libs.findPlugin("composeCompilerReportGenerator").get().get().pluginId)

        configure<KotlinMultiplatformExtension> {
            compilerOptions {
                configureKotlinCompose()
            }
        }
    }
}
