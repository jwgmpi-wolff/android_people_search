package com.jerrywolff.androidpeoplesearch.search

import android.net.Uri
import com.jerrywolff.androidpeoplesearch.model.PeopleSearchQuery
import com.jerrywolff.androidpeoplesearch.model.PersonSearchResult
import com.jerrywolff.androidpeoplesearch.model.SourceCategory

interface SearchProvider {
    val id: String
    val displayName: String
    val category: SourceCategory
    val requiresPayment: Boolean
    val requiresAuth: Boolean
    val lastVerified: String
    fun buildSearchUrl(query: PeopleSearchQuery): Uri
    suspend fun search(query: PeopleSearchQuery): List<PersonSearchResult>
    suspend fun search(query: PeopleSearchQuery, pageDepth: Int): List<PersonSearchResult> = search(query)
    suspend fun search(query: PeopleSearchQuery, pageDepth: Int, breadthLevel: Int): List<PersonSearchResult> =
        search(query, pageDepth)
}
