package com.example.sicenet.database

import app.cash.sqldelight.db.SqlDriver

expect class DriverFactory {
    fun createDriver(): SqlDriver
}

// Esta función ayudará a inicializar la base de datos fácilmente
fun createDatabase(driverFactory: DriverFactory): SicenetDb {
    val driver = driverFactory.createDriver()
    return SicenetDb(driver)
}