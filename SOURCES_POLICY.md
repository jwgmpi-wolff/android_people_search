# Sources policy

## Admission rules

A provider may be enabled only when it:

1. Exposes public information or a public search page without payment.
2. Requires neither credentials nor an authenticated scrape.
3. Can be used through a normal attributed browser link.
4. Does not require bypassing access controls, robots policy, CAPTCHA, rate limits, or terms.
5. Has a documented category, URL pattern, flags, confidence basis, and verification date.

Government, court, property, licensing, business-registration, campaign-finance, and open-data targets are jurisdiction-dependent. The app generates discovery links only; users must confirm that a specific portal is free and permits access. PACER and fee-based court systems are excluded.

## Prohibited sources and workflows

BeenVerified, Spokeo, PeopleFinders, Intelius, TruthFinder, PublicRecords.info, LexisNexis, TLO, CLEAR, Accurint, Delvepoint, IDI, paid legal systems, commercial report generators, credit/medical/education/financial records, utility data, SSNs, driver records, restricted criminal history, license-plate lookups, breach/password data, dark-web sources, and any pay-to-unlock result are prohibited.

No provider may automate private-address discovery, deanonymization, authentication, scraping, bulk export, or CAPTCHA/paywall bypass.

## Confidence

Official government sources receive a higher source weight, but a result becomes **High** only when at least three query signals also align. Professional and official organization pages are medium-weight. Social and forum links start low. Confidence always describes a search lead, never verified identity.
