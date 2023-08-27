@file:Suppress("MemberVisibilityCanBePrivate")

package com.meowool.sweekt.gradle.model

import com.meowool.sweekt.gradle.utils.withDebug

/**
 * @author chachako
 */
data class GradleVersion(
  val baseVersion: BasePart,
  val rcNumber: Int?,
  val milestoneNumber: Int?,
) {
  val fullVersion by lazy { toFullVersion() }
  val fullSweektVersion by lazy { toFullVersion(sweekt = true) }

  private fun toFullVersion(sweekt: Boolean = false) = buildString {
    append(if (sweekt) baseVersion.toSweekt() else baseVersion.toSimple())
    when {
      rcNumber != null -> append("-rc-$rcNumber")
      milestoneNumber != null -> append("-milestone-$milestoneNumber")
    }
  }

  data class BasePart(
    private val majorMinor: String,
    private val patch: Int?,
    val sweekt: Int? = null,
  ) {
    constructor(majorMinor: String, patch: String?, sweekt: String?) : this(
      majorMinor = majorMinor,
      patch = patch?.toIntOrNull(),
      sweekt = sweekt?.toIntOrNull(),
    )

    /**
     * Returns a simple base version of Gradle.
     *
     * This is consistent with the name of the official Gradle wrapper zip,
     * which will be omitted when the patch version is `0`.
     *
     * For example, `6.8.0` will be returned as `6.8`.
     */
    fun toSimple() = withDebug("toSimple") {
      buildString {
        append(majorMinor)
        if (patch != null && patch != 0) append(".$patch")
      }
    }

    /**
     * Returns a normalized base version of Gradle.
     * Make sure that the version has a patch version.
     *
     * For example, '7.0' will be converted to '7.0.0'.
     */
    fun toNormalized() = "$majorMinor.${patch ?: 0}"

    /**
     * Returns a normalized base version of Gradle with a sweekt version.
     *
     * For example, '7.0.0.1'.
     */
    fun toSweekt() = withDebug("toSweekt") {
      buildString {
        append(toNormalized())
        requireNotNull(sweekt) { "Cannot convert to the sweekt's base version" }
        append(".$sweekt")
      }
    }
  }
}
