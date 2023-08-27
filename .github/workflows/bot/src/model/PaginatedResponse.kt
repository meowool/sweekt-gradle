package com.meowool.sweekt.gradle.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

/**
 * @author chachako
 */
data class PaginatedResponse<T>(
  private val data: List<T>,
  val incompleteResults: Boolean?,
  val totalCount: Int?,
) : List<T> by data {
  companion object {
    inline operator fun <reified T> invoke(
      json: Json,
      elements: List<JsonElement>,
      incompleteResults: Boolean? = null,
      totalCount: Int? = null,
    ) = PaginatedResponse<T>(
      elements.map { json.decodeFromJsonElement(it.jsonObject) },
      incompleteResults,
      totalCount,
    )

    fun <T> Flow<PaginatedResponse<T>>.flatten(): Flow<T> = channelFlow {
      this@flatten.collect {
        it.data.forEach { data -> send(data) }
      }
    }
  }
}
