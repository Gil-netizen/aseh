plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "io.github.gilnetizen.aseh"

  compileSdk {
    version = release(37) {
      minorApiLevel = 0
    }
  }
  buildToolsVersion = "37.0.0"

  defaultConfig {
    applicationId = "io.github.gilnetizen.aseh"
    minSdk = 26
    targetSdk = 37
    versionCode = 1
    versionName = "0.1.0-alpha.1"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    testInstrumentationRunnerArguments["useTestStorageService"] = "true"
  }

  buildTypes {
    debug {
      applicationIdSuffix = ".debug"
      versionNameSuffix = "-debug"
    }
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro",
      )
    }
  }

  flavorDimensions += "environment"
  productFlavors {
    create("dev") {
      dimension = "environment"
      applicationIdSuffix = ".dev"
      versionNameSuffix = "-dev"
    }
    create("staging") {
      dimension = "environment"
      applicationIdSuffix = ".staging"
      versionNameSuffix = "-staging"
    }
    create("prod") {
      dimension = "environment"
    }
  }

  buildFeatures {
    compose = true
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  packaging {
    resources.excludes += setOf(
      "/META-INF/AL2.0",
      "/META-INF/LGPL2.1",
    )
  }

  lint {
    abortOnError = true
    checkReleaseBuilds = true
  }
}

dependencies {
  implementation(project(":core:database"))
  implementation(project(":core:designsystem"))
  implementation(project(":core:ui"))
  implementation(project(":feature:build"))
  implementation(project(":feature:now"))
  implementation(project(":feature:practice"))
  implementation(project(":feature:prayer"))
  implementation(project(":feature:study"))

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.appcompat)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.material3)
  implementation(libs.kotlinx.coroutines.core)

  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  testImplementation(libs.junit)

  androidTestImplementation(project(":core:testing"))
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.rules)
  androidTestImplementation(libs.androidx.test.espresso.core)
  androidTestUtil(libs.androidx.test.services)
}
