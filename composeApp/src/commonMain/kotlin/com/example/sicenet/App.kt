package com.example.sicenet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.sicenet.data.CalificacionFinal
import com.example.sicenet.data.MateriaCarga
import com.example.sicenet.data.MateriaKardex
import com.example.sicenet.data.MateriaUnidad
import com.example.sicenet.data.SicenetService
import com.example.sicenet.database.DriverFactory
import kotlinx.coroutines.launch

enum class Pantalla {
    LOGIN, KARDEX, CARGA, CALIFICACIONES, UNIDADES
}

@Composable
fun App(driverFactory: DriverFactory) {
    MaterialTheme {
        val scope = rememberCoroutineScope()
        val sicenetService = remember { SicenetService() }

        var pantallaActual by remember { mutableStateOf(Pantalla.LOGIN) }
        var listaMaterias by remember { mutableStateOf<List<MateriaKardex>>(emptyList()) }
        var listaCarga by remember { mutableStateOf<List<MateriaCarga>>(emptyList()) }
        var listaCalificaciones by remember { mutableStateOf<List<CalificacionFinal>>(emptyList()) }
        var listaUnidades by remember { mutableStateOf<List<MateriaUnidad>>(emptyList()) }
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
                            icon = { Text("🕒") }
                        )
                        NavigationBarItem(
                            selected = pantallaActual == Pantalla.KARDEX,
                            onClick = { pantallaActual = Pantalla.KARDEX },
                            label = { Text("Kárdex") },
                            icon = { Text("📋") }
                        )
                        NavigationBarItem(
                            selected = pantallaActual == Pantalla.CALIFICACIONES,
                            onClick = { pantallaActual = Pantalla.CALIFICACIONES },
                            label = { Text("Finales") },
                            icon = { Text("⭐") }
                        )
                        NavigationBarItem(
                            selected = pantallaActual == Pantalla.UNIDADES,
                            onClick = { pantallaActual = Pantalla.UNIDADES },
                            label = { Text("Unidades") },
                            icon = { Text("📊") }
                        )
                        NavigationBarItem(
                            selected = false,
                            onClick = {
                                pantallaActual = Pantalla.LOGIN
                                listaMaterias = emptyList()
                                listaCarga = emptyList()
                                listaCalificaciones = emptyList()
                                listaUnidades = emptyList()
                                mensaje = ""
                            },
                            label = { Text("Salir") },
                            icon = { Text("🚪") }
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
                                            listaCalificaciones = sicenetService.getCalificacionesParsed()
                                            listaUnidades = sicenetService.getCalifUnidadesByAlumnoParsed()
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
                    Pantalla.UNIDADES -> UnidadesScreen(listaUnidades)
                }
            }
        }
    }
}

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
                        Surface(
                            color = if (materia.Calif >= 70) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            shape = MaterialTheme.shapes.small
                        ) {
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
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
// Así debe quedar la línea corregida:
                            Text("🕒 ${materia.horario ?: "N/A"}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                            Text("📍 Aula: ${materia.aula ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

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
                            Text("Acreditación: ${calif.acred ?: "N/A"} | Grupo: ${calif.grupo ?: ""}", style = MaterialTheme.typography.bodySmall)
                        }
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

@Composable
fun UnidadesScreen(materiasUnidades: List<MateriaUnidad>) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Calificaciones por Unidad", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        if (materiasUnidades.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay unidades capturadas o cargando...", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(materiasUnidades) { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Imprimimos el nombre de la materia
                            Text(item.Materia, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Fila con scroll horizontal automático por si son muchas unidades
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Mapeamos las calificaciones reales capturadas del servidor
                                val calificaciones = listOfNotNull(
                                    item.C1, item.C2, item.C3, item.C4, item.C5,
                                    item.C6, item.C7, item.C8, item.C9, item.C10
                                )

                                calificaciones.forEachIndexed { index, calif ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("U${index + 1}", style = MaterialTheme.typography.bodySmall)
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = MaterialTheme.shapes.small,
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            Text(
                                                text = calif,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}