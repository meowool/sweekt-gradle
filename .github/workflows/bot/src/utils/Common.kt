@file:Suppress(
  "INVISIBLE_MEMBER",
  "INVISIBLE_REFERENCE",
  "NOTHING_TO_INLINE",
  "unused",
)

package com.meowool.sweekt.gradle.utils

import AnsiColors.greenBright
import actions.core.debug
import actions.core.infoImpl
import io.ktor.client.engine.js.JsError
import io.ktor.client.engine.js.compatibility.commonFetch
import io.ktor.client.fetch.RequestInit
import js.core.Record
import js.core.get
import js.core.set
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.onCompletion
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import node.process.process
import org.w3c.fetch.Response
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun env(key: String) = process.env[key]
  ?: error("Missing environment variable: $key")

fun envOrNull(key: String) = process.env[key]

inline fun <T : R, R> T?.ifNull(block: () -> R): R = this ?: block()

inline fun info(message: String) = infoImpl(greenBright(message))

inline fun log(message: String) = infoImpl(message)

inline fun <T> T.debug(name: String): T =
  also { actions.core.debug("$name: $it") }

inline fun <T> debug(name: String, value: () -> T): T {
  debug("-----------------------------------")
  debug("📝 $name")
  debug("-----------------------------------")
  return value()
}

inline fun <T> withDebug(name: String, value: () -> T): T {
  val result = debug(name, value)
  debug("   return: $result")
  debug("-----------------------------------")
  return result
}

suspend fun retry(
  name: String,
  delay: Duration = 3.seconds,
  max: Int = 5,
  action: suspend (Int) -> Unit,
) {
  var tryCount = 0
  while (true) {
    if (tryCount++ > max) break
    try {
      group("🔁 Start $tryCount attempt: $name") {
        action(tryCount)
      }
      break
    } catch (e: Throwable) {
      if (tryCount == max) throw e
      delay(delay)
    }
  }
}

suspend fun nodeFetch(
  url: String,
  method: String,
  body: dynamic,
  headers: Map<String, String>? = null,
): Response {
  val init = Record<String, dynamic> {
    set("method", method)
    set("body", body)
    headers?.apply {
      set(
        key = "headers",
        value = Record {
          forEach { (key, value) -> set(key, value) }
        },
      )
    }
  }
  return commonFetch(url, init.unsafeCast<RequestInit>()).also {
    log("nodeFetch: $url: " + it.statusText)
    @Suppress("UnsafeCastFromDynamic")
    it.body.on("error") { error -> throw JsError(error) }
  }
}

fun <T> Flow<T>.onSuccess(
  action: suspend FlowCollector<T>.() -> Unit,
): Flow<T> = onCompletion { if (it == null) action() }

fun jsonMapOf(vararg pairs: Pair<String, Any?>) = buildJsonObject {
  pairs.forEach { (key, value) -> put(key, value.toJsonElement()) }
}

private fun Any?.toJsonElement(): JsonElement = when (val unknown = this) {
  null -> JsonNull
  is Map<*, *> -> buildJsonObject {
    unknown.forEach { (key, value) ->
      put(
        key = key as? String ?: return@forEach,
        element = value.toJsonElement(),
      )
    }
  }
  is List<*> -> buildJsonArray {
    unknown.forEach {
      add(it.toJsonElement())
    }
  }
  is String -> JsonPrimitive(unknown)
  is Boolean -> JsonPrimitive(unknown)
  is Number -> JsonPrimitive(unknown)
  is Enum<*> -> JsonPrimitive(unknown.toString())
  else -> error("Can't serialize unknown type: $unknown")
}
