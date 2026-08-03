package com.jerrywolff.androidpeoplesearch.search

import android.net.Uri
import com.jerrywolff.androidpeoplesearch.model.ConfidenceLevel
import com.jerrywolff.androidpeoplesearch.model.PeopleSearchQuery
import com.jerrywolff.androidpeoplesearch.model.PersonSearchResult
import com.jerrywolff.androidpeoplesearch.model.SourceCategory
import com.jerrywolff.androidpeoplesearch.model.SocialPlatform
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class PeopleSearchEngineTest {
    @Test
    fun providerPresentationPrioritizesWebAndSocialBeforeDeveloperSources() {
        assertEquals(
            listOf("bing_web", "duckduckgo_web", "selected_social_references", "bing_linkedin", "bluesky"),
            ProviderCatalog.providers.take(5).map { it.id },
        )
        assertEquals(listOf("github", "stackoverflow"), ProviderCatalog.providers.takeLast(2).map { it.id })
    }

    @Test
    fun searchRunsEveryCompliantCategoryAutomatically() = runBlocking {
        val calls = AtomicInteger()
        val providers = listOf(
            fakeProvider("web", SourceCategory.WEB, calls),
            fakeProvider("professional", SourceCategory.PROFESSIONAL, calls),
            fakeProvider("records", SourceCategory.PUBLIC_RECORDS, calls),
            fakeProvider("paid", SourceCategory.WEB, calls, requiresPayment = true),
            fakeProvider("auth", SourceCategory.SOCIAL, calls, requiresAuth = true),
        )

        val results = PeopleSearchEngine(providers).search(PeopleSearchQuery(fullName = "Ada Lovelace"))

        assertEquals(3, calls.get())
        assertEquals(setOf("web", "professional", "records"), results.map { it.providerId }.toSet())
        assertFalse(results.any { it.providerId == "paid" || it.providerId == "auth" })
    }

    @Test
    fun searchReportsFailedProviderWithoutDiscardingSuccessfulResults() = runBlocking {
        val calls = AtomicInteger()
        val successful = fakeProvider("web", SourceCategory.WEB, calls)
        val failing = object : SearchProvider {
            override val id = "failing"
            override val displayName = "Unavailable source"
            override val category = SourceCategory.PUBLIC_RECORDS
            override val requiresPayment = false
            override val requiresAuth = false
            override val lastVerified = "2026-08-01"
            override fun buildSearchUrl(query: PeopleSearchQuery): Uri = error("Not used by engine test")
            override suspend fun search(query: PeopleSearchQuery): List<PersonSearchResult> = error("offline")
        }

        val report = PeopleSearchEngine(listOf(successful, failing))
            .searchWithDiagnostics(PeopleSearchQuery(fullName = "Ada Lovelace"))

        assertEquals(listOf("web"), report.results.map { it.providerId })
        assertEquals(listOf("Unavailable source"), report.failures.map { it.providerName })
        assertEquals(
            listOf(
                SourceResultCount("web", 1, false),
                SourceResultCount("Unavailable source", 0, true, "Unavailable"),
            ),
            report.sourceCounts,
        )
    }

    @Test
    fun searchLabelsRateLimitedProviderAccurately() = runBlocking {
        val provider = object : SearchProvider {
            override val id = "limited"
            override val displayName = "Limited source"
            override val category = SourceCategory.PUBLIC_RECORDS
            override val requiresPayment = false
            override val requiresAuth = false
            override val lastVerified = "2026-08-01"
            override fun buildSearchUrl(query: PeopleSearchQuery): Uri = error("Not used")
            override suspend fun search(query: PeopleSearchQuery): List<PersonSearchResult> =
                throw PublicApiHttpException(429)
        }

        val report = PeopleSearchEngine(listOf(provider))
            .searchWithDiagnostics(PeopleSearchQuery(fullName = "Ada Lovelace"))

        assertEquals("Rate limited", report.failures.single().reason)
        assertEquals("Rate limited", report.sourceCounts.single().failureReason)
    }

    @Test
    fun searchRejectsRecordWithoutRequestedIdentityEvidence() = runBlocking {
        val calls = AtomicInteger()
        val provider = fakeProvider("web", SourceCategory.WEB, calls, resultTitle = "Gayle Rankin", direct = true)

        val report = PeopleSearchEngine(listOf(provider)).searchWithDiagnostics(PeopleSearchQuery(fullName = "Alexis Wolff"))

        assertTrue(report.results.isEmpty())
        assertEquals(listOf(SourceResultCount("web", 0, false)), report.sourceCounts)
    }

    @Test
    fun exactNameRecordsRemainVisibleWhenIndexSnippetOmitsRefinements() = runBlocking {
        val calls = AtomicInteger()
        val providers = listOf(
            fakeProvider(
                "matching",
                SourceCategory.PROFESSIONAL,
                calls,
                resultTitle = "Alexis Wolff",
                resultDescription = "Engineer in Seattle, Washington",
            ),
            fakeProvider(
                "unrefined",
                SourceCategory.PROFESSIONAL,
                calls,
                resultTitle = "Alexis Wolff",
                resultDescription = "Author profile in Berlin",
            ),
        )

        val report = PeopleSearchEngine(providers).searchWithDiagnostics(
            PeopleSearchQuery(fullName = "Alexis Wolff", city = "Seattle", state = "Washington"),
        )

        assertEquals(setOf("matching", "unrefined"), report.results.map { it.providerId }.toSet())
        assertEquals(
            listOf(SourceResultCount("matching", 1, false), SourceResultCount("unrefined", 1, false)),
            report.sourceCounts,
        )
    }

    @Test
    fun screenshotShapedLinkedInDirectoryResultMatchesRequestedIdentity() = runBlocking {
        val calls = AtomicInteger()
        val provider = fakeProvider(
            "bing_linkedin",
            SourceCategory.PUBLIC_RECORDS,
            calls,
            resultTitle = "30+ \"Alexis Wolff\" profiles | LinkedIn",
            resultDescription = "Public people directory results",
            resultUrl = "https://www.linkedin.com/pub/dir?firstName=alexis&lastName=wolff",
        )

        val report = PeopleSearchEngine(listOf(provider)).searchWithDiagnostics(
            PeopleSearchQuery(
                fullName = "Alexis Wolff",
                city = "Seattle",
                state = "Washington",
                country = "United States",
                company = "Microsoft",
            ),
        )

        assertEquals(listOf("bing_linkedin"), report.results.map { it.providerId })
        assertEquals(1, report.sourceCounts.single().recordCount)
    }

    @Test
    fun directUsernameResultCanMatchCompactNameInProfileUrl() = runBlocking {
        val calls = AtomicInteger()
        val provider = fakeProvider(
            "github",
            SourceCategory.PROFESSIONAL,
            calls,
            resultTitle = "alexiswolff92-hue",
            resultUrl = "https://github.com/alexiswolff92-hue",
            direct = true,
        )

        val report = PeopleSearchEngine(listOf(provider))
            .searchWithDiagnostics(PeopleSearchQuery(fullName = "Alexis Wolff", freeText = "include GitHub"))

        assertEquals(1, report.results.size)
        assertEquals(listOf(SourceResultCount("github", 1, false)), report.sourceCounts)
    }

    @Test
    fun webAndSocialResultsRankAheadOfDeveloperProfiles() = runBlocking {
        val query = PeopleSearchQuery(fullName = "Claude Nazon", freeText = "GitHub and Stack Overflow")
        val web = rankedProvider(
            providerId = "bing_web",
            category = SourceCategory.PUBLIC_RECORDS,
            title = "Claude Nazon public profile",
            confidenceScore = 20,
            direct = false,
        )
        val github = rankedProvider(
            providerId = "bing_web_github",
            category = SourceCategory.PROFESSIONAL,
            title = "Claude Nazon",
            confidenceScore = 100,
            direct = true,
            resultUrl = "https://github.com/claude-nazon",
        )
        val stackOverflow = rankedProvider(
            providerId = "stackoverflow",
            category = SourceCategory.PROFESSIONAL,
            title = "Claude Nazon",
            confidenceScore = 90,
            direct = true,
        )

        val report = PeopleSearchEngine(listOf(github, stackOverflow, web)).searchWithDiagnostics(query)

        assertEquals(listOf("bing_web", "bing_web_github", "stackoverflow"), report.results.map { it.providerId })
    }

    @Test
    fun developerSourcesAreExcludedUnlessNamedInFreeText() = runBlocking {
        val calls = AtomicInteger()
        val github = fakeProvider("github", SourceCategory.PROFESSIONAL, calls)
        val stackOverflow = fakeProvider("stackoverflow", SourceCategory.PROFESSIONAL, calls)
        val indexedGitHub = rankedProvider(
            providerId = "bing_web",
            category = SourceCategory.WEB,
            title = "Alexis Wolff",
            confidenceScore = 80,
            direct = false,
            resultUrl = "https://github.com/alexis-wolff",
        )

        val ordinary = PeopleSearchEngine(listOf(github, stackOverflow, indexedGitHub))
            .searchWithDiagnostics(PeopleSearchQuery(fullName = "Alexis Wolff"))
        val stackOnly = PeopleSearchEngine(listOf(github, stackOverflow))
            .searchWithDiagnostics(PeopleSearchQuery(fullName = "Alexis Wolff", freeText = "Stack Overflow"))

        assertTrue(ordinary.results.isEmpty())
        assertEquals(listOf("bing_web"), ordinary.sourceCounts.map { it.providerName })
        assertEquals(listOf("stackoverflow"), stackOnly.sourceCounts.map { it.providerName })
    }

    @Test
    fun deselectedSocialProvidersAndIndexedUrlsAreExcluded() = runBlocking {
        val calls = AtomicInteger()
        val linkedIn = fakeProvider("bing_linkedin", SourceCategory.PROFESSIONAL, calls)
        val bluesky = fakeProvider("bluesky", SourceCategory.SOCIAL, calls)
        val indexedFacebook = fakeProvider(
            "other_web",
            SourceCategory.WEB,
            calls,
            resultTitle = "Ada Lovelace - Facebook",
            resultUrl = "https://www.facebook.com/ada.lovelace",
        )

        val report = PeopleSearchEngine(listOf(linkedIn, bluesky, indexedFacebook)).searchWithDiagnostics(
            PeopleSearchQuery(fullName = "Ada Lovelace", socialPlatforms = setOf(SocialPlatform.INSTAGRAM)),
        )

        assertEquals(1, calls.get())
        assertTrue(report.results.isEmpty())
        assertEquals(listOf(SourceResultCount("other_web", 0, false)), report.sourceCounts)
    }

    @Test
    fun searchForwardsBreadthLevelWithoutChangingQuery() = runBlocking {
        val submitted = PeopleSearchQuery(fullName = "Claude Nazon", state = "Florida")
        var receivedQuery: PeopleSearchQuery? = null
        var receivedBreadth = -1
        val provider = object : SearchProvider {
            override val id = "capture"
            override val displayName = "capture"
            override val category = SourceCategory.WEB
            override val requiresPayment = false
            override val requiresAuth = false
            override val lastVerified = "2026-08-01"
            override fun buildSearchUrl(query: PeopleSearchQuery): Uri = error("Not used")
            override suspend fun search(query: PeopleSearchQuery): List<PersonSearchResult> = emptyList()
            override suspend fun search(
                query: PeopleSearchQuery,
                pageDepth: Int,
                breadthLevel: Int,
            ): List<PersonSearchResult> {
                receivedQuery = query
                receivedBreadth = breadthLevel
                return emptyList()
            }
        }

        PeopleSearchEngine(listOf(provider)).searchWithDiagnostics(submitted, pageDepth = 2, breadthLevel = 1)

        assertEquals(submitted, receivedQuery)
        assertEquals(1, receivedBreadth)
    }

    @Test
    fun emptyInitialSearchAutomaticallyBroadensGeneralIndexes() = runBlocking {
        val receivedBreadths = mutableListOf<Int>()
        val provider = object : SearchProvider {
            override val id = "bing_web"
            override val displayName = "Bing public web references"
            override val category = SourceCategory.WEB
            override val requiresPayment = false
            override val requiresAuth = false
            override val lastVerified = "2026-08-01"
            override fun buildSearchUrl(query: PeopleSearchQuery): Uri = error("Not used")
            override suspend fun search(query: PeopleSearchQuery): List<PersonSearchResult> = emptyList()
            override suspend fun search(
                query: PeopleSearchQuery,
                pageDepth: Int,
                breadthLevel: Int,
            ): List<PersonSearchResult> {
                receivedBreadths += breadthLevel
                if (breadthLevel == 0) return emptyList()
                return listOf(
                    PersonSearchResult(
                        id = "claude-nazon",
                        providerId = id,
                        title = "Claude Nazon - public profile",
                        description = "Source-observed public profile",
                        url = "https://example.com/claude-nazon",
                        sourceName = displayName,
                        category = category,
                        confidence = ConfidenceLevel.MEDIUM,
                        confidenceScore = 60,
                    ),
                )
            }
        }

        val report = PeopleSearchEngine(listOf(provider))
            .searchWithDiagnostics(PeopleSearchQuery(firstName = "Claude", lastName = "Nazon"))

        assertEquals(listOf(0, 1), receivedBreadths)
        assertEquals(listOf("Claude Nazon - public profile"), report.results.map { it.title })
        assertEquals(1, report.sourceCounts.single().recordCount)
    }

    @Test
    fun oneIndexResultDoesNotPreventAnotherEmptyIndexFromBroadening() = runBlocking {
        val bingBreadths = mutableListOf<Int>()
        val duckDuckGoBreadths = mutableListOf<Int>()
        val bing = breadthAwareProvider("bing_web", bingBreadths, resultAtBreadth = 0)
        val duckDuckGo = breadthAwareProvider("duckduckgo_web", duckDuckGoBreadths, resultAtBreadth = 1)

        val report = PeopleSearchEngine(listOf(bing, duckDuckGo)).searchWithDiagnostics(
            PeopleSearchQuery(fullName = "Claude Nazon", state = "Florida"),
        )

        assertEquals(listOf(0), bingBreadths)
        assertEquals(listOf(0, 1), duckDuckGoBreadths)
        assertEquals(setOf("bing_web", "duckduckgo_web"), report.results.map { it.providerId }.toSet())
    }

    private fun fakeProvider(
        providerId: String,
        sourceCategory: SourceCategory,
        calls: AtomicInteger,
        requiresPayment: Boolean = false,
        requiresAuth: Boolean = false,
        resultTitle: String = "Ada Lovelace",
        resultDescription: String = "Result from $providerId",
        resultUrl: String = "https://example.com/$providerId",
        direct: Boolean = false,
    ) = object : SearchProvider {
        override val id = providerId
        override val displayName = providerId
        override val category = sourceCategory
        override val requiresPayment = requiresPayment
        override val requiresAuth = requiresAuth
        override val lastVerified = "2026-08-01"
        override fun buildSearchUrl(query: PeopleSearchQuery): Uri = error("Not used by engine test")
        override suspend fun search(query: PeopleSearchQuery): List<PersonSearchResult> {
            calls.incrementAndGet()
            return listOf(
                PersonSearchResult(
                    id = providerId,
                    providerId = providerId,
                    title = resultTitle,
                    description = resultDescription,
                    url = resultUrl,
                    sourceName = providerId,
                    category = sourceCategory,
                    confidence = ConfidenceLevel.LOW,
                    confidenceScore = 10,
                    isDirectResult = direct,
                ),
            )
        }
    }

    private fun rankedProvider(
        providerId: String,
        category: SourceCategory,
        title: String,
        confidenceScore: Int,
        direct: Boolean,
        resultUrl: String = "https://example.com/$providerId/claude-nazon",
    ) = object : SearchProvider {
        override val id = providerId
        override val displayName = providerId
        override val category = category
        override val requiresPayment = false
        override val requiresAuth = false
        override val lastVerified = "2026-08-01"
        override fun buildSearchUrl(query: PeopleSearchQuery): Uri = error("Not used")
        override suspend fun search(query: PeopleSearchQuery): List<PersonSearchResult> = listOf(
            PersonSearchResult(
                id = providerId,
                providerId = providerId,
                title = title,
                description = "Public profile for $title",
                url = resultUrl,
                sourceName = providerId,
                category = category,
                confidence = ConfidenceLevel.HIGH,
                confidenceScore = confidenceScore,
                isDirectResult = direct,
            ),
        )
    }

    private fun breadthAwareProvider(
        providerId: String,
        receivedBreadths: MutableList<Int>,
        resultAtBreadth: Int,
    ) = object : SearchProvider {
        override val id = providerId
        override val displayName = providerId
        override val category = SourceCategory.WEB
        override val requiresPayment = false
        override val requiresAuth = false
        override val lastVerified = "2026-08-03"
        override fun buildSearchUrl(query: PeopleSearchQuery): Uri = error("Not used")
        override suspend fun search(query: PeopleSearchQuery): List<PersonSearchResult> = emptyList()
        override suspend fun search(
            query: PeopleSearchQuery,
            pageDepth: Int,
            breadthLevel: Int,
        ): List<PersonSearchResult> {
            receivedBreadths += breadthLevel
            if (breadthLevel != resultAtBreadth) return emptyList()
            return listOf(
                PersonSearchResult(
                    id = "$providerId-$breadthLevel",
                    providerId = providerId,
                    title = "Claude Nazon - Florida public profile",
                    description = "Source-observed public profile",
                    url = "https://example.com/$providerId/claude-nazon",
                    sourceName = providerId,
                    category = category,
                    confidence = ConfidenceLevel.MEDIUM,
                    confidenceScore = 52,
                ),
            )
        }
    }
}
