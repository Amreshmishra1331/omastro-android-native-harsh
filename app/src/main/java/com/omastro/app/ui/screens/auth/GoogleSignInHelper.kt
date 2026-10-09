package com.omastro.app.ui.screens.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

/**
 * Google OAuth Web client ID used as the server client ID for
 * [GoogleSignInOptions.requestIdToken].
 *
 * The returned ID token's `aud` claim will be this client ID, and the
 * production backend (POST /auth/firebase) accepts it — it is in the
 * server's allow-list of Google OAuth client IDs.
 *
 * If Google Sign-In ever fails server-side with "Wrong audience", replace
 * this with the Web-application OAuth client ID from Google Cloud Console
 * (APIs & Services → Credentials) for the OmAstro project.
 *
 * ---------------------------------------------------------------------------
 * FIXING GOOGLE SIGN-IN ERROR 10 (DEVELOPER_ERROR) — SETUP STEPS
 * ---------------------------------------------------------------------------
 * Error 10 means Google does not recognise this app's signing certificate.
 * `requestIdToken()` needs a **Web** client ID (the one above — correct), but
 * Google Play Services ALSO requires an **Android** OAuth client registered
 * for this app's package name + SHA-1 fingerprint in the SAME Cloud project.
 *
 * One-time setup (do this in Google Cloud Console):
 *  1. Open https://console.cloud.google.com/ and select the project that owns
 *     the web client ID above (project number 613285624759).
 *  2. Go to "APIs & Services" → "Credentials".
 *  3. Click "+ CREATE CREDENTIALS" → "OAuth client ID".
 *  4. Application type: "Android".
 *  5. Name: "OmAstro Android" (any label).
 *  6. Package name: com.omastroai.app
 *     (this is the `applicationId` in app/build.gradle.kts — NOT the
 *     `namespace`, which is com.omastro.app and is irrelevant here).
 *  7. SHA-1 certificate fingerprint:
 *     27:37:38:7D:8C:2C:17:03:97:34:FA:0B:DA:6C:4F:BB:30:1C:7B:A1
 *     (this is the stable debug keystore committed at
 *     app/keystores/debug.keystore — every debug APK, local or CI-built,
 *     is signed with it, so the fingerprint never changes).
 *  8. Click "Create". No app-code change is needed after this — the existing
 *     `requestIdToken(WEB_CLIENT_ID)` call starts working once Google's
 *     servers propagate the new client (usually within minutes).
 *
 * For a Play Store release build later: generate a release keystore, add a
 * SECOND Android OAuth client with the release SHA-1 (same package name),
 * and keep both — debug and release fingerprints can coexist.
 */
object GoogleAuthConfig {
    const val GOOGLE_SERVER_CLIENT_ID =
        "613285624759-p3bksd1ersn333f4mqirv64are5uglsb.apps.googleusercontent.com"

    /**
     * SHA-1 of app/keystores/debug.keystore — the fingerprint to register in
     * Google Cloud Console (see KDoc above). Kept here so the setup docs and
     * the actual key can never drift apart silently.
     */
    const val DEBUG_SHA1_FINGERPRINT =
        "27:37:38:7D:8C:2C:17:03:97:34:FA:0B:DA:6C:4F:BB:30:1C:7B:A1"

    /** Package name Google sees (`applicationId`, not `namespace`). */
    const val APP_PACKAGE_NAME = "com.omastroai.app"
}

/**
 * "Sign in with Google" via the classic play-services-auth [GoogleSignInClient].
 *
 * Why not Credential Manager: its Google ID provider throws
 * "getCredentialAsync no provider dependencies found" on devices/emulators
 * whose Google Play Services build lacks the Credential Manager provider.
 * The classic [GoogleSignInClient] account-picker flow works on every
 * Play-Services device and returns the same Google ID token, which the
 * caller posts to POST /auth/firebase (see
 * [com.omastro.app.data.repository.AuthRepository.signInWithGoogle]).
 *
 * Usage (Compose):
 * ```
 * val launcher = rememberLauncherForActivityResult(
 *     ActivityResultContracts.StartActivityForResult()
 * ) { result ->
 *     when (val r = GoogleSignInHelper.parseResult(result.data)) {
 *         is GoogleSignInHelper.Result.Success -> viewModel.signInWithGoogle(r.idToken)
 *         is GoogleSignInHelper.Result.Cancelled -> { /* silent */ }
 *         is GoogleSignInHelper.Result.Error -> viewModel.setGoogleError(r.message)
 *     }
 * }
 * // onClick: sign out first so the account picker always shows, then launch.
 * val client = GoogleSignInHelper.getClient(context)
 * client.signOut().addOnCompleteListener { launcher.launch(client.signInIntent) }
 * ```
 */
object GoogleSignInHelper {

    sealed interface Result {
        data class Success(val idToken: String) : Result
        data class Cancelled(val message: String = "Sign-in was cancelled.") : Result
        data class Error(val message: String) : Result
    }

    fun getClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(GoogleAuthConfig.GOOGLE_SERVER_CLIENT_ID)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    /**
     * Parses the result [Intent] from the Google sign-in activity into a
     * [Result]. Never throws.
     */
    fun parseResult(data: Intent?): Result {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (!idToken.isNullOrBlank()) {
                Result.Success(idToken)
            } else {
                Result.Error("Google did not return a sign-in token. Please try again.")
            }
        } catch (e: ApiException) {
            // 12501 = SIGN_IN_CANCELLED (user dismissed the picker)
            if (e.statusCode == 12501) {
                Result.Cancelled()
            } else if (e.statusCode == 10) {
                // 10 = DEVELOPER_ERROR: the app's package name + SHA-1 is not
                // registered as an Android OAuth client in Google Cloud Console.
                // See GoogleAuthConfig KDoc for the exact one-time setup steps.
                Result.Error(
                    "Google sign-in is not set up for this app yet (error 10). " +
                    "The app's SHA-1 fingerprint must be registered in Google " +
                    "Cloud Console — see setup steps in the app's documentation."
                )
            } else {
                Result.Error("Google sign-in failed (code ${e.statusCode}). Please try again.")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Google sign-in failed. Please try again.")
        }
    }
}
