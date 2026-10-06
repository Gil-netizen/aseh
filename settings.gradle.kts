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

rootProject.name = "aseh"

include(
  ":app",
  ":core:database",
  ":core:designsystem",
  ":core:testing",
  ":core:ui",
  ":feature:build",
  ":feature:now",
  ":feature:practice",
  ":feature:prayer",
  ":feature:study",
)
