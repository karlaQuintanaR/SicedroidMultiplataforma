package com.example.sicenet.database

class SicenetRepository(driverFactory: DriverFactory) {
    private val database = SicenetDb(driverFactory.createDriver())
    private val queries = database.sicenetDbQueries

    // Agregamos 'suspend' porque SQLDelight está en modo async
    fun saveStudent(controlNumber: String, name: String, career: String, semester: Int) {
        queries.insertStudent(controlNumber, name, career, semester.toLong())
    }

    fun getStudentProfile() = queries.getStudent().executeAsOneOrNull()

    fun addGrade(subjectName: String, grade: Double, period: String, studentId: String) {
        queries.insertSubject(
            name = subjectName,
            grade = grade,
            period = period,
            studentId = studentId
        )
    }

    fun getGrades(studentId: String) = queries.getSubjectsByStudent(studentId).executeAsList()

    fun clearAllData() {
        queries.deleteAllData()
    }
}