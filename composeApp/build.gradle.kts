import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.sqlDelight)
    kotlin("plugin.serialization") version "2.0.0"
}

kotlin {
    androidTarget {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
    }

    jvm("desktop") // Asegúrate de que se llame "desktop"

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)

                implementation(libs.androidx.lifecycle.viewmodelCompose)
                implementation(libs.androidx.lifecycle.runtimeCompose)
                implementation(libs.sqldelight.runtime)

                implementation(libs.ktor.client.core)
                implementation("io.ktor:ktor-client-logging:2.3.11")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.11")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.11")
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation(libs.sqldelight.android.driver)
                implementation("io.ktor:ktor-client-okhttp:2.3.11")
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.sqldelight.jvm.driver)
                implementation("io.ktor:ktor-client-cio:2.3.11")
            }
        }
    }
}

android {
    namespace = "com.example.sicenet"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.sicenet"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

sqldelight {
    databases {
        create("SicenetDb") {
            packageName.set("com.example.sicenet.database")
            generateAsync.set(false) // <--- Agrega esto para evitar el error del compilador
        }
    }
}

compose.desktop {
    application { // <--- ESTA LLAVE ES INDISPENSABLE
        mainClass = "com.example.sicenet.MainKt" // Pon la ruta completa
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.example.sicenet"
            packageVersion = "1.0.0"
        }
    }
}