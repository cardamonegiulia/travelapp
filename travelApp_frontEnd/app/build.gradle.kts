import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

/**
 * Legge una proprietà da local.properties, con fallback sulla variabile
 * d'ambiente omonima e infine sul valore predefinito.
 */
fun proprietaLocale(
    chiave: String,
    predefinito: String
): String {

    val file = rootProject.file("local.properties")

    if (file.exists()) {
        val proprieta = Properties()

        file.inputStream().use {
            proprieta.load(it)
        }

        proprieta
            .getProperty(chiave)
            ?.takeIf { it.isNotBlank() }
            ?.let {
                return it.trim()
            }
    }

    val daAmbiente =
        System.getenv(
            chiave
                .replace('.', '_')
                .uppercase()
        )

    return daAmbiente
        ?.takeIf { it.isNotBlank() }
        ?.trim()
        ?: predefinito
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

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"

        /*
         * Schema AppAuth usato dal redirect URI dopo il login Keycloak.
         */
        manifestPlaceholders["appAuthRedirectScheme"] =
            "com.example.travelapp"

        /*
         * Backend e Keycloak vengono configurati da local.properties.
         *
         * In questo modo ogni sviluppatore può usare:
         * - localhost con adb reverse;
         * - 10.0.2.2 sull'emulatore;
         * - IP LAN su telefono fisico;
         *
         * senza modificare file versionati.
         */
        buildConfigField(
            "String",
            "BACKEND_BASE_URL",
            "\"${
                proprietaLocale(
                    "backend.base.url",
                    "http://localhost:8081/"
                )
            }\""
        )

        buildConfigField(
            "String",
            "KEYCLOAK_BASE_URL",
            "\"${
                proprietaLocale(
                    "keycloak.base.url",
                    "http://localhost:8090"
                )
            }\""
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

        sourceCompatibility =
            JavaVersion.VERSION_11

        targetCompatibility =
            JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {

        compose = true

        // Necessario per BACKEND_BASE_URL e KEYCLOAK_BASE_URL.
        buildConfig = true
    }
}

dependencies {

    // Animazioni
    implementation(
        "com.airbnb.android:lottie-compose:6.4.0"
    )

    // Android base
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.material)

    // Lifecycle
    implementation(
        libs.androidx.lifecycle.runtime.ktx
    )

    implementation(
        "androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4"
    )

    implementation(
        "androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4"
    )

    // Compose
    implementation(
        libs.androidx.activity.compose
    )

    implementation(
        platform(libs.androidx.compose.bom)
    )

    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)

    implementation(
        libs.androidx.ui.tooling.preview
    )

    implementation(
        libs.androidx.material3
    )

    // Navigazione
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

    // Keycloak / OAuth2 PKCE
    implementation(
        "net.openid:appauth:0.11.1"
    )

    // Coroutine
    implementation(
        "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3"
    )

    // Persistenza token
    implementation(
        "androidx.datastore:datastore-preferences:1.0.0"
    )

    // Immagini
    implementation(
        "io.coil-kt:coil-compose:2.6.0"
    )

    // Test
    testImplementation(
        libs.junit
    )

    androidTestImplementation(
        libs.androidx.junit
    )

    androidTestImplementation(
        libs.androidx.espresso.core
    )

    androidTestImplementation(
        platform(libs.androidx.compose.bom)
    )

    androidTestImplementation(
        libs.androidx.ui.test.junit4
    )

    debugImplementation(
        libs.androidx.ui.tooling
    )

    debugImplementation(
        libs.androidx.ui.test.manifest
    )
}

/*
 * Quando installDebug o assembleDebug vengono eseguiti con un dispositivo
 * fisico collegato via USB, espone automaticamente backend (8081)
 * e Keycloak (8090) tramite localhost sul dispositivo.
 *
 * Se non c'è un dispositivo collegato, il task non blocca la build.
 */
tasks.register("adbReversePortauto") {

    doLast {

        val isWindows =
            System.getProperty("os.name")
                .lowercase()
                .contains("windows")

        val adbExe =
            if (isWindows) {
                "adb.exe"
            } else {
                "adb"
            }

        val adb =
            File(
                android.sdkDirectory,
                "platform-tools/$adbExe"
            )

        if (!adb.exists()) {
            logger.warn(
                "adb non trovato in ${adb.path}, salto adb reverse"
            )
            return@doLast
        }

        for (port in listOf(8081, 8090)) {

            val risultato =
                project.exec {

                    commandLine(
                        adb.path,
                        "reverse",
                        "tcp:$port",
                        "tcp:$port"
                    )

                    isIgnoreExitValue = true
                }

            if (risultato.exitValue != 0) {
                logger.lifecycle(
                    "adb reverse per la porta $port non applicato " +
                            "(nessun dispositivo collegato, oppure più di uno)"
                )
            }
        }
    }
}

afterEvaluate {

    tasks
        .findByName("installDebug")
        ?.dependsOn("adbReversePortauto")

    tasks
        .findByName("assembleDebug")
        ?.dependsOn("adbReversePortauto")
}