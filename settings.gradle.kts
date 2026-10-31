pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "AI-DeviceCare"
include(":apps:devicecare", ":apps:devicecare-plus")
include(":core:evidence", ":core:quality", ":core:scoring", ":core:ui")
