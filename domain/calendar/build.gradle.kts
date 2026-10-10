import org.gradle.language.jvm.tasks.ProcessResources

plugins {
  alias(libs.plugins.kotlin.jvm)
}

kotlin {
  jvmToolchain(17)
}

dependencies {
  api(project(":domain:servicecatalog"))
  implementation(libs.kosherjava.zmanim)

  testImplementation(libs.junit)
}

tasks.named<ProcessResources>("processResources") {
  from(rootProject.file("third_party/kosherjava-zmanim/LICENSE-LGPL-2.1.txt")) {
    into("META-INF")
    rename { "LICENSE-kosherjava-zmanim.txt" }
  }
  from(rootProject.file("third_party/kosherjava-zmanim/NOTICE")) {
    into("META-INF")
    rename { "NOTICE-kosherjava-zmanim.txt" }
  }
}
