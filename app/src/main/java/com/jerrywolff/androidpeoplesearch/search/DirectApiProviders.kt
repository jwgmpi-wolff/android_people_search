package com.jerrywolff.androidpeoplesearch.search

import android.net.Uri
import com.jerrywolff.androidpeoplesearch.model.PeopleSearchQuery
import com.jerrywolff.androidpeoplesearch.model.PersonSearchResult
import com.jerrywolff.androidpeoplesearch.model.SourceCategory
import org.json.JSONObject
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.net.URI
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

private const val PAGE_SIZE = 20

abstract class DirectApiProvider(
    override val id: String,
    override val displayName: String,
    override val category: SourceCategory,
    private val sourceUrl: (PeopleSearchQuery) -> String,
    protected val client: PublicApiClient = HttpPublicApiClient(),
) : SearchProvider {
    override val requiresPayment = false
    override val requiresAuth = false
    override val lastVerified = "2026-08-01"
    override fun buildSearchUrl(query: PeopleSearchQuery): Uri = Uri.parse(sourceUrl(query.normalized()))

    final override suspend fun search(query: PeopleSearchQuery): List<PersonSearchResult> = search(query, 5)

    final override suspend fun search(query: PeopleSearchQuery, pageDepth: Int): List<PersonSearchResult> =
        fetchPages(query.normalized(), pageDepth.coerceIn(1, 5))

    protected abstract suspend fun fetchPages(query: PeopleSearchQuery, pageDepth: Int): List<PersonSearchResult>

    protected fun result(
        query: PeopleSearchQuery,
        itemId: String,
        title: String,
        description: String,
        url: String,
    ): PersonSearchResult {
        val confidence = ConfidenceScorer.score(query, category, "$title $description $url")
        return PersonSearchResult(
            id = "$id:$itemId", providerId = id, title = title, description = description,
            url = url, sourceName = displayName, category = category,
            confidence = confidence.level, confidenceScore = confidence.score,
            extractedSignals = confidence.signals, isDirectResult = true,
        )
    }
}

class BingPublicReferencesProvider(
    private val linkedInOnly: Boolean = false,
    client: PublicApiClient = HttpPublicApiClient(),
) : DirectApiProvider(
    id = if (linkedInOnly) "bing_linkedin" else "bing_web",
    displayName = if (linkedInOnly) "LinkedIn public references" else "Bing public web references",
    category = SourceCategory.PUBLIC_RECORDS,
    sourceUrl = { query ->
        ProviderUrlBuilder.query(
            "https://www.bing.com/search",
            "q",
            query.bingReferenceQuery(linkedInOnly),
        )
    },
    client = client,
) {
    override suspend fun fetchPages(query: PeopleSearchQuery, pageDepth: Int): List<PersonSearchResult> {
        val refinedSearch = query.bingReferenceQuery(linkedInOnly)
        if (refinedSearch.isBlank()) return emptyList()
        val searches = listOf(
            refinedSearch to pageDepth,
            query.bingIdentityQuery(linkedInOnly) to 1,
        ).distinctBy { it.first }
        return buildList {
            searches.forEach { (search, pages) ->
                for (pageIndex in 0 until pages) {
                    val first = pageIndex * 10 + 1
                    val url = "https://www.bing.com/search?q=${ProviderUrlBuilder.encode(search)}&format=rss&first=$first"
                    val items = parseRssItems(client.get(url))
                    items.filterNot { it.url.isProhibitedReference() }.forEach { item ->
                        add(result(query, item.url.hashCode().toString(), item.title, cleanHtml(item.description), item.url))
                    }
                    if (items.size < 10) break
                }
            }
        }.distinctBy { it.url }
    }
}

private data class RssItem(val title: String, val description: String, val url: String)

private fun parseRssItems(xml: String): List<RssItem> {
    require(!xml.contains("<!DOCTYPE", ignoreCase = true)) { "RSS document types are not allowed" }
    val factory = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = false
    }
    val document = factory.newDocumentBuilder().parse(ByteArrayInputStream(xml.toByteArray()))
    val nodes = document.getElementsByTagName("item")
    return (0 until nodes.length).mapNotNull { index ->
        val item = nodes.item(index) as? Element ?: return@mapNotNull null
        val title = item.getElementsByTagName("title").item(0)?.textContent.orEmpty().trim()
        val description = item.getElementsByTagName("description").item(0)?.textContent.orEmpty().trim()
        val url = item.getElementsByTagName("link").item(0)?.textContent.orEmpty().trim()
        RssItem(title, description, url).takeIf { title.isNotBlank() && url.startsWith("https://") }
    }
}

private fun PeopleSearchQuery.bingReferenceQuery(linkedInOnly: Boolean): String = listOf(
    personName().takeIf(String::isNotBlank)?.let { "\"$it\"" }.orEmpty(),
    city,
    county,
    state,
    country,
    company,
    username,
    "site:linkedin.com/in".takeIf { linkedInOnly }.orEmpty(),
).filter(String::isNotBlank).joinToString(" ")

private fun PeopleSearchQuery.bingIdentityQuery(linkedInOnly: Boolean): String = listOf(
    personName().takeIf(String::isNotBlank)?.let { "\"$it\"" }.orEmpty(),
    username,
    "site:linkedin.com/in".takeIf { linkedInOnly }.orEmpty(),
).filter(String::isNotBlank).joinToString(" ")

private val PROHIBITED_REFERENCE_HOSTS = setOf(
    "fastpeoplesearch.com",
    "spokeo.com",
    "beenverified.com",
    "peoplefinders.com",
    "truthfinder.com",
    "intelius.com",
    "contactout.com",
    "wiza.co",
    "publicrecords.info",
)

private fun String.isProhibitedReference(): Boolean = runCatching {
    val host = URI(this).host.orEmpty().lowercase(Locale.ROOT).removePrefix("www.")
    PROHIBITED_REFERENCE_HOSTS.any { host == it || host.endsWith(".$it") }
}.getOrDefault(true)

class CountyOfficialReferencesProvider(client: PublicApiClient = HttpPublicApiClient()) : DirectApiProvider(
    id = "county_official_references",
    displayName = "Official county public records",
    category = SourceCategory.PUBLIC_RECORDS,
    sourceUrl = { query ->
        ProviderUrlBuilder.query("https://www.bing.com/search", "q", query.countyOfficialQueries().firstOrNull().orEmpty())
    },
    client = client,
) {
    override suspend fun fetchPages(query: PeopleSearchQuery, pageDepth: Int): List<PersonSearchResult> {
        val searches = query.countyOfficialQueries()
        if (searches.isEmpty()) return emptyList()
        return buildList {
            searches.forEachIndexed { searchIndex, search ->
                val pages = if (searchIndex == 0) pageDepth else 1
                for (pageIndex in 0 until pages) {
                    val first = pageIndex * 10 + 1
                    val url = "https://www.bing.com/search?q=${ProviderUrlBuilder.encode(search)}&format=rss&first=$first"
                    val items = parseRssItems(client.get(url))
                    items.filter { it.url.isOfficialGovernmentReference() }.forEach { item ->
                        add(
                            result(
                                query,
                                item.url.hashCode().toString(),
                                item.title,
                                cleanHtml(item.description),
                                item.url,
                            ),
                        )
                    }
                    if (items.size < 10) break
                }
            }
        }.distinctBy { it.url }
    }
}

private fun PeopleSearchQuery.countyOfficialQueries(): List<String> {
    val name = personName()
    if (name.isBlank() || county.isBlank() || state.isBlank()) return emptyList()
    val location = "\"$name\" \"${countyLabel()}\" \"$state\""
    return listOf(
        "$location site:.gov",
        "$location (record OR agenda OR minutes OR notice OR board OR commission OR court OR docket OR permit OR license OR election OR contract) site:.gov",
    )
}

private fun PeopleSearchQuery.countyLabel(): String {
    val officialSuffixes = listOf("county", "parish", "borough", "census area", "municipality")
    return if (officialSuffixes.any { county.endsWith(it, ignoreCase = true) }) county else "$county County"
}

private fun String.isOfficialGovernmentReference(): Boolean = runCatching {
    val uri = URI(this)
    val host = uri.host.orEmpty().lowercase(Locale.ROOT).removeSuffix(".")
    uri.scheme.equals("https", ignoreCase = true) && (host.endsWith(".gov") || host == "gov")
}.getOrDefault(false)

class KingCountyEmployeesProvider(client: PublicApiClient = HttpPublicApiClient()) : DirectApiProvider(
    id = "king_county_employees",
    displayName = "King County employee directory",
    category = SourceCategory.PUBLIC_RECORDS,
    sourceUrl = { query -> query.kingCountyEmployeeSearchUrl() },
    client = client,
) {
    override suspend fun fetchPages(query: PeopleSearchQuery, pageDepth: Int): List<PersonSearchResult> {
        val (firstName, lastName) = query.exactNameParts()
        if (!query.isKingCountyWashington() || firstName.length < 2 || lastName.length < 2) return emptyList()
        return parseKingCountyEmployees(client.get(query.kingCountyEmployeeSearchUrl())).map { employee ->
            result(
                query,
                employee.id,
                employee.name,
                listOf(employee.title, employee.group, "King County, Washington employee")
                    .filter(String::isNotBlank)
                    .joinToString(" · "),
                "https://directory.kingcounty.gov/EmployeeDetail.asp?EmpID=${employee.id}",
            )
        }
    }
}

private fun PeopleSearchQuery.isKingCountyWashington(): Boolean {
    val selectedCounty = county.removeSuffix(" County").trim()
    return selectedCounty.equals("King", ignoreCase = true) &&
        (state.equals("Washington", ignoreCase = true) || state.equals("WA", ignoreCase = true))
}

private data class KingCountyEmployee(
    val id: String,
    val name: String,
    val group: String,
    val title: String,
)

private fun PeopleSearchQuery.kingCountyEmployeeSearchUrl(): String {
    val (firstName, lastName) = exactNameParts()
    return "https://directory.kingcounty.gov/EmployeeList.asp" +
        "?txtfname=${ProviderUrlBuilder.encode(firstName)}" +
        "&txtlname=${ProviderUrlBuilder.encode(lastName)}&btnSearch=Search"
}

private fun parseKingCountyEmployees(html: String): List<KingCountyEmployee> =
    Regex("(?is)<tr>.*?</tr>").findAll(html).mapNotNull { rowMatch ->
        val row = rowMatch.value
        val identity = Regex(
            "(?is)<a\\s+href=\"EmployeeDetail\\.asp\\?EmpID=(\\d+)\"[^>]*>\\s*<font[^>]*>(.*?)</font>",
        ).find(row) ?: return@mapNotNull null
        val cells = Regex("(?is)<td\\b[^>]*>(.*?)</td>").findAll(row)
            .map { cleanHtml(it.groupValues[1]) }
            .toList()
        if (cells.size < 4) return@mapNotNull null
        KingCountyEmployee(
            id = identity.groupValues[1],
            name = cleanHtml(identity.groupValues[2]).displayCountyEmployeeName(),
            group = cells[1],
            title = cells[3],
        )
    }.toList()

private fun String.displayCountyEmployeeName(): String {
    val parts = split(',', limit = 2).map(String::trim)
    return if (parts.size == 2) "${parts[1]} ${parts[0]}".trim() else this
}

class GitHubUsersApiProvider(client: PublicApiClient = HttpPublicApiClient()) : DirectApiProvider(
    id = "github", displayName = "GitHub public users", category = SourceCategory.PROFESSIONAL,
    sourceUrl = { query ->
        ProviderUrlBuilder.query("https://github.com/search", "q", "type:users ${query.searchText()}")
    }, client = client,
) {
    override suspend fun fetchPages(query: PeopleSearchQuery, pageDepth: Int): List<PersonSearchResult> = buildList {
        val search = query.username.ifBlank {
            query.personName().compactHandleQuery().ifBlank { query.freeText }
        }
        if (search.isBlank()) return@buildList
        repeat(pageDepth) { pageIndex ->
            val url = "https://api.github.com/search/users?q=${ProviderUrlBuilder.encode(search)}&per_page=$PAGE_SIZE&page=${pageIndex + 1}"
            val items = JSONObject(client.get(url)).getJSONArray("items")
            repeat(items.length()) { index ->
                val item = items.getJSONObject(index)
                add(result(query, item.getLong("id").toString(), item.getString("login"), "Public GitHub user profile", item.getString("html_url")))
            }
            if (items.length() < PAGE_SIZE) return@buildList
        }
    }
}

private fun String.compactHandleQuery(): String = lowercase(Locale.ROOT).filter(Char::isLetterOrDigit)

class BlueskyActorsApiProvider(client: PublicApiClient = HttpPublicApiClient()) : DirectApiProvider(
    id = "bluesky",
    displayName = "Bluesky public profiles",
    category = SourceCategory.SOCIAL,
    sourceUrl = { query ->
        ProviderUrlBuilder.query("https://bsky.app/search", "q", query.personName().ifBlank { query.username })
    },
    client = client,
) {
    override suspend fun fetchPages(query: PeopleSearchQuery, pageDepth: Int): List<PersonSearchResult> = buildList {
        val search = query.username.ifBlank { query.personName().ifBlank { query.freeText } }
        if (search.isBlank()) return@buildList
        var cursor: String? = null
        repeat(pageDepth) {
            val cursorParameter = cursor?.let { "&cursor=${ProviderUrlBuilder.encode(it)}" }.orEmpty()
            val url = "https://public.api.bsky.app/xrpc/app.bsky.actor.searchActors" +
                "?q=${ProviderUrlBuilder.encode(search)}&limit=$PAGE_SIZE$cursorParameter"
            val response = JSONObject(client.get(url))
            val actors = response.getJSONArray("actors")
            repeat(actors.length()) { index ->
                val actor = actors.getJSONObject(index)
                val did = actor.getString("did")
                val handle = actor.getString("handle")
                val displayName = actor.optString("displayName").ifBlank { handle }
                val description = listOf(
                    "@$handle",
                    actor.optString("description"),
                ).filter(String::isNotBlank).joinToString(" · ")
                add(result(query, did, displayName, description, "https://bsky.app/profile/$handle"))
            }
            cursor = response.optString("cursor").takeIf(String::isNotBlank)
            if (actors.length() < PAGE_SIZE || cursor == null) return@buildList
        }
    }
}

class SemanticScholarAuthorsApiProvider(client: PublicApiClient = HttpPublicApiClient()) : DirectApiProvider(
    id = "semantic_scholar",
    displayName = "Semantic Scholar authors",
    category = SourceCategory.PROFESSIONAL,
    sourceUrl = { query ->
        ProviderUrlBuilder.query("https://www.semanticscholar.org/search", "q", query.personName())
    },
    client = client,
) {
    override suspend fun fetchPages(query: PeopleSearchQuery, pageDepth: Int): List<PersonSearchResult> = buildList {
        val name = query.personName()
        if (name.isBlank()) return@buildList
        repeat(pageDepth) { pageIndex ->
            val url = "https://api.semanticscholar.org/graph/v1/author/search" +
                "?query=${ProviderUrlBuilder.encode(name)}&limit=$PAGE_SIZE&offset=${pageIndex * PAGE_SIZE}" +
                "&fields=name,affiliations,paperCount,url"
            val items = JSONObject(client.get(url)).getJSONArray("data")
            repeat(items.length()) { index ->
                val item = items.getJSONObject(index)
                val authorId = item.getString("authorId")
                val affiliations = item.optJSONArray("affiliations")
                val affiliationText = affiliations?.let { array ->
                    (0 until array.length()).joinToString(", ") { array.optString(it) }
                }.orEmpty()
                val description = listOf(
                    affiliationText,
                    "${item.optInt("paperCount")} linked publication(s)",
                ).filter(String::isNotBlank).joinToString(" · ")
                add(
                    result(
                        query,
                        authorId,
                        item.getString("name"),
                        description,
                        item.optString("url", "https://www.semanticscholar.org/author/$authorId"),
                    ),
                )
            }
            if (items.length() < PAGE_SIZE) return@buildList
        }
    }
}

class OpenLibraryAuthorsApiProvider(client: PublicApiClient = HttpPublicApiClient()) : DirectApiProvider(
    id = "open_library",
    displayName = "Open Library authors",
    category = SourceCategory.PROFESSIONAL,
    sourceUrl = { query ->
        ProviderUrlBuilder.query("https://openlibrary.org/search/authors", "q", query.personName())
    },
    client = client,
) {
    override suspend fun fetchPages(query: PeopleSearchQuery, pageDepth: Int): List<PersonSearchResult> = buildList {
        val name = query.personName()
        if (name.isBlank()) return@buildList
        repeat(pageDepth) { pageIndex ->
            val url = "https://openlibrary.org/search/authors.json?q=${ProviderUrlBuilder.encode(name)}" +
                "&limit=$PAGE_SIZE&offset=${pageIndex * PAGE_SIZE}"
            val items = JSONObject(client.get(url)).getJSONArray("docs")
            repeat(items.length()) { index ->
                val item = items.getJSONObject(index)
                val authorId = item.getString("key").substringAfterLast('/')
                val description = listOfNotNull(
                    item.optString("top_work").takeIf(String::isNotBlank)?.let { "Known for $it" },
                    item.optInt("work_count").takeIf { it > 0 }?.let { "$it cataloged work(s)" },
                ).joinToString(" · ")
                add(
                    result(
                        query,
                        authorId,
                        item.getString("name"),
                        description.ifBlank { "Open Library author record" },
                        "https://openlibrary.org/authors/$authorId",
                    ),
                )
            }
            if (items.length() < PAGE_SIZE) return@buildList
        }
    }
}

class StackOverflowUsersApiProvider(client: PublicApiClient = HttpPublicApiClient()) : DirectApiProvider(
    id = "stackoverflow", displayName = "Stack Overflow public users", category = SourceCategory.PROFESSIONAL,
    sourceUrl = { query ->
        ProviderUrlBuilder.query("https://stackoverflow.com/users", "tab", "Reputation ${query.personName()}")
    }, client = client,
) {
    override suspend fun fetchPages(query: PeopleSearchQuery, pageDepth: Int): List<PersonSearchResult> = buildList {
        val name = query.personName().ifBlank { query.username.ifBlank { query.freeText } }
        if (name.isBlank()) return@buildList
        repeat(pageDepth) { pageIndex ->
            val url = "https://api.stackexchange.com/2.3/users?site=stackoverflow&pagesize=$PAGE_SIZE&page=${pageIndex + 1}&inname=${ProviderUrlBuilder.encode(name)}"
            val items = JSONObject(client.get(url)).getJSONArray("items")
            repeat(items.length()) { index ->
                val item = items.getJSONObject(index)
                val location = item.optString("location").takeIf(String::isNotBlank)?.let { " · $it" }.orEmpty()
                add(result(query, item.getLong("user_id").toString(), cleanHtml(item.getString("display_name")), "Reputation ${item.optInt("reputation")}$location", item.getString("link")))
            }
            if (items.length() < PAGE_SIZE) return@buildList
        }
    }
}

class NpiRegistryApiProvider(client: PublicApiClient = HttpPublicApiClient()) : DirectApiProvider(
    id = "npi_registry",
    displayName = "NPI Registry",
    category = SourceCategory.PUBLIC_RECORDS,
    sourceUrl = { query ->
        ProviderUrlBuilder.query("https://npiregistry.cms.hhs.gov/search", "name", query.personName())
    },
    client = client,
) {
    override suspend fun fetchPages(query: PeopleSearchQuery, pageDepth: Int): List<PersonSearchResult> = buildList {
        val (firstName, lastName) = query.npiNameParts()
        if (firstName.length < 2 || lastName.length < 2) return@buildList
        val locationParameters = listOfNotNull(
            query.city.takeIf(String::isNotBlank)?.let { "city=${ProviderUrlBuilder.encode(it)}" },
            query.state.takeIf(String::isNotBlank)?.let { "state=${ProviderUrlBuilder.encode(it)}" },
            query.country.npiCountryCode()?.let { "country_code=$it" },
        ).joinToString("&").takeIf(String::isNotBlank)?.let { "&$it" }.orEmpty()
        repeat(pageDepth) { pageIndex ->
            val url = "https://npiregistry.cms.hhs.gov/api/?version=2.1&enumeration_type=NPI-1" +
                "&first_name=${ProviderUrlBuilder.encode(firstName)}&last_name=${ProviderUrlBuilder.encode(lastName)}" +
                "&use_first_name_alias=False&limit=$PAGE_SIZE&skip=${pageIndex * PAGE_SIZE}$locationParameters"
            val items = JSONObject(client.get(url)).optJSONArray("results") ?: return@buildList
            repeat(items.length()) { index ->
                val item = items.getJSONObject(index)
                val basic = item.getJSONObject("basic")
                val number = item.getString("number")
                val title = listOf(basic.optString("first_name"), basic.optString("middle_name"), basic.optString("last_name"))
                    .filter(String::isNotBlank)
                    .joinToString(" ") { part -> part.lowercase().replaceFirstChar(Char::uppercase) }
                val address = item.optJSONArray("addresses")?.optJSONObject(0)
                val location = listOfNotNull(
                    address?.optString("city")?.takeIf(String::isNotBlank),
                    address?.optString("state")?.takeIf(String::isNotBlank),
                    address?.optString("country_name")?.takeIf(String::isNotBlank),
                ).joinToString(", ")
                val taxonomy = item.optJSONArray("taxonomies")?.optJSONObject(0)?.optString("desc").orEmpty()
                add(
                    result(
                        query,
                        number,
                        title,
                        listOf(taxonomy, location).filter(String::isNotBlank).joinToString(" · "),
                        "https://npiregistry.cms.hhs.gov/provider-view/$number",
                    ),
                )
            }
            if (items.length() < PAGE_SIZE) return@buildList
        }
    }
}

private fun cleanHtml(value: String): String = value
    .replace(Regex("<[^>]+>"), " ")
    .replace("&quot;", "\"").replace("&#39;", "'").replace("&amp;", "&")
    .replace(Regex("\\s+"), " ").trim()

private fun PeopleSearchQuery.npiNameParts(): Pair<String, String> {
    return exactNameParts()
}

private fun PeopleSearchQuery.exactNameParts(): Pair<String, String> {
    if (firstName.isNotBlank() && lastName.isNotBlank()) return firstName to lastName
    val parts = personName().split(' ').filter(String::isNotBlank)
    return if (parts.size >= 2) parts.dropLast(1).joinToString(" ") to parts.last() else "" to ""
}

private fun String.npiCountryCode(): String? = takeIf(String::isNotBlank)?.let { country ->
    Locale.getISOCountries().firstOrNull { Locale("", it).displayCountry.equals(country, ignoreCase = true) }
}
