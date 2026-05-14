package com.example.sicenet

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.sicenet.database.*
import com.example.sicenet.data.SicenetService
import io.ktor.client.*
import kotlinx.coroutines.launch

@Composable
fun App(driverFactory: DriverFactory) {
    MaterialTheme {
        // 1. Inicializamos las herramientas (Service y Repository)
        // Usamos remember para que no se vuelvan a crear al escribir en los campos
        val repository = remember { SicenetRepository(driverFactory) }
        val scope = rememberCoroutineScope()

        // 2. Estados para controlar lo que pasa en la pantalla
        var numControl by remember { mutableStateOf("") }
        var nip by remember { mutableStateOf("") }
        var mensaje by remember { mutableStateOf("") }
        var cargando by remember { mutableStateOf(false) }
        val sicenetService = remember { SicenetService() }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Sicedroid Multiplataforma",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(30.dp))

                // Campo de Número de Control
                TextField(
                    value = numControl,
                    onValueChange = { numControl = it },
                    label = { Text("Número de Control") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !cargando, // Bloquea el campo si está cargando
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(15.dp))

                // Campo de NIP
                TextField(
                    value = nip,
                    onValueChange = { nip = it },
                    label = { Text("NIP (Contraseña)") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !cargando,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(30.dp))

                // Botón de Inicio de Sesión
                Button(
                    onClick = {
                        if (numControl.isBlank() || nip.isBlank()) {
                            mensaje = "Por favor, llena todos los campos"
                            return@Button
                        }

                        cargando = true
                        mensaje = "Conectando al servidor del Tec..."

                        scope.launch {
                            try {
                                // LLAMADA REAL A INTERNET (Sicenet)
                                val esExitoso = sicenetService.login(numControl, nip)

                                if (esExitoso) {
                                    // SI FUNCIONA: Guardamos en la base de datos local
                                    repository.saveStudent(
                                        controlNumber = numControl,
                                        name = "Alumno Autenticado", // Aquí podrías parsear el nombre después
                                        career = "Cargando...",
                                        semester = 1
                                    )
                                    mensaje = "¡Login exitoso! Bienvenido."
                                } else {
                                    mensaje = "Error: Matrícula o NIP incorrectos."
                                }
                            } catch (e: Exception) {
                                // Captura errores de falta de internet o servidor caído
                                mensaje = "Error de conexión: Revisa tu internet."
                            } finally {
                                cargando = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = !cargando
                ) {
                    if (cargando) {
                        // El circulito de carga que pediste
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Iniciar Sesión")
                    }
                }

                // Muestra el mensaje de éxito o error abajo del botón
                if (mensaje.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = mensaje,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (mensaje.contains("Error")) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Función necesaria para que el diseño se vea en el "Preview" de Android Studio
 * sin que truene por falta del DriverFactory.
 */
@Composable
@org.jetbrains.compose.ui.tooling.preview.Preview
fun AppPreview() {
    MaterialTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Vista Previa (Diseño)")
                TextField(value = "", onValueChange = {}, label = { Text("Matrícula") })
                Button(onClick = {}) { Text("Iniciar Sesión") }
            }
        }
    }
}