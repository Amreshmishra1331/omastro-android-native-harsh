# OmAstro AI — 100% Native Android Application

**OmAstro AI** is an enterprise-grade, high-performance Vedic Astrology mobile application built purely with **Kotlin** and **Jetpack Compose** (Material 3).

---

## 🌟 Architectural Highlights

- **Pure Native Kotlin**: 0% hybrid or cross-platform overhead. 100% standard Android toolchain.
- **Modern Jetpack Compose UI**: Declarative UI powered by Material 3 with a bespoke Cosmic Gold & Midnight Blue Vedic theme (`CosmicDeepBlue`, `SacredGold`, `RadiantAmber`).
- **Clean MVVM Architecture**: Strict separation of concerns across Data, Domain, and Presentation layers.
- **Zero App-Code Authentication Dynamic Toggling**:
  - The application queries `GET /wp-json/omastro/v1/auth/config` at runtime.
  - Dynamically switches its input forms between **Email OTP**, **Mobile SMS OTP**, and **WhatsApp OTP** strictly according to the WordPress backend master setting.
  - No client rebuilds or updates needed when the server admin toggles OTP methods.
- **Native Canvas Vedic Kundali Engine**:
  - North Indian Diamond Chart renderer built directly on Jetpack Compose `Canvas`.
  - Calculates and renders all 12 Bhavas, Rashi sign placements, and Grahas with high DPI precision.
- **Google Play In-App Review**:
  - Built-in Play Core Review Manager (`com.google.android.play:review-ktx`) allowing users to rate the app without leaving the experience.
- **Firebase Cloud Messaging**:
  - Native Push Notifications for Daily Rashifal, Panchang muhurats, and astrologer chat sessions.

---

## 📱 Modules & Features

1. **Cosmic Dashboard**: Daily Panchang banner, quick Vedic services grid, daily Mahamantra, and verified live astrologers.
2. **Janam Kundali (Birth Chart)**:
   - Lagna Chart (D1)
   - Navamsha Chart (D9)
   - Comprehensive Planetary Dignities & Longitudes table (Degree, Nakshatra, Pada).
3. **Daily Rashifal (Horoscope)**:
   - All 12 Rashis (Aries to Pisces / Mesh to Meen) with periods (Today, Tomorrow, Weekly).
   - Energy breakdown for Love, Career, and Health.
   - Lucky Number, Lucky Color, and personalized Vedic Remedy (Upay).
4. **Panchang & Shubh Muhurat**:
   - Sunrise, Sunset, Moonrise calculations.
   - Five limbs: Tithi, Nakshatra, Yoga, Karana, Vaar.
   - Auspicious (Abhijit, Amrit Kaal) and inauspicious (Rahu Kaal, Yamaganda, Gulika) time windows.
5. **Kundali Milan (Matchmaking)**:
   - Full 36 Guna Ashtakoota compatibility system (Varna, Vashya, Tara, Yoni, Graha Maitri, Gana, Bhakoot, Nadi).
   - Manglik Dosha detector for bride and groom.
   - Astrological verdict & synthesis.
6. **Live Astrologer Consultation & Chat**:
   - Live availability status, expertise filter, pricing per minute, and ratings.
   - Real-time Vedic consultation messaging thread.
7. **Cosmic Coins & Wallet**:
   - Real-time balance and transaction history.
   - Razorpay payment order integration with discount bonus packs.
8. **Settings & Preferences**:
   - In-app Google Play review trigger.
   - Granular push notification and WhatsApp automation toggles.

---

## 🛠️ Tech Stack & Dependencies

- **Language**: Kotlin 1.9.22
- **UI Toolkit**: Jetpack Compose (BOM 2024.02.01), Material 3
- **Networking**: Retrofit 2.9.0 + OkHttp 4.12.0 + Gson
- **Coroutines & Flow**: StateFlow, SharedFlow, Lifecycle ViewModel Compose
- **In-App Review**: `com.google.android.play:review-ktx:2.0.1`
- **Notifications**: `com.google.firebase:firebase-messaging-ktx:23.4.1`
- **Image Loading**: Coil Compose 2.5.0
- **Build System**: Gradle Kotlin DSL (`build.gradle.kts`)
- **Min SDK**: 24 (Android 7.0+)
- **Target / Compile SDK**: 34 (Android 14)

---

## 🚀 Build & Run

Open the project directory in **Android Studio Hedgehog / Iguana / Jellyfish** or compile via terminal:

```bash
# Debug APK
./gradlew assembleDebug

# Release Bundle (AAB)
./gradlew bundleRelease
```

### Firebase configuration (google-services.json)

`app/google-services.json` is intentionally **not committed** to this repository (it is listed in
`.gitignore`). CI injects the file at build time from a protected secret before running Gradle:

```bash
# CI step (example)
echo "$GOOGLE_SERVICES_JSON_BASE64" | base64 -d > app/google-services.json
./gradlew bundleRelease
```

For local development, place your own `google-services.json` from the Firebase console into
`app/` — it will never be committed.

---

## 🔗 WordPress Backend Integration

This app connects directly to the OmAstro AI WordPress backend engine:
- Domain: `https://www.omastroai.com`
- REST Namespace: `/wp-json/omastro/v1/`
- Documentation: Refer to `AI_CHANGELOG/` in the WordPress plugin repository for full endpoint specifications.
