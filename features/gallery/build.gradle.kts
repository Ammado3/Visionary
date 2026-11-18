plugins {
    id("visionary.kmp.feature")
    alias(libs.plugins.compose.multiplatform)
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
            implementation("org.jetbrains.compose.components:components-resources:1.12.2")
        }
    }
}

compose.resources {
    packageOfResClass = "com.o3.vanguard.visionary.features.gallery.generated.resources"
}
