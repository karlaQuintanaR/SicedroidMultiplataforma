package com.example.sicenet.database

import app.cash.sqldelight.db.SqlDriver

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        // Al lanzar este error simulado, evitamos que Desktop intente procesar
        // las tablas autogeneradas corruptas de SQLDelight
        throw NotImplementedError("La base de datos local no es necesaria en Desktop")
    }
}