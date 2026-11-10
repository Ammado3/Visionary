plugins {
    id("visionary.kmp.feature")
    alias(libs.plugins.compose.multiplatform)

}

kotlin {
    android {
        namespace = "com.o3.vanguard.visionary.features.vault"
        androidResources {
            enable = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:designsystem"))
            implementation(compose.components.resources)
            implementation(libs.composeUiToolingPreview)

        }
    }
}

compose.resources {
    packageOfResClass = "com.o3.vanguard.visionary.features.vault.generated.resources"
}
