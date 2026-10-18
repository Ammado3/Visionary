plugins {
    id("visionary.kmp.library")
}

kotlin {
    android {
        namespace = "com.o3.vanguard.visionary.core.testing"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":domain"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
        }
    }
}
