plugins {
    id("visionary.kmp.library")
}

kotlin {
    android {
        namespace = "com.o3.vanguard.visionary.core.testkit"
    }

    sourceSets {
        commonMain.dependencies {
            api(kotlin("test"))
            api(libs.kotlinx.coroutines.test)
            api(libs.turbine)

            implementation(project(":core:common"))
            implementation(project(":domain"))
            implementation(project(":core:mvi"))
            implementation(libs.lifecycle.viewmodel)
        }
    }
}
