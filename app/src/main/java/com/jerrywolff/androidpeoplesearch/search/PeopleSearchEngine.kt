package com.jerrywolff.androidpeoplesearch.search

import com.jerrywolff.androidpeoplesearch.model.PeopleSearchQuery
import com.jerrywolff.androidpeoplesearch.model.PersonSearchResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.net.SocketTimeoutException

data class ProviderFailure(val providerName: String, val reason: String = "Unavailable")
data class SourceResultCount(
    val providerName: String,
    val recordCount: Int,
    val failed: Boolean,
    val failureReason: String? = null,
)

data class SearchReport(
    val results: List<PersonSearchResult>,
    val failures: List<ProviderFailure>,
    val sourceCounts: List<SourceResultCount>,
)

class PeopleSearchEngine(private val providers: List<SearchProvider> = ProviderCatalog.providers) {
    suspend fun search(query: PeopleSearchQuery, pageDepth: Int = 5): List<PersonSearchResult> =
        searchWithDiagnostics(query, pageDepth).results

    suspend fun searchWithDiagnostics(query: PeopleSearchQuery, pageDepth: Int = 5): SearchReport = coroutineScope {
        val outcomes = providers.filter { !it.requiresPayment && !it.requiresAuth }
            .map { provider ->
                async {
                    runCatching { provider.search(query, pageDepth.coerceIn(1, 5)) }
                        .fold(
                            onSuccess = { ProviderOutcome(provider.displayName, it, null) },
                            onFailure = {
                                val reason = it.userFacingReason()
                                ProviderOutcome(
                                    provider.displayName,
                                    emptyList(),
                                    ProviderFailure(provider.displayName, reason),
                                )
                            },
                        )
                }
            }
            .awaitAll()
        val deduplicatedOutcomes = outcomes.map { outcome ->
            outcome.copy(results = outcome.results.distinctBy { it.url })
        }
        SearchReport(
            results = deduplicatedOutcomes.flatMap { it.results }
                .distinctBy { it.url }
                .sortedWith(
                    compareByDescending<PersonSearchResult> { it.isDirectResult }
                        .thenByDescending { it.confidenceScore }
                        .thenBy { it.sourceName },
                ),
            failures = deduplicatedOutcomes.mapNotNull { it.failure },
            sourceCounts = deduplicatedOutcomes.map { outcome ->
                SourceResultCount(
                    outcome.providerName,
                    outcome.results.size,
                    outcome.failure != null,
                    outcome.failure?.reason,
                )
            },
        )
    }

    private data class ProviderOutcome(
        val providerName: String,
        val results: List<PersonSearchResult>,
        val failure: ProviderFailure?,
    )
}

private fun Throwable.userFacingReason(): String = when {
    this is PublicApiHttpException && statusCode == 429 -> "Rate limited"
    this is PublicApiHttpException && statusCode in 500..599 -> "Source unavailable"
    this is SocketTimeoutException -> "Timed out"
    else -> "Unavailable"
}
