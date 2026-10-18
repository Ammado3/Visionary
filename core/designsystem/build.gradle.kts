plugins {
    id("visionary.kmp.compose")
}

kotlin {
    android {
        namespace = "com.o3.vanguard.visionary.core.designsystem"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(compose.material3)
            implementation(libs.coil.compose)
        }
    }
}
