pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Visionary"

include(":androidApp", ":composeApp")
include(":core:common", ":core:mvi", ":core:navigation", ":core:designsystem", ":core:database", ":core:testing", ":core:testkit")
include(":domain", ":data", ":platform")
include(
    ":features:onboarding", ":features:home", ":features:gallery",
    ":features:search", ":features:scanner", ":features:vault", ":features:settings",
    ":features:teleport",
)
