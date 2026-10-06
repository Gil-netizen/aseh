plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.kotlin.jvm) apply false
  alias(libs.plugins.ksp) apply false
  alias(libs.plugins.androidx.room) apply false
}

allprojects {
  dependencyLocking {
    lockAllConfigurations()
    lockMode.set(org.gradle.api.artifacts.dsl.LockMode.STRICT)
  }
}

tasks.register("checkNoDynamicVersions") {
  group = "verification"
  description = "Rejects dynamic, range, snapshot, and preview dependency selectors."

  val buildLogicFiles = rootProject.allprojects
    .map { project -> project.buildFile }
    .filter(File::isFile) +
    listOf(
      layout.projectDirectory.file("settings.gradle.kts").asFile,
      layout.projectDirectory.file("settings.gradle").asFile,
    ).filter(File::isFile)

  inputs.files(
    layout.projectDirectory.file("gradle/libs.versions.toml"),
    layout.projectDirectory.file("gradle/wrapper/gradle-wrapper.properties"),
    buildLogicFiles,
  )

  doLast {
    val forbidden = Regex(
      """(?ix)
        latest(?:\.[a-z]+)?
        | (?:^|\.)\+$
        | snapshot | alpha | beta | preview | milestone | eap
        | (?:^|[._-])rc(?:[._-]?\d+)?(?:$|[._-])
        | (?:^|[._-])m\d+(?:$|[._-])
        | (?:^|[._-])dev(?:\d+)?(?:$|[._-])
        | [\[\](),]
      """.trimIndent(),
    )
    val catalogAssignment = Regex("""(?m)^\s*[A-Za-z0-9_.-]+\s*=\s*"([^"]+)"\s*$""")
    val wrapperVersion = Regex("""gradle-([^/]+?)-(?:bin|all)\.zip""")
    val dependencyCoordinate = Regex("""["']([^"'$\s]+:[^"'$\s]+:([^"']+))["']""")
    val pluginVersion = Regex("""\bversion\s*(?:=\s*)?["']([^"']+)["']""")

    fun requireStable(selector: String, file: File) {
      check(!forbidden.containsMatchIn(selector)) {
        "Unstable or dynamic dependency selector '$selector' found in ${file.path}"
      }
    }

    inputs.files.forEach { file ->
      val text = file.readText()
      when (file.name) {
        "libs.versions.toml" -> catalogAssignment.findAll(text).forEach { match ->
          requireStable(match.groupValues[1], file)
        }
        "gradle-wrapper.properties" -> wrapperVersion.findAll(text).forEach { match ->
          requireStable(match.groupValues[1], file)
        }
        else -> {
          dependencyCoordinate.findAll(text).forEach { match ->
            requireStable(match.groupValues[2], file)
          }
          pluginVersion.findAll(text).forEach { match ->
            requireStable(match.groupValues[1], file)
          }
        }
      }
    }
  }
}
