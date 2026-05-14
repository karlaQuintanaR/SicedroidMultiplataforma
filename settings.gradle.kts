rootProject.name = "Sicedroid"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            content {
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
                includeGroupAndSubgroups("androidx")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/patch")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Repositorio para Compose Multiplatform
        maven("https://maven.pkg.jetbrains.space/public/p/compose/patch")
        // Repositorio de soporte para versiones específicas de Kotlin
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

include(":composeApp")