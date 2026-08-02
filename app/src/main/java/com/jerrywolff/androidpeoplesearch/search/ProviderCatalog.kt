package com.jerrywolff.androidpeoplesearch.search

object ProviderCatalog {
    val providers: List<SearchProvider> = listOf(
        BingPublicReferencesProvider(),
        BingPublicReferencesProvider(linkedInOnly = true),
        CountyOfficialReferencesProvider(),
        KingCountyEmployeesProvider(),
        NpiRegistryApiProvider(),
        SemanticScholarAuthorsApiProvider(),
        OpenLibraryAuthorsApiProvider(),
        BlueskyActorsApiProvider(),
        GitHubUsersApiProvider(),
        StackOverflowUsersApiProvider(),
    )
}
