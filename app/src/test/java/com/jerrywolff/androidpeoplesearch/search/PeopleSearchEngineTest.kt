package com.jerrywolff.androidpeoplesearch.search

import android.net.Uri
import com.jerrywolff.androidpeoplesearch.model.ConfidenceLevel
import com.jerrywolff.androidpeoplesearch.model.PeopleSearchQuery
import com.jerrywolff.androidpeoplesearch.model.PersonSearchResult
import com.jerrywolff.androidpeoplesearch.model.SourceCategory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class PeopleSearchEngineTest {
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
    fun searchShowsEveryRecordReturnedByACompliantProvider() = runBlocking {
        val calls = AtomicInteger()
        val provider = fakeProvider("web", SourceCategory.WEB, calls, resultTitle = "Gayle Rankin", direct = true)

        val report = PeopleSearchEngine(listOf(provider)).searchWithDiagnostics(PeopleSearchQuery(fullName = "Alexis Wolff"))

        assertEquals(listOf("Gayle Rankin"), report.results.map { it.title })
        assertEquals(listOf(SourceResultCount("web", 1, false)), report.sourceCounts)
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
            .searchWithDiagnostics(PeopleSearchQuery(fullName = "Alexis Wolff"))

        assertEquals(1, report.results.size)
        assertEquals(listOf(SourceResultCount("github", 1, false)), report.sourceCounts)
    }

    private fun fakeProvider(
        providerId: String,
        sourceCategory: SourceCategory,
        calls: AtomicInteger,
        requiresPayment: Boolean = false,
        requiresAuth: Boolean = false,
        resultTitle: String = providerId,
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
                    description = "Result from $providerId",
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
}
