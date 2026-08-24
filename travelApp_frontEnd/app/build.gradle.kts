import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

/**
 * Legge una proprieta' da `local.properties`, con fallback sulla variabile d'ambiente
 * omonima in maiuscolo (utile in CI, dove `local.properties` non esiste) e infine sul
 * default passato.
 */
fun proprietaLocale(chiave: String, predefinito: String): String {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        val proprieta = Properties()
        file.inputStream().use { proprieta.load(it) }
        proprieta.getProperty(chiave)?.takeIf { it.isNotBlank() }?.let { return it.trim() }
    }
    val daAmbiente = System.getenv(chiave.replace('.', '_').uppercase())
    return daAmbiente?.takeIf { it.isNotBlank() }?.trim() ?: predefinito
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
        // Deve coincidere ESATTAMENTE con la redirect URI registrata sul client
        // `travelapp-android` del realm (vedi docs/login-android-setup.md, Passo 1):
        // Keycloak confronta la stringa per intero e uno schema diverso fa fallire il
        // login con "Invalid parameter: redirect_uri", prima ancora della form.
        manifestPlaceholders["appAuthRedirectScheme"] =
            "com.example.travelapp"

        // Host di backend e Keycloak: NON scritti nel sorgente.
        //
        // Sono l'unica cosa che cambia da una macchina all'altra e da una rete all'altra
        // (l'IP della Wi-Fi e' in DHCP), quindi vivono in `local.properties`, che non e'
        // versionato: cosi' cambiare rete non produce piu' una modifica al codice che poi
        // non si deve committare. Valori di riferimento in `local.properties.example`.
        //
        // Il default e' `localhost`, che funziona in due casi senza configurare nulla:
        // sull'emulatore con il port forwarding, e su telefono fisico con
        // `adb reverse tcp:8081 tcp:8081` e `adb reverse tcp:8090 tcp:8090`.
        buildConfigField(
            "String",
            "BACKEND_BASE_URL",
            "\"${proprietaLocale("backend.base.url", "http://localhost:8081/")}\""
        )
        buildConfigField(
            "String",
            "KEYCLOAK_BASE_URL",
            "\"${proprietaLocale("keycloak.base.url", "http://localhost:8090")}\""
        )
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
        // necessario per i buildConfigField di defaultConfig
        buildConfig = true
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
