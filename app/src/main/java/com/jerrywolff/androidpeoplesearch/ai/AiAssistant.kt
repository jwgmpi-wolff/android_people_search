package com.jerrywolff.androidpeoplesearch.ai

import com.jerrywolff.androidpeoplesearch.model.AiSummary
import com.jerrywolff.androidpeoplesearch.model.NormalizedName
import com.jerrywolff.androidpeoplesearch.model.PeopleSearchQuery
import com.jerrywolff.androidpeoplesearch.model.PersonSearchResult

data class SearchRefinement(
    val query: PeopleSearchQuery,
    val reply: String,
    val appliedFields: List<String>,
)

interface AiAssistant {
    suspend fun isAvailable(): Boolean
    suspend fun summarizeResults(results: List<PersonSearchResult>): AiSummary
    suspend fun explainMatch(query: PeopleSearchQuery, result: PersonSearchResult): String
    suspend fun normalizeName(input: String): NormalizedName
    suspend fun refineSearchQuery(query: PeopleSearchQuery, input: String): SearchRefinement
}
