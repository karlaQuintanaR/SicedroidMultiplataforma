package com.example.sicenet.data

import kotlinx.serialization.Serializable

@Serializable
data class CalificacionFinal(
    val materia: String = "",
    val calif: Int = 0,
    val acred: String? = "N/A",
    val grupo: String? = ""
)