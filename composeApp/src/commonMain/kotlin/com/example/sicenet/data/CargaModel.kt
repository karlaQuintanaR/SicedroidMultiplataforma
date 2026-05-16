package com.example.sicenet.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MateriaCarga(
    @SerialName("Materia") val materia: String = "",
    @SerialName("Docente") val docente: String? = "Sin asignar",
    @SerialName("Horario") val horario: String? = "N/A",
    @SerialName("Aula") val aula: String? = "N/A",
    @SerialName("Grupo") val grupo: String? = "N/A"
)