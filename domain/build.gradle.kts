plugins {
    id("visionary.kmp.library")
}

kotlin {
    android {
        namespace = "com.o3.vanguard.visionary.domain"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
        }
    }
}
