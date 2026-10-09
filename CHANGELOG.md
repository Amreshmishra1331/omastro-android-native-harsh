# Changelog — OmAstro Android (Native Kotlin)

All notable changes to the native Android app. Each entry lists what was broken or missing, what was removed, and what was added.

---

## [2026-10-10] — Vastu Compass Mode (live compass + camera)

### Added
1. **Compass Mode in the Vastu screen** (`ui/screens/vastu/CompassModeScreen.kt`, `CompassSensor.kt`):
   - **Before**: facing direction could only be picked from a dropdown — users had to guess their entrance direction.
   - **After**: "Find direction with compass" button in the Facing & Entrance card opens a full-screen live compass: real-time heading from the device magnetometer + accelerometer (tilt-compensated via `SensorManager.getRotationMatrix`/`getOrientation`), CameraX live preview behind the dial — point the top of the phone at the main entrance and see the direction in real time.
   - **4 switchable compass styles** (style chips above the dial, all overlay the camera preview):
     - *Classic dial*: rotating ring with ticks; all 8 directions shown simultaneously; the live facing sector glows with a gold wedge and the live direction label is enlarged gold.
     - *Minimal*: thin ring with heading progress arc, 8 minimal ticks, large digital degrees + direction name in the center.
     - *Vastu Mandala*: 8 direction sectors with their dikpala guardian deities (Kubera/कुबेर N, Ishana/ईशान NE, Indra/इंद्र E, Agni/अग्नि SE, Yama/यम S, Nirriti/निरृति SW, Varuna/वरुण W, Vayu/वायु NW); the live sector glows gold; ॐ Brahmasthan center.
     - *Digital*: instrument panel with rotating arrow, big digital degrees readout, direction name.
   - **Photo capture**: shutter button → CameraX `ImageCapture` still → the live direction name + code, degrees, and timestamp are stamped onto the image in a bottom banner (the photo itself proves which direction was faced) → saved to the device gallery (`Pictures/OmAstro`) → result dialog with stamped preview.
   - **Attach to analysis**: "Use for house-map analysis" hands the stamped photo to the Vastu Photo tab (`VastuPhotoForm` prefill), which opens with the photo loaded and ready to analyze.
   - **"Use this direction"** captures the live 8-sector direction code and auto-fills the form's facing-direction field.
   - **Permissions**: CAMERA via the existing `rememberPermissionRequest` rationale/denial dialogs; sensor access needs no runtime permission. Gallery save uses MediaStore (WRITE_EXTERNAL_STORAGE declared maxSdk 28 for Android 9 and below).
   - **Honest states**: devices without magnetometer/accelerometer get a clear "Compass not available" screen (never fake headings); low-accuracy readings show a figure-8 calibration hint; capture failures show an honest error (no silent loss).
   - Fully bilingual (EN/हिन्दी), auto-switches with Settings language — 22 new keys in `FeatureStrings.kt` (`vastu_compass_*`) + `W.vastuDeityName()` for dikpala names.

### Changed
- `app/build.gradle.kts`: added CameraX 1.3.4 (`camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view`).
- `AndroidManifest.xml`: added WRITE_EXTERNAL_STORAGE (maxSdk 28) for gallery saves on Android 9 and below.
- `VastuScreen.kt`: compass entry button in the facing card; full-screen `CompassModeScreen` overlay wired to the `facing` form state; compass-captured photo hands off to the Photo tab via `prefillPhoto`.

### Removed
- Nothing.

---

## [2026-10-10] — World-class differentiators: voice, photo AI, palmistry, Vastu

NEW features beyond the website (backend: `POST /vastu/*`, `POST /ai/vision`, `POST /palmistry/analyze` — live on production, documented in `workspace/vastu-vision-api.md`).

### Added
1. **Voice typing in AI chat** (`VoiceSpeech.kt`, `AiChatScreen.kt`): mic button in the chat input bar → Android SpeechRecognizer (en-IN / hi-IN per chat language) → transcribed text appended to the input. RECORD_AUDIO permission with rationale dialog + graceful denial state. Listening state shown on the mic button. Bilingual: mic rationale dialog, voice-mode snackbar, mic/photo button descriptions, input hint, all SpeechRecognizer error messages — auto-switch with Settings language.
2. **Voice mode — spoken AI replies** (`VoiceSpeech.kt`): speaker toggle in the AI chat header → Android TextToSpeech reads each new AI reply aloud (markdown stripped; hi-IN voice when chat language is Hindi). Tapping the toggle while speaking stops it. Toggle persisted via `VoiceModePrefs`; TTS engine initialized in `OmAstroApp.onCreate`, stopped on screen exit.
3. **Photo attach in AI chat** (`PhotoAttachDialog.kt`, `PhotoAnalysisResultDialog.kt`): camera button in the input bar → chooser (Take Photo / Gallery / File) → preview + question field → `POST /ai/vision` multipart upload → result dialog with analysis text + "Attach to Chat" / "Ask AI" actions. CAMERA + READ_MEDIA_IMAGES permissions with rationale dialogs. `parsed_facts` attached as chat context via new `PhotoContextAttachment` (blue chips, same mechanism as birth-data chips) — follow-up questions reference the photo. Cross-screen handoff via `PendingPhotoContextHolder` (consumed once on chat open). Bilingual: chooser, preview, question field, errors, result dialog — auto-switch with Settings language.
4. **Face Reading screen** (`ui/screens/facereading/FaceReadingScreen.kt`): NEW — front-face photo guidance tips, camera/gallery capture → `POST /face-reading/analyze` (multipart `image`; auth) → rich Samudrika Shastra result (hero with overall personality, full reading text, 8 facial features with observation + interpretation each, career, relationships, health indications, guidance, disclaimer). Honesty refusal: when the photo has no clear face, shows the server's refusal honestly — never invents features. **Deep AI integration**: "Ask AI About This Reading" attaches the FULL reading as chat context (every feature's observation + interpretation, personality, career, relationships, health, guidance, parsed facts — or the refusal only) and opens AI chat. Fully bilingual (EN/हिन्दी), auto-switches with Settings language. New `Screen.FaceReading` route + drawer entry ("Face Reading — AI face reading"; drawer Hindi: चेहरा विश्लेषण). NOTE: server endpoint still being built — until live, the screen shows an honest "not available yet" state instead of a raw 404.
5. **Palmistry screen** (`ui/screens/palmistry/PalmistryScreen.kt`): photo guidance tips, left/right hand selector, camera/gallery capture → `POST /palmistry/analyze` → rich result (hand summary hero, full reading, lines with quality badges, mounts, key observations, guidance, "unclear in photo" honesty section, disclaimer). **Deep AI integration**: "Ask AI About This Reading" attaches the FULL reading as chat context — hand summary, complete reading text, every line, every mount, all observations, all guidance, uncertainties, raw parsed facts, plus an instruction to answer strictly from the reading — and opens AI chat, so the user can ask anything ("meri health line kya kehti hai?") and the AI answers from their actual reading. Fully bilingual (EN/हिन्दी), auto-switches with Settings language. New `Screen.Palmistry` route + drawer entry ("Palmistry — AI palm reading"; drawer Hindi: हस्तरेखा).
6. **Vastu Shastra screen** (`ui/screens/vastu/VastuScreen.kt`): two modes —
   - **Manual**: facing direction, entrance pada 1–32 with live devata/verdict helper (from `GET /vastu/reference`), plot shape/dimensions, slope/road/water/tank/septic/staircase direction pickers, dynamic room list (20 types × 9 directions) → `POST /vastu/analyze`.
   - **House-Map Photo**: camera/gallery → `POST /vastu/analyze-photo` → parsed inputs + full analysis.
   - **Results**: 0–100 score dial with grade, dosha counts by severity, dosha cards (title, severity badge, detail, classical rule cited, non-demolition remedies), placement verdict rows, disclaimer.
   - **Deep AI integration**: "Ask AI About This Reading" attaches the FULL analysis as chat context — score/grade, dosha counts, all analyzed inputs (manual values or photo-parsed inputs + notes), every direction/placement verdict with rule and detail, every dosha with severity, classical rule, detail and all non-demolition remedies, disclaimer, plus an instruction to suggest the listed remedies first — and opens AI chat, so the user can ask anything ("ye dosh kaise theek karun?") and the AI answers from their actual analysis.
   - Fully bilingual (EN/हिन्दी): tabs, form labels, direction/room/plot-shape picker names, severity labels, grades, buttons, errors — auto-switch with Settings language (no per-screen toggle). New `Screen.Vastu` route + drawer entry ("Vastu Shastra — Home-energy analysis"; drawer Hindi: वास्तु शास्त्र).
7. **Centralized bilingual strings** (`ui/i18n/FeatureStrings.kt`, object `W`): ~170 EN/हिन्दी keyed pairs for voice, photo, Palmistry, Vastu; all new screens collect `LangPrefs.language` and auto-switch. `LangPrefs` scope note updated; drawer now shows हस्तरेखा / वास्तु शास्त्र in Hindi; language dialog copy updated ("Menus, voice, photo, Palmistry & Vastu").
6. **VisionRepository** (`data/repository/VisionRepository.kt`): all 5 new endpoints with defensive parsing, honest error mapping (401 → sign-in prompt, 429 → rate-limit message, 422 → clearer-photo guidance).

### Changed
- `AndroidManifest.xml`: added RECORD_AUDIO, CAMERA, READ_MEDIA_IMAGES, READ_EXTERNAL_STORAGE (maxSdk 32).
- `OmAstroApiService.kt`: 5 new Retrofit methods (3 multipart).
- `AiChatViewModel.kt`: `photoAttachments` in UI state, included in `buildContext()`; `attachPhotoContext` / `removePhotoAttachment` / `consumePendingPhotoContext`.
- `VisionModels.kt`: `PalmistryReading.toContextBlock()` now renders the FULL reading (hand summary, complete text, all lines/mounts/observations/guidance/uncertainties/disclaimer + raw facts + answer-strictly-from-reading instruction); new `VastuAnalysis.toFullContextBlock(request, photoParse, photoNotes)` (score, inputs, every verdict, every dosha + remedies); shared `renderGeneric` helper.
- `VastuViewModel`: keeps last manual request / photo parse / notes for the AI handoff; `VastuUiState.photoParse` added.
- `VoiceSpeech.kt`: permission dialogs now bilingual and re-label live on language change (`updateLabels`).
- `LangPrefs.kt`: drawer Hindi labels for Palmistry / Vastu Shastra; scope note updated. Settings + drawer language rows no longer say "(partial)".
- Fixed CI build #61 compile errors: missing `okhttp3.MultipartBody` import in `OmAstroApiService.kt`; `TextFieldDefaults.colors` → `OutlinedTextFieldDefaults.colors` in `PhotoAttachDialog.kt` / `VastuScreen.kt`.

### Removed
- Nothing.

---

## [2026-10-09] — A-to-Z audit fixes (all 17 confirmed issues + missing features)

### Fixed confirmed issues
1. **Dashboard logout didn't clear the token** (`DashboardScreen.kt`): the Account-tab Logout only navigated to `auth`; the token stayed in encrypted storage so a cold start landed back on home. Now calls `AppContainer.authRepository.logout()` before navigating.
2. **Recent Kundalis opened a blank screen** (`DashboardScreen.kt`): taps ignored the chart id. Now looks the chart up and loads it via `PendingChartHolder` (Home + Activity tabs).
3. **Change Password was a no-op; "Forgot password?" went to login mode**: probed `/wp-json/omastro/v1` — no change-password route exists. Replaced the dead button with honest copy + "Send Password Reset Link" (real `POST auth/forgot-password` flow). Added `auth/forgot` route (`Screen.Auth.FORGOT_ROUTE`, `AuthScreen(initialMode=...)`) so the link opens forgot mode directly.
4. **Kundali `tob_hr`/`tob_mn` dropped AM/PM** (`KundaliViewModel.kt`, `AstroHealthViewModel.kt`): "08:30 PM" sent hr=8. Now both use the shared `parseTimeTo24` helper (20:30).
5. **Numerology Mobile tool hardcoded Moolank 1**: `needsDob()` excluded MOBILE_NUM and fell back to `Triple(1,1,2000)`. Now every tool collects DOB; the verdict compares against the user's real Moolank.
6. **Numerology client-side claims**: `POST /numerology` verified on server (rich: coreNumbers, personalYear/Month/Day, loshuGrid, luckyNumbers/Days/Colors, karmaNumbers, challenges). Tools now prefer server values with local fallback: Lo Shu (server loshuGrid), Time Cycles (server personalYear/Month/Day), Lucky Calendar (server luckyNumbers/Days/Colors), Karmic (server karmaNumbers + challenges), Moolank (server coreNumbers as extra section). Removed the unverified "matching the website engine" comment.
7. **Matchmaking fake defaults** (`MatchmakingModels.kt`, `MatchmakingScreen.kt`): defaults were 28.5/79%/"Highly Auspicious Match" — plausible fakes if the server omitted keys. Replaced with sentinels (-1/blank) + `hasRealData` guard; incomplete responses now show an honest error state, never a fake verdict.
8. **Release keystore password in plaintext gradle**: `app/build.gradle.kts` now reads `OMASTRO_KEYSTORE_PASSWORD` env (CI secret) with the existing value as local fallback.
9. **Duplicate `POST kundali/bnn` Retrofit declarations**: removed the uncalled Map-based `getBnnAnalysis`; single typed `getBnnReport` remains.
10. **Notification taps did nothing**: `notification_type` extra is now consumed — `MainActivity.routeForNotificationType()` maps ai_chat/chat→AI Chat, astrologer→Astrologers, panchang→Panchang, horoscope→Rashifal, both on cold start and via `onNewIntent` while alive.
11. **`AstrologyHeader` hardcoded `coinBalance = 500`**: parameter is now nullable; the badge renders only with a real balance, never a fake number.
12. **Astrologers directory had no search/sort/filter/refresh**: added search field (name/expertise/language), sort chips (Recommended/Top rated/Price/Most experienced), Online-only filter, and a refresh action. Empty states distinguish "no astrologers" from "no matches".
13. **Wallet had no refresh**: added refresh action in the top bar (balance + transactions).
14. **No proactive low-balance warning in astrologer chat**: tick handler now warns once via system message when `wallet_covers_sec` ≤ 60s, before the hard close.
15. **Deleted dead `HomeViewModel.kt`** (unreferenced). `POST /numerology` is now wired (no longer dead).
16. **Danger Zone advertised deletion with no action**: no `DELETE /me` route exists (probed). Replaced with honest copy directing to website support; no fake button.
17. **AI chat language hardcoded "en"**: added per-mode EN/हिन्दी selector pill row; `language` is sent with every message and saved with sessions.

### Added missing features (PDF rows where Android = MISSING)
- **Notification center** (website parity): `GET /notifications`, `POST /notifications/mark-read`, `POST /notifications/clear` in Retrofit; `NotificationModels.kt` + `NotificationRepository.kt` (unread badge state); `NotificationsScreen.kt` (list with read/unread styling, mark-read on tap, mark-all-read, clear-all with confirm); bell icon with red unread badge in the Dashboard header; `Screen.Notifications` route.
- **Dark/Light mode**: `applyAppPalette(dark)` in `Color.kt` (tokens are now palette-driven `var`s), `OmAstroTheme(darkTheme=...)`, `ThemePrefs` (persisted), `key(isDarkTheme)` recomposition in `MainActivity`, working toggle in Settings ("Dark — Cosmic Night" / "Light — Temple Gold"). 217 hardcoded `Color.White` usages migrated to palette-following `StarlightWhite`.
- **Hindi (honest partial scope)**: `LangPrefs` ("en"/"hi", persisted); real Hindi strings for bottom nav + drawer menus; working radio buttons in the Language dialog labeled "Menus & navigation only (partial)". AI chat reply language (EN/HI) is server-side and already real.
- **AI Chat History in drawer**: was "Coming soon" — now navigates to `ai_chat/history` which opens AI Chat with the real saved-sessions dialog open.
- **Report a Bug**: was "Coming soon" — now opens an email composer to support@omastroai.com with app version prefilled (drawer + Settings).
- **Refer & Earn**: probed server — no referral endpoint exists; kept as honest "Coming soon" (no fake implementation).

### Removed
- Dead `HomeViewModel.kt` and the empty `ui/screens/home/` package.
- Uncalled `getBnnAnalysis` (Map) Retrofit method + repository wrapper.
- Fake `coinBalance = 500` default; fake Change-Password button; fake Danger-Zone delete button.

---

## [2026-10-09] — Dashboard website parity rebuild (/dashboard/)

### Broken / missing
- **Dashboard was a services home screen, not the website's user dashboard**: The old `DashboardScreen` showed "Today's Oracle", AI search bar, service grid, astrologer carousel, divine connection, numerology, panchang — a marketing-style home page. The website's `/dashboard/` is a user account dashboard with header (avatar/name/email/wallet), tab navigation, and account management.
- **No user profile data**: App never called `GET /me` (user info with plan, avatar). No endpoint existed in `OmAstroApiService`.
- **No tab structure**: Website has 7 tabs (Home, Activity, Profile, Password, Plan, Billing, Account). App had none.
- **No Activity section**: Website's Activity tab has sub-tabs for Kundali (saved charts), Chats (AI sessions with delete/clear-all), Wallet (balance + transactions), Stats. App had no equivalent.
- **No Profile/Password/Plan/Billing/Account tabs**: All missing.

### Added
- `GET /me` endpoint in `OmAstroApiService` + `MeResponse`/`MeData`/`MePlan` models in `AuthModels.kt` (matches server's `kg_rest_me` response: user_id, email, name, display_name, username, is_admin, avatar, plan, chat_quota).
- New `DashboardViewModel.kt`: loads profile (`GET /me`), wallet balance + transactions, saved kundalis (`GET /me/charts`), AI chat sessions (`GET /ai/chats`). Exposes `deleteAiChat()`, `clearAllAiChats()`, `deleteSavedChart()`.
- **Rebuilt `DashboardScreen.kt` to match website `/dashboard/` exactly**:
  - **Header**: avatar (from `/me`), display name, email, wallet balance pill (taps to Billing tab).
  - **Tab nav**: Home, Activity, Profile, Password, Plan, Billing, Account (scrollable pills, website order).
  - **Home tab**: wallet hero card (orange gradient, total + paid/bonus breakdown like website), Add Money card, quick actions (Activity, Wallet, Edit Profile, Change Password), Recent Kundalis (5).
  - **Activity tab**: sub-tabs Kundali / Chats / Wallet / Stats.
    - Kundali: saved charts list with count, empty state with "Make Kundali" CTA.
    - Chats: AI sessions with title/mode/date, delete per chat, "Clear All Chats" button.
    - Wallet: balance hero + Add Money + transaction list.
    - Stats: 2x2 grid (Kundalis, Total Chats, Wallet Balance, Transactions).
  - **Profile tab**: avatar, Full Name, Username, Email (read-only display from `/me`).
  - **Password tab**: current email display, current/new/confirm password fields, Change Password button, forgot-password link.
  - **Plan tab**: plan name with Pro/Max indicator, Add Money CTA (website shows wallet card here).
  - **Billing tab**: wallet hero (total + paid/bonus), Add Money button, full transaction history.
  - **Account tab**: Danger Zone card, Logout, Settings navigation.
- All data from real APIs. No emojis (Jyotish glyphs only for plan indicators).

### Removed
- Old marketing-style dashboard content: "Today's Oracle" card, AI search bar with suggestion chips, 8-service grid, astrologer carousel, Divine Connection deities, Numerology grid, Special Methods cards, Panchang quick card. (These services remain accessible via bottom nav and sidebar.)

---

## [2026-10-09] — Chat History + Wallet website parity

### Broken / missing
- **Wallet transactions never loaded**: `WalletViewModel.refreshTransactions()` was a no-op (comment said "No-op if the repository pushes updates itself" — but nothing pushed). Transaction history always showed empty.
- **No Paid/Bonus breakdown**: API returns `paid_paise`, `bonus_paise`, `total_paise` (+ INR equivalents) but the UI only showed the total. Website shows the breakdown.
- **No "Clear All" in AI chat history**: `ChatRepository.clearAllChats()` (DELETE /ai/chats) existed but was never exposed in the ViewModel or UI.
- **Transaction amounts in raw paise**: `TransactionRow` displayed the raw integer (e.g. "-5000") instead of formatted INR (e.g. "-₹50.00").

### Added
- `WalletViewModel.refreshTransactions()` now calls `walletRepo.getTransactions()` and surfaces errors.
- `WalletViewModel.refreshBalance()` added; `WalletScreen` loads balance + transactions on launch via `LaunchedEffect(Unit)`.
- Paid/Bonus breakdown UI: two columns under the total balance showing "Paid Balance" and "Bonus Balance" in INR.
- `WalletRepository` now exposes `paidBalancePaise`, `bonusBalancePaise`, `paidBalanceInr`, `bonusBalanceInr` StateFlows, populated from `GET /wallet/balance`.
- `AiChatViewModel.clearAllSessions()` wired to `ChatRepository.clearAllChats()`; "Clear All" button added to `SavedSessionsDialog` (red, left-aligned, only shown when sessions exist).
- `SavedSessionsDialog` accepts optional `onClearAll` callback (defaults to no-op for backward compatibility).
- `AiChatScreen` passes `onClearAll = { viewModel.clearAllSessions() }`.

### Already present (verified, no changes needed)
- AI chat history: list (GET /ai/chats), open/restore (GET /ai/chats/{id} with messages + context), save (POST /ai/chats), delete (DELETE /ai/chats/{id}) — all wired in ViewModel + dialog UI.
- Astrologer chat history: resume via `GET /astrologers/chat/history` (session_id or astrologer_slug) — the website has no separate "list past astrologer chats" endpoint; history restores on session open, which the app already does.
- Wallet Add Money: Razorpay integration (createOrder → RazorpayPaymentActivity → verifyPayment) — already functional.

### Removed
- Nothing.

---

## [2026-10-09] — Kundali 23-tab build fixes (CI green)

### Fixed
- **Redeclaration: SaturnTransit** — the new Kundali model collided with the existing `HoroscopeModels.SaturnTransit` (rashifal). Merged: the HoroscopeModels class now carries the extra kundali keys (`house_from_moon`, `saturn_rashi`, `moon_rashi`, `description_hi`) with defaults, so one class serves both the rashifal `saturn_transit` and kundali `saturnTransit` payloads; the duplicate in `KundaliModels.kt` was removed.
- **Typed kundali envelopes restored** — `POST kundali/personality`, `kundali/bnn`, `kundali/lal-kitab`, `kundali/jaimini` return `Response<ReportEnvelope>` / `Response<JaiminiEnvelope>` (types committed in `KundaliModels.kt`); `AstroRepository` relies on the typed `.data` access.
- CI: Android Build **success** on `23cb5dd`.

---

## [2026-10-09] — Kundali: 8 tabs → 23 website-parity tabs (full A-Z rebuild)

### Broken / missing
- **Only 8 of 23 website tabs**: Charts, Grahas, Dasha, Yogas, Doshas, Ashtaka, Lal Kitab, Analysis. Missing entirely: Birth Info (+Lucky Elements), Navamsha, Karakamsha, Chandra Lagna, Surya Lagna, Chalit, Divisional (D1–D60), Sodasvarga, Personality, Panchang, Shadbal, Bhrigu Nadi, Numerology embed, Char Dasha, Faladesh, Remedies.
- **Dasha only 3 levels**: website shows 7 (MD→AD→PD→Sookshma→Prana→Adi→Deha); the app rendered MD/AD/PD only.
- **Grahas only 6 columns**: website Planets tab has 12 (Planet, Symbol, Sign, Degree, DMS, Nakshatra, Pada, Nakshatra Lord, House, Navamsha Sign, Navamsha House, Status).
- **Lal Kitab was a summary stub**: website renders the full server report via `POST /kundali/lal-kitab`.
- **Unused response data**: `suryaChart`, `chandraChart`, `chalitChart`, `chalitHouses`, `allDivisionalCharts`, `panchang`, `sodasvarga`, `shadbala`, `saturnTransit`, `currentTransits`, `analysis_tabs` were returned by `POST /kundali` but never parsed or shown.
- **No dedicated endpoints**: `kundali/personality`, `kundali/bnn` (typed), `kundali/lal-kitab`, `kundali/jaimini` were never called.

### Removed
- Old 8-tab list (`Charts, Grahas, Dasha, Yogas, Doshas, Ashtaka, Lal Kitab, Analysis`).
- Superseded `grahasTab` (6-col), `lalKitabTab` (summary stub), `analysisTab` + exclusive helpers (`LIFE_AREAS`, `PlanetAnalysisCard`, `HouseAnalysisCard`, `BirthPanchangCard`, `RemediesCard`, `buildPlanetRemedies`, `houseSignificance`, `planetInterpretation`, `TableCell`, `DignityChip`) — content now covered by dedicated tabs.
- Old `ExpandableDashaNode` 3-level expansion (now expands to whatever depth the server provides, up to 7).

### Added
- **Models (`KundaliModels.kt`)**: `KundaliResponse` gains `chalitHouses`, `suryaChart`, `chandraChart`, `chalitChart` (`ChartData`: lagnaRashi + houses, verified live), `allDivisionalCharts` (D1–D60 keys, verified), `panchang` (`BirthPanchang`), `sodasvarga` (D1–D60 → planet/rashi list, verified), `shadbala` (`ShadbalaData`: shadbala/bhavabala/vimshopaka as `Map<String, Map<String, Any>>` — server mixes doubles/ints/booleans), `saturnTransit` (`SaturnTransit`), `currentTransits` (MAP planet→`TransitInfo`, verified — not a list), `analysisTabs` (`AnalysisTabs`: ppills, grahaDetail, d1Analysis, d9Analysis, dasha, panchang, shadbal, personality, bnn). `VimshottariDasha` gains `currentSookshma/Prana/Adi/Deha`. New: `ReportEnvelope`/`ReportData` (htmlReport/textReport/analysis_text + `bestText()` + HTML stripper), `JaiminiEnvelope`/`JaiminiData` (karakas 7 Chara karakas, karakamsha, karakamshaHouses 0-based `ri`, charDashas/charAntars with JD bounds, curCharMD/AD), `julianDayToDateString()` (Fliegel–Van Flandern), `LuckyElements` (standard Vedic lagna mapping: colour/gem per rashi, number from lagna lord), `anyToDisplay()`.
- **API (`OmAstroApiService.kt`)**: `POST kundali/personality`, `POST kundali/bnn` (typed `getBnnReport`; untyped `getBnnAnalysis` kept), `POST kundali/lal-kitab`, `POST kundali/jaimini` — all take `KundaliRequest`, all shapes verified live 2026-10-09.
- **Repository (`AstroRepository.kt`)**: `getPersonality()`, `getBnnFull()`, `getLalKitabFull()` (shared typed `getReport` helper), `getJaimini()`.
- **ViewModel (`KundaliViewModel.kt`)**: `KUNDALI_TABS` (23, website order); `ReportUiState`/`JaiminiUiState`; lazy one-shot loading on tab select (13=Personality, 17=BNN, 18=Lal Kitab, 3/20=Jaimini) reusing `_lastRequest`; `retryReport()`; report caches reset on every new chart.
- **UI — new `KundaliExtendedTabs.kt`** (same package): Birth Info (+Lucky Elements card); Birth Chart extras (legend, planet pills, graha detail, D1 analysis, Sade Sati alert, Gochar strip); Navamsha (NI/SI + D9 planet table + D9 analysis); Karakamsha (AK→Karakamsha explainer, lagna badge, NI/SI charts, house-by-house table); Chandra/Surya/Chalit via generic `lagnaChartTab`; Divisional (16-varga selector, NI/SI, legend, varga significance essay); Sodasvarga (16×planet table); Personality (lazy report); Planets (12-column table); Panchang (birth panchang + analysis); Shadbal (6-strengths table + verdict, Bhavabala, Vimshopaka bars + analysis); Bhrigu Nadi (lazy report); Lal Kitab (full lazy report); Numerology (birth-data card → hub with `PendingNumerologyHolder` pre-fill); Char Dasha (7 karakas, current MD/AD hero, 36-period sequence with JD→date, antardashas); Faladesh (Sade Sati alert, per-planet Gochar cards, "Chat with Om Astro AI" button); Remedies (5 sub-tabs Ratna/Rudraksh/Dhatu/Jad/Vedic Upay driven by weak/afflicted planets + lagna lord, roots verified against published sources).
- **UI — `KundaliScreen.kt`**: 23-tab `ScrollableTabRow` (11sp); `when` dispatches all 23 tabs; Dasha hero shows all 7 current levels + `analysis_tabs.dasha`; `chartsTab` promoted to `internal` for reuse.
- **Navigation**: `KundaliScreen` gains `onNavigateToNumerology` / `onNavigateToAiChat` (wired in `MainActivity` to Numerology hub and AI Chat); new `PendingNumerologyHolder`; `NumerologyToolScreen` pre-fills name+DOB from it on open.

---

## [2026-10-09] — Astrologer consultation A-Z rebuild (website parity)

### Broken / missing
- **Wrong API params**: `chat/open` sent `astrologer_id` (server expects `slug`); `chat/history` sent `astrologer_id` query (server expects `session_id`/`astrologer_slug`); `chat/close` sent both (server takes only `session_id`). Sessions never registered correctly.
- **Billing tick was 60s**: website bills on a 30s heartbeat; the app ticked every 60s, drifting from server expectations.
- **No insufficient-balance handling**: session start/tick failures showed a generic error; the website blocks with an Add Money popup and locks input.
- **No humanizing delays**: replies appeared instantly as one bubble; the website uses 2-6s pre-typing pause, per-sentence bubbles (first 2-3.5s, later 3-12s by length), 0.7-1.3s between bubbles, send locked until done.
- **No greeting, no idle handling**: website sends a personalized greeting (2-6s delay + 1.2s typing) and nudges/closes idle chats (1-min nudge, 2-min hard close).
- **No birth-data attach**: website gates chat entry on birth attach and sends context with every message.
- **No review flow**: website shows a 5-star review card after close.
- **No auto-match**: website has "Finding best astrologers…" preferring online ₹5/min.
- **No rate consent**: website warns when the astrologer is offline or costs > ₹5/min.
- **No review endpoint**: `POST /astrologers/review` was missing from the API service.
- **AI messages lacked billing handshake**: astrologer messages didn't send `billing_mode: 'astrologer'` + `astro_session`, so the server couldn't skip the per-message AI debit.
- **Navigation passed `id` not `slug`**: chat route carried `astro.id` (e.g. "astro-01") but all chat/billing endpoints need the `slug`.

### Removed
- 60-second billing tick (now 30s per website).
- Instant single-bubble reply rendering in astrologer chat.
- `OpenChatRequest(astrologer_id)`, `CloseChatRequest(astrologer_id, session_id)` shapes.
- Old `ConsultationViewModel` chat flow (`initChat`, `endConsultation`, 60s timer).

### Added
- **API (`OmAstroApiService`)**: corrected `chat/open` (`{slug}`), `chat/history` (`?session_id=&astrologer_slug=`), `chat/close` (`{session_id}`); typed billing session models (`AstroSessionStartRequest`, `AstroSessionTickRequest`, `AstroSessionState/Response`); new `POST /astrologers/review`.
- **Models**: `AstroSessionState` (session_uuid, rate_paise, billed_seconds, free_seconds_used/left, wallet_total_paise, wallet_covers_sec, status, insufficient flag, resumed); `AstrologerReviewRequest`; `ChatHistoryResponse/Data`; `OpenChatData`.
- **Repository**: `openChat` returns server-minted UUID; `loadChatHistory` restores messages; `startBillingSession`/`tickBillingSession` return full state (callers check `insufficient`); `submitReview`.
- **ChatRepository**: `sendAiMessage` gains `billingMode`, `astroSession`, `ctxLabel` params.
- **ViewModel rewrite**: auto-match (online ₹5/min preferred), rate consent gate, birth attach gate, greeting with delays, humanizing per-sentence delivery with generation-counter abort, read ticks, 30s billing tick, 1s display timer, idle nudge/close, Add Money flow with retry-after-topup, review submission.
- **ChatScreen rewrite**: WhatsApp-style fullscreen chat; header with photo/name/online-or-typing status + FREE TRIAL badge; floating MM:SS timer pill; per-sentence bubbles; animated 3-dot typing; center system bubbles; read ticks; rate consent dialog; birth attach dialog (name/gender/DOB picker/TOB AM-PM picker/place search); Add Money popup; 5-star review card.
- **AstrologersScreen**: "Chat Now" auto-match card with "Finding best astrologers…" state; rate consent dialog.
- **Navigation**: chat route now carries the astrologer `slug` (`chat/{slug}`); dashboard + list pass `astro.slug`.

---

## [2026-10-09] — AI chat full context system (website parity)

### Broken / missing
- **No birth-data context**: AI chat sent only `message` + `persona` + `session_id`. No way to attach birth data, so the AI had no chart context (website's "Attach Kundali" feature was missing).
- **No context replay**: Server-returned `chatContext` was discarded; every turn started fresh instead of building on the computed chart.
- **No history sent**: Previous turns were not included in requests (website sends last 12).
- **No quota display**: `questions_used/limit/left` from responses was ignored.
- **No session persistence**: No save/load/delete of chat sessions via `/ai/chats`.
- **No model selection**: `ai_model` (lite/pro/expert) was never sent.

### Removed
- Nothing (existing chat UI preserved; new features are additive).

### Added
- **`AiChatModels.kt`** (new): `BirthDataAttachment` (name/gender/dob/tob/place/lat/lon/tz), `buildBirthDataContext()`, `AiChatQuota`, `AutoKundali`, `AiChatFullResponse`, `AiModel` enum (lite/pro/expert), `ChatHistoryTurn`, `SavedChatSession`, `SaveChatRequest`, session list/detail response models.
- **API**: `GET /ai/chats`, `POST /ai/chats`, `GET /ai/chats/{id}`, `DELETE /ai/chats/{id}`, `DELETE /ai/chats` in `OmAstroApiService`.
- **`ChatRepository`**: Full-context `sendAiMessage()` sends `message`, `ai_model`, `persona`, `language`, `context` (max 28k chars), `history` (last 12, 4k chars each), `session_id`. Parses `reply`, `chatContext`, `autoKundali`, `session_id`, quota from responses. Session CRUD methods.
- **`AiChatViewModel`**: Per-mode state (messages, chatContext, sessionId, attachments, quota, aiModel). Context built from attachments + replayed chatContext. Auto-registers server-extracted birth data. Attach/remove/clear birth data. Save/load/delete sessions with context restoration.
- **`AttachBirthDataDialog.kt`** (new): Form with Name, Gender toggle, DOB calendar picker (DD-MM-YYYY), TOB clock picker (AM/PM), Place autocomplete. Multiple attachments shown as removable chips.
- **`SavedSessionsDialog.kt`** (new): Saved chat list with load (restores messages + context) and delete.
- **`AiChatScreen`**: Attach button in input bar, attachment chips, quota pill in top bar, Lite/Pro/Expert model selector, saved-chats and save buttons, quota text display.

---

## [2026-10-09] — Release signing with Flutter app's official keystore

### Broken / missing
- **Release builds were unsigned**: The `release` build type had no `signingConfig`, so release APKs could not be published to the Play Store or share the Flutter app's identity. The Flutter app was built with an official keystore on the server (`omastro-release.keystore`).

### Removed
- Nothing (debug signing unchanged — still uses the committed stable `debug.keystore` for Appetize/CI debug builds).

### Added
- **`app/build.gradle.kts`**: New `signingConfigs.release` block pointing at `keystores/omastro-release.keystore` (store password `android`, key alias `omastro`, key password `android`). The `release` build type now uses this signing config. SHA-1: `9F:39:0C:5B:B2:3D:B8:8A:3C:7E:74:BA:67:6F:B7:53:47:81:36:E9`.
- **`.github/workflows/android-build.yml`**: CI now also runs `./gradlew assembleRelease` and uploads the signed release APK as `OmAstroApp-Release-<run_number>` (retention 14 days), alongside the existing debug APK artifact.
- **Google Sign-In note**: Package `com.omastroai.app` must be registered as an Android OAuth client with the release SHA-1 (`9F:39:0C:5B:B2:3D:B8:8A:3C:7E:74:BA:67:6F:B7:53:47:81:36:E9`) in the same Google Cloud project as the web client ID for release builds. Debug builds continue to use the debug SHA-1 (`27:37:38:7D:8C:2C:17:03:97:34:FA:0B:DA:6C:4F:BB:30:1C:7B:A1`).

---

## [2026-10-09] — FIX: Wallet balance (was 0) + Numerology welcome message

### Broken / missing
- **Wallet balance always 0**: `WalletRepository.coinBalance` was a `MutableStateFlow(0)` never updated from any API. No `/wallet/balance` endpoint existed on the server. Dashboard, Settings, and Wallet screens all displayed ₹0.00 regardless of actual balance.
- **Numerology hub header mismatched website**: App showed "Vedic · Pythagorean · Chaldean / Complete Number Science / Twelve tools..." — the website's numerology section uses a simple "Numerology" header.

### Removed
- Hardcoded 0 wallet balance (replaced with live API data).
- "Coins" terminology from WalletScreen balance display (now shows ₹ INR).
- Overly elaborate numerology hub header text.

### Added
- **Server**: New `GET /omastro/v1/wallet/balance` endpoint (`kg_rest_wallet_balance` in rest-api.php) returning `paid_paise`, `bonus_paise`, `total_paise` and INR equivalents from `kg_get_wallet_balance()`.
- **Android API**: `OmAstroApiService.getWalletBalance()`, `WalletBalanceData` and `WalletBalanceResponse` models.
- **Android repo**: `WalletRepository.refreshBalance()` fetches live balance into `walletBalancePaise` / `walletBalanceInr` StateFlows.
- **UI**: WalletScreen, DashboardScreen, and SettingsScreen now display the real INR balance (₹ formatted to 2 decimals). `HomeViewModel.loadData()` refreshes balance on startup; `WalletViewModel` refreshes on init.
- **Numerology**: Hub header simplified to "Numerology" + "Discover what your numbers reveal about your life path, personality and destiny." matching the website's tone.

---

## [2026-10-09] — FIX: Kundali input — place search, date/time pickers

### Broken / missing
- **Birth place search not working**: `OmAstroApiService.searchPlaces()` used `@POST("geocode/search")` with a JSON body, but the backend only exposes `GET /geocode/search?q=`. Every search returned 404, so the dropdown never showed results.
- **Geocode response parsing wrong**: models expected flat `places`/`results` keys, but the real API returns `{"success":true,"data":{"results":[...]}}` with per-place `display` and `tz` fields.
- **No date picker**: date fields were plain text inputs expecting YYYY-MM-DD typed manually.
- **No time picker**: time fields were plain text inputs expecting 24-hour HH:MM typed manually.
- **Kundali time validation rejected AM/PM**: `TOB_REGEX` only accepted 24-hour format.

### Removed
- Manual YYYY-MM-DD / HH:MM text entry for birth date/time across Kundali, Matchmaking, Astro Health, Gochar.
- `GeocodeSearchRequest` (POST body) — replaced by GET query param.
- Old flat geocode response parsing.

### Added
- **Working place search**: `searchPlaces` is now `@GET("geocode/search")` with `@Query("q")`; `GeocodeSearchResponse` unwraps `data.results`; `GeocodePlace` gains `display` and `tz` fields; `PlaceSearchField` calls the GET endpoint. Verified live against production.
- **New `DateTimePickerFields.kt`**: reusable `DatePickerField` (native Calendar picker, displays DD-MM-YYYY, reports YYYY-MM-DD to the API) and `TimePickerField` (native Clock picker in 12-hour mode with AM/PM, displays and reports "HH:MM AM/PM" which the backend accepts).
- **Applied to all birth-input screens**: Kundali, Matchmaking (groom + bride), Astro Health, Gochar (birth + transit date).
- **AM/PM validation**: `KundaliViewModel.TOB_REGEX` now accepts both 24-hour and 12-hour AM/PM formats.

---

## [2026-10-09] — FIX: Astrologers blank + Divine Chat separated

### Broken / missing
- **Astrologers screen was blank**: Two compounding bugs. (1) `OmAstroApiService.getAstrologers()` declared `Response<List<Astrologer>>` but the real API returns an envelope `{"success":true,"data":{"astrologers":[...]}}` — Retrofit/Gson threw on parsing so the list never populated. (2) Even past the envelope, every field name was wrong: `title` vs real (none — uses `specialisations`), `speciality` vs `specialisations`, `experience_years` vs `experience`, `price_per_min` vs `rate_paise`/`rate_label`, `total_consultations` vs `reviews`, `is_online` vs `online`, `avatar_url` vs `img`. Verified live: `curl /wp-json/omastro/v1/astrologers` returns the envelope with fields id/name/slug/img/rating/reviews/experience/specialisations/languages/about/online/rate_paise/rate_label/gender.
- **Divine Chat was buried inside AI Chat**: the website has three separate sections (AI Chat, Divine Chat, Astrologers) but the app only exposed AI Chat with an in-screen mode switcher. No dedicated Divine entry point existed.

### Removed
- Old `Astrologer` fields (`title`, `speciality`, `experience_years`, `price_per_min`, `total_consultations`, `is_online`, `avatar_url`) replaced with real API fields.

### Added
- **Real astrologer model**: `Astrologer` now matches the production API exactly; new `AstrologersResponse`/`AstrologersData` envelope types; `displayRate` derives "₹5/min" from server `rate_label` with paise fallback.
- **Repository unwraps the envelope**: `ConsultationRepository.getAstrologers()` reads `body.data.astrologers` and surfaces the server's `message` on failure.
- **Photos**: astrologer cards (Astrologers screen + Dashboard carousel) now load the real `img` photo via Coil, with person-icon fallback; online dot uses real `online` flag.
- **Divine Chat section**: new `Screen.DivineChat` route + `DivineChatScreen` (locked to the Divine persona, mode switcher hidden); `AiChatScreen` gained an optional `lockedMode` parameter; drawer menu now lists "AI Chat", "Divine Chat", and "Astrologers" as three separate entries, matching the website.

---

## [2026-10-09] — FIX: Google Sign-In error 10 (DEVELOPER_ERROR) — stable debug keystore + setup docs

### Broken / missing
- **Google Sign-In failed with error code 10 (DEVELOPER_ERROR)**: tapping "Sign in with Google" showed the account picker, then failed. Root cause: error 10 means Google does not recognise the app's signing certificate — no Android OAuth client was registered for this app's package name + SHA-1 in the Google Cloud project that owns the web client ID. Worse, CI runners generate a fresh random `~/.android/debug.keystore` on every run, so the APK's SHA-1 changed on every build — registering a fingerprint in Cloud Console could never work for CI/Appetize builds.
- **Error 10 showed a generic message**: users saw only "Google sign-in failed (code 10)" with no explanation of what to do.

### Removed
- Nothing removed. The `requestIdToken(GOOGLE_SERVER_CLIENT_ID)` call was already correct (web client ID `613285624759-p3bksd1ersn333f4mqirv64are5uglsb.apps.googleusercontent.com`); the failure was purely the missing Android-side registration.

### Added
- **Stable debug keystore committed** (`app/keystores/debug.keystore`, PKCS12, alias `androiddebugkey`, password `android`): debug keystores are not secrets — this mirrors the conventional `~/.android/debug.keystore`. `app/build.gradle.kts` now wires it into `signingConfigs.debug`, so EVERY debug APK (local or CI) is signed with the same key and the same SHA-1 forever:
  `27:37:38:7D:8C:2C:17:03:97:34:FA:0B:DA:6C:4F:BB:30:1C:7B:A1`
- **One-time Google Cloud Console setup documented** in `GoogleAuthConfig` KDoc (exact clicks): create an Android OAuth client in the project owning the web client ID, package name `com.omastroai.app` (the `applicationId` — not the `namespace`), SHA-1 as above. No app-code change needed afterwards.
- **Friendly error-10 message** in `GoogleSignInHelper.parseResult`: explains the SHA-1 registration requirement instead of a bare code.
- `GoogleAuthConfig.DEBUG_SHA1_FINGERPRINT` and `APP_PACKAGE_NAME` constants so docs and reality cannot drift apart.

---

## [2026-10-09] — FIX: Login envelope mismatch (login buffered forever) + official Google G logo

### Broken / missing
- **Login never completed (buffered forever)**: `LoginResponse` expected `token` at the JSON top level, but the real API returns `{"success": true, "data": {"token": "...", ...}}`. `AuthRepository.login()` checked `body.token != null` which was always null → login silently failed and the UI stayed in loading state. Same latent bug in `VerifySignupOtpResponse`.
- **API error messages were swallowed**: failures showed generic "Login failed: HTTP 200" instead of the server's `message` field.
- **Google "G" logo was a rough approximation**: hand-drawn arcs that did not resemble the official multicolor Google G (user compared with Flutter and rejected it).

### Removed
- Old flat `LoginResponse(token, user, ...)` and `VerifySignupOtpResponse(token, user, ...)` shapes.
- Old arc-based `GoogleGLogo` Canvas drawing.

### Added
- **Correct auth envelopes** (`AuthModels.kt`):
  - `LoginResponse(success, data: LoginData?, message, code)` and `VerifySignupOtpResponse(success, data: LoginData?, message, code)`.
  - New `LoginData(token, expires, userId, displayName, username, email, mobile, avatar)` matching the real `/auth/login` and `/auth/verify-signup-otp` `data` payloads.
- **Repository + ViewModel now read `data.token`** (`AuthRepository.login/verifySignupOtp`, `AuthViewModel.login/verifySignupOtp`); user profile is built from `LoginData` (name/email/mobile).
- **Server error messages surfaced**: repository failures now prefer `body.message` (e.g. wrong password / expired OTP) over the generic HTTP-code text; ViewModel shows the server message when no token is returned.
- **Official Google G logo**: `GoogleGLogo` now draws Google's exact brand SVG paths (24x24 viewBox) with the four official colors — blue #4285F4, green #34A853, yellow #FBBC05, red #EA4335 — scaled via `withTransform` to the composable size.

---

## [2026-10-09] — FIX: Google Sign-In broken + Auth UI matched to Flutter exactly

### Broken / missing
- **Google Sign-In was broken at runtime**: tapping "Sign in with Google" showed the inline error "getCredentialAsync no provider dependencies found - please ensure the desired provider dependencies are added". Root cause: Credential Manager's Google ID provider (`androidx.credentials` + `googleid`) requires a Play Services build that ships the Credential Manager provider; devices/emulators without it (including the Appetize test device) throw this error even though the Gradle dependencies were present.
- **Auth UI did not match the Flutter app** (user sent the Flutter screenshot): ours had the ॐ maroon logo, "Email"-only field, no Remember Password checkbox, no Terms checkbox on login, "or continue with email" divider, and the Google button above the login button. Flutter has: sparkle icon in a light-orange square, "OmAstro AI" in orange, "Sign In"/"Create Account" tabs, "Email or Mobile Number" field, Remember Password + Forgot Password row, Terms & Conditions checkbox, "Log In" button, "OR" divider, Google button after the login button, "Don't have an account? Create Account" footer.

### Removed
- Credential Manager Google Sign-In stack: `GoogleSignInHelper.signIn()` (suspend, Credential Manager based), `GetGoogleIdOption` usage, and the three Gradle artifacts `androidx.credentials:credentials`, `androidx.credentials:credentials-play-services-auth`, `com.google.android.libraries.identity.googleid:googleid`.
- Old auth styling: ॐ maroon-square brand header, cream page background, outlined white input fields, orange-pill tabs ("Sign Up"), "or continue with email" divider text, per-form "Welcome Back"/"Create Account" titles, "Secure Vedic astrology guidance" tagline.

### Added
- **Working Google Sign-In** via classic `play-services-auth` (`com.google.android.gms:play-services-auth:21.2.0`):
  - `GoogleSignInHelper.getClient(context)` builds `GoogleSignInOptions` with `requestIdToken(GOOGLE_SERVER_CLIENT_ID)` + `requestEmail`; `GoogleSignInHelper.parseResult(intent)` extracts the ID token from the account-picker result (never throws; maps status 12501 to Cancelled).
  - `AuthScreen` registers a `rememberLauncherForActivityResult(StartActivityForResult())`; on tap it calls `client.signOut()` first (so the account chooser always appears) then launches `client.signInIntent`. Success → `AuthViewModel.signInWithGoogle(idToken)` → POST /auth/firebase as before.
- **Flutter-exact auth UI** (`AuthScreen.kt`):
  - Light-gray page (#F4F5F7), white 24dp card with soft shadow; sparkle (`AutoAwesome`) icon in a light-orange rounded square, "OmAstro AI" in Flutter orange (#FF6B00), per-mode subtitle ("Welcome Back! Please log in to continue." etc.).
  - Tabs "Sign In" / "Create Account" on a light-gray pill; active tab is a white elevated pill with orange text.
  - Filled (borderless) light-gray input fields with orange leading icons: "Email or Mobile Number" (person icon), "Password" (lock icon + eye toggle); signup adds Full Name, Email, Mobile (+91), Password + strength meter, Confirm Password.
  - Login: "Remember Password" orange checkbox (checked by default) + "Forgot Password?" orange link on the right; "I agree to the Terms & Conditions and Privacy Policy" orange checkbox with orange underlined links opening real Terms/Privacy dialogs.
  - Orange "Log In" / "Create Account" buttons, "OR" divider, white Google button with the Canvas-drawn G mark placed AFTER the primary button, footer links ("Don't have an account? Create Account" / "Already have an account? Sign In").
- **Backend wiring for the new fields**:
  - `LoginRequest` gains `remember: Boolean` (server issues a 90-day token when true, 60-day otherwise — verified live against production `kg_rest_auth_login`).
  - `AuthViewModel`: `rememberPassword` (default true) and `termsAccepted` (default true) state; `login()` now accepts a valid email OR a 10-digit mobile number (backend resolves either via its `email`/`identifier`/`mobile` params) and requires the terms checkbox.
  - `AuthRepository.login()` and `LoginUseCase` accept the `remember` flag (default false, existing callers unaffected).

### Verification note
- Brace/paren balance verified on all touched files; no references remain to the removed `Light*` palette names or Credential Manager APIs. Awaiting CI build.

---

## [2026-10-09] — NEW: Sign in with Google + Flutter-style Light Auth Theme

### Missing
The auth screen had no "Sign in with Google" button even though the backend supports it (POST /auth/firebase accepts Google ID tokens and the web login uses Google). The auth UI was dark cosmic-gold while the user asked for the Flutter app's light theme with the logo icon on top.

### Removed
- Dark cosmic-gold auth styling (gradient background, dark cards, gold-on-dark text) — replaced by the light theme below. The rest of the app stays dark; only auth screens changed.

### Added
- **Sign in with Google** (end to end):
  - `POST /auth/firebase` endpoint in `OmAstroApiService` (posts `firebase_id_token`; envelope `{"success":true,"data":{"token":...}}`)
  - `FirebaseAuthRequest` / `FirebaseAuthData` / `FirebaseAuthEnvelope` models in `AuthModels.kt`
  - `AuthRepository.signInWithGoogle(idToken)`: posts the token, persists the OmAstro session token, updates the user profile
  - `AuthViewModel.signInWithGoogle(idToken)` + `setGoogleError(message)` for inline failures
  - `GoogleSignInHelper` (new): Credential Manager + `GetGoogleIdOption` account picker; `GoogleAuthConfig.GOOGLE_SERVER_CLIENT_ID` documents the server client ID and how to swap it if the backend ever reports "Wrong audience"
  - White "Sign in with Google" button on the login form with a Canvas-drawn Google "G" mark (brand icon, not an emoji); "or continue with email" divider below it; silent on user-cancel, inline error otherwise
  - New Gradle dependencies: `androidx.credentials:credentials`, `credentials-play-services-auth`, `googleid`
- **Flutter-style light theme** (`AuthScreen.kt` rewritten):
  - Cream page background (#FAF6EC, website day mode), white cards with warm borders
  - Brand header: gold ॐ in a maroon rounded square (like the website header) + "OmAstro AI" title
  - White input fields with gold focus rings, orange (#E8590C) primary buttons
  - All four modes kept: email/password login, signup (name/email/mobile/+91/password + strength meter), 6-box OTP verify, forgot password; `otp_channel` backend option untouched

### Verification note
Brace/paren balance checked on all touched files. Full compile verification via CI build on push.

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
