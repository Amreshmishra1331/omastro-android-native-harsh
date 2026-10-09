package com.omastro.app.data.remote

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.net.Uri
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.omastro.app.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Encrypted persistent store for the auth token.
 *
 * Uses EncryptedSharedPreferences (AES256-SIV for keys, AES256-GCM for values)
 * backed by the Android Keystore via MasterKey. The session survives process
 * death and app restarts until the user explicitly logs out.
 *
 * REQUIRED GRADLE DEPENDENCY (add to app/build.gradle.kts):
 *   implementation("androidx.security:security-crypto:1.1.0-alpha06")
 */
class SecureTokenStore private constructor(context: Context) {

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context.applicationContext,
        PREFS_NAME,
        MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /** Persist the auth token after a successful login / signup. */
    fun saveToken(token: String) {
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
    }

    /** Return the persisted token, or null when logged out. */
    fun getToken(): String? = prefs.getString(KEY_AUTH_TOKEN, null)

    /** True when a token is persisted. */
    fun hasToken(): Boolean = getToken() != null

    /** Remove the token on logout. */
    fun clearToken() {
        prefs.edit().remove(KEY_AUTH_TOKEN).apply()
    }

    companion object {
        private const val PREFS_NAME = "omastro_secure_prefs"
        private const val KEY_AUTH_TOKEN = "auth_token"

        @Volatile
        private var instance: SecureTokenStore? = null

        /** Initialise once at application start (called by SecureStoreInitProvider). */
        fun init(context: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = SecureTokenStore(context)
                    }
                }
            }
        }

        fun get(): SecureTokenStore =
            instance ?: throw IllegalStateException(
                "SecureTokenStore not initialised. Ensure SecureStoreInitProvider is registered in AndroidManifest.xml."
            )
    }
}

/**
 * Auto-initialisation provider.
 *
 * Runs before Application.onCreate: initialises the encrypted token store and
 * restores any persisted session into ApiClient so the Authorization header is
 * attached from the very first request after a cold start.
 *
 * Register in AndroidManifest.xml:
 * <provider
 *     android:name=".data.remote.SecureStoreInitProvider"
 *     android:authorities="${applicationId}.securestoreinit"
 *     android:exported="false" />
 */
class SecureStoreInitProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        val appContext = context?.applicationContext
        if (appContext != null) {
            SecureTokenStore.init(appContext)
            ApiClient.restorePersistedToken()
        }
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0
}

object ApiClient {
    private const val BASE_URL = "https://www.omastroai.com/wp-json/omastro/v1/"

    /** In-memory copy of the token; always mirrored to encrypted storage. */
    private var userAuthToken: String? = null

    /**
     * Set (or clear) the auth token. Persists to encrypted storage so the
     * session survives process death. Pass null on logout.
     */
    fun setAuthToken(token: String?) {
        userAuthToken = token
        try {
            if (token.isNullOrBlank()) {
                SecureTokenStore.get().clearToken()
            } else {
                SecureTokenStore.get().saveToken(token)
            }
        } catch (e: IllegalStateException) {
            // Store not initialised yet (e.g. unit tests) — keep in-memory only.
        }
    }

    /** Load the persisted token into memory. Called once at app start. */
    fun restorePersistedToken() {
        try {
            userAuthToken = SecureTokenStore.get().getToken()
        } catch (e: IllegalStateException) {
            userAuthToken = null
        }
    }

    /** True when a session token is available (memory or persisted). */
    fun isLoggedIn(): Boolean {
        if (!userAuthToken.isNullOrBlank()) return true
        return try {
            SecureTokenStore.get().hasToken()
        } catch (e: IllegalStateException) {
            false
        }
    }

    private val authInterceptor = Interceptor { chain ->
        // Lazily restore the persisted token on first use (cold-start safety).
        if (userAuthToken.isNullOrBlank()) {
            restorePersistedToken()
        }
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")

        userAuthToken?.let { token ->
            requestBuilder.header("Authorization", "Bearer $token")
        }

        chain.proceed(requestBuilder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // AND-21: never log request/response bodies (which contain bearer tokens
        // and credentials) in release builds.
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val apiService: OmAstroApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OmAstroApiService::class.java)
    }
}
