package com.example.sicenet.database
class SicenetRepository(driverFactory: DriverFactory) {
    // Intentaremos usar la ruta completa para forzar al compilador
    private val database = SicenetDb(driverFactory.createDriver())
    private val queries = database.sicenetDbQueries

    suspend fun saveStudent(controlNumber: String, name: String, career: String, semester: Int) {
        queries.insertStudent(
            controlNumber = controlNumber,
            fullName = name,
            career = career,
            semester = semester.toLong()
        )
    }

    suspend fun getStudentProfile() = queries.getStudent().executeAsOneOrNull()

    suspend fun clearAllData() {
        queries.deleteAllData()
    }
}