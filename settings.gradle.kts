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

        maven { setUrl("https://jitpack.io") }
    }
}

rootProject.name = "NUMAD24Sp_Team33_Final"
include(":app")
 