package com.jerrywolff.androidpeoplesearch.search

import com.jerrywolff.androidpeoplesearch.model.PeopleSearchQuery
import com.jerrywolff.androidpeoplesearch.model.PersonSearchResult
import com.jerrywolff.androidpeoplesearch.model.SourceCategory

class PeopleSearchEngine(private val providers: List<SearchProvider> = ProviderCatalog.providers) {
    suspend fun search(query: PeopleSearchQuery, enabled: Set<SourceCategory>): List<PersonSearchResult> {
        val results = mutableListOf<PersonSearchResult>()
        providers.filter { !it.requiresPayment && !it.requiresAuth && it.category in enabled }
            .forEach { provider -> results += provider.search(query) }
        return results.distinctBy { it.url }
            .sortedWith(compareByDescending<PersonSearchResult> { it.confidenceScore }.thenBy { it.sourceName })
    }
}
