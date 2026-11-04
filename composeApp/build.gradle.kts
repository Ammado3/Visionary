plugins {
    id("visionary.kmp.compose")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "com.o3.vanguard.visionary.composeapp"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:mvi"))
            implementation(project(":core:navigation"))
            implementation(project(":core:designsystem"))
            implementation(project(":core:database"))
            implementation(project(":core:testing"))
            implementation(project(":domain"))
            implementation(project(":data"))
            implementation(project(":platform"))

            implementation(project(":features:onboarding"))
            implementation(project(":features:home"))
            implementation(project(":features:gallery"))
            implementation(project(":features:search"))
            implementation(project(":features:scanner"))
            implementation(project(":features:vault"))
            implementation(project(":features:settings"))

            implementation(compose.material3)
            implementation(compose.components.resources)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.lifecycle.viewmodel.nav3)
            implementation(libs.nav3.ui)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
