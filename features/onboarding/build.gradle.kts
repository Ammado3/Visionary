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
            implementation(compose.components.resources)
        }
    }
}

compose.resources {
    packageOfResClass = "com.o3.vanguard.visionary.features.onboarding.generated.resources"
}
