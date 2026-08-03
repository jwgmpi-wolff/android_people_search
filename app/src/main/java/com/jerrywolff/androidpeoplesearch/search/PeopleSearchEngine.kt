package com.jerrywolff.androidpeoplesearch.search

import com.jerrywolff.androidpeoplesearch.model.PeopleSearchQuery
import com.jerrywolff.androidpeoplesearch.model.PersonSearchResult
import com.jerrywolff.androidpeoplesearch.model.SocialPlatform
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.net.URI
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

    suspend fun searchWithDiagnostics(
        query: PeopleSearchQuery,
        pageDepth: Int = 5,
        breadthLevel: Int = 0,
    ): SearchReport = coroutineScope {
        val eligibleProviders = providers.filter {
            !it.requiresPayment && !it.requiresAuth && it.isRequestedBy(query)
        }
        suspend fun execute(selectedProviders: List<SearchProvider>, breadth: Int) =
            selectedProviders.map { provider ->
                async {
                    runCatching { provider.search(query, pageDepth.coerceIn(1, 5), breadth) }
                        .fold(
                            onSuccess = { ProviderOutcome(provider.id, provider.displayName, it, null) },
                            onFailure = {
                                val reason = it.userFacingReason()
                                ProviderOutcome(
                                    provider.id,
                                    provider.displayName,
                                    emptyList(),
                                    ProviderFailure(provider.displayName, reason),
                                )
                            },
                        )
                }
            }
            .awaitAll()

        fun filter(outcomes: List<ProviderOutcome>) = outcomes.map { outcome ->
            outcome.copy(
                results = outcome.results
                    .filter { it.matchesRequestedIdentity(query) }
                    .filter { it.isRequestedBy(query) }
                    .distinctBy { it.url },
            )
        }

        val initialOutcomes = filter(execute(eligibleProviders, breadthLevel.coerceAtLeast(0)))
        val emptyGeneralProviderIds = initialOutcomes
            .filter { it.providerId in GENERAL_INDEX_PROVIDER_IDS && it.results.isEmpty() }
            .mapTo(mutableSetOf()) { it.providerId }
        val fallbackProviders = eligibleProviders.filter { it.id in emptyGeneralProviderIds }
        val fallbackOutcomes = if (
            breadthLevel < 1 &&
            fallbackProviders.isNotEmpty()
        ) {
            filter(execute(fallbackProviders, breadth = 1))
        } else {
            emptyList()
        }
        val fallbackByProvider = fallbackOutcomes.associateBy { it.providerId }
        val deduplicatedOutcomes = initialOutcomes.map { fallbackByProvider[it.providerId] ?: it }

        SearchReport(
            results = deduplicatedOutcomes.flatMap { it.results }
                .distinctBy { it.url }
                .sortedWith(
                    compareBy<PersonSearchResult> { it.isSupplementalDeveloperSource() }
                        .thenByDescending { it.isDirectResult }
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
        val providerId: String,
        val providerName: String,
        val results: List<PersonSearchResult>,
        val failure: ProviderFailure?,
    )
}

private val GENERAL_INDEX_PROVIDER_IDS = setOf("bing_web", "bing_linkedin", "duckduckgo_web")

private fun SearchProvider.isRequestedBy(query: PeopleSearchQuery): Boolean = when (id) {
    "github" -> query.requestsGitHub()
    "stackoverflow" -> query.requestsStackOverflow()
    "bing_linkedin" -> SocialPlatform.LINKEDIN in query.socialPlatforms
    "bluesky" -> SocialPlatform.BLUESKY in query.socialPlatforms
    "selected_social_references" -> query.socialPlatforms.isNotEmpty()
    else -> true
}

private fun PersonSearchResult.isRequestedBy(query: PeopleSearchQuery): Boolean = runCatching {
    val host = URI(url).host.orEmpty().lowercase().removePrefix("www.")
    val socialPlatform = SocialPlatform.entries.firstOrNull { it.matchesHost(host) }
    if (socialPlatform != null) return@runCatching socialPlatform in query.socialPlatforms
    when (host) {
        "github.com" -> query.requestsGitHub()
        "stackoverflow.com" -> query.requestsStackOverflow()
        else -> true
    }
}.getOrDefault(true)

private fun PeopleSearchQuery.requestsGitHub(): Boolean =
    Regex("\\bgithub\\b", RegexOption.IGNORE_CASE).containsMatchIn(freeText)

private fun PeopleSearchQuery.requestsStackOverflow(): Boolean =
    Regex("\\bstack\\s*overflow\\b", RegexOption.IGNORE_CASE).containsMatchIn(freeText)

private fun PersonSearchResult.isSupplementalDeveloperSource(): Boolean =
    providerId == "github" || providerId == "stackoverflow" || runCatching {
        val host = URI(url).host.orEmpty().lowercase().removePrefix("www.")
        host == "github.com" || host.endsWith(".github.com") ||
            host == "stackoverflow.com" || host.endsWith(".stackoverflow.com")
    }.getOrDefault(false)

private fun Throwable.userFacingReason(): String = when {
    this is PublicApiHttpException && statusCode == 429 -> "Rate limited"
    this is PublicApiHttpException && statusCode in 500..599 -> "Source unavailable"
    this is SocketTimeoutException -> "Timed out"
    else -> "Unavailable"
}

private fun PersonSearchResult.matchesRequestedIdentity(query: PeopleSearchQuery): Boolean {
    val normalized = query.normalized()
    val searchableText = "$title $description $url".lowercase()
    val searchableTokens = searchableText.fieldTokens()
    val searchableCompact = searchableText.filter(Char::isLetterOrDigit)

    val nameTokens = normalized.personName().fieldTokens()
    if (nameTokens.isNotEmpty()) {
        return nameTokens.all(searchableTokens::contains) ||
            searchableCompact.contains(nameTokens.joinToString(""))
    }
    val username = normalized.username.filter(Char::isLetterOrDigit)
    return username.isBlank() || searchableCompact.contains(username)
}

private fun String.fieldTokens(): Set<String> =
    lowercase().split(Regex("[^a-z0-9]+"))
        .filter { it.length > 1 }
        .toSet()

internal fun String.usStateAbbreviation(): String? = US_STATE_ABBREVIATIONS[lowercase()]

private val US_STATE_ABBREVIATIONS = """
    Alabama=AL|Alaska=AK|Arizona=AZ|Arkansas=AR|California=CA|Colorado=CO|Connecticut=CT|Delaware=DE|
    District of Columbia=DC|Florida=FL|Georgia=GA|Hawaii=HI|Idaho=ID|Illinois=IL|Indiana=IN|Iowa=IA|
    Kansas=KS|Kentucky=KY|Louisiana=LA|Maine=ME|Maryland=MD|Massachusetts=MA|Michigan=MI|Minnesota=MN|
    Mississippi=MS|Missouri=MO|Montana=MT|Nebraska=NE|Nevada=NV|New Hampshire=NH|New Jersey=NJ|
    New Mexico=NM|New York=NY|North Carolina=NC|North Dakota=ND|Ohio=OH|Oklahoma=OK|Oregon=OR|
    Pennsylvania=PA|Rhode Island=RI|South Carolina=SC|South Dakota=SD|Tennessee=TN|Texas=TX|Utah=UT|
    Vermont=VT|Virginia=VA|Washington=WA|West Virginia=WV|Wisconsin=WI|Wyoming=WY
""".trimIndent().replace("\n", "").split('|').associate { entry ->
    val (name, abbreviation) = entry.split('=', limit = 2)
    name.lowercase() to abbreviation
}
