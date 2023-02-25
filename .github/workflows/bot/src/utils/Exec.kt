@file:Suppress("unused")

package com.meowool.sweekt.gradle.utils

import com.github.ajalt.mordant.rendering.TextColors.brightBlue
import com.github.ajalt.mordant.rendering.TextColors.brightYellow
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.yellow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.PrintStream

data class ExecOutput(
  val exitCode: Int,
  val stdout: String,
  val stderr: String,
)

class ExecException(
  val command: List<String>,
  val exitCode: Int,
  val stdout: String? = null,
  val stderr: String? = null,
) : IllegalStateException(buildString {
  append("Failed to execute command: ")
  appendLine(brightBlue(command.joinToString(separator = " ", prefix = "$ ")))
  appendLine()
  append(brightYellow("Exit code: "))
  appendLine(brightYellow(exitCode.toString()))
  stdout?.apply {
    appendLine()
    appendLine(brightYellow("Standard error: "))
    appendLine(lines().joinToString("\n") { yellow(it) })
  }
}) {
  val log: String? = listOfNotNull(stdout, stderr)
    .joinToString("\n").trim()
    .takeIf { it.isNotBlank() }
}

@Suppress("BlockingMethodInNonBlockingContext")
suspend fun exec(vararg command: Any?): ExecOutput = withContext(IO) {
  coroutineScope {
    fun InputStream.printToString(printStream: PrintStream) = bufferedReader()
      .lineSequence().onEach(printStream::println).joinToString("\n")

    val commandList = command.mapNotNull { it?.toString() }

    val process = ProcessBuilder(commandList).start()

    println(cyan(commandList.joinToString(" ")))

    try {
      val stdout = async { process.inputStream.printToString(System.out) }
      val stderr = async { process.errorStream.printToString(System.err) }
      val exitCode = runInterruptible { process.waitFor() }

      val stdoutString = stdout.await()
      val stderrString = stderr.await()

      if (exitCode != 0) throw ExecException(
        command = commandList,
        exitCode = exitCode,
        stdout = stdoutString,
        stderr = stderrString,
      )

      ExecOutput(
        exitCode = exitCode,
        stdout = stdoutString,
        stderr = stderrString,
      )
    } catch (e: CancellationException) {
      process.destroy()
      throw e
    }
  }
}

suspend fun exec(commandLine: String, args: List<String?>): ExecOutput =
  exec(commandLine, *args.toTypedArray())
