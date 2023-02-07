package com.meowool.sweekt.gradle.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * @author chachako
 */
@Serializable
data class GithubIssue(
  val number: Int,
  val title: String,
  val body: String,
  val state: State,
  val labels: List<Label>,
  @SerialName("html_url") val url: String,
) {
  @Serializable(with = State.Companion::class)
  enum class State {
    Open,
    Closed,
    All;

    override fun toString(): String = name.lowercase()

    companion object : KSerializer<State> {
      override val descriptor = String.serializer().descriptor
      override fun deserialize(decoder: Decoder) = valueOf(
        decoder.decodeString().replaceFirstChar(Char::titlecase),
      )
      override fun serialize(encoder: Encoder, value: State) =
        encoder.encodeString(value.toString())
    }

    enum class Reason(private val str: String) {
      Completed("completed"),
      NotPlanned("not_planned"),
      ReOpened("reopened");

      override fun toString(): String = str
    }
  }

  @Serializable
  class Label(val name: String)
}
