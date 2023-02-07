package com.meowool.sweekt.gradle.utils

import AnsiColors.yellowBright
import actions.core.InputOptions
import actions.core.endGroup
import actions.core.error
import actions.core.getInput
import actions.core.setFailed
import actions.core.startGroup

/**
 * Gets the value of an input.
 * Returns an empty string if the value is not defined.
 *
 * @param name Name of the input to get
 * @param required Whether the input is required. If required and not present, will throw.
 * @param trimWhitespace Whether leading/trailing whitespace will be trimmed for the input.
 */
fun getInput(
  name: String,
  required: Boolean = false,
  trimWhitespace: Boolean = true,
): String = getInput(
  name,
  object : InputOptions {
    override var required: Boolean? = required
    override var trimWhitespace: Boolean? = trimWhitespace
  },
)

/**
 * Wrap an asynchronous function call in a group.
 *
 * Returns the same type as the function itself.
 *
 * @param name The name of the group
 * @param action The function to wrap in the group
 */
inline fun <T> group(name: String, action: () -> T): T {
  log(yellowBright("------------------------------------------------"))
  startGroup(yellowBright(name))
  try {
    return action()
  } finally {
    endGroup()
    log(yellowBright("------------------------------------------------"))
  }
}

inline fun runAction(action: () -> Unit) {
  try {
    action()
  } catch (e: Throwable) {
    error(e.stackTraceToString())
    setFailed(e)
  }
}

fun error(throwable: Throwable) = error(throwable.stackTraceToString())
