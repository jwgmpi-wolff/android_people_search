# Migration notes

Reference reviewed read-only: `C:\Users\jerrywolff\Documents\.git\people_search_mvp`.

## Conceptually reused

- Provider-based multi-source search organization.
- URL deduplication and deterministic relevance scoring.
- Clear source attribution, source grouping, and fallback behavior.
- Dark charcoal, blue/teal visual cues, adapted to Material 3 light and dark schemes.

## Rewritten for Android

- Flask/FastAPI routes and Jinja/JavaScript UI became Kotlin, Compose, Navigation, and a ViewModel.
- Python provider functions became `SearchProvider` implementations that generate encoded browser targets.
- SQLite search history became opt-in Preferences DataStore with a 20-entry cap and clear action.
- Adaptive term-weight learning became transparent, deterministic confidence scoring to avoid behavioral profiling.
- Server deployment became Android Gradle builds, an APK artifact, and Android CI.

## Excluded

- Direct DuckDuckGo/Brave HTML scraping and BeautifulSoup parsing: brittle and potentially noncompliant.
- Webcam/livestream scanning, URL probing, pattern expansion, and saved camera databases: outside the people-search goal and creates surveillance risk.
- Redis/RQ, background scanner threads, CSV bulk import/export, Azure App Service infrastructure, and server port scripts: not applicable to a device-hosted app.
- Fictional fallback people presented as results: could confuse users and undermine provenance.
- Existing `.db`, logs, virtual environment, caches, and deployment artifacts: local/server state must not migrate.

## Assumptions

- A generated search link is a result target; the app does not claim that the person appears there until the user opens and verifies it.
- Public-record coverage is discovered through official-domain searches because county/state portal structures vary and change.
- Deterministic local behavior is the supported assistant implementation; no device-model or cloud dependency is required.
