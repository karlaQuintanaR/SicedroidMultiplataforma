package com.example.sicenet.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        val databasePath = File(System.getProperty("user.home"), "sicenet.db")
        val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:${databasePath.absolutePath}")

        // Esto crea las tablas si no existen en el archivo .db del escritorio
        if (!databasePath.exists()) {
            SicenetDb.Schema.create(driver)
        }
        return driver
    }
}