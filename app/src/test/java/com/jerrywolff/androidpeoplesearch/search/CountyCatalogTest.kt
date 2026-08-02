package com.jerrywolff.androidpeoplesearch.search

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CountyCatalogTest {
    @Test
    fun stateCatalogCoversAllStatesAndDistrictOfColumbia() {
        val states = usStateNames()

        assertEquals(51, states.size)
        assertTrue(states.containsAll(listOf("Alaska", "District of Columbia", "Louisiana", "Washington")))
    }

    @Test
    fun countiesForStateUsesOfficialFipsAndReturnsSortedDistinctNames() = runBlocking {
        val requestedUrls = mutableListOf<String>()
        val catalog = CountyCatalog(
            PublicApiClient { url ->
                requestedUrls += url
                """{"features":[
                    {"attributes":{"NAME":"Yakima County"}},
                    {"attributes":{"NAME":"Adams County"}},
                    {"attributes":{"NAME":"Adams County"}}
                ]}"""
            },
        )

        val counties = catalog.countiesForState("Washington")

        assertTrue(requestedUrls.single().contains("STATE%3D%2753%27"))
        assertEquals(listOf("Adams County", "Yakima County"), counties)
    }

    @Test
    fun countiesForUnknownStateDoesNotCallNetwork() = runBlocking {
        var calls = 0
        val catalog = CountyCatalog(PublicApiClient { calls += 1; "" })

        assertTrue(catalog.countiesForState("Not a state").isEmpty())
        assertEquals(0, calls)
    }
}
