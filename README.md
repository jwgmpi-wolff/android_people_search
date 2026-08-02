# Android People Search

A native Android app that generates attributed search targets for free, legally accessible public sources. It uses no paid result APIs, secret keys, cloud AI, analytics, account scraping, or private databases.

Results are leads, not verified identities. The app labels confidence, links to every source, keeps history off by default, and provides a clear-local-data action.

## Direct download

Download the installable Android APK from either repository mirror:

- [jerrywolff_microsoft APK](https://github.com/jerrywolff_microsoft/android_people_search/releases/latest/download/android-people-search-1.0.0.apk)
- [jwgmpi-wolff APK](https://github.com/jwgmpi-wolff/android_people_search/releases/latest/download/android-people-search-1.0.0.apk)

Allow installation from your browser or file manager when Android prompts, or install from a connected computer:

```powershell
adb install -r android-people-search-1.0.0.apk
```

## Features

- Kotlin, Jetpack Compose, Material 3, and Navigation Compose
- Public web, profile, professional, news, domain, username, and government-record search targets
- Every search automatically runs all compliant direct-record and public-index providers and shows a final record count for each source
- County filtering, U.S. state and country selectors, and an on-device search-assistant chat for applying conversational specifics
- A persisted 1–5 page-depth setting controls direct retrieval (up to 20 records per page and 100 per source)
- Returned candidates must contain the requested identity; populated details refine provider queries and rank source-observed matches without hiding exact-name records whose snippets omit those details
- Deterministic on-device query refinement, normalization, duplicate removal, confidence scoring, explanations, and summaries
- Deterministic on-device assistant with no cloud dependency
- DataStore settings and opt-in-only local history
- Adaptive three-person launcher icon, including round and legacy variants
- Unit tests, Compose smoke test, and GitHub Actions build

## Prerequisites

- Android Studio Ladybug or newer
- Android SDK Platform 35 and Build Tools 35
- JDK 17
- An emulator or Android device running API 23 or later
- Git; GitHub CLI is optional

No API keys are required. Do not add `local.properties`, signing keys, tokens, cookies, or credentials to Git.

## Open and run in Android Studio

1. Open `C:\Users\jerrywolff\Documents\.git\android_people_search`.
2. Allow Gradle sync to finish and install SDK 35 if prompted.
3. Select the `app` run configuration and an API 23+ emulator/device.
4. Click **Run**.

The app requests only Internet permission. It does not request contacts, location, storage, camera, microphone, or phone permissions.

## Open in VS Code

```powershell
cd "C:\Users\jerrywolff\Documents\.git\android_people_search"
code .
```

Install the recommended extensions when prompted. Android Studio remains the recommended emulator, SDK Manager, and visual preview host; VS Code can edit Kotlin and run Gradle tasks.

## Build and test

```powershell
cd "C:\Users\jerrywolff\Documents\.git\android_people_search"
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

Or run:

```powershell
.\scripts\build_debug.ps1
```

The debug APK is written to `app\build\outputs\apk\debug\app-debug.apk`.

For an unsigned/minified release build:

```powershell
.\gradlew.bat assembleRelease
```

Configure Android signing outside the repository before publishing. Never commit a keystore or its password.

## Initial repository commands

The requested PowerShell setup sequence is:

```powershell
cd "C:\Users\jerrywolff\Documents\.git"
mkdir android_people_search
cd android_people_search
git init
git status
```

Because this generated project already exists, initialize with `scripts\init_repo.ps1` or `git init`, then commit and create the GitHub repository:

```powershell
git add .
git commit -m "Initial Android People Search app scaffold"
gh repo create android_people_search --private --source . --remote origin --push
```

If GitHub CLI is not installed, create an empty private repository on GitHub, then run:

```powershell
git remote add origin https://github.com/<your-account>/android_people_search.git
git branch -M main
git push -u origin main
```

GitHub authentication is required only for creating or pushing the remote repository.

## Architecture and policies

- [Architecture](ARCHITECTURE.md)
- [Migration notes](MIGRATION_NOTES.md)
- [Privacy](PRIVACY.md)
- [Sources policy](SOURCES_POLICY.md)
- [Source matrix](docs/source_matrix.md)
- [Roadmap](ROADMAP.md)

## Regenerate with GitHub Copilot

The workspace includes the complete [`/regenerate-android-people-search`](.github/prompts/regenerate-android-people-search.prompt.md) prompt. In VS Code Chat, type `/regenerate-android-people-search` or run **Chat: Run Prompt...** and select **Regenerate Android People Search**. The prompt specifies the exact toolchain, providers, settings, UI, privacy controls, tests, CI workflow, and APK completion gates needed to rebuild the app from an empty workspace.

## Direct results and limitations

Paginated direct APIs request the configured 1–5 pages and stop early when a source returns a short page. Bing public references, indexed public LinkedIn references, and official county public records are grouped as publicly accessible records while retaining separate provenance labels. Bing runs the populated-field query first and one exact-identity fallback query, then deduplicates their URLs; this preserves exact-name public records when an index snippet omits employer or location details. Selecting any U.S. state or D.C. loads its official county or county-equivalent names from Census TIGERweb; manual entry remains available. For every selected county, the app runs a broad exact-name official `.gov` search plus a public-record document search covering records, agendas, minutes, notices, boards, commissions, court references, dockets, permits, licenses, elections, and contracts. This is not limited to county employees. The King County provider additionally submits its official exact-name employee-directory form as a separate direct source and returns public work identity fields, excluding phone and mailstop data. Bing RSS does not scrape LinkedIn or require credentials. Prohibited people-finder, contact-enrichment, commercial report-generation, property-owner, and private-address sources are filtered. After retrieval, the app requires source-observed identity evidence. Populated company, location, domain, country, username, and free-text fields refine provider requests and increase confidence only when observed in source content; they are never copied into descriptions to manufacture evidence. Clean no-match sources show `0 found`; failures distinguish `Rate limited`, `Timed out`, `Source unavailable`, and other unavailable conditions. Search forms themselves are excluded from results. The app does not crawl, bypass login, or access private profiles.

Every assistant feature runs through deterministic local logic without a network or device-model dependency.
