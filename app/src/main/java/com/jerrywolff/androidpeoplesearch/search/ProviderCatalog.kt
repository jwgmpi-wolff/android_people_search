package com.jerrywolff.androidpeoplesearch.search

object ProviderCatalog {
    val providers: List<SearchProvider> = listOf(
        BingPublicReferencesProvider(),
        BingPublicReferencesProvider(linkedInOnly = true),
        BlueskyActorsApiProvider(),
        CountyOfficialReferencesProvider(),
        KingCountyEmployeesProvider(),
        NpiRegistryApiProvider(),
        SemanticScholarAuthorsApiProvider(),
        OpenLibraryAuthorsApiProvider(),
        GitHubUsersApiProvider(),
        StackOverflowUsersApiProvider(),
    )
}
