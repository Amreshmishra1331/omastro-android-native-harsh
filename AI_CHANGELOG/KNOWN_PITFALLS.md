# Known Pitfalls & Architecture Lessons — OmAstro Android Native

1. **SerializedName vs Parameter Names**:
   - `MatchmakingResponse` defines `@SerializedName("manglik_boy") val manglikBoy: String`.
   - Never use `manglikStatusBoy` when instantiating `MatchmakingResponse` in repositories.

2. **Zero App-Code Authentication**:
   - Do not hardcode OTP dispatch mechanisms in Jetpack Compose UI.
   - Always rely on `GetAuthConfigUseCase` (`/auth/config`) to dynamically switch between Email, SMS, and WhatsApp inputs.

3. **Google Play In-App Review Testing**:
   - `ReviewManager.launchReviewFlow` can only show the bottom sheet when downloaded via internal testing tracks on Google Play Console with a valid Google Account.
   - Local debug builds safely resume coroutine with `false` or fallback gracefully.
