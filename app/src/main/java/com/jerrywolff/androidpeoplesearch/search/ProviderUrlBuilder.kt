package com.jerrywolff.androidpeoplesearch.search

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object ProviderUrlBuilder {
    fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.toString()).replace("+", "%20")
    fun query(baseUrl: String, parameter: String, value: String): String = "$baseUrl?$parameter=${encode(value)}"
    fun path(baseUrl: String, value: String): String = "${baseUrl.trimEnd('/')}/${encode(value)}"
}
