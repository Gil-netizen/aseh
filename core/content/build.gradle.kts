plugins {
  alias(libs.plugins.kotlin.jvm)
}

kotlin {
  jvmToolchain(17)
}

dependencies {
  testImplementation(libs.junit)
  testRuntimeOnly(libs.sqlite.jdbc)
}

val developmentPackAsset = layout.buildDirectory.file(
  "generated/developmentPackAssets/aseh-development-fixture.asehpack",
)

/**
 * Produces the bundled development fixture through the same deterministic
 * compiler and signing boundary used by pack tooling. The generator and its
 * fixed synthetic private key live in the test source set and are never part
 * of the core artifact or any Android runtime classpath.
 */
tasks.register<JavaExec>("generateDevelopmentPackAsset") {
  group = "build"
  description = "Generate the signed synthetic content pack used only by the dev flavor."
  dependsOn(tasks.testClasses)
  classpath = sourceSets.test.get().runtimeClasspath
  mainClass.set("io.github.gilnetizen.aseh.core.content.DevelopmentPackAssetGenerator")
  outputs.file(developmentPackAsset)
  args(developmentPackAsset.get().asFile.absolutePath)
}
