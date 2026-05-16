plugins {
    // Usamos exactamente los nombres que definimos en el archivo TOML
    kotlin("plugin.serialization") version "1.9.23"
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.sqlDelight) apply false
}