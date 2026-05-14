package com.example.sicenet

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.example.sicenet.database.DriverFactory

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App(DriverFactory()) // Desktop no necesita parámetros
    }
}