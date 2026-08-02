# Public source matrix

All active providers have `Paid = No` and `Auth = No`. Paginated direct API providers retrieve the user-configured 1–5 pages of 20 records and stop on a short page. Counts are calculated after retrieval and per-source deduplication; every compliant returned record is displayed with an explainable confidence score. Browser links and search forms do not participate in results. Last reviewed: 2026-08-01.

County choices load from the official, keyless Census TIGERweb State/County layer after a U.S. state is selected. Manual county entry remains available when that catalog cannot be reached.

| Source | Category | Auth | Paid results excluded | Retrieval | Search URL pattern |
|---|---|---:|---:|---|---|
| Google | Public web | No | Yes | Browser link | `google.com/search?q={query}` |
| Bing public web references | Publicly accessible records | No | Yes | Direct RSS retrieval, configured 1–5 pages | `bing.com/search?q={query}&format=rss&first={offset}` |
| LinkedIn public references | Publicly accessible records | No | Yes | Bing-indexed public profile references, configured 1–5 pages | Bing RSS `site:linkedin.com/in` |
| DuckDuckGo | Public web | No | Yes | Browser link | `duckduckgo.com/?q={query}` |
| Wikipedia | Public web | No | No | Inactive: snippet search produced unrelated identity results | Removed 2026-08-01 |
| Wikidata | Open knowledge graph | No | No | Inactive: entity search was too broad for people matching | Removed 2026-08-01 |
| NPI Registry | US federal public records | No | Yes | Direct API, configured 1–5 pages | `npiregistry.cms.hhs.gov/api/?version=2.1&skip={offset}` |
| Official county references | County employee/public official records | No | Yes | State-and-county-aware Bing RSS retrieval; HTTPS `.gov` results only | Bing RSS with exact name, selected county, and selected state |
| King County employee directory | County employee/public official records | No | Yes | Direct exact-name form retrieval; public work name, group, and title only | `directory.kingcounty.gov/EmployeeList.asp?txtfname={first}&txtlname={last}` |
| Semantic Scholar | Scholarly profiles | No | Yes | Direct author API, configured 1–5 pages | `api.semanticscholar.org/graph/v1/author/search?offset={offset}` |
| Open Library | Published-author profiles | No | Yes | Direct author API, configured 1–5 pages | `openlibrary.org/search/authors.json?offset={offset}` |
| GitHub | Professional | No | Yes | Direct API, up to 5 pages | `api.github.com/search/users?...&page={page}` |
| Bluesky | Social | No | Yes | Direct public actor API, configured 1–5 pages | `public.api.bsky.app/xrpc/app.bsky.actor.searchActors?cursor={cursor}` |
| LinkedIn API, X, Facebook, Instagram | Professional/social | Yes | Yes | Native APIs inactive without approved authenticated access; public LinkedIn references retrieved through Bing index | Links/forms excluded |
| TikTok, Threads, Snapchat, Pinterest | Social | Yes | Yes | Inactive: no anonymous public people-record API | Links/forms excluded |
| YouTube, Reddit, Tumblr, Twitch | Social | Yes | Yes | Inactive: supported record APIs require credentials or do not expose people search | Links/forms excluded |
| Mastodon | Social | Varies | Yes | Inactive: no complete cross-instance anonymous actor index | Links/forms excluded |
| Flickr, Medium, ResearchGate, Goodreads, Quora | Social/professional | Varies | Yes | Inactive: no suitable anonymous public people-record API | Links/forms excluded |
| Stack Overflow users | Professional | No | Yes | Direct API, up to 5 pages | `api.stackexchange.com/2.3/users?...&page={page}` |
| Google News | News | No | Yes | Browser link | `news.google.com/search?q={query}` |
| Data.gov | Government/open data | No | Yes | Inactive: API unavailable | Legacy CKAN endpoint returned 404 on 2026-08-01 |
| SEC EDGAR | Federal/business | No | Yes | Browser link | `sec.gov/edgar/search/?q={query}` |
| Federal Election Commission | Federal/campaign | No | Yes | Browser link | `fec.gov/data/receipts/individual-contributions/?contributor_name={query}` |
| USAspending.gov | Federal/contracts | No | Yes | Browser link | `usaspending.gov/search/?hash={query}` |
| USPTO public records | Federal/IP | No | Yes | Browser link | Google `site:uspto.gov {query}` |
| State official records | State/licenses/business/courts | No | Yes | Browser link | Google `site:.gov (license OR corporation OR court) {query}` |
| County-specific directory adapters | County employee/public official records | No | Yes | King County active; other stable anonymous official directories can be added while generic official references cover selected counties | Exact-name official directory forms only |
| County property/assessor records | County property records | No | Yes | Excluded: the app does not discover private addresses or property owners | Not queried |
| GitHub username | Username | No | Yes | Browser link | `github.com/{username}` |
| Company website | Domain | No | Yes | Browser link | Google `site:{domain} {query}` |

PACER, paid aggregators, authenticated social scraping, paid unlocks, restricted criminal-history systems, and credentialed/private databases are not providers. Discord, Telegram, private groups, private profiles, and non-public content cannot be searched lawfully without accounts or platform authorization. Government availability and lawful permitted use must be checked at the destination portal.
