import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.sqlDelight)
    kotlin("plugin.serialization") version "1.9.23"
}

kotlin {
    androidTarget {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
    }

    // 🚀 CAMBIO AQUÍ: Quita el "desktop" y déjalo solo como jvm()
    jvm {
        compilations.all {
            compilerOptions.configure {
                freeCompilerArgs.add("-Xuse-k2=false")
            }
        }
    }

    targets.all {
        compilations.all {
            compilerOptions.configure {
                freeCompilerArgs.add("-Xuse-k2=false")
            }
        }
    }

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
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation(libs.sqldelight.android.driver)
                implementation("io.ktor:ktor-client-okhttp:2.3.11")
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                // 🚀 SQLDelight desactivado para evitar el error interno del compilador en PC
                // implementation(libs.sqldelight.jvm.driver)
                implementation("io.ktor:ktor-client-cio:2.3.11")
            }
        }
    } // <-- Aquí se cierra sourceSets
} // <-- Aquí se cierra todo el bloque kotlin

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
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.example.sicenet.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.example.sicenet"
            packageVersion = "1.0.0"
        }
    }
}