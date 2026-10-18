import org.gradle.api.Plugin
import org.gradle.api.Project
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

            kmp.iosArm64()
            kmp.iosSimulatorArm64()

            kmp.compilerOptions {
                allWarningsAsErrors.set(true)
                freeCompilerArgs.add("-Xexpect-actual-classes")
            }
        }
    }
}
