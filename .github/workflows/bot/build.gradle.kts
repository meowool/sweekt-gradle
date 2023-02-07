import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile
import org.jetbrains.kotlin.gradle.targets.js.yarn.yarn

val ktlint = configurations.detachedConfiguration(
  dependencies.create("com.pinterest:ktlint:0.48.2"),
)

plugins {
  fun kt(name: String) = kotlin(name) version "1.8.0"
  kt("js")
  kt("plugin.serialization")
}

dependencies {
  repositories.mavenCentral()
  implementationOf(
    platform("io.ktor:ktor-bom:2.2.2"),

    npm("@actions/core", "^1"),
    npm("@actions/exec", "^1"),
    npm("string_decoder", "^1"),
    npm("ansi-colors", "^4"),
    npm("semver", "^7"),

    "io.ktor:ktor-client-js",
    "io.ktor:ktor-client-logging-js",
    "io.ktor:ktor-client-content-negotiation",
    "io.ktor:ktor-serialization-kotlinx-json",

    "org.jetbrains.kotlin-wrappers:kotlin-node:18.11.17-+",
    "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4",

    "io.insert-koin:koin-core:3.3.2",
  )
}

kotlin {
  js {
    nodejs()
    useCommonJs()
    binaries.executable()
  }
  sourceSets.main {
    kotlin.srcDir("src")
  }
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

  withType<KotlinJsCompile> {
    dependsOn(ktlint)
  }

  build {
    val directory = buildDir.resolve("js/packages/${project.name}")

    doLast {
      directory.walk().filter { it.extension == "js" }.forEach { file ->
        val content = file.readText()
        if (content.contains("eval('require')")) {
          println("Fixing '${file.absolutePath}'")
          file.writeText(content.replace("eval('require')", "require"))
        }
      }
      exec {
        workingDir(directory)
        commandLine("npx", "ncc", "build", "-o", file("dist").absolutePath)
      }
    }
  }
}

yarn.lockFileDirectory = projectDir
