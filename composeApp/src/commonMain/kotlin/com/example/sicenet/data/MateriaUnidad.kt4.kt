package com.example.sicenet.data

import kotlinx.serialization.Serializable

@Serializable
data class MateriaUnidad(
    val Materia: String,              // 🚀 Con M mayúscula
    val UnidadesActivas: String? = "", // 🚀 Atrapamos este campo extra
    val Observaciones: String? = "",
    val C1: String? = null,           // 🚀 El Sicenet usa C de "Calificación"
    val C2: String? = null,
    val C3: String? = null,
    val C4: String? = null,
    val C5: String? = null,
    val C6: String? = null,
    val C7: String? = null,
    val C8: String? = null,
    val C9: String? = null,
    val C10: String? = null,
    val C11: String? = null,
    val C12: String? = null,
    val C13: String? = null
)