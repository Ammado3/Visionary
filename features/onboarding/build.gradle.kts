plugins {
    id("visionary.kmp.feature")
}

kotlin {
    android {
        namespace = "com.o3.vanguard.visionary.features.onboarding"
        androidResources {
            enable = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":platform"))
        }
        androidMain.dependencies {
            implementation(libs.composeUiTooling)
        }
    }
}

compose.resources {
    packageOfResClass = "com.o3.vanguard.visionary.features.onboarding.generated.resources"
}

dependencies {
    commonMainImplementation(compose.components.resources)
    commonMainImplementation(libs.composeUiToolingPreview)
}