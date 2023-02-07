package com.meowool.sweekt.gradle.model

/**
 * @author chachako
 */
value class IntVersion(val number: Int) {
  constructor(version: String) : this(version.removePrefix("v").toInt())

  companion object {
    fun String.toIntVersion() = IntVersion(this).number
  }
}
