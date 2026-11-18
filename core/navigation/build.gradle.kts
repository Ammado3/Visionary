plugins {
    id("visionary.kmp.compose")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "com.o3.vanguard.visionary.core.navigation"
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.nav3.ui)
            implementation(project(":core:designsystem"))
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
