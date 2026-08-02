package com.jerrywolff.androidpeoplesearch.search

import android.net.Uri
import com.jerrywolff.androidpeoplesearch.model.PeopleSearchQuery
import com.jerrywolff.androidpeoplesearch.model.PersonSearchResult
import com.jerrywolff.androidpeoplesearch.model.SourceCategory

class UrlSearchProvider(
    override val id: String,
    override val displayName: String,
    override val category: SourceCategory,
    override val requiresAuth: Boolean = false,
    override val lastVerified: String = "2026-08-01",
    private val urlFactory: (PeopleSearchQuery) -> String,
) : SearchProvider {
    override val requiresPayment: Boolean = false

    internal fun buildSearchUrlString(query: PeopleSearchQuery): String = urlFactory(query.normalized())

    override fun buildSearchUrl(query: PeopleSearchQuery): Uri = Uri.parse(buildSearchUrlString(query))

    override suspend fun search(query: PeopleSearchQuery): List<PersonSearchResult> {
        val url = buildSearchUrlString(query)
        val score = ConfidenceScorer.score(query, category, "${query.searchText()} $displayName")
        return listOf(
            PersonSearchResult(
                id = id, providerId = id, title = "Search $displayName", description = description(),
                url = url, sourceName = displayName, category = category, confidence = score.level,
                confidenceScore = score.score, extractedSignals = score.signals,
            ),
        )
    }

    private fun description(): String = when (category) {
        SourceCategory.PUBLIC_RECORDS -> "Open the official or public search target. Availability and coverage vary by jurisdiction."
        SourceCategory.SOCIAL, SourceCategory.USERNAME -> "Search public profile pages; no account or authenticated scraping is used."
        else -> "Open a free public search target in your browser."
    }
}
