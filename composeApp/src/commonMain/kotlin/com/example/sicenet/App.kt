package com.example.sicenet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.sicenet.data.CalificacionFinal
import com.example.sicenet.data.MateriaCarga
import com.example.sicenet.data.MateriaKardex
import com.example.sicenet.data.SicenetService
import com.example.sicenet.database.DriverFactory
import kotlinx.coroutines.launch

// Definimos las pantallas posibles
enum class Pantalla {
    LOGIN, KARDEX, CARGA, CALIFICACIONES
}

@Composable
fun App(driverFactory: DriverFactory) {
    MaterialTheme {
        val scope = rememberCoroutineScope()
        val sicenetService = remember { SicenetService() }

        // Estados de navegación y datos
        var pantallaActual by remember { mutableStateOf(Pantalla.LOGIN) }
        var listaMaterias by remember { mutableStateOf<List<MateriaKardex>>(emptyList()) }
        var listaCarga by remember { mutableStateOf<List<MateriaCarga>>(emptyList()) }
        var listaCalificaciones by remember { mutableStateOf<List<CalificacionFinal>>(emptyList()) }

        // Estados de Login
        var numControl by remember { mutableStateOf("") }
        var nip by remember { mutableStateOf("") }
        var mensaje by remember { mutableStateOf("") }
        var cargando by remember { mutableStateOf(false) }

        Scaffold(
            bottomBar = {
                if (pantallaActual != Pantalla.LOGIN) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = pantallaActual == Pantalla.CARGA,
                            onClick = { pantallaActual = Pantalla.CARGA },
                            label = { Text("Horario") },
                            icon = { Icon(Icons.Default.DateRange, null) }
                        )
                        NavigationBarItem(
                            selected = pantallaActual == Pantalla.KARDEX,
                            onClick = { pantallaActual = Pantalla.KARDEX },
                            label = { Text("Kárdex") },
                            icon = { Icon(Icons.Default.List, null) }
                        )
                        NavigationBarItem(
                            selected = pantallaActual == Pantalla.CALIFICACIONES,
                            onClick = { pantallaActual = Pantalla.CALIFICACIONES },
                            label = { Text("Finales") },
                            icon = { Icon(Icons.Default.Star, null) }
                        )
                        NavigationBarItem(
                            selected = false,
                            onClick = {
                                pantallaActual = Pantalla.LOGIN
                                listaMaterias = emptyList()
                                listaCarga = emptyList()
                                listaCalificaciones = emptyList()
                                mensaje = ""
                            },
                            label = { Text("Salir") },
                            icon = { Icon(Icons.Default.ExitToApp, null) }
                        )
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                when (pantallaActual) {
                    Pantalla.LOGIN -> {
                        LoginScreen(
                            numControl, nip, mensaje, cargando,
                            onNumChange = { numControl = it },
                            onNipChange = { nip = it },
                            onLoginClick = {
                                cargando = true
                                mensaje = "Iniciando sesión..."
                                scope.launch {
                                    try {
                                        val ok = sicenetService.login(numControl, nip)
                                        if (ok) {
                                            mensaje = "Cargando datos..."
                                            listaMaterias = sicenetService.getKardexParsed(numControl, nip)
                                            listaCarga = sicenetService.getCargaParsed()

                                            // 🚀 CORRECCIÓN: Se ejecuta dentro del hilo asíncrono tras el login
                                            listaCalificaciones = sicenetService.getCalificacionesParsed()

                                            pantallaActual = Pantalla.CARGA
                                        } else {
                                            mensaje = "Error: Datos incorrectos"
                                        }
                                    } catch (e: Exception) {
                                        mensaje = "Error: ${e.message}"
                                    } finally {
                                        cargando = false
                                    }
                                }
                            }
                        )
                    }
                    Pantalla.KARDEX -> KardexScreen(listaMaterias)
                    Pantalla.CARGA -> CargaScreen(listaCarga)
                    Pantalla.CALIFICACIONES -> CalificacionesScreen(listaCalificaciones)
                }
            }
        }
    }
}

// --- VISTAS SEPARADAS ---

@Composable
fun LoginScreen(
    numControl: String, nip: String, mensaje: String, cargando: Boolean,
    onNumChange: (String) -> Unit, onNipChange: (String) -> Unit, onLoginClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Sicedroid", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(32.dp))
        TextField(value = numControl, onValueChange = onNumChange, label = { Text("N° de Control") }, modifier = Modifier.fillMaxWidth(), enabled = !cargando)
        Spacer(modifier = Modifier.height(16.dp))
        TextField(value = nip, onValueChange = onNipChange, label = { Text("NIP") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), enabled = !cargando)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onLoginClick, modifier = Modifier.fillMaxWidth().height(50.dp), enabled = !cargando) {
            if (cargando) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            else Text("Entrar")
        }
        if (mensaje.isNotEmpty()) {
            Text(mensaje, modifier = Modifier.padding(top = 16.dp), color = if (mensaje.contains("Error")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun KardexScreen(materias: List<MateriaKardex>) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Mi Kárdex", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(materias) { materia ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(materia.Materia, style = MaterialTheme.typography.titleSmall)
                            Text("Clave: ${materia.ClvOfiMat}", style = MaterialTheme.typography.bodySmall)
                        }
                        Surface(color = if (materia.Calif >= 70) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, shape = MaterialTheme.shapes.small) {
                            Text(materia.Calif.toString(), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CargaScreen(materias: List<MateriaCarga>) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Carga Académica", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(materias) { materia ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(materia.materia, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Text("Docente: ${materia.docente ?: "Pendiente"}", style = MaterialTheme.typography.bodyMedium)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("  ${materia.horario ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
                            Text("  Aula: ${materia.aula ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

// 🚀 CORRECCIÓN: Separada limpiamente fuera de CargaScreen
@Composable
fun CalificacionesScreen(calificaciones: List<CalificacionFinal>) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Calificaciones Finales", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(calificaciones) { calif ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(calif.materia, style = MaterialTheme.typography.titleMedium)
                            // CAMBIADO: calif.acred y calif.grupo
                            Text("Acreditación: ${calif.acred ?: "N/A"} | Grupo: ${calif.grupo ?: ""}", style = MaterialTheme.typography.bodySmall)
                        }

                        // CAMBIADO: calif.calif y corregido el MaterialTheme.colorScheme
                        Surface(
                            color = if (calif.calif >= 70) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = calif.calif.toString(),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }
}