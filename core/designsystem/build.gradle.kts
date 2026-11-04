plugins {
    id("visionary.kmp.compose")
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.o3.vanguard.visionary.core.designsystem.generated.resources"

}

kotlin {
    android {
        namespace = "com.o3.vanguard.visionary.core.designsystem"
        androidResources {
            enable = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(libs.coil.compose)

            // Compose Multiplatform resources
            implementation(compose.components.resources)
        }

        androidMain.dependencies {
            implementation(libs.composeUiTooling)
        }
    }
}