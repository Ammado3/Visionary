plugins {
    id("visionary.kmp.compose")
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    android {
        namespace = "com.o3.vanguard.visionary.platform"
        // For IndexingForegroundService's notification icon (res/drawable).
        androidResources {
            enable = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:designsystem"))
            implementation(project(":domain"))
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.coil.compose)
        }

        androidMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.material3)
            implementation(libs.androidx.activity.compose)
            implementation(libs.lifecycle.runtime.compose)
            implementation(libs.mlkit.text.recognition)
            implementation(libs.mlkit.image.labeling)
            implementation(libs.kotlinx.coroutines.play.services)
            implementation(libs.androidx.biometric)
            implementation(libs.androidx.work.runtime)
            implementation(libs.coil.video)
        }
    }
}
