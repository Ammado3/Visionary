plugins {
    id("visionary.kmp.feature")
}

kotlin {
    android {
        namespace = "com.o3.vanguard.visionary.features.vault"
        androidResources {
            enable = true
        }
    }
}

compose.resources {
    packageOfResClass = "com.o3.vanguard.visionary.features.vault.generated.resources"
}

dependencies {
    commonMainImplementation(compose.components.resources)
}
