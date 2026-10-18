import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("visionary.kmp.compose")

            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
            val compose = extensions.getByType<ComposeExtension>()

            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.named("commonMain").configure {
                    dependencies {
                        implementation(project(":domain"))
                        implementation(project(":core:common"))
                        implementation(project(":core:mvi"))
                        implementation(project(":core:navigation"))
                        implementation(project(":core:designsystem"))

                        implementation(compose.dependencies.material3)

                        implementation(libs.findLibrary("koin-compose").get())
                        implementation(libs.findLibrary("koin-compose-viewmodel").get())
                        implementation(libs.findLibrary("lifecycle-runtime-compose").get())
                    }
                }
            }
        }
    }
}
