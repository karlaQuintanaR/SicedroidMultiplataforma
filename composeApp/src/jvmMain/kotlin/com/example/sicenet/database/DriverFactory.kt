package com.example.sicenet.database

import app.cash.sqldelight.db.SqlDriver

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        // Dejamos el error simulado para que Desktop no intente crear tablas reales
        throw NotImplementedError("La base de datos local no es necesaria en Desktop")
    }
}