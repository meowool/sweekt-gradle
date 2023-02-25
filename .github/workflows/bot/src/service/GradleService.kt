package com.meowool.sweekt.gradle.service

import com.meowool.sweekt.gradle.model.GradleVersion
import com.meowool.sweekt.gradle.utils.debug
import com.meowool.sweekt.gradle.utils.exec
import com.meowool.sweekt.gradle.utils.retry
import com.meowool.sweekt.gradle.utils.withDebug
import kotlin.io.path.Path
import kotlin.io.path.moveTo
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * @author chachako
 */
class GradleService(private val git: GitService) {

  /**
   * Executes the Gradle command via `gradlew`.
   */
  @Suppress("SpellCheckingInspection")
  suspend operator fun invoke(
    vararg args: String,
    properties: Array<String> = emptyArray(),
  ) = exec(
    ExecutableFile,
    *args,
    *properties.map { "-P$it" }.toTypedArray(),
    "--no-parallel",
    "--no-configuration-cache",
    "-Dfile.encoding=UTF-8",
    "-Duser.language=en",
  )

  suspend fun clean() = retry("clean project", max = 2) {
    // We need to retry because the CI build sometimes fails
    invoke("clean")
  }

  inline fun disableVerification(block: () -> Unit) {
    val backup = Path("$VerificationMetadataFile.bak")
    // Disable Gradle dependency verification
    VerificationMetadataFile.moveTo(backup)
    try {
      block()
    } finally {
      // Enable Gradle dependency verification
      backup.moveTo(VerificationMetadataFile)
    }
  }

  suspend fun pinWrapperVersion(): String? = withDebug("pinWrapperVersion") {
    fun String.replaceDistributionUrl(newUrl: String) =
      replace(DistributionUrlRegex, "$1$newUrl\$2")

    val oldContent = WrapperPropFile.readText()
    // We only need to pin the snapshot distribution url
    if (oldContent.contains(SnapshotDistributionUrlPath)) {
      val fullVersion = calculateVersion().fullVersion
      val newContent = oldContent.replaceDistributionUrl(
        "https\\://$DistributionUrlPath/gradle-$fullVersion-bin.zip",
      )
      if (oldContent != newContent) {
        WrapperPropFile.writeText(newContent)
        return fullVersion
      }
    }
    return null
  }

  /**
   * Calculates the full version information of Gradle according to
   * the name of the [branch] and the `version.txt` file.
   */
  suspend fun calculateVersion(branch: Any? = null): GradleVersion {
    if (branch != null) git.checkout(branch)

    val currentBranch = branch?.toString() ?: git.currentBranch()

    fun Regex.extractVersion() = find(currentBranch)?.groupValues?.get(1)

    return withDebug("calculateGradleVersion") {
      GradleVersion(
        baseVersion = parseBaseVersion(),
        rcNumber = RCVersionRegex.extractVersion()?.toInt(),
        milestoneNumber = MilestoneVersionRegex.extractVersion()?.toInt(),
      )
    }
  }

  /**
   * Parsing the `version.txt` file to the base version part of the
   * Gradle version.
   */
  private suspend fun parseBaseVersion(): GradleVersion.BasePart =
    withDebug("parseVersionFile") {
      val content = VersionFile.readText().debug("versionFileContent")
      requireNotNull(
        BaseVersionRegex.find(content)?.run {
          GradleVersion.BasePart(
            majorMinor = requireNotNull(groups["majorMinor"]?.value) {
              "Cannot parse the major and minor version of Gradle"
            },
            patch = groups["patch"]?.value ?: groups["patchSingle"]?.value,
            sweekt = groups["sweekt"]?.value,
          )
        },
      ) {
        "Cannot parse the base version of the branch " +
          "'${git.currentBranch()}', version file content: $content"
      }
    }

  companion object {
    const val RepositoryUrl = "https://github.com/gradle/gradle"
    const val ExecutableFile = "./gradlew"

    val WrapperPropFile = Path("gradle/wrapper/gradle-wrapper.properties")
    val VerificationMetadataFile = Path("gradle/verification-metadata.xml")
    val VersionFile = Path("version.txt")

    private const val ServicesHost = "services.gradle.org"
    private const val DistributionUrlPath = "$ServicesHost/distributions"
    private const val SnapshotDistributionUrlPath = "$DistributionUrlPath-snapshots"

    /** 7.0 or 7.0.1 or 7.0.1.2 */
    private val BaseVersionRegex = Regex(
      "(?<majorMinor>\\d+\\.\\d+)(\\.(" + // 7.0
        "(?<patch>\\d+)\\.(?<sweekt>\\d+)|" + // 1.2
        "(?<patchSingle>\\d+)" + // 1
        "))?",
      RegexOption.MULTILINE,
    )

    /** v7.0.1-RC1 */
    private val RCVersionRegex = Regex("-RC(\\d+)\$")

    /** v7.0.1-M1 */
    private val MilestoneVersionRegex = Regex("-M(\\d+)\$")

    /** distributionUrl=https\:xxx */
    private val DistributionUrlRegex = Regex(
      pattern = "([\\s\\S]*^distributionUrl=).*\$([\\s\\S]*)",
      option = RegexOption.MULTILINE,
    )
  }
}
