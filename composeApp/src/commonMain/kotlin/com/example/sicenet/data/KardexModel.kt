package com.example.sicenet.data // Asegúrate de que el package coincida con tu carpeta

import kotlinx.serialization.Serializable

@Serializable
data class KardexResponse(
    val lstKardex: List<MateriaKardex>
)

@Serializable
data class MateriaKardex(
    val Materia: String,
    val Calif: Int,
    val Acred: String,
    val ClvOfiMat: String, // Clave de la materia
    val Cdts: Int,         // Créditos
    val P1: String?,       // Periodo (AGO-DIC)
    val A1: String?        // Año (2022)
)