package com.omastro.app.ui.screens.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

/**
 * Google OAuth Web client ID used as the server client ID for Credential
 * Manager's [GetGoogleIdOption].
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
 * Thin wrapper around Credential Manager for "Sign in with Google".
 *
 * Flow: [signIn] shows the Google account picker → returns the Google ID
 * token → caller posts it to POST /auth/firebase (see
 * [com.omastro.app.data.repository.AuthRepository.signInWithGoogle]).
 */
object GoogleSignInHelper {

    sealed interface Result {
        data class Success(val idToken: String) : Result
        data class Cancelled(val message: String = "Sign-in was cancelled.") : Result
        data class Error(val message: String) : Result
    }

    suspend fun signIn(context: Context): Result {
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(GoogleAuthConfig.GOOGLE_SERVER_CLIENT_ID)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val credentialManager = CredentialManager.create(context)
            val response = credentialManager.getCredential(context, request)
            val credential = response.credential

            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                try {
                    val googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(credential.data)
                    Result.Success(googleIdTokenCredential.idToken)
                } catch (e: GoogleIdTokenParsingException) {
                    Result.Error("Could not read the Google credential. Please try again.")
                }
            } else {
                Result.Error("Unexpected credential type. Please try again.")
            }
        } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
            Result.Cancelled()
        } catch (e: androidx.credentials.exceptions.NoCredentialException) {
            Result.Error("No Google accounts found on this device. Add a Google account in Settings and try again.")
        } catch (e: Exception) {
            Result.Error(e.message ?: "Google sign-in failed. Please try again.")
        }
    }
}
