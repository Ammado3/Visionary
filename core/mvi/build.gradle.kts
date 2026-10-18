plugins {
    id("visionary.kmp.compose")
}

kotlin {
    android {
        namespace = "com.o3.vanguard.visionary.core.mvi"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(libs.lifecycle.viewmodel)
            implementation(libs.lifecycle.runtime.compose)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
