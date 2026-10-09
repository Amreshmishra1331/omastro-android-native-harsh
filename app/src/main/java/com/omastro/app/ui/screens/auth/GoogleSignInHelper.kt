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
 */
object GoogleAuthConfig {
    const val GOOGLE_SERVER_CLIENT_ID =
        "613285624759-p3bksd1ersn333f4mqirv64are5uglsb.apps.googleusercontent.com"
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
            } else {
                Result.Error("Google sign-in failed (code ${e.statusCode}). Please try again.")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Google sign-in failed. Please try again.")
        }
    }
}
