import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")
            pluginManager.apply("com.android.kotlin.multiplatform.library")

            val kmp = extensions.getByType<KotlinMultiplatformExtension>()
            val aware = kmp as ExtensionAware
            val androidLibraryExt = aware.extensions.getByName("androidLibrary")
            val setCompileSdk = androidLibraryExt.javaClass.methods.firstOrNull { it.name == "setCompileSdk" && it.parameterCount == 1 }
            setCompileSdk?.invoke(androidLibraryExt, 37)
            val setMinSdk = androidLibraryExt.javaClass.methods.firstOrNull { it.name == "setMinSdk" && it.parameterCount == 1 }
            setMinSdk?.invoke(androidLibraryExt, 26)

            val withHostTest = androidLibraryExt.javaClass.methods.firstOrNull {
                it.name == "withHostTest" && it.parameterTypes.firstOrNull()?.name == "kotlin.jvm.functions.Function1"
            }
            withHostTest?.invoke(androidLibraryExt, { _: Any? -> })

            kmp.iosArm64()
            kmp.iosSimulatorArm64()

            kmp.compilerOptions {
                allWarningsAsErrors.set(true)
                freeCompilerArgs.add("-Xexpect-actual-classes")
            }

            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

            kmp.sourceSets.all {
                if (name.lowercase().contains("test")) {
                    languageSettings.optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
                }
            }

            kmp.sourceSets.named("commonTest").configure {
                dependencies {
                    implementation(kotlin("test"))
                    implementation(libs.findLibrary("kotlinx-coroutines-test").get())
                    implementation(libs.findLibrary("turbine").get())
                    if (target.path != ":core:testkit" && target.findProject(":core:testkit") != null) {
                        implementation(project(":core:testkit"))
                    }
                }
            }
        }
    }
}
