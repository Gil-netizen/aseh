plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

abstract class StageDevelopmentPackAsset : DefaultTask() {
    @get:InputFile
    abstract val inputPack: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun stage() {
        val destination = outputDirectory.file("aseh-development-fixture.asehpack").get().asFile
        destination.parentFile.mkdirs()
        inputPack.get().asFile.copyTo(destination, overwrite = true)
    }
}

android {
    namespace = "io.github.gilnetizen.aseh.feature.study"

    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }
    buildToolsVersion = "37.0.0"

    defaultConfig {
        minSdk = 26
    }

    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
        }
        create("staging") {
            dimension = "environment"
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
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:ui"))
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)

    debugImplementation(libs.androidx.compose.ui.tooling)
    add("devImplementation", project(":core:content-android"))
    testImplementation(libs.junit)
}

val stageDevelopmentPackAsset = tasks.register<StageDevelopmentPackAsset>("stageDevelopmentPackAsset") {
    inputPack.set(
        project(":core:content").layout.buildDirectory.file(
            "generated/developmentPackAssets/aseh-development-fixture.asehpack",
        ),
    )
    outputDirectory.set(layout.buildDirectory.dir("generated/devPackAssets"))
    dependsOn(":core:content:generateDevelopmentPackAsset")
}

androidComponents {
    onVariants(selector().withFlavor("environment" to "dev")) { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(
            stageDevelopmentPackAsset,
            StageDevelopmentPackAsset::outputDirectory,
        )
    }
}
