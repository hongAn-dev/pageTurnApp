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

rootProject.name = "PageTurn"
include(":app")
include(":core:model")
include(":core:common")
include(":core:network")
include(":core:database")
include(":core:domain")
include(":core:data")
include(":core:designsystem")
include(":feature:library")
include(":feature:reader")
