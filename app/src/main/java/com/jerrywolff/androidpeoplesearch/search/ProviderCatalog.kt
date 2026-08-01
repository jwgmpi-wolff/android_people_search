package com.jerrywolff.androidpeoplesearch.search

import com.jerrywolff.androidpeoplesearch.model.SourceCategory

object ProviderCatalog {
    val providers: List<SearchProvider> = listOf(
        queryProvider("google", "Google", SourceCategory.WEB, "https://www.google.com/search"),
        queryProvider("bing", "Bing", SourceCategory.WEB, "https://www.bing.com/search"),
        queryProvider("duckduckgo", "DuckDuckGo", SourceCategory.WEB, "https://duckduckgo.com/"),
        queryProvider("linkedin", "LinkedIn public pages", SourceCategory.PROFESSIONAL, "https://www.google.com/search", prefix = "site:linkedin.com/in "),
        queryProvider("github", "GitHub", SourceCategory.PROFESSIONAL, "https://github.com/search", parameter = "q", prefix = "type:users "),
        queryProvider("x", "X public pages", SourceCategory.SOCIAL, "https://www.google.com/search", prefix = "site:x.com "),
        queryProvider("facebook", "Facebook public pages", SourceCategory.SOCIAL, "https://www.google.com/search", prefix = "site:facebook.com "),
        queryProvider("instagram", "Instagram public pages", SourceCategory.SOCIAL, "https://www.google.com/search", prefix = "site:instagram.com "),
        queryProvider("youtube", "YouTube", SourceCategory.SOCIAL, "https://www.youtube.com/results", parameter = "search_query"),
        queryProvider("reddit", "Reddit", SourceCategory.SOCIAL, "https://www.reddit.com/search/"),
        queryProvider("medium", "Medium", SourceCategory.NEWS, "https://medium.com/search"),
        queryProvider("stackoverflow", "Stack Overflow users", SourceCategory.PROFESSIONAL, "https://www.google.com/search", prefix = "site:stackoverflow.com/users "),
        queryProvider("google_news", "Google News", SourceCategory.NEWS, "https://news.google.com/search"),
        queryProvider("data_gov", "Data.gov", SourceCategory.PUBLIC_RECORDS, "https://catalog.data.gov/dataset/"),
        queryProvider("sec_edgar", "SEC EDGAR", SourceCategory.PUBLIC_RECORDS, "https://www.sec.gov/edgar/search/", parameter = "q"),
        queryProvider("fec", "Federal Election Commission", SourceCategory.PUBLIC_RECORDS, "https://www.fec.gov/data/receipts/individual-contributions/", parameter = "contributor_name"),
        queryProvider("usaspending", "USAspending.gov", SourceCategory.PUBLIC_RECORDS, "https://www.usaspending.gov/search/", parameter = "hash", prefix = ""),
        queryProvider("uspto", "USPTO public records", SourceCategory.PUBLIC_RECORDS, "https://www.google.com/search", prefix = "site:uspto.gov "),
        queryProvider("state_records", "State official records", SourceCategory.PUBLIC_RECORDS, "https://www.google.com/search", prefix = "site:.gov (license OR corporation OR court) "),
        queryProvider("county_records", "County official records", SourceCategory.PUBLIC_RECORDS, "https://www.google.com/search", prefix = "site:.gov (assessor OR recorder OR parcel OR court) county "),
        UrlSearchProvider("username_github", "GitHub username", SourceCategory.USERNAME) { q -> ProviderUrlBuilder.path("https://github.com", q.username.ifBlank { q.freeText }) },
        UrlSearchProvider("company_domain", "Company website", SourceCategory.DOMAIN) { q -> ProviderUrlBuilder.query("https://www.google.com/search", "q", "site:${q.emailDomain} ${q.searchText()}") },
    ).filterNot { it.requiresPayment }

    private fun queryProvider(
        id: String,
        name: String,
        category: SourceCategory,
        baseUrl: String,
        parameter: String = "q",
        prefix: String = "",
    ) = UrlSearchProvider(id, name, category) { query ->
        ProviderUrlBuilder.query(baseUrl, parameter, prefix + query.searchText())
    }
}
