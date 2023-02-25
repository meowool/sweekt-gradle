import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

val keepDependencies = arrayOf(
  "org.jetbrains.kotlin:kotlin-stdlib-jdk8",
)

val ktlint = configurations.detachedConfiguration(
  dependencies.create("com.pinterest:ktlint:0.48.2"),
)

plugins {
  arrayOf("jvm", "plugin.serialization").forEach {
    kotlin(it) version "1.8.20-Beta"
  }
  id("com.github.johnrengelman.shadow") version "7.1.2"
}

dependencies {
  repositories.mavenCentral()
  implementationOf(
    platform("io.ktor:ktor-bom:2.2.2"),

    "io.ktor:ktor-client",
    "io.ktor:ktor-client-okhttp",
    "io.ktor:ktor-client-logging",
    "io.ktor:ktor-client-content-negotiation",
    "io.ktor:ktor-serialization-kotlinx-json",
    "io.insert-koin:koin-core:3.3.2",
    "io.github.z4kn4fein:semver:1.4.2",
    "com.github.ajalt.mordant:mordant:2.0.0-beta11",
    "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4",

    *keepDependencies,
  )
}

kotlin {
  sourceSets.main {
    kotlin.srcDir("src")
  }
  jvmToolchain(11)
}

tasks {
  val ktlint by registering(JavaExec::class) {
    group = "verification"
    description = "Check and format Kotlin code style."
    classpath = ktlint
    args = listOf(
      "--color",
      "--format",
      "--relative",
      "src/**/*.kt",
      "*.kts",
    )
    mainClass.set("com.pinterest.ktlint.Main")
    doFirst { println("Linting & formatting...") }
  }

  withType<KotlinJvmCompile> { dependsOn(ktlint) }

  shadowJar {
    archiveVersion.set("")
    archiveClassifier.set("")
    archiveFileName.set("dist.jar")
    destinationDirectory.set(projectDir)
    manifest.attributes["Main-Class"] = "com.meowool.sweekt.gradle.MainKt"
    minimize {
      keepDependencies.forEach {
        exclude(dependency(it))
      }
    }
  }
}
