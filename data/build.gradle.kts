plugins {
    id("visionary.kmp.library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "com.o3.vanguard.visionary.data"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:database"))
            implementation(project(":domain"))
            implementation(libs.room3.runtime)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
