import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.Lint
import com.ehviewer.configureKotlin
import com.ehviewer.configureKotlinCompose
import com.ehviewer.configureLint
import com.ehviewer.configureSpotless
import com.ehviewer.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidExtension

@Suppress("unused")
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        apply(plugin = libs.plugins.kotlin.android.get().pluginId)
        apply(plugin = libs.plugins.android.application.get().pluginId)
        apply(plugin = libs.plugins.compose.compiler.get().pluginId)
        apply(plugin = libs.plugins.composeCompilerReportGenerator.get().pluginId)

        configure<KotlinAndroidExtension> {
            jvmToolchain(21)
            compilerOptions {
                configureKotlin()
                configureKotlinCompose()
                jvmDefault = JvmDefaultMode.NO_COMPATIBILITY
            }
        }
        configure<ApplicationExtension> {
            lint(Lint::configureLint)
        }
        configureSpotless()
    }
}
