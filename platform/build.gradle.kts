plugins {
    id("visionary.kmp.library")
}

kotlin {
    android {
        namespace = "com.o3.vanguard.visionary.platform"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":domain"))
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
        }

        androidMain.dependencies {
            implementation(libs.mlkit.text.recognition)
            implementation(libs.mlkit.image.labeling)
            implementation(libs.mlkit.document.scanner)
            implementation(libs.kotlinx.coroutines.play.services)
            implementation(libs.androidx.biometric)
            implementation(libs.androidx.work.runtime)
        }
    }
}
