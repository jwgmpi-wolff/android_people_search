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
            normalized.personName().takeIf { it.matchesObservedText(haystack) }?.let { add("Name") }
            normalized.company.takeIf { it.matchesObservedText(haystack) }?.let { add("Company") }
            normalized.city.takeIf { it.matchesObservedText(haystack) }?.let { add("City") }
            normalized.county.takeIf { it.matchesObservedText(haystack) }?.let { add("County") }
            normalized.state.takeIf {
                it.matchesObservedText(haystack) ||
                    it.usStateAbbreviation()?.matchesObservedText(haystack) == true
            }?.let { add("State") }
            normalized.country.takeIf { it.matchesObservedText(haystack) }?.let { add("Country") }
            normalized.username.takeIf { it.matchesObservedText(haystack) }?.let { add("Username") }
            normalized.emailDomain.takeIf { it.matchesObservedText(haystack) }?.let { add("Domain") }
            normalized.freeText.takeIf { it.matchesObservedText(haystack) }?.let { add("Context") }
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

private fun String.matchesObservedText(haystack: String): Boolean {
    if (isBlank()) return false
    val expectedTokens = lowercase().split(Regex("[^a-z0-9]+"))
        .filter { it.length > 1 }
    if (expectedTokens.isEmpty()) return false
    val observedTokens = haystack.split(Regex("[^a-z0-9]+"))
        .filter { it.length > 1 }
        .toSet()
    val observedCompact = haystack.filter(Char::isLetterOrDigit)
    return expectedTokens.all(observedTokens::contains) ||
        observedCompact.contains(expectedTokens.joinToString(""))
}
