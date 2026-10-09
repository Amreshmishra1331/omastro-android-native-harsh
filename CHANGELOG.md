# Changelog — OmAstro Android (Native Kotlin)

All notable changes to the native Android app. Each entry lists what was broken or missing, what was removed, and what was added.

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
