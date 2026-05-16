package com.example.sicenet

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import com.example.sicenet.database.DriverFactory

fun main() = application {
    // Definimos un tamaño inicial para la ventana de la PC (estilo celular)
    val windowState = rememberWindowState(width = 400.dp, height = 750.dp)

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "Sicedroid Desktop"
    ) {
        // Llamamos a la UI unificada que está en commonMain
        App(driverFactory = DriverFactory())
    }
}