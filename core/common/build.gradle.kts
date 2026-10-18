plugins {
    id("visionary.kmp.library")
}

kotlin {
    android {
        namespace = "com.o3.vanguard.visionary.core.common"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
        }
    }
}
