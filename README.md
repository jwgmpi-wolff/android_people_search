# Android People Search

A native Android app that generates attributed search targets for free, legally accessible public sources. It uses no paid result APIs, secret keys, cloud AI, analytics, account scraping, or private databases.

Results are leads, not verified identities. The app labels confidence, links to every source, keeps history off by default, and provides a clear-local-data action.

## Features

- Kotlin, Jetpack Compose, Material 3, and Navigation Compose
- Public web, profile, professional, news, domain, username, and government-record search links
- Browser Custom Tabs instead of brittle or noncompliant scraping
- Deterministic on-device query normalization, duplicate removal, confidence scoring, explanations, and summaries
- Compile-safe Gemini Nano/ML Kit GenAI adapter with automatic local fallback
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

## Known limitation

The ML Kit Gemini Nano adapter is intentionally dependency-free and reports unavailable until a supported-device GenAI dependency/version matrix is selected. Every AI feature works now through deterministic local logic. Search targets open in a browser; the app does not claim to parse or aggregate third-party pages.
