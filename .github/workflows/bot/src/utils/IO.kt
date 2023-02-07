package com.meowool.sweekt.gradle.utils

import js.core.Record
import js.core.set
import node.buffer.BufferEncoding
import node.fs.CopyOptions
import node.fs.PathLike
import node.fs.cp
import node.fs.readFile
import node.fs.realpathSync
import node.fs.rename
import node.fs.writeFile
import node.os.tmpdir
import node.path.path

val temporaryDirectory: String get() = realpathSync(tmpdir())

operator fun PathLike.div(joinPath: String) = path.join(this, joinPath)

suspend fun PathLike.readFile() = readFile(this, BufferEncoding.utf8)
suspend fun PathLike.writeFile(text: String) = writeFile(this, text)
suspend fun PathLike.renameTo(newPath: PathLike) = rename(this, newPath)
suspend fun PathLike.readFileBuffer() = readFile(this)

suspend fun copyDirectory(source: PathLike, destination: PathLike) =
  debug("copyDirectory") {
    cp(
      source,
      destination,
      opts = Record { set("recursive", true) }.unsafeCast<CopyOptions>(),
    )
  }

/**
 * Executes the [action] and ensure that the [file] is not modified during
 * this process.
 *
 * @return Return `true` if the [file] was modified while executing [action].
 */
suspend inline fun preserveFile(
  file: String,
  action: (oldContent: String) -> Unit,
): Boolean {
  val oldContent = file.readFile()
  try {
    action(oldContent)
  } finally {
    val newContent = file.readFile()
    val modified = oldContent != newContent
    if (modified) file.writeFile(oldContent)
    return modified
  }
}
