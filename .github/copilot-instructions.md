# Android People Search coding instructions

- Maintain the Kotlin, coroutines, and Jetpack Compose architecture.
- Do not add paid APIs, paid result providers, enrichment vendors, or secret keys.
- Prefer on-device AI with a deterministic local fallback; cloud AI must never be required.
- Keep every source public, free, legally accessible, and compliant with its terms and robots policy.
- Do not implement paywall bypass, account scraping, CAPTCHA bypass, credential discovery, breach lookup, private-address discovery, or private-data discovery.
- Add or update provider URL and confidence tests with provider changes.
- Update `docs/source_matrix.md` whenever a provider is added, removed, or changed.
- Keep history disabled by default and preserve the clear-local-data behavior.
- Treat confidence as an explainable lead score, never proof of identity.
