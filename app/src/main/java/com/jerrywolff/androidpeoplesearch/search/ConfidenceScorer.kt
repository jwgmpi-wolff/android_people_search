package com.jerrywolff.androidpeoplesearch.search

import com.jerrywolff.androidpeoplesearch.model.ConfidenceLevel
import com.jerrywolff.androidpeoplesearch.model.PeopleSearchQuery
import com.jerrywolff.androidpeoplesearch.model.SourceCategory

data class ConfidenceScore(val score: Int, val level: ConfidenceLevel, val signals: List<String>)

object ConfidenceScorer {
    fun score(query: PeopleSearchQuery, category: SourceCategory, searchableText: String): ConfidenceScore {
        val normalized = query.normalized()
        val haystack = searchableText.lowercase()
        val signals = buildList {
            normalized.personName().takeIf { it.isNotBlank() && haystack.contains(it.lowercase()) }?.let { add("Name") }
            normalized.company.takeIf { it.isNotBlank() && haystack.contains(it.lowercase()) }?.let { add("Company") }
            normalized.city.takeIf { it.isNotBlank() && haystack.contains(it.lowercase()) }?.let { add("City") }
            normalized.state.takeIf { it.isNotBlank() && haystack.contains(it.lowercase()) }?.let { add("State") }
            normalized.username.takeIf { it.isNotBlank() && haystack.contains(it.lowercase()) }?.let { add("Username") }
            normalized.emailDomain.takeIf { it.isNotBlank() && haystack.contains(it.lowercase()) }?.let { add("Domain") }
        }
        val sourceWeight = when (category) {
            SourceCategory.PUBLIC_RECORDS -> 35
            SourceCategory.PROFESSIONAL, SourceCategory.DOMAIN -> 25
            SourceCategory.NEWS, SourceCategory.WEB -> 20
            SourceCategory.SOCIAL, SourceCategory.USERNAME -> 10
        }
        val score = (sourceWeight + signals.size * 12).coerceIn(0, 100)
        val level = when {
            score >= 70 && signals.size >= 3 -> ConfidenceLevel.HIGH
            score >= 40 && signals.isNotEmpty() -> ConfidenceLevel.MEDIUM
            else -> ConfidenceLevel.LOW
        }
        return ConfidenceScore(score, level, signals)
    }
}
