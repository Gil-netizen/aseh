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
  ":core:content",
  ":core:content-android",
  ":core:designsystem",
  ":core:model",
  ":core:testing",
  ":core:ui",
  ":domain:calendar",
  ":domain:servicecatalog",
  ":domain:workspace",
  ":domain:zmanim",
  ":feature:build",
  ":feature:now",
  ":feature:practice",
  ":feature:prayer",
  ":feature:study",
  ":feature:workspace",
)
