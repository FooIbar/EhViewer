import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.android.build.api.dsl.Lint
import com.android.build.api.dsl.androidLibrary
import com.ehviewer.configureKotlin
import com.ehviewer.configureLint
import com.ehviewer.configureSpotless
import com.ehviewer.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

@OptIn(ExperimentalKotlinGradlePluginApi::class)
class MultiplatformLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        apply(plugin = libs.plugins.kotlin.multiplatform.get().pluginId)
        apply(plugin = libs.plugins.android.kotlin.multiplatform.library.get().pluginId)
        apply(plugin = libs.plugins.android.lint.get().pluginId)

        configure<KotlinMultiplatformExtension> {
            jvmToolchain(21)
            compilerOptions {
                configureKotlin()
            }

            applyHierarchyTemplate {
                common {
                    group("jvm") {
                        withCompilations { it.target is KotlinMultiplatformAndroidLibraryTarget }
                        withJvm()
                    }
                }
            }

            // jvm("desktop") {
            //     compilerOptions {
            //         jvmDefault = JvmDefaultMode.NO_COMPATIBILITY
            //     }
            // }

            androidLibrary {
                namespace = "com.ehviewer${path.replace(':', '.')}"

                withHostTestBuilder {
                }

                enableCoreLibraryDesugaring = true

                compilerOptions {
                    jvmDefault = JvmDefaultMode.NO_COMPATIBILITY
                }
            }
        }
        configure<Lint>(Lint::configureLint)
        configureSpotless()

        dependencies {
            "coreLibraryDesugaring"(libs.desugar)
        }
    }
}
