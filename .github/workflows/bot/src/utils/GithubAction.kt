@file:Suppress("NOTHING_TO_INLINE")

package com.meowool.sweekt.gradle.utils

import com.github.ajalt.mordant.rendering.TextColors.brightGreen
import com.github.ajalt.mordant.rendering.TextColors.brightYellow
import kotlin.system.exitProcess

private fun escapeData(data: String) = data
  .replace("%", "%25")
  .replace("\r", "%0D")
  .replace("\n", "%0A")

fun isDebug() = System.getenv("RUNNER_DEBUG") == "1"

fun info(message: String) = println(brightGreen(message))

fun debug(message: String) = println("::debug::${escapeData(message)}")

fun <T> T.debug(name: String): T = also { debug(message = "$name: $it") }

fun warning(message: String) = println("::warning::${escapeData(message)}")

fun error(message: String) = println("::error::${escapeData(message)}")

fun error(throwable: Throwable) = error(throwable.stackTraceToString())

fun startGroup(name: String) = println("::group::${escapeData(name)}")

fun endGroup() = println("::endgroup::")

/**
 * Wrap an asynchronous function call in a group.
 *
 * Returns the same type as the function itself.
 *
 * @param name The name of the group
 * @param action The function to wrap in the group
 */
inline fun <T> group(name: String, action: () -> T): T {
  println(brightYellow("------------------------------------------------"))
  startGroup(brightYellow(name))
  try {
    return action()
  } finally {
    endGroup()
    println(brightYellow("------------------------------------------------"))
  }
}

inline fun runAction(action: () -> Unit) {
  try {
    action()
  } catch (e: Throwable) {
    error(e)
    exitProcess(1)
  }
}
