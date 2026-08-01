package com.jerrywolff.androidpeoplesearch.ai

import com.jerrywolff.androidpeoplesearch.model.AiSummary
import com.jerrywolff.androidpeoplesearch.model.NormalizedName
import com.jerrywolff.androidpeoplesearch.model.PeopleSearchQuery
import com.jerrywolff.androidpeoplesearch.model.PersonSearchResult

class MlKitGeminiNanoAssistant(
    private val fallback: AiAssistant = LocalHeuristicAssistant(),
) : AiAssistant {
    // TODO: Wire ML Kit GenAI Prompt API after adding a supported-device dependency matrix.
    // Keeping this adapter dependency-free ensures unsupported devices always build and work.
    override suspend fun isAvailable(): Boolean = false
    override suspend fun summarizeResults(results: List<PersonSearchResult>): AiSummary = fallback.summarizeResults(results)
    override suspend fun explainMatch(query: PeopleSearchQuery, result: PersonSearchResult): String = fallback.explainMatch(query, result)
    override suspend fun normalizeName(input: String): NormalizedName = fallback.normalizeName(input)
}
