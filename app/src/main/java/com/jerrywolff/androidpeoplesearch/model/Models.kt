package com.jerrywolff.androidpeoplesearch.model

enum class SourceCategory(val label: String) {
    WEB("Public web"),
    SOCIAL("Social profiles"),
    PROFESSIONAL("Business & professional"),
    PUBLIC_RECORDS("Government & public records"),
    NEWS("News & public mentions"),
    DOMAIN("Domain-specific"),
    USERNAME("Username search"),
}

enum class ConfidenceLevel(val label: String) { HIGH("High"), MEDIUM("Medium"), LOW("Low") }

data class PeopleSearchQuery(
    val firstName: String = "",
    val lastName: String = "",
    val fullName: String = "",
    val city: String = "",
    val county: String = "",
    val state: String = "",
    val country: String = "",
    val company: String = "",
    val emailDomain: String = "",
    val username: String = "",
    val freeText: String = "",
) {
    fun normalized(): PeopleSearchQuery = copy(
        firstName = firstName.clean(), lastName = lastName.clean(), fullName = fullName.clean(),
        city = city.clean(), county = county.clean(), state = state.clean(), country = country.clean(),
        company = company.clean(),
        emailDomain = emailDomain.clean().removePrefix("@"), username = username.clean().removePrefix("@"),
        freeText = freeText.clean(),
    )

    fun personName(): String = fullName.ifBlank { listOf(firstName, lastName).filter(String::isNotBlank).joinToString(" ") }

    fun searchText(): String = listOf(
        personName().quoteIfMultiword(), city, county, state, country, company.quoteIfMultiword(),
        emailDomain.takeIf(String::isNotBlank)?.let { "site:$it" }.orEmpty(), username, freeText,
    ).filter(String::isNotBlank).joinToString(" ")

    fun tokens(): Set<String> = searchText().lowercase().split(Regex("[^a-z0-9]+"))
        .filter { it.length > 1 && it !in STOP_WORDS }.toSet()

    fun isBlank(): Boolean = searchText().isBlank()
}

private val STOP_WORDS = setOf("the", "and", "or", "site")
private fun String.clean() = trim().replace(Regex("\\s+"), " ")
private fun String.quoteIfMultiword() = if (contains(' ') && isNotBlank()) "\"$this\"" else this

data class PersonSearchResult(
    val id: String,
    val providerId: String,
    val title: String,
    val description: String,
    val url: String,
    val sourceName: String,
    val category: SourceCategory,
    val confidence: ConfidenceLevel,
    val confidenceScore: Int,
    val extractedSignals: List<String> = emptyList(),
    val retrievedAtEpochMillis: Long = System.currentTimeMillis(),
    val isDirectResult: Boolean = false,
)

data class AiSummary(val headline: String, val summary: String, val caveats: List<String>)
data class NormalizedName(val givenName: String, val familyName: String, val displayName: String)
data class SearchSession(val query: PeopleSearchQuery, val resultCount: Int, val searchedAtEpochMillis: Long)

data class AppSettings(
    val historyEnabled: Boolean = false,
    val privacyMode: Boolean = true,
    val pageDepth: Int = 5,
)
