plugins {
    id("visionary.kmp.feature")
}

kotlin {
    android {
        namespace = "com.o3.vanguard.visionary.features.gallery"
        androidResources {
            enable = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.components.resources)
        }
    }
}

compose.resources {
    packageOfResClass = "com.o3.vanguard.visionary.features.gallery.generated.resources"
}
