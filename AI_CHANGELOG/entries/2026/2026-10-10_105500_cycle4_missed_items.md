# AI Changelog Entry: CL-AND-20261010-105500-006

- **Entry ID**: CL-AND-20261010-105500-006
- **Timestamp**: 2026-10-10 10:55:00 IST (UTC+05:30)
- **Status**: COMPLETED — CI pending at write time; GitHub Actions "Android Build" (`assembleDebug` + `assembleRelease`) is the compile gate. Local Gradle unusable in this environment (wrapper cannot download Gradle 8.8 — SSL/proxy failure), so compile verification is CI-only.
- **Risk Class**: R2
- **Author**: Subagent (parent orchestrator, audit loop Cycle 4 of 10 — Android MISSED-ITEMS)

## 1. Task
Cycle 4 of 10 — Android missed-items fix. Verify each audit-flagged item is actually broken before fixing; skip if already fine. Bilingual W.t() (Hindi+English), no emojis, no fake/mock data, CHANGELOG before/after, commit + push origin/main, CI green.

## 2. Item-by-item verdicts (with verification evidence)

### Item 1 — Gochar "New Delhi" silent fallback: FIXED (was partially broken)
- GocharScreen already blocked invalid lat/lon with a `honesty_gochar_coords` form error (Cycle 3 work), BUT two silent fakes remained: (a) blank name/dob/transitDate made the submit button silently no-op (`return@Button`); (b) `pob.trim().ifBlank { "New Delhi" }` faked the location label. FIXED: all four fields now required with bilingual error highlights + summary error; pob is sent as-is (validated non-blank).
- `GocharRequest` and `KundaliRequest` still carried Delhi-coordinate defaults (`lat=28.6139, lon=77.2090, pob="New Delhi"`) — a trap where any caller forgetting coords silently calculates a Delhi chart. FIXED: defaults are now `Double.NaN` with `init require(lat.isFinite() && lon.isFinite())` — omission fails loudly. All call sites verified to pass explicit validated coords (KundaliViewModel, AstroHealthViewModel, GocharViewModel, SavedChart.toKundaliRequest).
- `SavedChart.toKundaliRequest()` label fallback `birthPlace.ifBlank { "New Delhi" }` changed to `""` (coords already validated; label no longer faked).
- MuhuratViewModel's "New Delhi" + 28.6139/77.2090 is a VISIBLE prefilled default in the place field (user sees and can change it) — NOT a silent fallback; left as-is.

### Item 2 — Matchmaking silent no-op on blank form: FIXED (was broken)
- Before: submit with empty fields did literally nothing (`if (allNotBlank) { ... }`).
- After: all 8 fields validated on submit; missing fields get red borders + bilingual "This field is required" hints, summary error `match_err_missing` above the button. Errors clear on edit.

### Item 3 — Dead password inputs: FIXED (was broken)
- Dashboard → Password tab had Current/New/Confirm password inputs wired to nothing (button just navigated to the forgot-password route). Probed `/wp-json/omastro/v1` route index 2026-10-10: auth has forgot-password / reset-password-otp / set-password but NO change-password route — wiring was impossible.
- FIXED: the three dead inputs removed; tab keeps the email card, honest explainer text, and the "Send Password Reset Link" button → real forgot-password flow. Removed now-unused `PasswordVisualTransformation`/`KeyboardOptions`/`KeyboardType` imports.

### Item 4 — Festivals: BLOCKED (audit claim NOT verifiable)
- Searched for the website's Festivals feature: no `/festivals` page (404), no festivals route in the full REST index (97 routes probed 2026-10-10, none festival-related), no festivals key in the live `/panchang` payload, no festival URLs in the WP sitemap (sitemap itself returned empty), homepage shows no festivals section.
- Per the no-fake-data rule, NO Festivals screen was added — a hardcoded festival list would be invented data. What's needed: a server endpoint (e.g. `GET /festivals`) or the actual website festivals page URL.

### Item 5 — Prashna Kundali: FIXED (endpoint EXISTS — live-verified)
- `POST /wp-json/omastro/v1/prashna-kundali` probed 2026-10-10: accepts `{question, lat?, lon?, category?, language?}`; returns full horary analysis (question, prashn_time, location, lagna_info, panchang, planets, house_analysis, kp_analysis, moon_analysis, dasha_analysis, planetary_strengths, timing, special_yogas, remedies, analysis_text{verdict,summary,timing,score,max,factors}). Also in `/config` features as `prashna_kundali: true`. Accepts and honors lat/lon (verified: passing 26.9/80.9 returned "Lat 26.9°, Lon 80.9°").
- Wired end-to-end: `PrashnaRequest/PrashnaResponse` (defensive maps, like Gochar), `AstroRepository.getPrashnaKundali`, `GetPrashnaKundaliUseCase`, `AppContainer.getPrashnaKundaliUseCase`, `PrashnaViewModel`, `PrashnaScreen` (route `prashna`, drawer entry "Prashna Kundali", MainActivity composable). Question required with bilingual validation; place optional with honest disclosure that the server defaults to New Delhi coordinates when unset. Renders verdict/score/summary/timing, question + prashn meta, key factors, lagna, panchang, planets, house analysis, KP/Moon/strength blocks, timing, yogas, remedies — only keys present in the payload; server emoji icons skipped (no-emoji rule), Jyotish glyphs used for planets.
- 30 new HI/EN i18n keys.

### Item 6 — Wallet packs hardcoded: FIXED (now server-driven)
- Before: `WalletRepository.getRechargePacks()` hardcoded ₹25→₹35, ₹50→₹80, ₹100→₹200, ₹200→₹400. Deleted.
- After: `WalletViewModel.loadPackQuotes()` fetches `POST /checkout/validate-coupon` per slab (`{product_id:704, coins}` — verified that including `qty` makes the server return a fixed default quote, so NO qty is sent) and parses the authoritative "₹X Credit" figure from `item_title` (live-verified: 2500→₹35, 5000→₹80, 10000→₹200, 20000→₹400; subtotal + GST total also server-computed). Pack cards and the bonus-slab explainer render from these quotes; loading → "Pay ₹X" + loading note; failure → honest error, never a guessed number. Refresh button re-fetches quotes. Checkout sheet was already server-quoted (Cycle 3).

## 3. Files changed
- `data/model/KundaliModels.kt` — KundaliRequest: Delhi defaults removed, NaN + init require.
- `data/remote/OmAstroApiService.kt` — GocharRequest same treatment; `POST prashna-kundali` + PrashnaRequest/PrashnaResponse; SavedChart label fallback "".
- `data/repository/AstroRepository.kt` — getPrashnaKundali.
- `data/repository/WalletRepository.kt` — hardcoded getRechargePacks() deleted (comment documents the server-driven replacement).
- `data/model/WalletModels.kt` — RechargePack doc updated (legacy binding note).
- `domain/usecase/AstroUseCases.kt` — GetPrashnaKundaliUseCase (+ imports).
- `di/AppContainer.kt` — getPrashnaKundaliUseCase.
- `ui/screens/gochar/GocharScreen.kt` — bilingual required-field validation, no Delhi label fake.
- `ui/screens/matching/MatchmakingScreen.kt` — bilingual validation with field highlights.
- `ui/screens/dashboard/DashboardScreen.kt` — dead password inputs removed.
- `ui/screens/prashna/PrashnaViewModel.kt`, `PrashnaScreen.kt` — NEW.
- `ui/screens/wallet/WalletViewModel.kt` — PackQuote + loadPackQuotes.
- `ui/screens/wallet/WalletScreen.kt` — server-driven pack cards + slab explainer; refresh re-fetches quotes.
- `ui/navigation/Screen.kt` — Prashna route.
- `ui/components/NavigationDrawer.kt` — Prashna drawer entry.
- `MainActivity.kt` — Prashna composable.
- `ui/i18n/FeatureStrings.kt` — ~45 new HI/EN keys (validation, prashna, wallet quotes).
- `CHANGELOG.md` — Cycle 4 before/after entry.

## 4. Verification evidence
- Live probes (curl, 2026-10-10): full `/wp-json/omastro/v1/` route index (97 routes — prashna-kundali present, no festivals route); prashna-kundali payload shape + lat/lon honored; validate-coupon quotes for 2500/5000/10000/20000 coins (no qty) returning per-slab credit/subtotal/total; with qty=1 the server returns a fixed 25/35 default (hence no qty in the app).
- Static: brace/paren balance check on all 18 touched files (one pre-existing stripper artifact in KundaliModels.kt confirmed identical before/after via git stash); grep-confirmed no remaining `getRechargePacks` references, no `ifBlank { "New Delhi" }` in request paths, no `Icons` missing (material-icons-extended present).
- Build: CI-only (local Gradle wrapper cannot download distribution in this environment).

## 5. Rejected approaches
- Hardcoded festival list: REJECTED — no server source; would be invented data.
- Faking a change-password call (e.g. POSTing to set-password without OTP): REJECTED — no such contract; dishonest.
- Driving pack credits from validate-coupon WITH qty: REJECTED — probed; server ignores/mangles qty and returns a fixed default quote.
- Leaving Delhi defaults with a comment: REJECTED — silent-fake trap must fail loudly.
