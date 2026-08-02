package com.jerrywolff.androidpeoplesearch.ai

import com.jerrywolff.androidpeoplesearch.model.ConfidenceLevel
import com.jerrywolff.androidpeoplesearch.model.PeopleSearchQuery
import com.jerrywolff.androidpeoplesearch.model.PersonSearchResult
import com.jerrywolff.androidpeoplesearch.model.SourceCategory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalHeuristicAssistantTest {
    @Test
    fun summarizeResultsReviewsEveryDisplayedRecord() = runBlocking {
        val results = listOf(
            result("github:1", "GitHub public users", ConfidenceLevel.LOW),
            result("github:2", "GitHub public users", ConfidenceLevel.MEDIUM),
            result("bluesky:1", "Bluesky public profiles", ConfidenceLevel.HIGH),
        )

        val review = LocalHeuristicAssistant().summarizeResults(results)

        assertEquals("Reviewed all 3 displayed record(s)", review.headline)
        assertTrue(review.summary.contains("GitHub public users: 2"))
        assertTrue(review.summary.contains("Bluesky public profiles: 1"))
        assertTrue(review.summary.contains("Confidence: 1 high, 1 medium, 1 low"))
    }

    @Test
    fun summarizeResultsReviewsEmptyResultSet() = runBlocking {
        val review = LocalHeuristicAssistant().summarizeResults(emptyList())

        assertEquals("Reviewed all 0 displayed record(s)", review.headline)
        assertTrue(review.summary.contains("No matching records returned"))
    }

    @Test
    fun refineSearchQuery_appliesSpecificStructuredDetails() = runBlocking {
        val refinement = LocalHeuristicAssistant().refineSearchQuery(
            PeopleSearchQuery(fullName = "Alexis Wolff"),
            "Lives in Seattle, King County, United States and works at Contoso",
        )

        assertEquals("Seattle", refinement.query.city)
        assertEquals("King County", refinement.query.county)
        assertEquals("United States", refinement.query.country)
        assertEquals("Contoso", refinement.query.company)
        assertTrue(refinement.appliedFields.containsAll(listOf("city", "county", "country", "company")))
    }

    @Test
    fun refineSearchQuery_preservesUnrecognizedContext() = runBlocking {
        val refinement = LocalHeuristicAssistant().refineSearchQuery(
            PeopleSearchQuery(fullName = "Alexis Wolff"),
            "Previously spoke at open source conferences",
        )

        assertEquals("Previously spoke at open source conferences", refinement.query.freeText)
        assertTrue(refinement.appliedFields.isEmpty())
    }

    private fun result(id: String, source: String, confidence: ConfidenceLevel) = PersonSearchResult(
        id = id,
        providerId = id.substringBefore(':'),
        title = id,
        description = "Public profile",
        url = "https://example.com/$id",
        sourceName = source,
        category = SourceCategory.SOCIAL,
        confidence = confidence,
        confidenceScore = 25,
        isDirectResult = true,
    )
}
