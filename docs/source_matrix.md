# Public source matrix

All active providers have `Paid = No` and `Auth = No`. Every search performs fresh network retrieval with HTTP caching disabled; previous result cards and counts are cleared before requests begin. If the initial pass yields no identity-matched records, Bing and DuckDuckGo general indexes automatically rerun with broader queries. Google and Yahoo result-page automation is excluded by their robots policies. Paginated direct API providers retrieve the user-configured pages and stop on a short page. Counts are calculated after source-observed identity filtering and per-source deduplication. Populated details refine requests and confidence ranking but do not hide exact-name records when snippets omit those details. Submitted fields are never injected into result evidence. Browser links and search forms do not participate in results. Last reviewed: 2026-08-02.

County choices load from the official, keyless Census TIGERweb State/County layer after a U.S. state is selected. Manual county entry remains available when that catalog cannot be reached.

| Source | Category | Auth | Paid results excluded | Retrieval | Search URL pattern |
|---|---|---:|---:|---|---|
| Google | Public web | No | Yes | Browser link | `google.com/search?q={query}` |
| Bing public web references | Publicly accessible records | No | Yes | Refined RSS retrieval plus exact-identity fallback at configured depth; unchanged resubmission adds unquoted name/location, social-profile, `.gov/.edu/.org`, and academic/author passes; GitHub or Stack Overflow is queried and retained only when explicitly named in AI free text; domain/context refinements, partial-result recovery, URL deduplication | `bing.com/search?q={query}&format=rss&first={offset}` |
| LinkedIn public references | Publicly accessible records | No | Yes | Bing-indexed refined retrieval plus exact-name `site:linkedin.com/in` fallback at configured depth; unchanged resubmission adds an unquoted constrained pass; partial-result recovery | Bing RSS `site:linkedin.com/in` |
| DuckDuckGo public web references | Publicly accessible records | No | Yes | Live retrieval from the robots-allowed HTML endpoint; redirect unwrapping, prohibited-host filtering, HTTPS-only records, and URL deduplication; one result page because GET offsets are not honored | `html.duckduckgo.com/html/?q={query}` |
| Selected social public references | Social | No | Yes | Bing RSS query constrained to every selected social domain; defaults to all platforms; only selected HTTPS social hosts survive parsing and identity filtering | Bing RSS with combined selected `site:` constraints |
| Wikipedia | Public web | No | No | Inactive: snippet search produced unrelated identity results | Removed 2026-08-01 |
| Wikidata | Open knowledge graph | No | No | Inactive: entity search was too broad for people matching | Removed 2026-08-01 |
| NPI Registry | US federal public records | No | Yes | Direct API, configured 1–5 pages | `npiregistry.cms.hhs.gov/api/?version=2.1&skip={offset}` |
| Official county public records | County public records | No | Yes | Two-pass state-and-county-aware Bing RSS retrieval: broad exact-name official pages plus records/agendas/minutes/notices/boards/commissions/courts/dockets/permits/licenses/elections/contracts; HTTPS `.gov` results only; URL deduplication | Bing RSS with exact name, selected county, selected state, and `site:.gov` |
| King County employee directory | County employee/public official records | No | Yes | Direct exact-name form retrieval; public work name, group, and title only | `directory.kingcounty.gov/EmployeeList.asp?txtfname={first}&txtlname={last}` |
| Semantic Scholar | Scholarly profiles | No | Yes | Direct author API, configured 1–5 pages | `api.semanticscholar.org/graph/v1/author/search?offset={offset}` |
| Open Library | Published-author profiles | No | Yes | Direct author API, configured 1–5 pages | `openlibrary.org/search/authors.json?offset={offset}` |
| GitHub | Professional | No | Yes | Opt-in only when GitHub is explicitly named in AI free text; direct API, up to 5 pages | `api.github.com/search/users?...&page={page}` |
| Bluesky | Social | No | Yes | Direct public actor API, configured 1–5 pages | `public.api.bsky.app/xrpc/app.bsky.actor.searchActors?cursor={cursor}` |
| LinkedIn API, X, Facebook, Instagram | Professional/social | Yes | Yes | Native APIs inactive without approved authenticated access; selected publicly indexed pages are retrieved without login; LinkedIn also has a constrained public-reference provider | Selected public-page index only |
| TikTok, Threads, Snapchat, Pinterest | Social | No | Yes | Selected publicly indexed pages only; no login, native account API, private content, or account scraping | Selected public-page index only |
| YouTube, Reddit, Tumblr, Twitch | Social | Yes | Yes | Inactive: supported record APIs require credentials or do not expose people search | Links/forms excluded |
| Mastodon | Social | Varies | Yes | Selected public pages on `mastodon.social`; no complete cross-instance anonymous actor index | Selected public-page index only |
| Flickr, Medium, ResearchGate, Goodreads, Quora | Social/professional | Varies | Yes | Inactive: no suitable anonymous public people-record API | Links/forms excluded |
| Stack Overflow users | Professional | No | Yes | Opt-in only when Stack Overflow is explicitly named in AI free text; direct API, up to 5 pages | `api.stackexchange.com/2.3/users?...&page={page}` |
| Google News | News | No | Yes | Browser link | `news.google.com/search?q={query}` |
| Data.gov | Government/open data | No | Yes | Inactive: API unavailable | Legacy CKAN endpoint returned 404 on 2026-08-01 |
| SEC EDGAR | Federal/business | No | Yes | Browser link | `sec.gov/edgar/search/?q={query}` |
| Federal Election Commission | Federal/campaign | No | Yes | Browser link | `fec.gov/data/receipts/individual-contributions/?contributor_name={query}` |
| USAspending.gov | Federal/contracts | No | Yes | Browser link | `usaspending.gov/search/?hash={query}` |
| USPTO public records | Federal/IP | No | Yes | Browser link | Google `site:uspto.gov {query}` |
| State official records | State/licenses/business/courts | No | Yes | Browser link | Google `site:.gov (license OR corporation OR court) {query}` |
| County-specific directory adapters | County employee/public official records | No | Yes | King County is an additional direct adapter; generic official public-record retrieval covers every Census-selected county/county equivalent | Exact-name official directory forms only |
| County property/assessor records | County property records | No | Yes | Excluded: the app does not discover private addresses or property owners | Not queried |
| GitHub username | Username | No | Yes | Browser link | `github.com/{username}` |
| Company website | Domain | No | Yes | Browser link | Google `site:{domain} {query}` |

PACER, paid aggregators, authenticated social scraping, paid unlocks, restricted criminal-history systems, and credentialed/private databases are not providers. Discord, Telegram, private groups, private profiles, and non-public content cannot be searched lawfully without accounts or platform authorization. Government availability and lawful permitted use must be checked at the destination portal.
