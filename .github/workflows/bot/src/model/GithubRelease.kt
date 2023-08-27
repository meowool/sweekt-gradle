package com.meowool.sweekt.gradle.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @author chachako
 */
@Serializable
data class GithubRelease(
  @SerialName("tag_name") val tag: String,
  @SerialName("upload_url") val uploadUrl: String,
  @SerialName("html_url") val htmlUrl: String,
  val assets: List<Asset>,
) {
  val assetNames: List<String> get() = assets.map { it.name }

  @Serializable
  data class Asset(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
  )
}
