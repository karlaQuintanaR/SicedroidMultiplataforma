package com.example.sicenet.data

import kotlinx.serialization.json.Json
import io.ktor.client.*
import io.ktor.client.plugins.HttpRedirect
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class SicenetService() {
    private val client = HttpClient {
        install(io.ktor.client.plugins.cookies.HttpCookies)
        install(Logging) {
            level = LogLevel.ALL
        }
        followRedirects = true
        install(HttpRedirect) {
            checkHttpMethod = false
        }
    }

    private val BASE_URL = "http://sicenet.surguanajuato.tecnm.mx/ws/wsalumnos.asmx"

    // Configuración para leer el JSON del Kárdex
    private val jsonConfig = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    suspend fun login(matricula: String, contrasenia: String): Boolean {
        val matriculaLimpia = matricula.trim().uppercase()
        val contraseniaEscapada = contrasenia
            .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&apos;").replace("%", "&#37;")

        val soapBody = """
            <?xml version="1.0" encoding="utf-8"?>
            <soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
                           xmlns:xsd="http://www.w3.org/2001/XMLSchema" 
                           xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
              <soap:Body>
                <accesoLogin xmlns="http://tempuri.org/">
                  <strMatricula>$matriculaLimpia</strMatricula>
                  <strContrasenia>$contraseniaEscapada</strContrasenia>
                  <tipoUsuario>ALUMNO</tipoUsuario>
                </accesoLogin>
              </soap:Body>
            </soap:Envelope>
        """.trimIndent()

        return try {
            val response = client.post(BASE_URL) {
                header("Content-Type", "text/xml; charset=utf-8")
                header("SOAPAction", "\"http://tempuri.org/accesoLogin\"")
                setBody(soapBody)
            }
            val responseText = response.bodyAsText()
            responseText.contains("\"acceso\":true")
        } catch (e: Exception) {
            false
        }
    }

    // 1. Esta función obtiene el XML crudo
    suspend fun getKardexRaw(matricula: String, contrasenia: String): String {
        val soapBody = """
            <?xml version="1.0" encoding="utf-8"?>
            <soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
                           xmlns:xsd="http://www.w3.org/2001/XMLSchema" 
                           xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
              <soap:Body>
                <getAllKardexConPromedioByAlumno xmlns="http://tempuri.org/">
                  <aluLineamiento>1</aluLineamiento>
                </getAllKardexConPromedioByAlumno>
              </soap:Body>
            </soap:Envelope>
        """.trimIndent()

        return try {
            val response = client.post(BASE_URL) {
                header("Content-Type", "text/xml; charset=utf-8")
                header("SOAPAction", "\"http://tempuri.org/getAllKardexConPromedioByAlumno\"")
                setBody(soapBody)
            }
            response.bodyAsText()
        } catch (e: Exception) { "" }
    }

    // 2. Esta función CONVIERTE el XML en una Lista de Materias
    suspend fun getKardexParsed(matricula: String, contrasenia: String): List<MateriaKardex> {
        val rawXml = getKardexRaw(matricula, contrasenia)
        return try {
            val jsonString = rawXml
                .substringAfter("<getAllKardexConPromedioByAlumnoResult>")
                .substringBefore("</getAllKardexConPromedioByAlumnoResult>")

            val response = jsonConfig.decodeFromString<KardexResponse>(jsonString)
            response.lstKardex
        } catch (e: Exception) {
            emptyList()
        }
    }
    // 1. Obtiene el XML crudo de la carga
    suspend fun getCargaRaw(): String {
        val soapBody = """
        <?xml version="1.0" encoding="utf-8"?>
        <soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
                       xmlns:xsd="http://www.w3.org/2001/XMLSchema" 
                       xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
          <soap:Body>
            <getCargaAcademicaByAlumno xmlns="http://tempuri.org/" />
          </soap:Body>
        </soap:Envelope>
    """.trimIndent()

        return try {
            val response = client.post(BASE_URL) {
                header("Content-Type", "text/xml; charset=utf-8")
                header("SOAPAction", "\"http://tempuri.org/getCargaAcademicaByAlumno\"")
                setBody(soapBody)
            }
            response.bodyAsText()
        } catch (e: Exception) { "" }
    }

    // 2. Convierte el XML en una Lista de Materias de Carga
    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
         suspend fun getCargaParsed(): List<MateriaCarga> {
        val rawXml = getCargaRaw()
        return try {
            val jsonString = rawXml
                .substringAfter("<getCargaAcademicaByAlumnoResult>")
                .substringBefore("</getCargaAcademicaByAlumnoResult>")

            // FÍJATE AQUÍ: Ahora pedimos List<MateriaCarga> directamente, no CargaResponse
            val lista = jsonConfig.decodeFromString<List<MateriaCarga>>(jsonString)

            lista
        } catch (e: Exception) {
            println("DEBUG_CARGA_ERROR_PARSE: ${e.message}")
            emptyList()
        }
    }
}