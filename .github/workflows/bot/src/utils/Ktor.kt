package com.meowool.sweekt.gradle.utils

import com.meowool.sweekt.gradle.model.PaginatedResponse
import com.meowool.sweekt.gradle.model.PaginatedResponse.Companion.flatten
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.koin.core.context.GlobalContext.get as getKoin

inline fun <reified T> HttpClient.githubPaginate(
  urlString: String,
  json: Json = getKoin().get(),
  crossinline block: HttpRequestBuilder.() -> Unit = {},
): Flow<PaginatedResponse<T>> = channelFlow {
  var url: Url? = URLBuilder(urlString).apply {
    parameters.append("per_page", "100")
    parameters.append("page", "1")
  }.build()

  while (url != null) {
    val response = get(url, block)
    val nextLink = response.headers["Link"]?.let {
      Regex("<([^>]+)>;\\s*rel=\"next\"").find(it)?.groupValues?.get(1)
    }
    url = nextLink?.let(::Url)

    // Normalize response
    val result: PaginatedResponse<T> = when (
      val element = json.parseToJsonElement(response.bodyAsText())
    ) {
      is JsonArray -> PaginatedResponse(json, element)
      is JsonObject -> PaginatedResponse(
        json = json,
        elements = element.filterKeys {
          it != "total_count" &&
            it != "incomplete_results" &&
            it != "repository_selection"
        }.values.first().jsonArray,
        incompleteResults = element["incomplete_results"]
          ?.jsonPrimitive
          ?.booleanOrNull,
        totalCount = element["total_count"]
          ?.jsonPrimitive
          ?.intOrNull,
      )
      else -> kotlin.error("Unknown response type: $element")
    }

    send(result)
  }
}.flowOn(Dispatchers.Default)

inline fun <reified T> HttpClient.githubFlatPaginate(
  urlString: String,
  json: Json = getKoin().get(),
  crossinline block: HttpRequestBuilder.() -> Unit = {},
): Flow<T> = githubPaginate<T>(urlString, json, block).flatten()
