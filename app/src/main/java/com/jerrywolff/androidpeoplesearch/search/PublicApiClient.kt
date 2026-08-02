package com.jerrywolff.androidpeoplesearch.search

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

fun interface PublicApiClient {
    suspend fun get(url: String): String
}

class PublicApiHttpException(val statusCode: Int) :
    IllegalStateException("Public API returned HTTP $statusCode")

class HttpPublicApiClient : PublicApiClient {
    override suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 8_000
            connection.readTimeout = 10_000
            connection.useCaches = false
            connection.defaultUseCaches = false
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Cache-Control", "no-cache, no-store, max-age=0")
            connection.setRequestProperty("Pragma", "no-cache")
            connection.setRequestProperty("User-Agent", "AndroidPeopleSearch/1.0 (public-source research app)")
            if (connection.responseCode !in 200..299) {
                throw PublicApiHttpException(connection.responseCode)
            }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
