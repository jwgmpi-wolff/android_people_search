# Public source matrix

All automatic providers have `Paid = No` and `Auth = No`. Social/profile providers use public search-engine indexing where direct unauthenticated access is uncertain. Patterns show the human-readable template before URL encoding. Last reviewed: 2026-08-01.

| Source | Category | Auth | Paid results excluded | Search URL pattern |
|---|---|---:|---:|---|
| Google | Public web | No | Yes | `google.com/search?q={query}` |
| Bing | Public web | No | Yes | `bing.com/search?q={query}` |
| DuckDuckGo | Public web | No | Yes | `duckduckgo.com/?q={query}` |
| LinkedIn public pages | Professional | No | Yes | Google `site:linkedin.com/in {query}` |
| GitHub | Professional | No | Yes | `github.com/search?q=type:users {query}` |
| X public pages | Social | No | Yes | Google `site:x.com {query}` |
| Facebook public pages | Social | No | Yes | Google `site:facebook.com {query}` |
| Instagram public pages | Social | No | Yes | Google `site:instagram.com {query}` |
| YouTube | Social | No | Yes | `youtube.com/results?search_query={query}` |
| Reddit | Social | No | Yes | `reddit.com/search/?q={query}` |
| Medium | News/blog | No | Yes | `medium.com/search?q={query}` |
| Stack Overflow users | Professional | No | Yes | Google `site:stackoverflow.com/users {query}` |
| Google News | News | No | Yes | `news.google.com/search?q={query}` |
| Data.gov | Government/open data | No | Yes | `catalog.data.gov/dataset/?q={query}` |
| SEC EDGAR | Federal/business | No | Yes | `sec.gov/edgar/search/?q={query}` |
| Federal Election Commission | Federal/campaign | No | Yes | `fec.gov/data/receipts/individual-contributions/?contributor_name={query}` |
| USAspending.gov | Federal/contracts | No | Yes | `usaspending.gov/search/?hash={query}` |
| USPTO public records | Federal/IP | No | Yes | Google `site:uspto.gov {query}` |
| State official records | State/licenses/business/courts | No | Yes | Google `site:.gov (license OR corporation OR court) {query}` |
| County official records | County/assessor/recorder/courts | No | Yes | Google `site:.gov (assessor OR recorder OR parcel OR court) county {query}` |
| GitHub username | Username | No | Yes | `github.com/{username}` |
| Company website | Domain | No | Yes | Google `site:{domain} {query}` |

PACER, paid aggregators, authenticated social scraping, paid unlocks, restricted criminal-history systems, and credentialed/private databases are not providers. Government availability and lawful permitted use must be checked at the destination portal.
