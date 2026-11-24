
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
}

// ============================================
// URL BASE DEL SERVIDOR REMOTO
// ============================================

val recordatoriosBaseUrl = providers
    .gradleProperty("RECORDATORIOS_BASE_URL")
    .orElse("http://10.0.2.2:8080/")
    .get()

android {

    namespace = "com.example.misrecordatorios"

    compileSdk {
        version = release(37)
    }

    defaultConfig {

        applicationId = "com.example.misrecordatorios"

        minSdk = 26
        targetSdk = 37

        versionCode = 3
        versionName = "3.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"

        // URL configurable para Retrofit

        buildConfigField(
            "String",
            "RECORDATORIOS_BASE_URL",
            "\"$recordatoriosBaseUrl\""
        )
    }

    buildTypes {

        release {

            optimization {

                enable = true

                packageScope = setOf(
                    "androidx.**",
                    "kotlin.**",
                    "kotlinx.**"
                )
            }
        }
    }

    compileOptions {

        sourceCompatibility =
            JavaVersion.VERSION_11

        targetCompatibility =
            JavaVersion.VERSION_11
    }

    buildFeatures {

        compose = true

        buildConfig = true
    }
}

dependencies {

    // ========================================
    // 1. JETPACK COMPOSE
    // ========================================

    implementation(
        platform(libs.androidx.compose.bom)
    )

    implementation(libs.androidx.activity.compose)

    implementation(libs.androidx.compose.material3)

    implementation(libs.androidx.compose.ui)

    implementation(libs.androidx.compose.ui.graphics)

    implementation(libs.androidx.compose.ui.tooling.preview)


    // ========================================
    // 2. ANDROID CORE Y LIFECYCLE
    // ========================================

    implementation(libs.androidx.core.ktx)

    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.lifecycle.runtime.compose)


    // ========================================
    // 3. NAVIGATION COMPOSE
    // ========================================

    implementation(libs.androidx.navigation.compose)


    // ========================================
    // 4. CORRUTINAS
    // ========================================

    implementation(libs.kotlinx.coroutines.android)


    // ========================================
    // 5. DATASTORE PREFERENCES
    // ========================================

    implementation(
        "androidx.datastore:datastore-preferences:1.1.7"
    )


    // ========================================
    // 6. ROOM DATABASE
    // ========================================

    implementation(
        "androidx.room:room-runtime:2.8.4"
    )

    implementation(
        "androidx.room:room-ktx:2.8.4"
    )

    add(
        "ksp",
        "androidx.room:room-compiler:2.8.4"
    )


    // ========================================
    // 7. RETROFIT
    // ========================================

    implementation(
        "com.squareup.retrofit2:retrofit:2.11.0"
    )

    implementation(
        "com.squareup.retrofit2:converter-gson:2.11.0"
    )


    // ========================================
    // 8. OKHTTP
    // ========================================

    implementation(
        "com.squareup.okhttp3:okhttp:4.12.0"
    )

    implementation(
        "com.squareup.okhttp3:logging-interceptor:4.12.0"
    )


    // ========================================
    // 9. PRUEBAS UNITARIAS
    // ========================================

    testImplementation(libs.junit)


    // ========================================
    // 10. PRUEBAS INSTRUMENTADAS
    // ========================================

    androidTestImplementation(
        platform(libs.androidx.compose.bom)
    )

    androidTestImplementation(
        libs.androidx.compose.ui.test.junit4
    )

    androidTestImplementation(
        libs.androidx.espresso.core
    )

    androidTestImplementation(
        libs.androidx.junit
    )


    // ========================================
    // 11. HERRAMIENTAS DE DESARROLLO
    // ========================================

    debugImplementation(
        libs.androidx.compose.ui.test.manifest
    )

    debugImplementation(
        libs.androidx.compose.ui.tooling
    )
}
 