plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// google-services.json is gitignored (contains Firebase API keys).
// Apply the plugin only when the file exists: local dev machines and CI
// (which injects it as a secret) get full Firebase; other builds compile
// and run with FCM gracefully disabled at runtime.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.omastro.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.omastroai.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("keystores/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        // Release signing: the SAME keystore the Flutter app was built with
        // (server: /var/www/omastroai keystores, copied to app/keystores/).
        // This keeps the app identity stable for Play Store updates and for
        // Google Sign-In SHA-1 registration. SHA-1:
        //   9F:39:0C:5B:B2:3D:B8:8A:3C:7E:74:BA:67:6F:B7:53:47:81:36:E9
        // Package "com.omastroai.app" must be registered as an Android OAuth
        // client with this SHA-1 in the same Google Cloud project as the web
        // client ID used in GoogleSignInHelper.
        create("release") {
            storeFile = file("keystores/omastro-release.keystore")
            // Prefer env injection (CI secret OMASTRO_KEYSTORE_PASSWORD); the
            // checked-in fallback keeps local builds working.
            storePassword = System.getenv("OMASTRO_KEYSTORE_PASSWORD") ?: "android"
            keyAlias = "omastro"
            keyPassword = System.getenv("OMASTRO_KEYSTORE_PASSWORD") ?: "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Signed with the Flutter app's official release keystore
            // (keystores/omastro-release.keystore) so Play Store updates and
            // Google Sign-In keep the same app identity.
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isDebuggable = true
            // Stable debug signing key committed to the repo (debug keys are NOT
            // secrets - this mirrors the conventional ~/.android/debug.keystore).
            // WHY: CI runners generate a fresh random debug keystore on every run,
            // so without this the APK's SHA-1 changes on every build and Google
            // Sign-In (error 10, DEVELOPER_ERROR) can never be fixed by registering
            // a fingerprint in Google Cloud Console. With this key, every debug
            // APK - local or CI - has the same SHA-1:
            //   27:37:38:7D:8C:2C:17:03:97:34:FA:0B:DA:6C:4F:BB:30:1C:7B:A1
            // Register that SHA-1 with package "com.omastroai.app" as an Android
            // OAuth client in the same Google Cloud project as the web client ID
            // used in GoogleSignInHelper (see its KDoc for exact steps).
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // AndroidX & Core
    implementation("androidx.core:core-ktx:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.9.0")

    // Jetpack Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.04.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Lifecycle & ViewModel Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    // Networking - Retrofit & OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Image Loading - Coil
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Google Play In-App Review
    implementation("com.google.android.play:review-ktx:2.0.1")

    // Firebase Cloud Messaging
    implementation("com.google.firebase:firebase-messaging-ktx:23.4.1")

    // Google Sign-In via classic play-services-auth (Sign in with Google button).
    // NOTE: Credential Manager (androidx.credentials + googleid) was removed:
    // its Google ID provider throws "getCredentialAsync no provider dependencies
    // found" on devices/emulators whose Play Services lacks the provider. The
    // classic GoogleSignInClient flow below works on all Play-Services devices.
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Razorpay Standard SDK
    implementation("com.razorpay:checkout:1.6.33")

    // Encrypted shared preferences for secure token storage
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // CameraX — Vastu Compass Mode (live camera preview behind the compass)
    val cameraxVersion = "1.3.4"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
