package com.jerrywolff.androidpeoplesearch.ai

import com.jerrywolff.androidpeoplesearch.model.AiSummary
import com.jerrywolff.androidpeoplesearch.model.ConfidenceLevel
import com.jerrywolff.androidpeoplesearch.model.NormalizedName
import com.jerrywolff.androidpeoplesearch.model.PeopleSearchQuery
import com.jerrywolff.androidpeoplesearch.model.PersonSearchResult
import java.util.Locale

class LocalHeuristicAssistant : AiAssistant {
    override suspend fun summarizeResults(results: List<PersonSearchResult>): AiSummary {
        val categories = results.groupingBy { it.category.label }.eachCount().entries.sortedByDescending { it.value }
        val sources = results.groupingBy { it.sourceName }.eachCount().entries.sortedByDescending { it.value }
        val high = results.count { it.confidence == ConfidenceLevel.HIGH }
        val medium = results.count { it.confidence == ConfidenceLevel.MEDIUM }
        val low = results.count { it.confidence == ConfidenceLevel.LOW }
        return AiSummary(
            headline = "Reviewed all ${results.size} displayed record(s)",
            summary = listOf(
                sources.joinToString(", ") { "${it.key}: ${it.value}" }.ifBlank { "No matching records returned" },
                categories.joinToString(", ") { "${it.key}: ${it.value}" }.ifBlank { "No result categories" },
                "Confidence: $high high, $medium medium, $low low",
            ).joinToString(". ") + ".",
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

    override suspend fun refineSearchQuery(query: PeopleSearchQuery, input: String): SearchRefinement {
        val message = input.trim()
        if (message.isBlank()) return SearchRefinement(query, "Add a specific detail for the search.", emptyList())
        var refined = query
        val applied = mutableListOf<String>()

        fun apply(label: String, value: String, change: (PeopleSearchQuery, String) -> PeopleSearchQuery) {
            if (value.isNotBlank()) {
                refined = change(refined, value.trim().trim(',', '.', ';'))
                applied += label
            }
        }

        labeledValue(message, "name")?.let {
            apply("name", it) { current, value -> current.copy(fullName = value, firstName = "", lastName = "") }
        }
        labeledValue(message, "city")?.let { apply("city", it) { current, value -> current.copy(city = value) } }
        labeledValue(message, "state")?.let { apply("state", it) { current, value -> current.copy(state = value) } }
        labeledValue(message, "country")?.let { apply("country", it) { current, value -> current.copy(country = value) } }
        labeledValue(message, "company")?.let { apply("company", it) { current, value -> current.copy(company = value) } }

        Regex("(?i)\\b([a-z][a-z .'-]+?)\\s+county\\b").find(message)?.groupValues?.get(1)?.let {
            apply("county", "$it County") { current, value -> current.copy(county = value) }
        }
        Regex("(?i)\\b(?:works?|worked|employed)\\s+(?:at|by)\\s+([^,.;]+)").find(message)?.groupValues?.get(1)?.let {
            apply("company", it) { current, value -> current.copy(company = value) }
        }
        Regex("(?i)\\b(?:username|handle)\\s*(?:is|:)?\\s*@?([a-z0-9_.-]+)").find(message)?.groupValues?.get(1)?.let {
            apply("username", it) { current, value -> current.copy(username = value) }
        }
        Regex("(?i)\\b(?:email\\s+)?domain\\s*(?:is|:)?\\s*@?([a-z0-9.-]+\\.[a-z]{2,})").find(message)?.groupValues?.get(1)?.let {
            apply("email domain", it) { current, value -> current.copy(emailDomain = value) }
        }
        findCountry(message)?.takeIf { "country" !in applied }?.let {
            apply("country", it) { current, value -> current.copy(country = value) }
        }
        Regex("(?i)\\b(?:lives?|located|based)\\s+in\\s+([^,.;]+)").find(message)?.groupValues?.get(1)?.let {
            val city = it.trim()
            if (!city.endsWith("county", ignoreCase = true) && "city" !in applied) {
                apply("city", city) { current, value -> current.copy(city = value) }
            }
        }

        val uniqueApplied = applied.distinct()
        if (uniqueApplied.isEmpty()) {
            refined = refined.copy(freeText = listOf(refined.freeText, message).filter(String::isNotBlank).joinToString(" "))
        }
        val missing = listOfNotNull(
            "county".takeIf { refined.county.isBlank() },
            "country".takeIf { refined.country.isBlank() },
            "company".takeIf { refined.company.isBlank() },
            "username".takeIf { refined.username.isBlank() },
        )
        val reply = if (uniqueApplied.isEmpty()) {
            "I kept that as additional search context. Add labeled details such as county, country, company, or username for tighter matching."
        } else {
            "Applied ${uniqueApplied.joinToString()}." +
                missing.take(2).takeIf(List<String>::isNotEmpty)?.let { " You can also add ${it.joinToString(" or ")}." }.orEmpty()
        }
        return SearchRefinement(refined.normalized(), reply, uniqueApplied)
    }

    private fun labeledValue(input: String, label: String): String? =
        Regex("(?i)\\b${Regex.escape(label)}\\s*(?:is|:|=)\\s*([^,.;]+)")
            .find(input)?.groupValues?.get(1)

    private fun findCountry(input: String): String? = Locale.getISOCountries()
        .map { Locale("", it).displayCountry }
        .sortedByDescending(String::length)
        .firstOrNull { input.contains(it, ignoreCase = true) }
}
