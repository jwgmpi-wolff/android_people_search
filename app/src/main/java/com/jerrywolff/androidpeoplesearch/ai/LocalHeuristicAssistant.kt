package com.jerrywolff.androidpeoplesearch.ai

import com.jerrywolff.androidpeoplesearch.model.AiSummary
import com.jerrywolff.androidpeoplesearch.model.ConfidenceLevel
import com.jerrywolff.androidpeoplesearch.model.NormalizedName
import com.jerrywolff.androidpeoplesearch.model.PeopleSearchQuery
import com.jerrywolff.androidpeoplesearch.model.PersonSearchResult

class LocalHeuristicAssistant : AiAssistant {
    override suspend fun isAvailable() = true

    override suspend fun summarizeResults(results: List<PersonSearchResult>): AiSummary {
        val categories = results.groupingBy { it.category.label }.eachCount().entries.sortedByDescending { it.value }
        val high = results.count { it.confidence == ConfidenceLevel.HIGH }
        return AiSummary(
            headline = "${results.size} public search targets across ${categories.size} categories",
            summary = categories.joinToString(". ") { "${it.key}: ${it.value}" } + ". $high high-confidence target(s).",
            caveats = listOf("Targets are leads, not verified identity matches.", "Confirm details at each attributed source."),
        )
    }

    override suspend fun explainMatch(query: PeopleSearchQuery, result: PersonSearchResult): String =
        if (result.extractedSignals.isEmpty()) {
            "This is a broad public-source lead. No corroborating identity signal has been observed yet."
        } else {
            "The generated search includes ${result.extractedSignals.joinToString().lowercase()} signals from the query. Verify them at the source before drawing conclusions."
        }

    override suspend fun normalizeName(input: String): NormalizedName {
        val parts = input.trim().split(Regex("\\s+")).filter(String::isNotBlank)
        val display = parts.joinToString(" ") { it.lowercase().replaceFirstChar(Char::titlecase) }
        return NormalizedName(parts.firstOrNull().orEmpty(), parts.drop(1).joinToString(" "), display)
    }
}
