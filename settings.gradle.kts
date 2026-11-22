pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

rootProject.name = "earbuds-anc"
include(":proto")
// :app (Android) gets added in Phase 2 — needs the Android SDK on the dev machine.
