# Changelog — OmAstro Android (Native Kotlin)

All notable changes to the native Android app. Each entry lists what was broken or missing, what was removed, and what was added.

---

## [2026-10-09] — NEW: Rich Left Navigation Drawer + Settings Overhaul (omastroai.com parity)

### Missing
The app had no navigation drawer at all — the website's left sidebar (profile header, grouped menu, legal pages, log out) had zero app equivalent. Main screens could only be reached via the bottom bar; deep features (Gochar, Muhurat, Saved Kundalis) were unreachable from several screens. Settings was a thin page: account card, one wallet shortcut, rate-us, disclaimers, and a hardcoded wrong version string ("v1.0.0 (Build 100)" while build.gradle says versionCode 1). No language row, no terms, no saved-charts/activity shortcuts, no refer-and-earn entry.

### Removed
- Nothing user-facing removed. Fake toggles (notifications/theme/language) were already removed in an earlier pass and stay removed.

### Added
- **Navigation drawer** (`ui/components/NavigationDrawer.kt`, new):
  - Profile header: gold-gradient avatar with name initial, user name + email/phone, tappable wallet chip (₹ balance → Wallet screen)
  - Grouped menu matching the website sidebar — Menu (Dashboard, AI Chat, Janam Kundali, Rashifal, Panchang, Kundali Milan, Astrologers, Numerology, Gochar, Muhurat), Account (Add Money, Saved Kundalis, AI Chat History, Activity History → Wallet transactions, Refer & Earn, Rate this App, Settings), Preferences (Language), Legal (Privacy Policy, Terms of Service, Report a Bug), plus a red Log Out row with confirmation
  - Selected-route highlighting; drawer closes before every navigation/dialog
  - `DrawerDialog` sealed interface + `DrawerDialogHost` composable: honest dialogs for Coming Soon (AI Chat History, Refer & Earn, Report a Bug — no API exists for these), Privacy Policy (real disclaimer text), Terms of Service, Language (English active, हिन्दी marked coming soon — no fake locale switching), and Log Out confirmation
- **MainActivity wiring**: `ModalNavigationDrawer` wraps the app scaffold (edge-swipe disabled while closed to avoid carousel conflicts); hamburger menu added to all six bottom-nav screens (Dashboard, Kundali, Rashifal, AI Chat, Astrologers, Settings) via a new optional `onMenuClick` parameter — screens keep their back arrow when the parameter is absent, so standalone behavior is unchanged; logout clears the session and returns to the auth gate with a cleared back stack; Play Store in-app review launches from the drawer
- **Settings overhaul** (`ui/screens/settings/SettingsScreen.kt`, rewritten):
  - Profile card now shows wallet balance (₹) next to the name/email
  - Wallet card shows live balance with "Add Money" subtitle
  - New "My Astrology" section: Saved Kundalis, Activity History (→ Wallet transactions)
  - New "Rewards" section: Refer & Earn (honest coming-soon dialog)
  - New "Preferences" section: App Language (English active, हिन्दी coming soon)
  - Support & Feedback extended: Report a Bug (coming soon), Terms of Service (real dialog)
  - About footer now reads the real version from `BuildConfig` (was hardcoded "Build 100")
  - Reuses `DrawerDialogHost` so drawer and settings share identical dialogs
- No emojis anywhere; every drawer/settings entry either navigates to a real screen, performs a real action (review, logout), or shows an honest coming-soon state.

---

## [2026-10-09] — Auth Redesign: Rich Login/Signup UI + `otp_channel` API Support

### What was basic
The auth screens were functional but plain: flat background, single shared card, one plain 6-digit text field for OTP, no password guidance, no mode tabs, basic error text. The signup already collected a mobile number (optional), but the API had no way to ever switch OTP delivery from email to mobile/WhatsApp.

### Removed
- Old flat `AuthScreen.kt` layout (plain column, basic `AuthTextField`/`AuthPrimaryButton` without icons, single OTP `OutlinedTextField`, text-only "Show/Hide" password toggle).

### Added
- **Rich auth UI** (`ui/screens/auth/AuthScreen.kt`, rewritten):
  - Cosmic gradient background with soft gold radial glow behind the brand header
  - Brand header: gold-ringed "ॐ" emblem, "OmAstro AI" title, "Vedic Astrology Guidance" subtitle
  - Segmented "Sign In / Sign Up" tab control with gold gradient active pill
  - `AnimatedContent` mode transitions (fade + directional slide based on mode order)
  - Text fields with leading icons (person/email/phone/lock), "+91" prefix on the mobile field, gold focus ring, visibility eye-icon password toggle
  - Password strength meter on signup (4-segment bar: Weak/Fair/Good/Strong with color coding)
  - Six individual OTP digit boxes with auto-advance on entry, focus-back on clear, gold highlight on filled boxes, auto-focus on first box
  - Gradient gold primary buttons with loading spinner state
  - Error/success message banners (tinted cards with colored borders instead of plain text)
  - Terms-of-use note on signup; footer navigation links per mode
  - No emojis anywhere; no fabricated data — all messages come from the ViewModel/server
- **`otp_channel` API support** (email default; mobile/WhatsApp enablement ready):
  - `data/model/AuthModels.kt`: new `OtpChannels` object (`EMAIL`/`MOBILE`/`WHATSAPP` + `DEFAULT = EMAIL`) with KDoc documenting the exact server + app steps to enable mobile/WhatsApp OTP later; `otp_channel` field added to `RegisterRequest`, `VerifySignupOtpRequest`, and new `ResendOtpRequest`
  - `OmAstroApiService.resendOtp` now takes `ResendOtpRequest` (was an untyped `Map<String, String>`)
  - `AuthRepository.register/verifySignupOtp/resendSignupOtp` accept `otpChannel` (default email) and forward it
  - `AuthUseCases` (`Register`/`VerifySignupOtp`/`ResendSignupOtp`) accept and forward `otpChannel`
  - `AuthViewModel`: new `otpChannel` StateFlow (default email) + `setOtpChannel()` for a future channel selector UI; channel label shown dynamically in signup/OTP subtitles ("sent to your email" today, "mobile number via SMS"/"WhatsApp" when enabled)
- Existing auth flow unchanged (email+password login → name/email/mobile/password signup → email OTP verify → forgot password), matching the Flutter app's flow the user approved.

---

## [2026-10-09] — NEW: Kundali "Analysis" Tab (website-rich analysis)

### Missing — rich kundali analysis like omastroai.com/kundali/
**What was missing:** The app had 7 data tabs (Charts, Grahas, Dasha, Yogas, Doshas, Ashtaka, Lal Kitab) but no consolidated analysis view. The website shows life-area scores, per-planet detailed analysis, 12-house analysis, birth panchang and remedies.
**What was added** (`KundaliScreen.kt`, new tab index 7 "Analysis"):
- **Life Area Scores:** 8 cards (Career, Wealth, Marriage, Health, Education, Children, Spirituality, Property) with x/10 scores derived from server-computed Bhava Bala house strength — color-coded progress bars (green ≥7, gold ≥4.5, red below). No invented numbers: scores come from the API's `bhavBala.score`.
- **Planet Analysis:** Expandable per-planet cards — glyph, Hindi name, rashi/house/degree, dignity chips (Exalted/Debilitated/Own/Vargottama/Yogakaraka/Combust/Retrograde from server `specialStatus`), nakshatra/pada/lord, navamsha, avastha, chara karaka, ruled houses, aspects. Interpretation text built ONLY from real attributes (dignity + house + nakshatra), no invented predictions.
- **Twelve Houses:** Expandable cards per house — number, rashi, lord, SAV, bhav-bala score, planets present, standard house significations (static jyotish knowledge).
- **Birth Panchang:** Date, time, place, nakshatra/pada, moon sign, lagna with degrees, ayanamsa — from the kundali response's real birth/meta data.
- **Remedies (Upay):** Aggregated from API dosha `remedies` lists (active doshas only) + planet-strengthening upay for debilitated/combust planets (standard mantra/daan per planet, applied only where the chart shows affliction).
**Rules followed:** No emojis (Jyotish glyphs only ♈ ☉ ♂); no fabricated values; calculation stays server-side (scores are server bhav-bala, interpretations describe real attributes).

---

## [2026-10-09] — NEW: Numerology Suite (12 Tools, matching omastroai.com)

### Missing feature — full numerology suite
**What was missing:** The app had a single basic numerology profile screen (name + DOB → one analysis text). The website offers 12 distinct numerology tools. 11 of 12 were absent.
**API contracts (verified live 2026-10-09 against production):**
- `POST /wp-json/omastro/v1/numerology` with `{name, dob}` → `{"success":true,"data":{"analysis_text", ...}}` (Moolank, Bhagyank, Success Number, karmic debts, missing numbers + full profile text)
- `POST /wp-json/omastro/v1/numerology/compatibility` with `{name1,dob1,name2,dob2}` → `{"success":true,"data":{"score", "analysis_text"}}`
- `POST /wp-json/omastro/v1/numerology/name-check` with `{name, dob}` → `{"success":true,"data":{"expression","soul","personality","chaldean","analysis_text"}}`
**Removed:** Old single-screen `NumerologyScreen.kt` + `NumerologyViewModel.kt` (basic profile only).
**Added:**
- `OmAstroApiService.getNumerologyCompatibility()` + `getNumerologyNameCheck()`; `AstroRepository` wrappers with honest error propagation
- `ui/screens/numerology/NumerologyTools.kt`: pure deterministic computations (digital-root reduction preserving master numbers 11/22, Moolank, Bhagyank, Success Number, karmic-debt detection 13/14/16/19, Lo Shu 3x3 grid + 8 arrows, Personal Year/Month/Day, Kua number + East/West group lucky directions, lucky weekdays, Vedic planet rulers, gem/color/day/mantra remedies per planet, mobile-number compatibility scoring, pinnacles)
- `NumerologyTool.kt`: 12-tool registry (Moolank & Life Path, Lo Shu Grid, Name Numerology, Time Cycles, Karmic Debt, Compatibility, Gems & Remedies, Lucky Calendar, Mobile/Lucky Number, Name Correction, Kua Number, Planetary Numbers) in website order
- `NumerologyHubScreen.kt`: 2-column tool-card grid entry screen ("Vedic · Pythagorean · Chaldean — Complete Number Science")
- `NumerologyToolViewModel.kt`: per-tool input state (name/DOB/second person/phone/gender), validation, dispatches to real APIs (compatibility, name-check) or client-side computation; structured `ToolResult` (sections/lines) with honest error states
- `NumerologyToolScreen.kt`: generic tool screen — dynamic input form per tool (only the fields each tool needs), loading spinner, structured result cards (highlighted key numbers), error card with Retry
- `Screen.NumerologyTool("numerology_tool/{toolId}")` route + `MainActivity` composable with `toolId` nav argument
- No emojis anywhere; no fabricated values — API tools show server data, computed tools show deterministic derivations from the user's real inputs with honest empty states

---

## [2026-10-09] — NEW: Muhurat Screen (Shubh Muhurat Finder)

### Missing feature added — real POST /muhurat wiring
**What was missing:** No Muhurat screen existed at all. The Dashboard's "Muhurat" service tile navigated to the Panchang screen as a placeholder — wrong destination, missing feature.
**API contract (verified live 2026-10-09 against production):** `POST /wp-json/omastro/v1/muhurat` with `{event, date (YYYY-MM-DD), days_ahead (1-365), lat, lon, tz, language}` returns `{"success": true, "data": {"event", "from", "days_ahead", "core_data": [{date, yr, mo, dy, tithi, tithi_full, nakshatra (1-27), vara (0=Sun..6=Sat), yoga (1-27), score, max_score, rating (0-100), verdict (excellent|good|acceptable), checks{tithi,avoid_tithi,nakshatra,vara{ok,value}}}], "analysis_text"}}`. Supported events verified against `KG_Muhurat_Engine::get_event_rules`: `vivah`, `grih_pravesh`, `vahan`, `property` (unknown keys fall back to `vivah` server-side).
**Removed:** Dashboard Muhurat tile's placeholder navigation to `Screen.Panchang` — now routes to the new `Screen.Muhurat`.
**Added:**
- `data/model/MuhuratModels.kt`: `MuhuratRequest`, `MuhuratResponse` (defensive envelope), `MuhuratData`, `MuhuratDay`, `MuhuratChecks`, `MuhuratCheck`; verified lookup tables — `MUHURAT_EVENTS` (4 events), `NAKSHATRA_NAMES` (27, 1-indexed; verified 4=Rohini, 27=Revati against engine comments), `VARA_NAMES`/`VARA_NAMES_SHORT` (0=Sun..6=Sat; verified against engine varas), `YOGA_NAMES` (27), `TITHI_NAMES` (15 + `muhuratDayTithiName()` paksha helper handling Krishna Amavasya)
- `OmAstroApiService.getMuhurat()` (`POST muhurat`); `AstroRepository.getMuhurat()` with honest envelope unwrap (fails loudly on unsuccessful/empty); `GetMuhuratUseCase` registered in `AppContainer` as `getMuhuratUseCase`
- `ui/navigation/Screen.Muhurat` route; `MainActivity` composable route; Dashboard tile now navigates correctly
- `ui/screens/muhurat/MuhuratViewModel.kt`: form state (event, startDate, daysAhead, place/lat/lon), `findMuhurat()`, loading/error/result StateFlows, honest error messages
- `ui/screens/muhurat/MuhuratScreen.kt`: event selector chips (4 events with subtitles), start-date field opening `DatePickerDialog`, search-window chips (7/15/30/60/90 days), `PlaceSearchField` for location, gold "Find Muhurat" button; results sorted by rating — summary card (days analyzed, excellent/good counts), per-day cards (date + weekday, verdict badge color-coded: Excellent=green / Good=gold / Acceptable=blue, rating progress bar with score/max_score, Tithi/Nakshatra/Yoga/Vara rows with pass/fail check icons from `checks`), `analysis_text` card; honest empty state ("Select an event and tap Find Muhurat"), loading spinner, error card with Retry
- No emojis anywhere; no fabricated values — every date/score/verdict comes from the live API; calculation stays server-side per standing rule

---

## [2026-10-09] — Panchang Screen Redesign to Match omastroai.com/panchang/

### Panchang models rebuilt to match the REAL POST /panchang response
**What was broken:** `PanchangResponse` expected flat fields (`date`, `day`, `sunrise`, `sunset`, `moonrise`, `tithi`, `nakshatra`, `yoga`, `karana`, `rahu_kaal`, `yamaganda`, `gulika_kaal`, `abhijit_muhurat`, `amrit_kaal`) that the production API does NOT return — the real API returns `{"success": true, "data": {"core_data": {...}, "analysis_text": "..."}}` with nested `tithi{num,start_jd,end_jd}`, `nakshatra{idx,data{n,hi,lord,pada}}`, `yoga{idx}`, `karana1/karana2{name_idx}`, `paksha`, `vara_idx`, `sun_rashi/moon_rashi`, `amanta_month`, `purnimanta_month`, samvats. Verified live against production on 2026-10-09. The old shape could never parse, so the screen was permanently stuck on its error state (P0 fabricated fallbacks were removed earlier).
**Removed:** Old flat `PanchangResponse` fields (`moonrise`, `rahu_kaal`, `yamaganda`, `gulika_kaal`, `abhijit_muhurat`, `amrit_kaal`, `is_auspicious`) — none of them exist in the real contract; the "Auspicious & Inauspicious Muhurat" section that rendered them is gone (no client-side muhurat computation — calculation stays server-side per standing rule).
**Added:**
- `PanchangEnvelope` / `PanchangDataPayload` / `PanchangCoreData` (+ `TithiInfo`, `NakshatraInfo`, `YogaInfo`, `KaranaInfo`, `RashiInfo`, `RashiPosition`) matching the live contract; `AstroRepository.getPanchang` unwraps the envelope honestly and fails loudly on empty data
- `PanchangLookups`: verified name tables (30 tithis, 27 yogas, 11 karanas, 7 varas), Julian-date → IST time conversion (`jdToIstTime` "6:18 AM", `jdToIstDateTime` "8 Oct, 10:16 PM" — both verified against the live `analysis_text`), `toUiData()` mapper producing the display-ready `PanchangResponse` UI model (kept `tithi`/`nakshatra`/`yoga`/`karana`/`day`/`sunrise`/`sunset` field names so the Dashboard's panchang bindings keep working)
- Real `POST /hora` wiring: `HoraEnvelope` / `HoraData` (day + night planetary hours with lord/start/end) / `HoraHour`; `AstroRepository.getHora()`; `GetHoraUseCase` registered in `AppContainer` as `getHoraUseCase`

### Screen redesigned to website structure (banner → controls → details → hora)
**What was missing:** No date picker (always today), no timezone display, no tithi start/end times, no karana split, no paksha/months/samvat, no Hora section.
**Added:**
- Maroon gradient banner: "PANCHANG" gold letter-spaced title + "Tithi · Vara · Nakshatra · Yoga · Karana" subtitle (matches website)
- Controls card: DATE label + date button opening a Material3 `DatePickerDialog` (IST), TIMEZONE label + "IST +5:30 (India)", full-width gold "Get Panchang" button
- Date banner card (pale cream like website): "Friday, October 9, 2026" + "Sunrise · 6:18 AM / Sunset · 5:58 PM"
- "Panchang Details" card: rows with caps-gray label (left) and bold value (right) — Tithi with green "Starts:" / red "Ends:" times, Nakshatra with pada/lord/upto detail, Yoga with upto, Karana ×2, Paksha, Weekday, Amanta/Purnimanta Month, Moonsign, Sunsign
- "Hora — Planetary Hours" card (real `/hora` data): Day/Night tab toggle, per-hour rows (lord in gold + start–end times), current hora auto-highlighted with gold border and "NOW" badge (only for today's date)
- "Calendar" card: Vikram/Shaka samvat line + Pravishte
- Honest loading spinner, "Panchang Unavailable" error card with Retry; hora loads best-effort and never blocks the panchang
- No emojis anywhere; no fabricated values — every string comes from the live API or verified lookup tables

---

## [2026-10-09] — NEW: Gochar (Planetary Transits) Screen

### Entire feature was missing from the app
**What was missing:** Website has `/gochar` and `/transit` endpoints for planetary transit analysis; the app had zero Gochar presence. The Dashboard "Transits" service tile navigated to the Rashifal screen (`Screen.Horoscope`) as a placeholder — wrong destination.
**Removed:** Nothing (pure addition, except fixing the Dashboard tile destination).
**Added:**
- API: `POST("gochar")` in `OmAstroApiService` + `GocharRequest` (birth details: name, dob, tob, pob, lat, lon, gender + `transit_date`) and `GocharResponse` (defensive `success/message/data` envelope; `data` kept as `Map<String, Any>` since the transit payload shape is rich and evolving)
- Repository: `AstroRepository.getGochar()` with honest HTTP/error propagation
- UseCase: `GetGocharUseCase`, registered in `AppContainer` as `getGocharUseCase`
- `GocharViewModel`: birth-detail + transit-date form state, `calculate()` invoking the use case, loading/error/result StateFlows, `todayString()` default transit date, `clearResult()`
- `GocharScreen` (`ui/screens/gochar/`): birth form (name, DOB, time, `PlaceSearchField` birth-place autocomplete with lat/lon auto-fill + manual fallback, gender FilterChips, transit date field); empty state card explaining the feature; loading ("Calculating transits..."); honest error state ("Transit Analysis Unavailable" + Retry); results with Sade Sati section (when `sade_sati`/`sadesati` key present), per-planet transit cards with Jyotish glyphs (☉ ☽ ♂ ☿ ♃ ♀ ♄ ☊ ☋) when a planets/transits list is present, timeline section, summary prose, and a defensive key-value renderer for any remaining payload keys — renders only keys actually present, never invented values; "Analyze Another Date" reset button
- Navigation: `Screen.Gochar("gochar", "Gochar")` route + composable in `MainActivity`; Dashboard "Transits" tile now routes to `Screen.Gochar`
- No emojis anywhere; professional Jyotish glyphs only

---

## [2026-10-09] — Rashifal Screen Redesign to Match omastroai.com/rashifal/

### Rashifal models rebuilt to match the REAL POST /rashifal response
**What was broken:** `DailyHoroscopeResponse` expected flat fields (`prediction`, `love_score`, `career_score`, `health_score`, `lucky_number`, `lucky_color`, `remedy`) that the production API does NOT return, and carried fabricated default values (`loveScore = 85`, `luckyNumber = 7`, etc.) — a P0 data-integrity violation. The real API returns `{"success": true, "data": {...}}` with `data.summary`, `data.scores{overall,career,finance,health,love,family,travel}`, `data.areas{career|finance|health|love|family|travel: {label,score,text}}`, `data.lucky{number,color,direction,gem,time}`, `data.remedies[]`, `data.dasha{md,ad,pd,primary_name}`, `data.saturn_transit{active,type,name_en,name_hi,description_en}`. Verified live against production on 2026-10-09.
**Removed:** Old flat `DailyHoroscopeResponse` class and all fabricated score/color/remedy defaults.
**Added:**
- `RashifalEnvelope` (success + data) — `OmAstroApiService.getRashifal` now returns `Response<RashifalEnvelope>`; `AstroRepository.getDailyHoroscope` unwraps `data` honestly and fails loudly on an empty envelope
- `RashifalData`, `RashifalDasha`, `RashifalArea`, `RashifalLucky`, `SaturnTransit` models matching the live contract
- `HoroscopeSign` extended with `ruler` (Mars/Venus/…) and `glyphColor` (website sign-circle colors: Aries red, Taurus/Gemini orange, Cancer/Leo yellow, Virgo/Libra green, Scorpio teal, Sagittarius blue, Capricorn/Aquarius purple, Pisces pink)

### Screen redesigned to website flow (sign grid → detail panel → periods → results)
**What was missing:** No sign selector (app auto-loaded Aries on open); periods were Today/Tomorrow/Weekly; only 3 scores; no alerts, no lucky details, no remedies list, no dasha info.
**Removed:** Auto-fetch-on-open behavior; `LazyRow` sign carousel; Today/Tomorrow/Weekly chips; 3-metric "ENERGY SPECTRUM" card.
**Added:**
- Maroon banner card: "RASHIFAL" gold title + "Select your sign and read detailed horoscope" subtitle
- "Select Your Sign" 12-sign grid (4 columns): colored circular zodiac glyph (♈..♓), bold sign name, "Ruler: X" gray caption; selected = orange border
- Detail panel after selection: glyph circle + "♌ Leo / Lord: Sun" + "Change" outlined button
- Period tabs Daily/Weekly/Monthly/Yearly (active = solid gold, inactive = gold outline) — reloading results when tapped after a result is shown
- "View Rashifal" gold CTA button (website parity)
- Results: title card with "Leo — Daily Rashifal" + "OVERALL X/10" (color-coded); red Saturn-transit alert card when `saturn_transit.active` (name + Active badge + description); "COSMIC GUIDANCE" summary paragraph; 6 "LIFE AREA ENERGIES" category cards with score bars (green 8–10, orange 6–7, red ≤5) + per-area text; "Lucky Signs" card (Number/Color/Direction/Gem/Lucky Time); "Vedic Remedies" numbered list; "CURRENT PLANETARY PERIOD" strip (Mahadasha/Antardasha/Dominant)
- Honest states kept: "Reading the stars…" loading, "Rashifal Unavailable" + Retry on error

---

## [2026-10-09] — Kundali Screen Redesign to Match Website API Depth

### Kundali models rewritten to match the REAL POST /kundali response
**What was broken:** `KundaliResponse` expected flat fields (`ascendant_sign`, `d1_houses`, `dasha_timeline`, `yogas` as YogaDoshaItem, etc.) that the production API does NOT return. The real API returns `{"success": true, "data": {...}}` with `data.planets[]` (full detail incl. server glyph `symbol`, `nameHi`, `rashiHi`, `specialStatus[]`), `data.houses[]` (with `bhavBala`), `data.d9Houses[]`, `data.yogas[]` ({name, description, type, prediction}), `data.doshas[]` ({name, active, severity, detail, analysis, prediction, remedies[]}), `data.vimshottariDasha` (nested mahadasha→antardasha→pratyantar→sookshma), `data.ashtakavarga.savByHouse[]`, `data.lalKitab`, `data.divisionalCharts` (Navamsha, Dashamsha with real houses). Verified live against production on 2026-10-09 — no deserializer or envelope-unwrapping existed, so chart data could not parse.
**Removed:** Old flat `PlanetInfo`, `VimshottariDashaItem`, `YogaDoshaItem`, `AshtakavargaHouseBindu`, `LalKitabRemedyItem`, `BNNCombination`, `JaiminiKaraka`, `AstroHealthIndicator` classes and fabricated default values (`ascendantSignName="Aries (Mesha)"` etc.).
**Added:**
- `KundaliEnvelope` (success + data) — `OmAstroApiService.calculateKundali` now returns `Response<KundaliEnvelope>`; `AstroRepository.getKundali` unwraps `data` honestly and fails loudly on an empty envelope
- `KundaliResponse` = real `data` payload: meta, birth, lagna, navamshaLagna, suryaLagna, chandraLagna, planets, houses, d9Houses, yogas, doshas, vimshottariDasha, ashtakavarga, lalKitab, divisionalCharts
- `ApiPlanet` with server glyph, Hindi names, deg/min/sec, nakshatra + pada + lord, D9 rashi/house, `specialStatus[]` dignity chips, charaKaraka, baladiAvastha, ruledHousesFmt
- `DashaNode` recursive model handling all nesting levels (antardasha/pratyantar/sookshma/prana/adi/deha)
- Helpers: `rashiGlyph()` (♈..♓), `rashiNameToIndex()`, `planetGlyphFallback()` (☉ ☽ ♂ ☿ ♃ ♀ ♄ ☊ ☋), `planetHindiFallback()`

### Birth form rebuilt with website-parity fields
**What was missing:** No gender field (API accepts `gender`; website asks for it). Flat ungrouped field list.
**Removed:** Single flat field column.
**Added:** Grouped sections — "Personal Details" (Full Name + Gender segmented control Male/Female/Other), "Date & Time of Birth" (DOB, TOB + unknown-time guidance), "Place of Birth" (PlaceSearchField + lat/lon). Gender flows through `KundaliViewModel.birthGender` → `KundaliRequest.gender`; saved-chart load restores it.

### North Indian diamond chart — visually rich rewrite
**What was broken:** Basic canvas — rashi numbers only, plain planet names, fixed 320dp box, `SacredGoldDark` thin lines.
**Removed:** Old number-only rendering.
**Added:** Gold lines on dark cosmic background; large rashi glyphs (♈..♓) per bhava; planet Jyotish glyphs from the server `symbol` field; gold "Lg" Lagna badge on house 1; dim house numbers at cell corners; responsive `fillMaxWidth + aspectRatio(1f)` sizing; glyph legend caption. D1/D9/D10 all render (D9 from `d9Houses`, D10 from real `divisionalCharts.Dashamsha`).

### South Indian chart — glyph upgrade
**What was broken:** Two-letter planet abbreviations ("Su", "Mo"), sign numbers in headers.
**Removed:** `planetAbbreviation()` helper.
**Added:** Server glyphs (☉ ☽ ♂ …) per planet, rashi glyph headers, Asc highlight kept; sign-based placement unchanged (verified correct).

### Grahas tab — detailed planet table
**What was broken:** Simple cards (name, rashi, dignity string).
**Removed:** Old card list.
**Added:** Horizontally-scrollable production table — GRAHA (glyph + EN/HI name) | RASHI (EN/HI) | HOUSE | DEGREE (° ′ + avastha) | NAKSHATRA (pada, lord, D9 rashi) | DIGNITY/STATUS (color-coded chips: Own Sign/Exalted/Vargottama/Yogakaraka gold-green, Debilitated/Combust red, Retrograde blue + Hindi status string).

### Dasha tab — current chain + expandable timeline
**What was broken:** Flat mahadasha list only (lord/start/end/duration from a model the API never returned).
**Removed:** Old flat dasha cards.
**Added:** "Current Period" hero (Mahadasha → Antardasha → Pratyantardasha chain with ACTIVE badge, real dates from API); expandable mahadasha timeline — tap expands antardashas, antardashas expand to pratyantars (real nested API data); current periods auto-expanded and gold-highlighted.

### Yogas & Doshas — split into two honest sections
**What was broken:** Yogas and doshas merged in one tab from a fabricated model.
**Removed:** Combined "Yogas & Doshas" tab, old `YogaDoshaItem` rendering.
**Added:** "Yogas" tab — Raja Yoga (gold) vs Ava-Yoga (red) badges, bilingual names/descriptions (split on |), Phal predictions. "Doshas" tab — severity badges (Warning amber / Critical red), Active/Not-Active state, detail + analysis + prediction, numbered Upay (remedies) list; honest "No major doshas detected" empty state.

### Charts tab — 12 Bhavas with Bhav Bala
**Added:** Per-bhava rows with rashi (EN/HI), lord, SAV bindus, and server `bhavBala` strength label chips.

### Ashtakavarga & Lal Kitab — real data wiring
**What was broken:** Old tabs consumed models the API never returned (always empty states).
**Removed:** Old `AshtakavargaHouseBindu`/`LalKitabRemedyItem`/`BNN`/`Jaimini`/`Health` placeholder tabs.
**Added:** Ashtakavarga tab — SAV total hero + 12 house rows with points and Strong/Average/Weak strength labels. Lal Kitab tab — lagna rashi, server note, yoga strings; honest empty states. Tabs are now: Charts, Grahas, Dasha, Yogas, Doshas, Ashtaka, Lal Kitab.

---

## [2026-10-09] — AI Chat Screen Redesign to Match Website

### AI Chat redesigned to match omastroai.com/ai-chat/
**What was missing:** Basic chat UI — plain TabRow mode switch, static typing dots, no suggestion chips, no timestamps, bare empty state ("Ask your first question"), square-ish input bar.
**Removed:** Old TabRow-based mode tabs, static (non-animated) typing indicator, old empty-state layout.
**Added:**
- Custom segmented mode control (General / Divine / Mundane) — rounded container, gold pill for active mode, per-mode subtitles retained in top bar
- Chat bubbles: user right-aligned with SacredGold accent + dark text; AI left-aligned with CosmicCharcoal card bg + white text; "AI Jyotishi" sender label on AI messages; chat-tail corner styling
- Animated typing indicator — 3 gold dots with staggered alpha bounce (infinite transition), "AI Jyotishi" label + "typing..." caption
- "Try asking:" suggestion chips — 4 per mode (General/Divine/Mundane specific); tap fills the input field; vertical card list in empty state; horizontal scrollable gold-outline strip above input during conversation
- Empty state: emblem circle with Jyotish glyph per mode (Jupiter/Om/Earth), welcome headline, mode description, suggestion list
- Input bar: rounded 26dp field, multi-line (max 5), sentence capitalization, IME Send action, circular gold send button (disabled state dimmed), proper imePadding + navigationBarsPadding keyboard handling, clears focus on send
- Message timestamps (hh:mm a, subtle) under each bubble
- Auto-scroll to latest message / typing indicator via animateScrollToItem
- New-chat action uses Add icon; refresh icon removed
- NO pictographic emojis; professional Jyotish glyphs only

---

## [2026-10-09] — Dashboard UI Overhaul to Match Website

### Dashboard redesigned to match omastroai.com homepage
**What was broken:** Dashboard had a basic layout (header, panchang card, 8 quick-action rows, mantra, astrologer carousel) that did not match the website's rich homepage structure. Missing: Today's Oracle card, AI chat entry bar, 8-service grid, Divine Connection, Numerology tools grid, Special Methods section.
**Removed:** Old QuickActionItem rows layout, old AstrologerMiniCard.
**Added:**
- Today's Oracle card (greeting "Hey, Seeker", date, daily mantra, panchang tithi/nakshatra)
- "Ask Om Astro AI" search bar with "Kundali · Career · Marriage · Remedies — anything" placeholder and "Try asking" suggestion chips → navigates to AI Chat
- 8-service grid matching website: Kundali, Matching, Faladesh, Rashifal, Yoga/Dosh, Transits, Panchang, Muhurat (each with Jyotish glyph: ☉ ♀ ☽ ♈ ♂ ♄ ☀ ♃)
- "Chat with Astrologers" carousel with photo placeholder, name, rating, experience years, specialties, ₹/min price, online/offline indicator, "Chat Now" button
- Divine Connection section (6 deity cards: Ganesha, Shiva, Vishnu, Lakshmi, Saraswati, Hanuman) → AI Chat
- Numerology section (12 tools: Moolank, Lo Shu Grid, Name Numerology, Time Cycles, Karmic Debt, Compatibility, Gems, Lucky Calendar, Mobile Number, Name Correction, Kua Number, Planetary Numbers) → Numerology screen
- Special Astrology Methods (Bhrigu Nandi Nadi, Astro Health, Saved Charts cards)
- Today's Panchang quick card at bottom
- Section headers with proper typography hierarchy, 20dp section spacing
- All cards tappable with correct navigation; honest loading/empty states
- NO pictographic emojis; professional Jyotish glyphs only

---

## [2026-10-09] — P0 Integrity Fix Pass + AI Chat (v2.0.0-hardening)

### Authentication — rebuilt on the real backend contract
**What was broken:** The app used a fake OTP-login model (demo OTP `654321`, fake JWT `demo_jwt_token`, app booted with a pre-logged-in fake user id 108). The production backend uses email+password login and emailed OTP only for signup verification — there is no SMS/WhatsApp OTP endpoint on the backend (verified against `rest-api.php`).
**Removed:** `SendOtpRequest`/`VerifyOtpRequest` OTP-login models, `fetchAuthConfig`/`sendOtp`/`verifyOtp` repository methods, demo backdoor strings, pre-logged-in fake user.
**Added:** `LoginRequest{email,password}`, `RegisterRequest{name,email,password,mobile?}`, `VerifySignupOtpRequest{email,otp}` (field corrected: `email`, not `destination`); `POST /auth/login`, `/auth/register`, `/auth/verify-signup-otp`, `/auth/forgot-password`, `/auth/resend-otp`; full auth UI (login, signup with validation, 6-digit OTP verify with resend cooldown, forgot password). Inline errors, loading states, zero fabricated success.

### Fabricated data — removed
**What was broken:** `AstroRepository` returned invented kundali, horoscope, panchang and milan data (`createDefault*()`) on every API failure.
**Removed:** All `createDefault*()` fallbacks.
**Added:** Honest `Result.failure` propagation on every non-2xx/exception. UI shows real error states.

### Free-money bug — closed
**What was broken:** `WalletRepository.verifyPayment` credited coins locally when verification failed, and a fake `pay_mock_`/`sig_verified` path existed.
**Removed:** `creditRecharge()`-on-failure, all mock payment strings.
**Added:** Failure now returns `Result.failure`. Real Razorpay Checkout launch (`Checkout.open()` with server order), real verify with payment signature.

### Endpoints — remapped to real routes
**What was broken:** 10 declared endpoints returned 404 (`horoscope/daily` GET, `panchang` GET, `matchmaking`, `chat/*`, `wallet/balance`, `wallet/order`, `wallet/verify`, `auth/config`, `auth/send-otp`).
**Removed:** `auth/config`, `auth/send-otp` declarations.
**Added:** `POST /rashifal`, `POST /panchang`, `POST /matching`, `GET /astrologers/chat/history`, `POST /astrologers/chat/open|close`, `GET /wallet/transactions`, `POST /wallet/topup-order|verify`, `POST /astrologers/session/start|tick|end`, `POST /numerology`, `POST /kundali/bnn`, `POST /astro-health`, `POST /ai/chat`.

### Security hardening
**Removed:** `android:usesCleartextTraffic="true"`; unconditional HTTP BODY logging; in-memory-only token storage; `google-services.json` committed to git (now gitignored; CI injects it; plugin applies conditionally).
**Added:** BODY logging gated behind `BuildConfig.DEBUG`; `EncryptedSharedPreferences` token store (survives process death; `security-crypto` dependency added); README documents CI secret injection.

### Kundali — real user data
**What was broken:** Hardcoded "Arjun Sharma" birth data; decorative South-Indian button; bogus D9 +4° offset.
**Removed:** Hardcoded fetch, fake South-Indian button, invented D9 offset.
**Added:** Birth-input form (name, date, time, place, lat/lon) as entry point; real `SouthIndianChart.kt` (fixed-sign 4x4 grid, planets by true sign, ascendant marker); divisionals render only API-returned data.

### Numerology / AstroHealth — real backends
**What was broken:** Hardcoded mulank=6, hardcoded organ list, "84% vitality".
**Removed:** All hardcoded values.
**Added:** `NumerologyViewModel` → `POST /numerology`; `AstroHealthViewModel` → `POST /astro-health` via `AstroRepository`; loading/error/empty states; medical disclaimer.

### Astrologer consultation — real billing + real messaging
**What was broken:** Keyword-bot fake chat replies; timer deducted coins locally with no server call.
**Removed:** Keyword responder, local coin deduction.
**Added:** Real session billing (`session/start` → `tick` every 60s → `end`); real messaging via `POST /ai/chat` (same mechanism as the website's astrologer chat); honest session UI with timer and End Consultation.

### AI Chat — new feature
**What was missing:** No AI chat existed in the native app.
**Added:** `AiChatScreen` + `AiChatViewModel` + `ChatRepository`; 3 modes (General / Divine / Mundane) with separate sessions; `POST /ai/chat`; typing indicator, auto-scroll, error handling, empty states; bottom-nav entry.

### UI polish
**Removed:** All pictographic emojis from UI strings (🪙🧘🕉️🙏🌅🌇🌙). Professional jyotish glyphs (♈☉♂) retained as standard notation.
**Added:** Material icons replacements; ~50 professional string resources.

### CI
**Added:** `.github/workflows/android-build.yml` (JDK 17, assembleDebug, APK artifact); google-services plugin applies only when `google-services.json` exists.
**Fixed:** Removed `android-actions/setup-android` step (was failing) — `ubuntu-latest` runners ship the Android SDK preinstalled.

### Not removed (verified present)
- In-app review: `RequestInAppReviewUseCase` (Google Play `review-ktx`) — intact, never removed.
- FCM push: `OmAstroFirebaseMessagingService` + `POST /auth/fcm-token` — intact.

### Notes
- SMS/WhatsApp OTP for login was never a real backend feature; the app contained only a local simulation. Not re-added as "real" because no such backend endpoint exists. Email+password + emailed signup OTP is the real contract.
- No WhatsApp/email marketing campaign endpoint exists on the backend; nothing was removed from the app in this area.

---

## [2026-10-09] — CI Build Fix Pass (v2.0.1-ci)

### Build failures — resolved
**What was broken:** CI build #6 failed at the `Build Debug APK` (Gradle) step with 30+ Kotlin compilation errors. The code was written by parallel agents and never compiled.
**Errors fixed:**
- `ApiClient.kt`: `Unresolved reference: BuildConfig` — the `buildConfig` feature was not enabled in `build.gradle.kts`. **Added:** `buildConfig = true` to `buildFeatures`.
- `AuthScreen.kt`: `Unresolved reference: align` — `Modifier.align()` used inside form functions that were not `ColumnScope` receivers. **Fixed:** made `LoginForm`, `SignupForm`, `VerifyOtpForm`, `ForgotPasswordForm` into `ColumnScope` extension functions.
- `AuthViewModel.kt`: `Unresolved reference: AuthToken` — the `AuthToken` wrapper class never existed; repository returns `LoginResponse`/`VerifySignupOtpResponse`. **Fixed:** use the real response types and their `token` fields.
- `KundaliScreen.kt`: `Unresolved reference: LazyColumnScope` — missing import. **Added:** `import androidx.compose.foundation.lazy.LazyColumnScope`.
- `RazorpayPaymentActivity.kt`: `'onPaymentSuccess'/'onPaymentError' overrides nothing` — single-arg/two-arg overloads belong to `PaymentResultListener`, not the implemented `PaymentResultWithDataListener`. **Removed:** the two invalid overloads.
- `WalletViewModel.kt`: `Unresolved reference: RazorpayOrderResponse` (renamed to `TopupOrderResponse`); `Cannot find parameter: pack` on `verifyPayment`; nullable `message.ifBlank` on `TopupVerifyResponse.message`. **Fixed:** all three.
**Result:** Build #7 triggered; awaiting CI result.

### CI build fix pass — continued
**What was broken:** After the first fix batch, build #8 still failed: `LazyColumnScope` import unresolved in `KundaliScreen.kt` (the type was not resolving from the Compose BOM); build #9 failed on `WalletScreen.kt:75` — `po.order.keyId` (field is `rzpKeyId` in `TopupOrderResponse`).
**Fixed:** Replaced `LazyColumnScope` with `LazyListScope` (stable parent interface); corrected `keyId` to `rzpKeyId`.
**Result:** Build #10 — SUCCESS. APK artifact produced (19.4 MB).

---

## [2026-10-09] — P0 Fabricated Data Removal: Matchmaking, Rashifal, Panchang, Dashboard

### Matchmaking (Kundali Milan) — fake preload removed, birth-input form added
**What was broken:** `MatchmakingViewModel.init{}` hardcoded a complete fake compatibility report: totalScore=34.0/36, percentage=94, verdict="Excellent Match (Uttam Vivah)", manglikBoy/Girl="Non-Manglik", and 8 hardcoded `GunaDetail` rows. The screen displayed this fabricated report on launch with no user input, and the UI had no birth-details form.
**Removed:** All hardcoded fake data from `init{}` (fake scores, verdict, manglik status, 8 fake guna rows, fake conclusion). Removed all `?:` fallback defaults in `MatchmakingScreen` (e.g., `?: 34.0`, `?: "Uttam Vivah"`, `?: 94`, `?: "Non-Manglik"`).
**Added:** Two-person birth details input form (name, date of birth, time of birth, place of birth for both groom and bride) with validation. Real `POST /matching` API is called only on form submit. Honest empty state ("Enter birth details to check compatibility") when no result. Honest error state with "Try Again" button on API failure. "Check Another Pair" button to reset. `errorMessage` StateFlow added to ViewModel for failure propagation.

### Rashifal (Horoscope) — fabricated fallbacks removed
**What was broken:** `HoroscopeScreen` displayed hardcoded fake data when the API returned null: prediction "Planetary alignments favor bold initiatives today...", loveScore=85, careerScore=78, healthScore=92, luckyNumber=7, luckyColor="Saffron", remedy "Chant Gayatri Mantra 11 times...".
**Removed:** All 7 hardcoded fallback values.
**Added:** `errorMessage` StateFlow to `HoroscopeViewModel` with `retry()` function. Honest "Horoscope Unavailable" error state with Retry button when API fails or returns null. Real API data displayed only on success.

### Panchang — fabricated fallbacks removed
**What was broken:** `PanchangScreen` displayed hardcoded fake values when API returned null: sunrise "06:18 AM", sunset "06:22 PM", moonrise "08:45 PM", tithi "Shukla Paksha Ekadashi", nakshatra "Rohini", yoga "Siddhi", karana "Bava", day "Brihaspativar (Thursday)", Abhijit "11:51 AM - 12:38 PM", Amrit Kaal "02:15 PM - 03:45 PM", Rahu Kaal "01:30 PM - 03:00 PM", Yamaganda "06:00 AM - 07:30 AM", Gulika Kaal "09:00 AM - 10:30 AM".
**Removed:** All 13 hardcoded fallback values.
**Added:** `errorMessage` StateFlow to `PanchangViewModel`. Honest "Panchang Unavailable" error state with Retry button. Real API data displayed only on success.

### Dashboard — fabricated Panchang fallbacks removed
**What was broken:** `DashboardScreen` quick Panchang card showed fake values: tithi ?: "Shukla Paksha Dashami", nakshatra ?: "Rohini", yoga ?: "Siddhi".
**Removed:** All 3 hardcoded fallback strings.
**Added:** Neutral "—" placeholder shown when Panchang data is null (loading or failed), instead of fabricated values.

---

## [2026-10-09] — Geocode Place Search (v2.1.0-geocode)

### Place search — added (blocks real usage previously)
**What was broken:** All birth forms required users to manually type latitude/longitude (with a tip saying "find coordinates in a maps app"). Unusable for most users.
**Added:**
- `POST /geocode/search` endpoint in `OmAstroApiService` (`GeocodeSearchRequest{query}`, `GeocodeSearchResponse` accepting both `places` and `results` envelope keys, `GeocodePlace{name,lat,lon,country,state}` with `displayName`).
- New reusable `PlaceSearchField` composable (`ui/components/PlaceSearchField.kt`): debounced (~400ms) autocomplete dropdown, "Searching..." indicator, "No places found" empty state, graceful error handling, tap-to-select with coordinates shown per row.
**Integrated into:**
- Kundali birth form: selecting a place auto-fills latitude/longitude (manual entry still available as fallback).
- Matchmaking (groom + bride): place autocomplete for accurate place names (server geocodes the name).
- AstroHealth birth form: selecting a place auto-fills latitude/longitude (manual entry still available).
**Removed:** The "find coordinates in a maps app" tip text in Kundali/AstroHealth forms (replaced with auto-fill guidance).

---

## [2026-10-09] — Saved Charts Feature (v2.1.0)

### Added: Saved Kundali Charts
**What was missing:** Website has `/me/charts` for saved kundalis; the app required re-entering birth details every time.
**Added:**
- API: `GET /me/charts` (list), `POST /me/charts` (save with name + birth details), `DELETE /me/charts/{id}` (delete) in `OmAstroApiService`
- Models: `SavedChart`, `SavedChartsResponse`, `SaveChartRequest`, `SaveChartResponse` (defensive parsing of `charts`/`data` envelope keys)
- `SavedChartsRepository`: list/save/delete with honest error propagation, no fabricated data
- `SavedChartsScreen`: chart list with name/birth details/created date, tap-to-load, delete with confirmation dialog, pull-to-refresh, empty state ("No saved charts yet"), error state with retry
- `SavedChartsViewModel`: loading/error/delete states via StateFlow
- `KundaliScreen`: bookmark icon in top bar (opens Saved Charts), save icon when viewing a chart (dialog to name and save current birth details); auto-loads chart selected from Saved Charts via `PendingChartHolder`
- `KundaliViewModel`: tracks `lastRequest` (birth details of displayed chart), new `loadSavedChart()` method
- Navigation: `Screen.SavedCharts` route; accessible from Kundali top bar and Dashboard quick actions
- `DashboardScreen`: new "Saved Charts" quick action tile

---

## [2026-10-09] — Settings Screen Fixes + Terminology (v2.0.2-settings)

### Fake notification toggles — removed
**What was broken:** Settings had Daily Rashifal Alert, Shubh Muhurat & Panchang, and WhatsApp notification toggles using local `remember` state only. They saved nothing to the server and scheduled nothing — purely decorative switches.
**Checked:** No `/me/preferences` or notification-preference endpoint exists in `OmAstroApiService.kt`.
**Removed:** All three fake toggles and the "Notifications & Alerts" section. (FCM push registration via `/auth/fcm-token` remains intact.)

### Fake theme toggle — removed
**What was broken:** Theme Mode switch toggled a boolean but the app is dark-only by design — switching did nothing visually.
**Removed:** The entire "Appearance & Display" theme row.

### Fake language switch — removed
**What was broken:** "Switch" button only toggled a label string between "English (en)" and "हिन्दी (Hindi)" — it never changed the app locale.
**Removed:** The language switcher row. (Full i18n is a separate feature; a fake toggle is worse than none.)

### Dead Privacy & Disclaimers card — made functional
**What was broken:** The card had no `onClick` — tapping it did nothing.
**Added:** Tapping now opens an `AlertDialog` with a real astrological disclaimer and privacy notice.

### Terminology — Coins to INR wallet
**What was broken:** Dashboard badge showed `"$coins Coins"` and Settings showed "Recharge & Coin Balance", while the website uses INR wallet (₹) terminology.
**Changed:**
- Dashboard badge: `"$coins Coins"` → `"₹$walletBalance"` (variable renamed `coins` → `walletBalance` for clarity)
- Settings wallet row: "Recharge & Coin Balance" → "Wallet Balance & Add Money" with a wallet icon

### Dead code — deleted
**Removed:** `app/src/main/java/com/omastro/app/ui/screens/home/HomeScreen.kt` (321 lines, never routed to — `MainActivity` uses `DashboardScreen`). `HomeViewModel.kt` is kept (still used by `DashboardScreen`).
