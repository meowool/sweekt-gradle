package com.meowool.sweekt.gradle.model

import com.meowool.sweekt.gradle.utils.ExecException

/**
 * @author chachako
 */
class BotIssueBodyTemplate(val context: Context) {
  val sections = mutableListOf<Pair<String, String>>()
  lateinit var stepsToReproduce: String
  lateinit var related: Any
  lateinit var errors: Any

  override fun toString(): String = buildString {
    sections.forEach { (title, content) ->
      appendLine("## $title")
      appendLine(content)
      appendLine()
    }
    if (::stepsToReproduce.isInitialized) {
      appendLine("## Steps to reproduce")
      appendLine(stepsToReproduce)
      appendLine()
    }
    when (val e = errors) {
      is Map<*, *> -> {
        appendLine("## Errors")
        e.forEach { (key, value) ->
          val title = key.toString().replace(
            regex = Regex("`(.+?)`"),
            replacement = "<code>$1</code>",
          )
          val content = parseError(value)
          appendLine("<details><summary>$title</summary>")
          appendLine()
          appendLine(content)
          appendLine()
          appendLine("</details>")
        }
      }
      else -> {
        appendLine("## Error")
        appendLine(parseError(e))
      }
    }
    appendLine()
    appendLine(
      "> View the full workflow running log in ${context.workflowRunUrl}",
    )
    appendLine()
    appendLine("<!-- related: $related -->")
  }

  private fun parseError(error: Any?) = when (error) {
    is ExecException -> buildString {
      appendLine("```console")
      append("$ ").appendLine(error.command.joinToString(" "))
      appendLine().appendLine(error.log)
      appendLine("```")
    }
    else -> buildString {
      appendLine("```")
      appendLine((error as? Throwable)?.stackTraceToString() ?: error)
      appendLine("```")
    }
  }

  companion object {
    inline fun createBotIssueBodyTemplate(
      context: Context,
      block: BotIssueBodyTemplate.() -> Unit,
    ) = BotIssueBodyTemplate(context).apply(block)

    inline fun BotIssueBodyTemplate.buildIssueBody(
      block: BotIssueBodyTemplate.() -> Unit,
    ) = apply(block).toString()
  }
}
