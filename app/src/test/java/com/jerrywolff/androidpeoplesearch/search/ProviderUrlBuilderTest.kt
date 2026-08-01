package com.jerrywolff.androidpeoplesearch.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ProviderUrlBuilderTest {
    @Test
    fun query_encodesNamesAndReservedCharacters() {
        assertEquals(
            "https://www.google.com/search?q=%22Ada%20Lovelace%22%20site%3Aexample.org",
            ProviderUrlBuilder.query("https://www.google.com/search", "q", "\"Ada Lovelace\" site:example.org"),
        )
    }

    @Test
    fun catalog_excludesPaidAndAuthenticatedProviders() {
        assertFalse(ProviderCatalog.providers.any { it.requiresPayment || it.requiresAuth })
    }
}
