package com.meowool.sweekt.gradle.module

import actions.core.isDebug
import com.meowool.sweekt.gradle.model.Context
import com.meowool.sweekt.gradle.service.GithubRepositoryService
import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.DefaultJson
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module

val NetworkModule get() = module {
  single {
    Json(DefaultJson) {
      ignoreUnknownKeys = true
      isLenient = true
    }
  }
  factory {
    HttpClient(Js) {
      install(Logging) {
        level = if (isDebug()) LogLevel.BODY else LogLevel.INFO
      }
      install(ContentNegotiation) {
        json(get())
        expectSuccess = true
      }
    }
  }
  single(named<GithubRepositoryService>()) {
    get<HttpClient>().config {
      defaultRequest {
        get<Context>().headers.forEach { (key, value) ->
          header(key, value)
        }
      }
    }
  }
}
