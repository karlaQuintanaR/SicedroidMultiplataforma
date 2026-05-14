package com.example.sicenet.data

import io.ktor.client.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class SicenetService() {

    private val client = HttpClient {
        install(Logging) {
            level = LogLevel.BODY
        }
    }

    private val BASE_URL = "https://sicenet.surguanajuato.tecnm.mx/ws/wsalumnos.asmx"

    /**
     * Intenta iniciar sesión en el servidor del TecNM.
     * Limpia la matrícula y escapa caracteres especiales en la contraseña (como el %).
     */
    suspend fun login(matricula: String, contrasenia: String): Boolean {

        // 1. Limpieza de datos: Matrícula en mayúsculas y sin espacios
        val matriculaLimpia = matricula.trim().uppercase()

        // 2. Escapar caracteres especiales en la contraseña para que el XML no se rompa
        // Esto soluciona el problema con el símbolo '%'
        val contraseniaEscapada = contrasenia
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
            .replace("%", "&#37;")

        // 3. Construcción del sobre SOAP
        val soapBody = """
            <?xml version="1.0" encoding="utf-8"?>
            <soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
                           xmlns:xsd="http://www.w3.org/2001/XMLSchema" 
                           xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
              <soap:Body>
                <accesoLogin xmlns="http://tempuri.org/">
                  <strUsuario>$matriculaLimpia</strUsuario>
                  <strContrasenia>$contraseniaEscapada</strContrasenia>
                </accesoLogin>
              </soap:Body>
            </soap:Envelope>
        """.trimIndent()

        return try {
            val response = client.post(BASE_URL) {
                header("Content-Type", "text/xml; charset=utf-8")
                header("SOAPAction", "http://tempuri.org/accesoLogin")
                setBody(soapBody)
            }

            val responseText = response.bodyAsText()

            // El servidor responde con un XML que contiene <accesoLoginResult>
            // Si el login es correcto, el contenido es 'true'
            responseText.contains("<accesoLoginResult>true</accesoLoginResult>") ||
                    responseText.contains("accesoLoginResult=\"true\"")

        } catch (e: Exception) {
            // Si hay error de red o el servidor está caído, lo verás en el Logcat
            println("Error en SicenetService: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}