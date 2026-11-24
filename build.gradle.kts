
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false

    // API 3: KSP con soporte para Built-in Kotlin
    id("com.google.devtools.ksp") version "2.3.6" apply false
}
 