package com.jerrywolff.androidpeoplesearch.search

import com.jerrywolff.androidpeoplesearch.model.ConfidenceLevel
import com.jerrywolff.androidpeoplesearch.model.PeopleSearchQuery
import com.jerrywolff.androidpeoplesearch.model.SourceCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfidenceScorerTest {
    @Test
    fun officialRecordWithThreeSignals_isHighConfidence() {
        val query = PeopleSearchQuery(fullName = "Ada Lovelace", company = "Engine Labs", city = "London")
        val score = ConfidenceScorer.score(query, SourceCategory.PUBLIC_RECORDS, "Ada Lovelace Engine Labs London")
        assertEquals(ConfidenceLevel.HIGH, score.level)
        assertTrue(score.score >= 70)
    }

    @Test
    fun socialTargetWithoutObservedSignals_isLowConfidence() {
        val score = ConfidenceScorer.score(PeopleSearchQuery(fullName = "Ada Lovelace"), SourceCategory.SOCIAL, "Unrelated profile")
        assertEquals(ConfidenceLevel.LOW, score.level)
    }
}
