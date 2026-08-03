package com.jerrywolff.androidpeoplesearch.search

import com.jerrywolff.androidpeoplesearch.model.PeopleSearchQuery
import com.jerrywolff.androidpeoplesearch.model.SocialPlatform
import com.jerrywolff.androidpeoplesearch.model.SourceCategory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectApiProviderTest {
    @Test
    fun selectedSocialReferencesQueryAndReturnOnlySelectedPlatforms() = runBlocking {
        val requestedUrls = mutableListOf<String>()
        val provider = SelectedSocialReferencesProvider(
            PublicApiClient { url ->
                requestedUrls += url
                """<?xml version="1.0"?><rss><channel>
                    <item><title>Alexis Wolff - Instagram</title><link>https://www.instagram.com/alexiswolff</link><description>Public profile</description></item>
                    <item><title>Alexis Wolff - Facebook</title><link>https://www.facebook.com/alexiswolff</link><description>Public profile</description></item>
                    <item><title>Alexis Wolff elsewhere</title><link>https://example.com/alexiswolff</link><description>Index noise</description></item>
                </channel></rss>"""
            },
        )

        val results = provider.search(
            PeopleSearchQuery(
                fullName = "Alexis Wolff",
                socialPlatforms = setOf(SocialPlatform.INSTAGRAM, SocialPlatform.SNAPCHAT),
            ),
            pageDepth = 5,
        )

        assertEquals(2, requestedUrls.size)
        assertTrue(requestedUrls.single { it.contains("site%3Ainstagram.com") }.contains("site%3Asnapchat.com").not())
        assertTrue(requestedUrls.single { it.contains("site%3Asnapchat.com") }.contains("site%3Ainstagram.com").not())
        assertTrue(requestedUrls.none { it.contains("site%3Afacebook.com") })
        assertEquals(listOf("https://www.instagram.com/alexiswolff"), results.map { it.url })
    }

    @Test
    fun selectedSocialReferencesQueryEveryDomainWhenAllAreChecked() = runBlocking {
        val requestedUrls = mutableListOf<String>()
        val provider = SelectedSocialReferencesProvider(
            PublicApiClient { url ->
                requestedUrls += url
                "<?xml version=\"1.0\"?><rss><channel/></rss>"
            },
        )

        provider.search(PeopleSearchQuery(fullName = "Alexis Wolff"), pageDepth = 5)

        val expectedDomains = SocialPlatform.entries.flatMap(SocialPlatform::domains).toSet()
        assertEquals(expectedDomains.size, requestedUrls.size)
        expectedDomains.forEach { domain ->
            assertEquals(1, requestedUrls.count { it.contains("site%3A$domain") })
        }
    }

    @Test
    fun duckDuckGoUnwrapsPublicProfilesAndExcludesPeopleFinders() = runBlocking {
        val requestedUrls = mutableListOf<String>()
        val provider = DuckDuckGoPublicReferencesProvider(
            PublicApiClient { url ->
                requestedUrls += url
                """
                    <div class="result">
                      <a class="result__a" href="//duckduckgo.com/l/?uddg=https%3A%2F%2Fwww.spokeo.com%2FAlexis-Wolff&amp;rut=abc">Alexis Wolff report</a>
                      <a class="result__snippet">Paid report</a>
                    </div>
                    <div class="result">
                      <a class="result__a" href="//duckduckgo.com/l/?uddg=https%3A%2F%2Fwww.facebook.com%2Falexis.wolff%2F&amp;rut=def">Alexis Wolff - Facebook</a>
                      <a class="result__snippet">Public profile in Seattle</a>
                    </div>
                """.trimIndent()
            },
        )

        val result = provider.search(
            PeopleSearchQuery(fullName = "Alexis Wolff", city = "Seattle"),
            pageDepth = 5,
        ).single()

        assertTrue(requestedUrls.single().startsWith("https://html.duckduckgo.com/html/?q="))
        assertEquals("https://www.facebook.com/alexis.wolff/", result.url)
        assertEquals("Public profile in Seattle", result.description)
    }

    @Test
    fun kingCountySubmitsExactNameAndReturnsPublicWorkIdentityFields() = runBlocking {
        val requestedUrls = mutableListOf<String>()
        val client = PublicApiClient { url ->
            requestedUrls += url
            """
                <table class="data-table-2025">
                    <tr>
                        <td class="unshaded"><a href="EmployeeDetail.asp?EmpID=49744"><font>Wolff, Alexis</font></a></td>
                        <td class="unshaded">DNRP - Public Services</td>
                        <td class="unshaded"><font>206-555-0199</font><br/><font>PRIVATE-MAILSTOP</font></td>
                        <td class="unshaded">Program Manager</td>
                    </tr>
                </table>
            """.trimIndent()
        }

        val result = KingCountyEmployeesProvider(client).search(
            PeopleSearchQuery(
                firstName = "Alexis",
                lastName = "Wolff",
                county = "King County",
                state = "Washington",
            ),
            pageDepth = 5,
        ).single()

        assertEquals(
            "https://directory.kingcounty.gov/EmployeeList.asp?txtfname=Alexis&txtlname=Wolff&btnSearch=Search",
            requestedUrls.single(),
        )
        assertEquals("Alexis Wolff", result.title)
        assertEquals("https://directory.kingcounty.gov/EmployeeDetail.asp?EmpID=49744", result.url)
        assertTrue(result.description.contains("Program Manager"))
        assertTrue(result.description.contains("DNRP - Public Services"))
        assertTrue(result.description.contains("206-555-0199").not())
        assertTrue(result.description.contains("PRIVATE-MAILSTOP").not())
        assertTrue(result.isDirectResult)
    }

    @Test
    fun kingCountyReturnsNoRecordWhenDirectoryReportsNoMatch() = runBlocking {
        val provider = KingCountyEmployeesProvider(
            PublicApiClient { "<p>Employee &quot;Alexis Wolff&quot; - was not found.</p>" },
        )

        val results = provider.search(
            PeopleSearchQuery(fullName = "Alexis Wolff", county = "King", state = "WA"),
            pageDepth = 1,
        )

        assertTrue(results.isEmpty())
    }

    @Test
    fun countyReferencesUseSelectedStateAndKeepOnlyOfficialGovernmentResults() = runBlocking {
        val requestedUrls = mutableListOf<String>()
        val client = PublicApiClient { url ->
            requestedUrls += url
            """<?xml version="1.0"?><rss><channel>
                <item><title>Alexis Wolff - County Program Manager</title><link>https://directory.examplecounty.gov/staff/alexis-wolff</link><description>Official staff directory</description></item>
                <item><title>Alexis Wolff insecure record</title><link>http://records.examplecounty.gov/alexis-wolff</link><description>Insecure government page</description></item>
                <item><title>Alexis Wolff profile</title><link>https://peoplefinder.example.com/alexis</link><description>Unofficial profile</description></item>
            </channel></rss>"""
        }

        val result = CountyOfficialReferencesProvider(client).search(
            PeopleSearchQuery(fullName = "Alexis Wolff", county = "Example", state = "Oregon"),
            pageDepth = 1,
        ).single()

        assertEquals(2, requestedUrls.size)
        assertTrue(requestedUrls.all { it.contains("%22Example%20County%22") })
        assertTrue(requestedUrls.all { it.contains("%22Oregon%22") })
        assertTrue(requestedUrls.all { it.contains("site%3A.gov") })
        assertTrue(requestedUrls.first().contains("agenda").not())
        assertTrue(requestedUrls.last().contains("agenda"))
        assertEquals("https://directory.examplecounty.gov/staff/alexis-wolff", result.url)
        assertEquals("Official staff directory", result.description)
    }

    @Test
    fun countyProvidersDoNotQueryWithoutMatchingCountyAndState() = runBlocking {
        var calls = 0
        val client = PublicApiClient { calls += 1; error("should not be called") }
        val query = PeopleSearchQuery(fullName = "Alexis Wolff", county = "Snohomish", state = "Washington")

        val genericResults = CountyOfficialReferencesProvider(client).search(query.copy(state = ""), pageDepth = 1)
        val kingResults = KingCountyEmployeesProvider(client).search(query, pageDepth = 1)

        assertTrue(genericResults.isEmpty())
        assertTrue(kingResults.isEmpty())
        assertEquals(0, calls)
    }

    @Test
    fun countyReferencesPreserveOfficialParishName() = runBlocking {
        val requestedUrls = mutableListOf<String>()
        val provider = CountyOfficialReferencesProvider(
            PublicApiClient { url ->
                requestedUrls += url
                "<?xml version=\"1.0\"?><rss><channel></channel></rss>"
            },
        )

        provider.search(
            PeopleSearchQuery(fullName = "Alexis Wolff", county = "Orleans Parish", state = "Louisiana"),
            pageDepth = 1,
        )

        assertEquals(2, requestedUrls.size)
        assertTrue(requestedUrls.all { it.contains("%22Orleans%20Parish%22") })
        assertTrue(requestedUrls.none { it.contains("Orleans%20Parish%20County") })
    }

    @Test
    fun bingLinkedInReturnsIndexedProfilesAndExcludesProhibitedAggregators() = runBlocking {
        val requestedUrls = mutableListOf<String>()
        val client = PublicApiClient { url ->
            requestedUrls += url
            """<?xml version="1.0"?><rss><channel>
                <item><title>Alexis Wolff - Technical Director</title><link>https://www.linkedin.com/in/alexis-wolff</link><description>Seattle, Washington</description></item>
                <item><title>Alexis Wolff contact details</title><link>https://contactout.com/Alexis-Wolff</link><description>Paid enrichment profile</description></item>
                <item><title>33 public records for Alexis Wolff</title><link>https://www.publicrecords.info/people/found</link><description>Email report generator</description></item>
            </channel></rss>"""
        }

        val results = BingPublicReferencesProvider(linkedInOnly = true, client = client).search(
            PeopleSearchQuery(fullName = "Alexis Wolff", city = "Seattle", county = "King County"),
            pageDepth = 1,
        )

        assertEquals(2, requestedUrls.size)
        assertTrue(requestedUrls.first().contains("%22Alexis%20Wolff%22"))
        assertTrue(requestedUrls.first().contains("King%20County"))
        assertTrue(requestedUrls.all { it.contains("site%3Alinkedin.com%2Fin") })
        assertTrue(requestedUrls.last().contains("King%20County").not())
        assertEquals(1, results.size)
        assertEquals("https://www.linkedin.com/in/alexis-wolff", results.single().url)
        assertEquals(SourceCategory.PUBLIC_RECORDS, results.single().category)
        assertTrue(results.single().isDirectResult)
    }

    @Test
    fun bingUsesDomainAndAssistantContextInRefinedQuery() = runBlocking {
        val requestedUrls = mutableListOf<String>()
        val provider = BingPublicReferencesProvider(
            client = PublicApiClient { url ->
                requestedUrls += url
                "<?xml version=\"1.0\"?><rss><channel/></rss>"
            },
        )

        provider.search(
            PeopleSearchQuery(
                fullName = "Alexis Wolff",
                emailDomain = "example.gov",
                freeText = "advisory board",
            ),
            pageDepth = 1,
        )

        assertTrue(requestedUrls.first().contains("site%3Aexample.gov"))
        assertTrue(requestedUrls.first().contains("advisory%20board"))
        assertTrue(requestedUrls.last().contains("example.gov").not())
    }

    @Test
    fun unchangedRefreshAddsUnquotedBrowserStyleQuery() = runBlocking {
        val requestedUrls = mutableListOf<String>()
        val provider = BingPublicReferencesProvider(
            client = PublicApiClient { url ->
                requestedUrls += url
                "<?xml version=\"1.0\"?><rss><channel/></rss>"
            },
        )
        val query = PeopleSearchQuery(fullName = "Claude Nazon", state = "Florida")

        provider.search(query, pageDepth = 1, breadthLevel = 0)
        assertEquals(2, requestedUrls.size)

        requestedUrls.clear()
        provider.search(query, pageDepth = 1, breadthLevel = 1)

        assertEquals(6, requestedUrls.size)
        assertTrue(requestedUrls.any { it.contains("Claude%20Nazon%20Florida") })
        assertTrue(requestedUrls.any { it.contains("%22Claude%20Nazon%22") })
        assertTrue(requestedUrls.any { it.contains("site%3Afacebook.com") && it.contains("site%3Absky.app") })
        assertTrue(requestedUrls.any { it.contains("site%3A.gov") && it.contains("site%3A.edu") })
        assertTrue(requestedUrls.any { it.contains("site%3Asemanticscholar.org") && it.contains("site%3Aopenlibrary.org") })
        assertTrue(requestedUrls.none { it.contains("site%3Agithub.com") || it.contains("site%3Astackoverflow.com") })
    }

    @Test
    fun bingKeepsSuccessfulRecordsWhenFallbackRequestFails() = runBlocking {
        var requestCount = 0
        val provider = BingPublicReferencesProvider(
            client = PublicApiClient {
                requestCount += 1
                if (requestCount == 1) {
                    """<?xml version="1.0"?><rss><channel>
                        <item><title>Alexis Wolff - Advisory Board</title><link>https://example.gov/alexis-wolff</link><description>Official public record</description></item>
                    </channel></rss>"""
                } else {
                    throw PublicApiHttpException(429)
                }
            },
        )

        val results = provider.search(
            PeopleSearchQuery(fullName = "Alexis Wolff", company = "Example"),
            pageDepth = 3,
        )

        assertEquals(2, requestCount)
        assertEquals("https://example.gov/alexis-wolff", results.single().url)
    }

    @Test
    fun bingPublicReferencesPaginatesRssResults() = runBlocking {
        val requestedUrls = mutableListOf<String>()
        val client = PublicApiClient { url ->
            requestedUrls += url
            val page = requestedUrls.size
            val items = (1..10).joinToString("") { item ->
                "<item><title>Alexis Wolff reference $page-$item</title><link>https://example.org/$page/$item</link><description>Seattle public reference</description></item>"
            }
            "<?xml version=\"1.0\"?><rss><channel>$items</channel></rss>"
        }

        val results = BingPublicReferencesProvider(client = client).search(
            PeopleSearchQuery(fullName = "Alexis Wolff", city = "Seattle"),
            pageDepth = 2,
        )

        assertEquals(4, requestedUrls.size)
        assertTrue(requestedUrls[1].contains("first=11"))
        assertTrue(requestedUrls[2].contains("Seattle").not())
        assertTrue(requestedUrls.last().contains("first=11"))
        assertEquals(40, results.size)
        assertTrue(results.all { it.category == SourceCategory.PUBLIC_RECORDS })
    }

    @Test
    fun bingLinkedInPaginatesExactNameFallbackWhenRefinedQueryHasNoRecords() = runBlocking {
        val requestedUrls = mutableListOf<String>()
        val provider = BingPublicReferencesProvider(
            linkedInOnly = true,
            client = PublicApiClient { url ->
                requestedUrls += url
                if (url.contains("Microsoft")) {
                    "<?xml version=\"1.0\"?><rss><channel/></rss>"
                } else if (url.contains("first=11")) {
                    """<?xml version="1.0"?><rss><channel>
                        <item><title>Alexis Wolff - U.S. Diplomat | LinkedIn</title><link>https://www.linkedin.com/in/alexiswolff</link><description>Public LinkedIn profile</description></item>
                    </channel></rss>"""
                } else {
                    (1..10).joinToString("", "<?xml version=\"1.0\"?><rss><channel>", "</channel></rss>") { item ->
                        "<item><title>Other profile $item</title><link>https://www.linkedin.com/in/other-$item</link><description>Public LinkedIn profile</description></item>"
                    }
                }
            },
        )

        val results = provider.search(
            PeopleSearchQuery(fullName = "Alexis Wolff", company = "Microsoft"),
            pageDepth = 3,
        )

        assertEquals(3, requestedUrls.size)
        assertTrue(requestedUrls.first().contains("Microsoft"))
        assertTrue(requestedUrls[1].contains("Microsoft").not())
        assertTrue(requestedUrls.last().contains("first=11"))
        assertTrue(results.any { it.url == "https://www.linkedin.com/in/alexiswolff" })
    }

    @Test
    fun bingRejectsRssDocumentTypesBeforeParsing() = runBlocking {
        val provider = BingPublicReferencesProvider(
            client = PublicApiClient {
                """<?xml version="1.0"?><!DOCTYPE rss [<!ENTITY xxe SYSTEM "file:///etc/passwd">]><rss><channel/></rss>"""
            },
        )

        val failure = runCatching {
            provider.search(PeopleSearchQuery(fullName = "Alexis Wolff"), pageDepth = 1)
        }.exceptionOrNull()

        assertEquals("RSS document types are not allowed", failure?.message)
    }

    @Test
    fun npiRegistryUsesExactNameAndSafeLocationFields() = runBlocking {
        val requestedUrls = mutableListOf<String>()
        val client = PublicApiClient { url ->
            requestedUrls += url
            """{"result_count":1,"results":[{"number":"1234567890","basic":{"first_name":"ALEXIS","middle_name":"","last_name":"WOLFF"},"addresses":[{"city":"SEATTLE","state":"WA","country_name":"United States"}],"taxonomies":[{"desc":"Physician"}]}]}"""
        }

        val results = NpiRegistryApiProvider(client).search(
            PeopleSearchQuery(
                fullName = "Alexis Wolff",
                city = "Seattle",
                county = "King",
                state = "WA",
                country = "United States",
            ),
            pageDepth = 1,
        )

        assertTrue(requestedUrls.single().contains("use_first_name_alias=False"))
        assertTrue(requestedUrls.single().contains("city=Seattle"))
        assertTrue(requestedUrls.single().contains("state=WA"))
        assertTrue(requestedUrls.single().contains("country_code=US"))
        assertTrue(requestedUrls.single().contains("county").not())
        assertEquals("Alexis Wolff", results.single().title)
        assertTrue(results.single().description.contains("SEATTLE, WA, United States"))
        assertTrue(results.single().description.contains("1234567890").not())
    }

    @Test
    fun semanticScholarReturnsDirectAuthorsAcrossConfiguredPages() = runBlocking {
        val requestedUrls = mutableListOf<String>()
        val client = PublicApiClient { url ->
            requestedUrls += url
            val offset = Regex("[?&]offset=(\\d+)").find(url)?.groupValues?.get(1)?.toInt() ?: 0
            val items = (1..20).joinToString(",") { item ->
                val id = offset + item
                """{"authorId":"$id","name":"Alexis Wolff","affiliations":["Example University"],"paperCount":3,"url":"https://www.semanticscholar.org/author/$id"}"""
            }
            """{"total":60,"data":[$items]}"""
        }

        val results = SemanticScholarAuthorsApiProvider(client).search(
            PeopleSearchQuery(fullName = "Alexis Wolff"),
            pageDepth = 3,
        )

        assertEquals(3, requestedUrls.size)
        assertTrue(requestedUrls.first().contains("query=Alexis%20Wolff"))
        assertTrue(requestedUrls.last().contains("offset=40"))
        assertEquals(60, results.size)
        assertEquals("https://www.semanticscholar.org/author/1", results.first().url)
        assertTrue(results.all { it.isDirectResult })
    }

    @Test
    fun openLibraryReturnsDirectAuthorProfile() = runBlocking {
        val provider = OpenLibraryAuthorsApiProvider(
            PublicApiClient {
                """{"numFound":1,"docs":[{"key":"OL123A","name":"Alexis Wolff","top_work":"Example Book","work_count":2}]}"""
            },
        )

        val result = provider.search(PeopleSearchQuery(fullName = "Alexis Wolff"), pageDepth = 1).single()

        assertEquals("Alexis Wolff", result.title)
        assertEquals("https://openlibrary.org/authors/OL123A", result.url)
        assertTrue(result.description.contains("Example Book"))
        assertTrue(result.isDirectResult)
    }

    @Test
    fun blueskyReturnsDirectProfilesAcrossConfiguredPages() = runBlocking {
        val requestedUrls = mutableListOf<String>()
        val client = PublicApiClient { url ->
            requestedUrls += url
            val page = requestedUrls.size
            val actors = (1..20).joinToString(",") { item ->
                val id = (page - 1) * 20 + item
                """{"did":"did:plc:$id","handle":"alexis$id.example","displayName":"Alexis Wolff","description":"Public profile $id"}"""
            }
            """{"cursor":"next-$page","actors":[$actors]}"""
        }

        val results = BlueskyActorsApiProvider(client).search(
            PeopleSearchQuery(fullName = "Alexis Wolff"),
            pageDepth = 3,
        )

        assertEquals(3, requestedUrls.size)
        assertTrue(requestedUrls.first().contains("q=Alexis%20Wolff"))
        assertTrue(requestedUrls[1].contains("cursor=next-1"))
        assertTrue(requestedUrls[2].contains("cursor=next-2"))
        assertEquals(60, results.size)
        assertEquals("https://bsky.app/profile/alexis1.example", results.first().url)
        assertTrue(results.all { it.isDirectResult })
    }

    @Test
    fun github_fetchesFivePagesAndCapsResultsAtOneHundred() = runBlocking {
        val requestedUrls = mutableListOf<String>()
        val client = PublicApiClient { url ->
            requestedUrls += url
            val page = Regex("[?&]page=(\\d+)").find(url)?.groupValues?.get(1)?.toInt() ?: 1
            val items = (1..20).joinToString(",") { item ->
                val id = (page - 1) * 20 + item
                """{"id":$id,"login":"person$id","html_url":"https://github.com/person$id"}"""
            }
            """{"items":[$items]}"""
        }

        val results = GitHubUsersApiProvider(client).search(PeopleSearchQuery(fullName = "Ada Lovelace"))

        assertEquals(5, requestedUrls.size)
        assertTrue(requestedUrls.last().contains("page=5"))
        assertEquals(100, results.size)
        assertTrue(results.all { it.isDirectResult })
    }

    @Test
    fun github_honorsConfiguredThreePageDepth() = runBlocking {
        val requestedUrls = mutableListOf<String>()
        val client = PublicApiClient { url ->
            requestedUrls += url
            val page = Regex("[?&]page=(\\d+)").find(url)?.groupValues?.get(1)?.toInt() ?: 1
            val items = (1..20).joinToString(",") { item ->
                val id = (page - 1) * 20 + item
                """{"id":$id,"login":"person$id","html_url":"https://github.com/person$id"}"""
            }
            """{"items":[$items]}"""
        }

        val results = GitHubUsersApiProvider(client).search(PeopleSearchQuery(fullName = "Ada Lovelace"), pageDepth = 3)

        assertEquals(3, requestedUrls.size)
        assertTrue(requestedUrls.first().contains("q=adalovelace"))
        assertTrue(requestedUrls.last().contains("page=3"))
        assertEquals(60, results.size)
    }

    @Test
    fun github_returnsNoSearchCardWhenApiHasNoMatches() = runBlocking {
        val provider = GitHubUsersApiProvider(PublicApiClient { """{"items":[]}""" })

        val results = provider.search(PeopleSearchQuery(fullName = "No Such Public Profile"))

        assertTrue(results.isEmpty())
    }

    @Test
    fun githubProfileUrlReturnsSourceObservedPublicDetails() = runBlocking {
        val requestedUrls = mutableListOf<String>()
        val provider = GitHubUsersApiProvider(
            PublicApiClient { url ->
                requestedUrls += url
                """{
                    "id":583231,"login":"octocat","name":"The Octocat","type":"User",
                    "company":"GitHub","location":"San Francisco","blog":"https://github.blog",
                    "bio":"Public open source profile","public_repos":8,"followers":18000,
                    "html_url":"https://github.com/octocat"
                }"""
            },
        )

        val result = provider.search(
            PeopleSearchQuery(freeText = "Review https://github.com/octocat"),
            pageDepth = 5,
        ).single()

        assertEquals(listOf("https://api.github.com/users/octocat"), requestedUrls)
        assertEquals("The Octocat", result.title)
        assertTrue(result.description.contains("@octocat"))
        assertTrue(result.description.contains("Public open source profile"))
        assertTrue(result.description.contains("GitHub"))
        assertTrue(result.description.contains("San Francisco"))
        assertTrue(result.description.contains("https://github.blog"))
        assertTrue(result.description.contains("8 public repositories"))
        assertTrue(result.description.contains("18,000 followers"))
        assertEquals("https://github.com/octocat", result.url)
    }

    @Test
    fun inaccessibleGitHubProfileUrlReturnsNoRecord() = runBlocking {
        val provider = GitHubUsersApiProvider(
            PublicApiClient { throw PublicApiHttpException(404) },
        )

        val results = provider.search(
            PeopleSearchQuery(freeText = "https://github.com/atiy_microsoft"),
            pageDepth = 1,
        )

        assertTrue(results.isEmpty())
    }

    @Test
    fun github_exposesApiFailureToTheSearchEngine() = runBlocking {
        val provider = GitHubUsersApiProvider(PublicApiClient { error("rate limited") })

        val failure = runCatching { provider.search(PeopleSearchQuery(fullName = "Ada Lovelace")) }.exceptionOrNull()

        assertEquals("rate limited", failure?.message)
    }
}
