package com.jerrywolff.androidpeoplesearch.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PeopleSearchQueryTest {
    @Test
    fun normalized_trimsAndCanonicalizesFields() {
        val query = PeopleSearchQuery(fullName = "  Ada   Lovelace ", emailDomain = " @example.org ", username = " @ada ").normalized()
        assertEquals("Ada Lovelace", query.fullName)
        assertEquals("example.org", query.emailDomain)
        assertEquals("ada", query.username)
    }

    @Test
    fun searchText_includesQuotedNameAndFilters() {
        val text = PeopleSearchQuery(fullName = "Ada Lovelace", city = "London", company = "Analytical Engine").searchText()
        assertEquals("\"Ada Lovelace\" London \"Analytical Engine\"", text)
        assertFalse(PeopleSearchQuery(freeText = "researcher").isBlank())
    }
}
