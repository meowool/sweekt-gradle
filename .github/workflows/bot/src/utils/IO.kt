package com.meowool.sweekt.gradle.utils

import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.copyToRecursively
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes

@OptIn(ExperimentalPathApi::class)
fun Path.copyToRecursively(
  target: Path,
  overwrite: Boolean = true
): Path = copyToRecursively(target, overwrite = overwrite, followLinks = true)

/**
 * Executes the [action] and ensure that the [file] is not modified during
 * this process.
 *
 * @return Return `true` if the [file] was modified while executing [action].
 */
inline fun preserveFile(
  file: Path,
  action: () -> Unit,
): Boolean {
  var modified = false
  val oldContent = file.readBytes()
  try {
    action()
  } finally {
    val newContent = file.readBytes()
    if (!oldContent.contentEquals(newContent)) {
      file.writeBytes(oldContent)
      modified = true
    }
  }
  return modified
}
