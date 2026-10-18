import androidx.room3.gradle.RoomExtension

plugins {
    id("visionary.kmp.library")
    alias(libs.plugins.ksp)
    alias(libs.plugins.room3)
}

kotlin {
    android {
        namespace = "com.o3.vanguard.visionary.core.database"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            api(libs.room3.runtime)
            api(libs.sqlite.bundled)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.core)
        }
    }
}

dependencies {
    add("kspAndroid", libs.room3.compiler)
    add("kspIosArm64", libs.room3.compiler)
    add("kspIosSimulatorArm64", libs.room3.compiler)
}

configure<RoomExtension> {
    schemaDirectory("$projectDir/schemas")
}
