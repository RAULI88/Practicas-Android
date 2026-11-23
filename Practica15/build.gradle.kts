// Archivo de compilación de nivel superior
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // Plugin de Google Services (Firebase)
    alias(libs.plugins.google.services) apply false
}
