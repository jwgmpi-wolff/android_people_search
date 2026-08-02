---
name: "Regenerate Android People Search"
description: "Regenerate the complete Android People Search app from an empty workspace with every build setting, provider, screen, privacy rule, test, CI workflow, and installable APK requirement."
argument-hint: "Optionally describe a destination folder or required compatible version change"
agent: "agent"
---

# Regenerate Android People Search

Build the complete **Android People Search** native Android application from an empty workspace. Implement every requirement below; do not produce only a plan, sample, mockup, partial scaffold, pseudocode, browser-only search form, or TODO placeholder. Continue through source generation, unit tests, APK assembly, and validation.

If files already exist, inspect them first, preserve compatible user changes, and make the smallest changes needed to reach this specification. Never delete unrelated work. Do not commit credentials or generated build directories.

## Product contract

Create a privacy-conscious public-source people research app that:

- Runs every compliant provider automatically for each search.
- Retrieves and displays actual public records or public profile references, not browser search forms.
- Displays candidates with source-observed identity evidence. Populated fields refine provider queries and confidence ranking, while exact-name records remain visible when snippets omit those details; never copy submitted values into result text to manufacture a match.
- Deduplicates records by canonical result URL within each provider and across providers.
- Shows a final post-deduplication count for every provider, including `0 found`.
- Distinguishes provider failures as `Rate limited`, `Timed out`, `Source unavailable`, or `Unavailable`.
- Automatically reviews every displayed record with deterministic on-device logic.
- Treats confidence as an explainable lead score, never proof of identity.
- Opens attributed results in Android Custom Tabs and supports copying each URL.
- Requires no account sign-in, API key, paid API, paid provider, cloud AI, analytics, or secret.
- Keeps search history disabled by default and provides a working clear-local-data action.
- Supports Android API 23 and later.

## Mandatory safety and source policy

Every source must be public, free, legally accessible, and compatible with its terms and robots policy.

Never implement:

- Account scraping or authenticated LinkedIn access.
- Paywall, CAPTCHA, authentication, or robots-policy bypass.
- Credential discovery, breach lookup, dark-web lookup, SSN lookup, or password lookup.
- Private-address discovery, private-data discovery, property-owner discovery, or deanonymization.
- Paid people finders, contact enrichment, data brokers, restricted legal databases, credit, medical, financial, driver, license-plate, or restricted criminal-history sources.
- Bulk export of personal information.

Block references from these hosts and their subdomains:

- `fastpeoplesearch.com`
- `spokeo.com`
- `beenverified.com`
- `peoplefinders.com`
- `truthfinder.com`
- `intelius.com`
- `contactout.com`
- `wiza.co`
- `publicrecords.info`

LinkedIn support must use public Bing RSS references constrained to `site:linkedin.com/in`; it must not require LinkedIn sign-in, cookies, scraping, or a LinkedIn API token.

## Exact toolchain and build settings

Create a single-module Gradle Android application using Kotlin DSL and a version catalog.

- Gradle wrapper: `8.9`, binary distribution.
- Android Gradle Plugin: `8.7.3`.
- Kotlin and Kotlin Compose plugin: `2.1.0`.
- Java source and target compatibility: `17`.
- Kotlin JVM target: `17`.
- Namespace/application ID: `com.jerrywolff.androidpeoplesearch`.
- `compileSdk = 35`.
- `targetSdk = 35`.
- `minSdk = 23`.
- `versionCode = 2`.
- `versionName = "1.1.0"`.
- Test runner: `androidx.test.runner.AndroidJUnitRunner`.
- Compose and generated `BuildConfig` enabled.
- Vector drawable support library enabled.
- Exclude `/META-INF/{AL2.0,LGPL2.1}` from packaged resources.
- Release minification enabled with `proguard-android-optimize.txt` and `app/proguard-rules.pro`.
- Define `ENABLE_VERBOSE_LOGGING=false` in debug and release build types.
- Do not add reflection-dependent application code or unnecessary ProGuard rules.

Use these dependency versions exactly:

- `androidx.core:core-ktx:1.15.0`
- `androidx.lifecycle:lifecycle-runtime-ktx:2.8.7`
- `androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7`
- `androidx.activity:activity-compose:1.10.0`
- Compose BOM `2025.01.01`
- Compose UI, graphics, tooling preview, Material 3, and extended Material icons through the BOM
- `androidx.navigation:navigation-compose:2.8.5`
- `androidx.datastore:datastore-preferences:1.1.2`
- `androidx.browser:browser:1.8.0`
- `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1`
- JUnit `4.13.2`
- `org.json:json:20250517` for local tests
- AndroidX JUnit `1.2.1`
- Espresso `3.6.1`
- Compose UI test JUnit4 and test manifest through the Compose BOM

Set root project name to `Android People Search` and include only `:app`. Plugin management repositories are `google()`, `mavenCentral()`, and `gradlePluginPortal()`. Dependency resolution uses `RepositoriesMode.FAIL_ON_PROJECT_REPOS` with `google()` and `mavenCentral()`.

Set these exact `gradle.properties` values:

```properties
org.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

Ignore at least:

- `.gradle/`, `.idea/`, `.kotlin/`, `local.properties`
- all `build/` directories and `dist/`
- `*.jks`, `*.keystore`, `keystore.properties`, `secrets.properties`
- `google-services.json`, logs, captures, native build output, and OS metadata

## Manifest and Android resources

Request only `android.permission.INTERNET`.

Configure one exported `MainActivity` with `MAIN` and `LAUNCHER` intent filters. Set:

- `allowBackup=false`
- app label `Android People Search`
- adaptive and round launcher icons
- theme `Theme.AndroidPeopleSearch`
- network security config with cleartext HTTP disabled
- data extraction/backup rules that exclude app preferences

Create light and dark Material 3 themes. Use an intentional multi-hue palette centered on teal, amber, and rose rather than a one-note blue/purple theme. Preserve these principal colors where supported:

- Light primary `#006A67`
- Light secondary `#825500`
- Light tertiary `#9B4050`
- Light background `#F8FAF7`
- Dark primary `#58D9D3`
- Dark secondary `#FFBA49`
- Dark tertiary `#FFB1BD`
- Dark background `#171C1B`

Create an adaptive three-person launcher icon with round and legacy variants. Do not use remote image assets.

## Required package and file architecture

Use this package structure:

```text
com.jerrywolff.androidpeoplesearch/
  MainActivity.kt
  model/Models.kt
  search/SearchProvider.kt
  search/ProviderCatalog.kt
  search/DirectApiProviders.kt
  search/PeopleSearchEngine.kt
  search/ProviderUrlBuilder.kt
  search/PublicApiClient.kt
  search/ConfidenceScorer.kt
  search/CountyCatalog.kt
  ai/AiAssistant.kt
  ai/LocalHeuristicAssistant.kt
  data/SettingsRepository.kt
  ui/AndroidPeopleSearchApp.kt
  ui/PeopleSearchViewModel.kt
  ui/Screens.kt
  ui/theme/Theme.kt
```

Use Kotlin coroutines, immutable data classes, `StateFlow`, `AndroidViewModel`, Jetpack Compose Material 3, and Navigation Compose. Keep HTTP and provider logic outside composables.

## Exact data models

In `Models.kt`, implement:

```kotlin
enum class SourceCategory(val label: String) {
    WEB("Public web"),
    SOCIAL("Social profiles"),
    PROFESSIONAL("Business & professional"),
    PUBLIC_RECORDS("Government & public records"),
    NEWS("News & public mentions"),
    DOMAIN("Domain-specific"),
    USERNAME("Username search"),
}

enum class ConfidenceLevel(val label: String) {
    HIGH("High"), MEDIUM("Medium"), LOW("Low")
}
```

`PeopleSearchQuery` must have empty-string defaults for:

- `firstName`, `lastName`, `fullName`
- `city`, `county`, `state`, `country`
- `company`, `emailDomain`, `username`, `freeText`

Implement:

- `normalized()`: trim fields, collapse whitespace, remove leading `@` from email domain and username.
- `personName()`: use `fullName`, otherwise join first and last name.
- `searchText()`: combine name, location, company, domain, username, and free text; quote multiword name/company and express domain as `site:<domain>`.
- `tokens()`: lowercase alphanumeric tokens longer than one character, excluding `the`, `and`, `or`, and `site`.
- `isBlank()`.

`PersonSearchResult` must contain:

- `id`, `providerId`, `title`, `description`, `url`, `sourceName`
- `category`, `confidence`, `confidenceScore`
- `extractedSignals: List<String> = emptyList()`
- `retrievedAtEpochMillis = System.currentTimeMillis()`
- `isDirectResult: Boolean = false`

Also create `AiSummary`, `NormalizedName`, `SearchSession`, and:

```kotlin
data class AppSettings(
    val historyEnabled: Boolean = false,
    val privacyMode: Boolean = true,
    val pageDepth: Int = 5,
)
```

Clamp page depth to `1..5` whenever persisted or used.

## Provider interface and HTTP client

`SearchProvider` must expose:

- `id`, `displayName`, `category`
- `requiresPayment`, `requiresAuth`, `lastVerified`
- `buildSearchUrl(query): Uri`
- `search(query)`
- `search(query, pageDepth)` with a default delegation

All active providers must report `requiresPayment=false`, `requiresAuth=false`, and `lastVerified="2026-08-01"`.

Create a GET-only `PublicApiClient` abstraction and `HttpPublicApiClient` based on `HttpURLConnection`:

- HTTPS only.
- `connectTimeout = 8_000` milliseconds.
- `readTimeout = 10_000` milliseconds.
- Request method `GET`.
- Header `Accept: application/json`.
- Disable connection and default caches, and send `Cache-Control: no-cache, no-store, max-age=0` plus `Pragma: no-cache` so every search uses live responses.
- Header `User-Agent: AndroidPeopleSearch/1.0 (public-source research app)`.
- Return response text for HTTP 2xx.
- Throw `PublicApiHttpException(statusCode)` for non-2xx responses.
- Close streams and disconnect reliably.

Use `android.net.Uri` or structured URL APIs for encoding. Do not concatenate unencoded user input into URLs.

## Exact provider catalog

Register these twelve providers in this order:

1. `BingPublicReferencesProvider()`
2. `DuckDuckGoPublicReferencesProvider()`
3. `SelectedSocialReferencesProvider()`
4. `BingPublicReferencesProvider(linkedInOnly = true)`
5. `BlueskyActorsApiProvider()`
6. `CountyOfficialReferencesProvider()`
7. `KingCountyEmployeesProvider()`
8. `NpiRegistryApiProvider()`
9. `SemanticScholarAuthorsApiProvider()`
10. `OpenLibraryAuthorsApiProvider()`
11. `GitHubUsersApiProvider()`
12. `StackOverflowUsersApiProvider()`

### Direct provider base

Create `DirectApiProvider` that:

- Normalizes the query.
- Clamps requested pages to `1..5`.
- Defaults parameterless search to five pages.
- Builds `PersonSearchResult` through `ConfidenceScorer`.
- Marks results `isDirectResult=true`.

### Bing public web references

- ID `bing_web`; display name `Bing public web references`; category `PUBLIC_RECORDS`.
- Use `https://www.bing.com/search` with `format=rss` and `first=1,11,21,31,41`.
- Query includes quoted person name plus nonblank city, county, state, country, company, and username.
- Parse RSS with a DOM parser.
- Explicitly reject any document containing `<!DOCTYPE` before parsing.
- Accept only nonblank titles and HTTPS links.
- Clean HTML entities/markup from descriptions.
- Filter every prohibited host listed in the safety policy.
- Stop pagination when fewer than ten RSS items are returned.

### LinkedIn public references

- Same provider implementation with `linkedInOnly=true`.
- ID `bing_linkedin`; display name `LinkedIn public references`; category `PUBLIC_RECORDS`.
- Add `site:linkedin.com/in` to the Bing query.
- Return publicly indexed references without LinkedIn authentication.
- Apply the same prohibited-host and XML protections.

### Official county public records

- ID `county_official_references`; display name `Official county public records`; category `PUBLIC_RECORDS`.
- Run only when person name, county, and state are nonblank.
- Run a broad exact-name query containing the official county label, state, and `site:.gov` for the configured page depth.
- Run one additional query for the same identity/location with `(record OR agenda OR minutes OR notice OR board OR commission OR court OR docket OR permit OR license OR election OR contract) site:.gov`.
- Preserve county-equivalent suffixes: County, Parish, Borough, Census Area, Municipality; append `County` only when no suffix exists.
- Deduplicate records returned by the two query passes by URL.
- Retain only HTTPS results whose host is exactly `gov` or ends in `.gov`.
- Do not limit generic county retrieval to employee or staff directories.
- Exclude commercial report generators such as `publicrecords.info`, property-owner discovery, and private-address records.

### King County employee directory

- ID `king_county_employees`; display name `King County employee directory`; category `PUBLIC_RECORDS`.
- Run only for King County and Washington/WA.
- Require an exact first and last name derived from structured fields or a full name.
- GET `https://directory.kingcounty.gov/EmployeeList.asp` with `txtfname`, `txtlname`, and `btnSearch=Search` query parameters.
- Parse employee table rows and `EmpID` links.
- Return employee name, organization/group, title, and official employee detail URL.
- Deliberately omit phone number and mailstop.

### NPI Registry

- ID `npi_registry`; display name `NPI Registry`; category `PUBLIC_RECORDS`.
- Use `https://npiregistry.cms.hhs.gov/api/`, version `2.1`, enumeration type `NPI-1`, aliases disabled, limit 20, and skip offsets.
- Include first/last name and optional city, state, and ISO country code.
- Parse individual provider basic data, first address, and primary taxonomy into an attributed public professional record.
- Stop on a short page.

### Semantic Scholar authors

- ID `semantic_scholar`; display name `Semantic Scholar authors`; category `PROFESSIONAL`.
- Use `https://api.semanticscholar.org/graph/v1/author/search` with name, limit 20, offset, and fields `name,affiliations,paperCount,url`.
- Parse author ID, name, affiliations, paper count, and URL.
- Stop on a short page.

### Open Library authors

- ID `open_library`; display name `Open Library authors`; category `PROFESSIONAL`.
- Use `https://openlibrary.org/search/authors.json` with name, limit 20, and offset.
- Parse author key, name, top work, and work count.
- Build `https://openlibrary.org/authors/<key>` detail URLs.
- Stop on a short page.

### Bluesky public profiles

- ID `bluesky`; display name `Bluesky public profiles`; category `SOCIAL`.
- Search username, otherwise person name, otherwise free text.
- Use `https://public.api.bsky.app/xrpc/app.bsky.actor.searchActors` with limit 20 and cursor pagination.
- Parse DID, handle, display name, and description.
- Link to `https://bsky.app/profile/<handle>`.

### Selected social public references

- Add a multi-select query field and Compose dropdown for Facebook, Instagram, Threads, X, Bluesky, LinkedIn, Snapchat, TikTok, Pinterest, and Mastodon; default to all and provide Select all.
- Query Bing RSS with the requested identity plus combined `site:` constraints for only selected domains.
- Keep only HTTPS records whose destination host belongs to a selected platform, then apply the mandatory source-observed identity gate.
- Do not execute direct LinkedIn or Bluesky providers when their platforms are deselected. Do not scrape accounts, authenticated pages, private content, or CAPTCHA-protected pages.

### GitHub public users

- ID `github`; display name `GitHub public users`; category `PROFESSIONAL`.
- Search username first; otherwise compact the person name to lowercase alphanumeric; otherwise use free text.
- Use `https://api.github.com/search/users` with `per_page=20` and page pagination.
- Parse numeric ID, login, and `html_url`.
- Stop on a short page.

### Stack Overflow public users

- ID `stackoverflow`; display name `Stack Overflow public users`; category `PROFESSIONAL`.
- Search person name, otherwise username, otherwise free text.
- Use `https://api.stackexchange.com/2.3/users` with site `stackoverflow`, `pagesize=20`, page, and `inname`.
- Parse user ID, display name, reputation, optional location, and link.
- Stop when `has_more=false` or a short page is returned.

## County catalog

Use the keyless official Census TIGERweb State/County service:

`https://tigerweb.geo.census.gov/arcgis/rest/services/TIGERweb/State_County/MapServer/1/query`

- Maintain an insertion-ordered mapping for all 50 states plus District of Columbia to two-digit state FIPS codes.
- Query layer 1 with `where=STATE='<FIPS>'`, `outFields=NAME`, `returnGeometry=false`, and `f=json`.
- Parse `features[].attributes.NAME`.
- Return sorted distinct county/county-equivalent names.
- Return no network request and an empty list for an unknown/blank state.
- Expose all 51 jurisdiction names through `usStateNames()`.
- UI must allow manual county entry if loading fails.

## Search engine and diagnostics

Run all providers concurrently with structured coroutines, excluding any provider that requires payment or authentication.

Create:

```kotlin
data class ProviderFailure(val providerName: String, val reason: String = "Unavailable")
data class SourceResultCount(
    val providerName: String,
    val recordCount: Int,
    val failed: Boolean,
    val failureReason: String? = null,
)
data class SearchReport(
    val results: List<PersonSearchResult>,
    val failures: List<ProviderFailure>,
    val sourceCounts: List<SourceResultCount>,
)
```

Behavior:

1. Coerce page depth to `1..5`.
2. Catch each provider independently so one failure never discards successful results.
3. Map HTTP 429 to `Rate limited`.
4. Map HTTP 500 through 599 to `Source unavailable`.
5. Map `SocketTimeoutException` to `Timed out`.
6. Map other exceptions to `Unavailable`.
7. Require every requested name token or the compact full name in source title, description, or URL. If no name was supplied, require the supplied username.
8. Use populated company, city, county, state, domain, country, username, and free-text fields to refine provider requests and add confidence only when source-observed. Do not reject an exact-name record merely because an index snippet omits a populated refinement.
9. For Bing public web and LinkedIn references, include domain and free-text context in the populated-field query, run both that query and the exact-identity fallback for the configured page depth, preserve successful earlier records if a later request fails, then deduplicate URLs.
10. If the complete initial pass has no post-filter records, automatically rerun Bing web, Bing LinkedIn, and DuckDuckGo with breadth level 1. Track consecutive submissions of the same normalized fields. An unchanged resubmission must make fresh network requests and add, in priority order: an unquoted name plus city/state query; a social-profile query across Facebook, Instagram, Threads, X, Bluesky, and Mastodon; an authoritative `.gov/.edu/.org` query; and an academic/author query. Add GitHub or Stack Overflow constrained queries only when that source is explicitly named in AI free text. Add an unquoted `site:linkedin.com/in` query for LinkedIn. Every eligible direct provider must also rerun live. Field edits reset this broader pass. Keep source-observed identity filtering unchanged.
11. Never inject submitted fields into result descriptions or confidence evidence.
12. Deduplicate each provider result list by URL and report counts from the exact visible per-provider lists.
13. Merge and deduplicate globally by URL.
14. Present web and social providers before other sources. Do not execute or display GitHub or Stack Overflow, including developer URLs returned through general indexes, unless the corresponding source is explicitly named in AI free text. When requested, sort those results after all other matching sources; within each tier sort direct results first, then confidence descending, then source name.
15. Keep identity-matching low-confidence leads visible; reject candidates that fail identity evidence.

## Confidence scoring

Create `ConfidenceScore(score, level, signals)`.

Extract explainable signals by testing normalized query values against combined result title, description, and URL:

- Name
- Company
- City
- County
- State
- Country
- Username
- Domain

Use source weights:

- `PUBLIC_RECORDS`: 35
- `PROFESSIONAL` and `DOMAIN`: 25
- `NEWS` and `WEB`: 20
- `SOCIAL` and `USERNAME`: 10

Score = source weight + 12 per observed signal, clamped to 0 through 100.

- HIGH only when score is at least 70 and at least three signals are observed.
- MEDIUM when score is at least 40 and at least one signal is observed.
- LOW otherwise.

Never describe confidence as identity verification.

## Deterministic on-device assistant

Do not add Gemini Nano, cloud AI, or a fake availability adapter. Implement `AiAssistant` and `LocalHeuristicAssistant` as the active assistant.

`AiAssistant` provides:

- `summarizeResults(results)`
- `explainMatch(query, result)`
- `normalizeName(input)`
- `refineSearchQuery(query, input)`

Create `SearchRefinement(query, reply, appliedFields)`.

Assistant behavior:

- Review all displayed records.
- Summarize counts grouped by source and category plus high/medium/low confidence totals.
- Headline: `Reviewed all N displayed record(s)`.
- Caveats: targets are leads, not verified matches; confirm details at attributed sources.
- Explain no-signal results as broad public-source leads.
- Normalize names deterministically by whitespace splitting and title casing.
- Parse labeled `name`, `city`, `state`, `country`, and `company` values.
- Parse `<name> County`, `works/worked/employed at/by`, username/handle, email domain, known country names, and lives/located/based in city phrases.
- Append unrecognized text to `freeText` rather than losing it.
- Suggest up to two missing details among county, country, company, and username.

## DataStore settings and privacy

Use Preferences DataStore named `privacy_settings` with:

- `history_enabled`: false by default.
- `privacy_mode`: true by default.
- `page_depth`: 5 by default and always clamped to 1 through 5.
- `search_history`: string set, maximum 20 entries, newest first, encoded as timestamp/search text/result count without sensitive enrichment.

Implement:

- A `Flow<AppSettings>`.
- `setHistoryEnabled`; when disabling history, delete existing history immediately.
- `setPrivacyMode`.
- `setPageDepth`.
- `record(SearchSession)` only when history is enabled.
- `clearLocalData()` to clear every preference and history entry.

Do not enable Android backup for preferences. Set `allowBackup=false`, `fullBackupContent=false`, and use data extraction rules that exclude `sharedpref` path `.` from both encrypted cloud backup and device transfer. The cloud-backup rule must set `disableIfNoEncryptionCapabilities=true`. Do not add analytics, ads, tracking, background workers, contacts, location, storage, camera, microphone, or phone permissions.

## ViewModel state and behavior

Create `SearchChatMessage(fromAssistant, text)` and `SearchUiState` with defaults for:

- query, results, selected result, summary, settings
- `isSearching=false`
- failed providers and source counts
- county options, county-loading flag, county-load-failed flag
- initial assistant message: `Add details about the person, location, employer, domain, or username.`
- optional UI message

`PeopleSearchViewModel` must:

- Own `PeopleSearchEngine`, `CountyCatalog`, `LocalHeuristicAssistant`, and `SettingsRepository`.
- Collect settings into StateFlow.
- Clear stale county when state changes.
- Automatically set country to United States when a U.S. state is selected.
- Load counties asynchronously and ignore stale responses if the state changes during loading.
- Preserve manual county entry and indicate loading failure.
- Refine search from assistant chat and retain the last messages.
- Reject a completely blank search with a useful UI message.
- Clear old results/counts/errors when a new search starts.
- Run engine search at configured page depth.
- Generate automatic review from the exact displayed result list.
- Record sessions only through the repository’s opt-in history behavior.
- Before each search, clear prior results, selected result, summary, failures, and source counts so stale records are never presented as live retrieval.
- Expose result selection, explanation, settings mutation, and clear-local-data actions.

## Navigation and UI

Use a single-activity Compose app with routes:

- `home`
- `results`
- `detail`
- `settings`
- `summary`
- `privacy`
- `about`

Use a Material 3 scaffold and top app bar with access to Sources & settings, Privacy & safety, and About.

### Home screen

Provide:

- Public-source notice.
- Full name field.
- First and last name fields in a responsive row.
- City and U.S. state selector.
- Editable county selector populated from TIGERweb.
- Country selector populated from ISO locale country names.
- Company field.
- Email domain and username fields.
- Search assistant showing the last four messages.
- Multiline assistant input and icon send button.
- Search button disabled while searching.
- Clear button.
- Text stating all compliant public source categories run automatically.

Do not add marketing hero content or tutorial cards.

### Results screen

Provide:

- Back navigation and public notice.
- `Records found by source` with every registered provider in catalog order.
- Count text `<N> found` for successful providers, including zero.
- Accurate colored failure reason for failed providers.
- Distinct no-results text depending on whether providers failed.
- Automatic review headline, summary, and caveats.
- A `Records` section containing every displayed result.
- Stable result cards with title, confidence chip, source, category, provenance, description, URL, Open source, and Copy link.

Provenance labels:

- `bing_linkedin`: `Public LinkedIn reference`
- `bing_web`: `Public web index`
- `county_official_references`: `Official county public record`
- `king_county_employees`: `Official county directory`
- Others: `Direct API`

### Detail screen

Show title, source/category, confidence, low-confidence warning, deterministic explanation, extracted query signals, URL, Open source, Copy, and the submitted query.

### Settings screen

Show:

- Automatic source coverage statement.
- Page-depth decrement/increment controls with stable dimensions and range 1 through 5.
- Text explaining up to `pageDepth * 20` records.
- History toggle, off by default, with deletion explanation.
- Privacy mode toggle, on by default.
- `Deterministic on-device assistant active` status.
- Clear local data button and completion message.

### Privacy and About

Privacy copy must state lawful/respectful use, prohibit stalking/harassment/doxxing/credential or private-address discovery, explain no auth/paywall/CAPTCHA/robots bypass, warn that results may be incomplete or inaccurate, and explain history defaults.

About must show version `1.1.0`, Kotlin/Compose architecture, no keys/paid providers/analytics/cloud AI, and attributed Custom Tab behavior.

Use current Compose APIs: auto-mirrored directional icons, typed `menuAnchor` overloads, and `HorizontalDivider`. Avoid deprecated API warnings.

## Tests required

Use deterministic fakes; unit tests must not require live network access.

Create unit tests covering at least:

### Query models

- Trimming/canonicalization and leading `@` removal.
- Quoted multiword search text and token filtering.

### Confidence

- Official record plus at least three signals yields HIGH.
- Social result with no observed signals yields LOW.
- County and country appear as explainable signals.

### Engine

- Every free/no-auth category runs automatically.
- Paid/auth providers are skipped.
- One provider failure does not discard successful records.
- HTTP 429 becomes `Rate limited`.
- Unrelated identities are rejected while exact-name rows remain visible when snippets omit refinements.
- Populated refinements affect provider queries and confidence only when observed, with state-name/postal-code equivalence.
- Per-source counts match visible deduplicated records.
- Compact username/profile URL leads remain visible.

### Providers and URLs

- Reserved characters and Unicode are URL encoded.
- Catalog contains no paid/auth providers.
- Bing and LinkedIn RSS retrieval and short-page stopping.
- Prohibited hosts are removed.
- RSS containing DOCTYPE is rejected.
- County query uses selected state and county and keeps only `.gov` HTTPS URLs.
- County-equivalent suffixes are preserved.
- County provider no-ops when required fields are missing.
- King County exact-name submission and row parsing.
- King County results omit phone and mailstop.
- All JSON direct-provider parsers and pagination boundaries.

### County catalog

- 51 U.S. jurisdictions.
- Washington maps to FIPS 53.
- County names sort and deduplicate.
- Unknown states make no request.

### Assistant and persistence

- Summary reviews every displayed record, including an empty set.
- Structured conversational refinement.
- Unrecognized context is preserved.
- History is off by default.
- Disabling history clears entries.
- Page depth clamps to 1 through 5.
- Clear local data clears all keys.

Create a Compose instrumentation smoke test that launches the app and asserts `Search public sources` is visible.

## CI, documentation, and release output

Create `.github/workflows/android.yml`:

- Trigger on pushes to `main` and pull requests.
- `permissions: contents: read`.
- Ubuntu latest.
- Checkout v4.
- Temurin Java 17 with Gradle cache.
- `android-actions/setup-android@v3`.
- Run `chmod +x gradlew` before Gradle.
- Run `./gradlew testDebugUnitTest assembleDebug --stacktrace`.
- Upload `app/build/outputs/apk/debug/app-debug.apk` using upload-artifact v4.

Create and keep synchronized:

- `README.md` with features, exact prerequisites, local build/test/install commands, privacy summary, provider behavior, and direct APK release links.
- `ARCHITECTURE.md` with UI → ViewModel → engine/providers/assistant/store boundaries.
- `PRIVACY.md`.
- `SOURCES_POLICY.md`.
- `docs/source_matrix.md` listing active and intentionally inactive sources, auth/payment status, retrieval method, URL pattern, restrictions, and review date.
- `.github/copilot-instructions.md` preserving all source, privacy, test, and confidence guardrails.

Provide PowerShell commands:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
```

The direct-install artifact must be a valid signed debug APK for sideload testing. Keep signing keys out of Git. Clearly label debug-signed artifacts as unsuitable for Play Store publication.

## Completion gates

Do not finish until all are true:

1. No `TODO`, `FIXME`, placeholder implementation, dead adapter, or fake availability path remains.
2. No paid/auth provider is active.
3. No secret, key, cookie, credential, generated build directory, or signing file is committed.
4. Every provider has URL/parser/pagination tests appropriate to its behavior.
5. `docs/source_matrix.md` exactly matches `ProviderCatalog`.
6. Only candidates matching source-observed identity reach the visible result list after URL deduplication; populated refinements improve retrieval and ranking without suppressing exact-name records.
7. History is disabled by default and clear-local-data works.
8. Cleartext traffic is disabled and INTERNET is the only permission.
9. `testDebugUnitTest` passes.
10. `assembleDebug` passes without Kotlin/Compose deprecation warnings.
11. The Compose smoke test passes when a device/emulator is available; otherwise report that limitation explicitly.
12. Report the APK path, size, SHA-256, test results, and any remaining environmental limitation.

Implement, test, and validate the complete application now.
