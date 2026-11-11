pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "Launcher"
include(":app")
include(":phoneguide")
include(":testnotifier")   // CI-only test app for the emulator check (never released)
include(":eleven")         // Eleven (LineageOS music app, lineage-17.1) vendored, with NO album art ever; its own APK, org.lineageos.eleven (TASKS BV1)
// AOSP Launcher3 icon library (see launcher3/PATCHES.md)
include(":iconloaderlib")
project(":iconloaderlib").projectDir = file("launcher3/iconloaderlib")
