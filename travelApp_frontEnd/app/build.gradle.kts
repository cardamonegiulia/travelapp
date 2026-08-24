plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.travelapp"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.travelapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"

        // AppAuth / Keycloak:
        // schema utilizzato dal redirect URI dopo il login.
        manifestPlaceholders["appAuthRedirectScheme"] =
            "com.example.travelapp"
    }

    buildTypes {
        release {
            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("com.airbnb.android:lottie-compose:6.4.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(
        "androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4"
    )
    implementation(
        "androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4"
    )

    // Compose
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Navigazione Compose
    implementation(
        "androidx.navigation:navigation-compose:2.8.4"
    )

    // Retrofit / HTTP
    implementation(
        "com.squareup.retrofit2:retrofit:2.11.0"
    )
    implementation(
        "com.squareup.retrofit2:converter-gson:2.11.0"
    )
    implementation(
        "com.squareup.okhttp3:okhttp:4.12.0"
    )

    // Keycloak / OAuth2 con PKCE
    implementation(
        "net.openid:appauth:0.11.1"
    )

    // Coroutine
    implementation(
        "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3"
    )

    // DataStore per persistenza token/sessione
    implementation(
        "androidx.datastore:datastore-preferences:1.0.0"
    )

    // Immagini
    implementation(
        "io.coil-kt:coil-compose:2.6.0"
    )

    // Test
    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(
        platform(libs.androidx.compose.bom)
    )
    androidTestImplementation(
        libs.androidx.ui.test.junit4
    )

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(
        libs.androidx.ui.test.manifest
    )
}