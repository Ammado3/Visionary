plugins {
    `kotlin-dsl`
}

group = "com.o3.vanguard.visionary.buildlogic"

dependencies {
    compileOnly("com.android.tools.build:gradle:${libs.versions.agp.get()}")
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
    compileOnly("org.jetbrains.compose:compose-gradle-plugin:${libs.versions.composeMultiplatform.get()}")
    compileOnly("org.jetbrains.kotlin:compose-compiler-gradle-plugin:${libs.versions.kotlin.get()}")
    compileOnly("com.google.devtools.ksp:symbol-processing-gradle-plugin:${libs.versions.ksp.get()}")
    compileOnly("androidx.room3:room3-gradle-plugin:${libs.versions.room3.get()}")
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "visionary.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("kmpCompose") {
            id = "visionary.kmp.compose"
            implementationClass = "KmpComposeConventionPlugin"
        }
        register("kmpFeature") {
            id = "visionary.kmp.feature"
            implementationClass = "KmpFeatureConventionPlugin"
        }
        register("androidApplication") {
            id = "visionary.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
    }
}
