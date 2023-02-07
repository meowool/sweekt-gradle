@file:Suppress("unused")

package com.meowool.sweekt.gradle.utils

import StringDecoder
import actions.exec.ExecListeners
import actions.exec.ExecOptions
import actions.exec.ExecOutput
import actions.exec.exec
import kotlinx.coroutines.await
import node.buffer.Buffer

private class ExecOutputImpl(
  override var exitCode: Number,
  override var stdout: String,
  override var stderr: String,
) : ExecOutput

class ExecException(
  cause: Throwable,
  commandLine: String,
  args: Array<String>?,
  var exitCode: Number? = null,
  val stdout: String? = null,
  val stderr: String? = null,
) : IllegalStateException(cause) {
  val statement = commandLine + args?.joinToString(
    separator = " ",
    prefix = " ",
  ).orEmpty()

  val log: String? = listOfNotNull(stdout, stderr)
    .joinToString("\n").trim()
    .takeIf { it.isNotBlank() }
}

suspend fun exec(
  commandLine: String,
  vararg args: Any?,
): ExecOutput {
  val arguments = args.takeIf { it.isNotEmpty() }
    ?.mapNotNull { it?.toString() }
    ?.toTypedArray()

  var stdout = ""
  var stderr = ""
  val stdoutDecoder = StringDecoder("utf8")
  val stderrDecoder = StringDecoder("utf8")
  val options = object : ExecOptions {
    override var listeners: ExecListeners? = object : ExecListeners {
      override var stdout = fun(data: Buffer) {
        stdout += stdoutDecoder.write(data)
      }
      override var stderr = fun(data: Buffer) {
        stderr += stderrDecoder.write(data)
      }
    }
  }

  try {
    val exitCode = try {
      when (arguments) {
        null -> exec(commandLine, options = options)
        else -> exec(commandLine, arguments, options)
      }.await()
    } finally {
      // Flush any remaining characters
      stdout += stdoutDecoder.end()
      stderr += stderrDecoder.end()
    }
    return ExecOutputImpl(exitCode, stdout, stderr)
  } catch (e: Throwable) {
    throw ExecException(
      cause = e,
      commandLine = commandLine,
      args = arguments,
      stdout = stdout,
      stderr = stderr,
    )
  }
}

suspend fun exec(commandLine: String, args: List<String?>): ExecOutput =
  exec(commandLine, *args.toTypedArray())
