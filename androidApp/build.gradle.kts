plugins {
    id("visionary.android.application")
}

dependencies {
    implementation(project(":composeApp"))
    implementation(project(":core:common"))
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":platform"))

    implementation(libs.koin.android)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)
}

android {
    namespace = "com.o3.vanguard.visionary"
    defaultConfig {
        applicationId = "com.o3.vanguard.visionary"
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}
