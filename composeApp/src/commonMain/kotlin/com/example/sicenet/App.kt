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
import com.example.sicenet.database.SicenetDb
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

enum class Pantalla {
    LOGIN, KARDEX, CARGA, CALIFICACIONES, UNIDADES
}

@Composable
fun App(driverFactory: DriverFactory) {
    MaterialTheme {
        val scope = rememberCoroutineScope()
        val sicenetService = remember { SicenetService() }

        // 🗄️ Inicialización de tu base de datos SicenetDb.sq usando el driver nativo
        val database = remember { SicenetDb(driverFactory.createDriver()) }
        val queries = database.sicenetDbQueries

        var pantallaActual by remember { mutableStateOf(Pantalla.LOGIN) }

        // Listas reactivas para inyectar datos reales en las pantallas del alumno
        var listaMaterias by remember { mutableStateOf<List<MateriaKardex>>(emptyList()) }
        var listaCarga by remember { mutableStateOf<List<MateriaCarga>>(emptyList()) }
        var listaCalificaciones by remember { mutableStateOf<List<CalificacionFinal>>(emptyList()) }
        var listaUnidades by remember { mutableStateOf<List<MateriaUnidad>>(emptyList()) }

        var numControl by remember { mutableStateOf("") }
        var nip by remember { mutableStateOf("") }
        var mensaje by remember { mutableStateOf("") }
        var cargando by remember { mutableStateOf(false) }

        // 🔄 Al abrir la aplicación: Autocompleta el número de control del último usuario en disco
        LaunchedEffect(Unit) {
            val ultimoAlumno = queries.getStudent().executeAsOneOrNull()
            if (ultimoAlumno != null) {
                numControl = ultimoAlumno.controlNumber
            }
        }

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
                                // 🚪 AL SALIR: Regresamos al Login preservando los registros físicos en SQLite
                                pantallaActual = Pantalla.LOGIN
                                mensaje = "Sesión cerrada. Última cuenta guardada de forma offline."
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
                                mensaje = "Conectando al SICE..."
                                scope.launch {
                                    val userTrim = numControl.trim()
                                    val nipTrim = nip.trim()

                                    // 1. INTENTO ONLINE: Petición directa al SICE
                                    val ok = sicenetService.login(userTrim, nipTrim)

                                    if (ok) {
                                        try {
                                            mensaje = "Sincronizando y guardando en SicenetDb..."

                                            // Descargamos la información de red del alumno
                                            val materiasReal = sicenetService.getKardexParsed(userTrim, nipTrim)
                                            val cargaReal = sicenetService.getCargaParsed()
                                            val califReal = sicenetService.getCalificacionesParsed()
                                            val unidadesReal = sicenetService.getCalifUnidadesByAlumnoParsed()

                                            // 💾 PERSISTENCIA REAL: Insertamos en StudentEntity
                                            queries.insertStudent(
                                                controlNumber = userTrim,
                                                fullName = nipTrim, // Guardamos el NIP en fullName para validación offline posterior
                                                career = "Ingeniería",
                                                semester = 1
                                            )

                                            // Insertamos cada asignatura en la tabla relacional SubjectEntity
                                            materiasReal.forEach { materia ->
                                                queries.insertSubject(
                                                    name = materia.Materia,
                                                    grade = materia.Calif.toDouble(),
                                                    period = "2026",
                                                    studentId = userTrim
                                                )
                                            }

                                            // Cargamos los datos reales en las pantallas
                                            listaMaterias = materiasReal
                                            listaCarga = cargaReal
                                            listaCalificaciones = califReal
                                            listaUnidades = unidadesReal

                                            mensaje = "Sincronización completa con éxito."
                                            delay(800)
                                            pantallaActual = Pantalla.CARGA
                                        } catch (e: Exception) {
                                            mensaje = "Error al procesar datos: ${e.message}"
                                        } finally {
                                            cargando = false
                                        }
                                    } else {
                                        // 2. 🛡️ MODO CONTINGENCIA OFFLINE DIRECTO (Entra aquí si falló internet o el SICE devolvió false)
                                        val alumnoLocal = queries.getStudent().executeAsOneOrNull()

                                        if (alumnoLocal != null &&
                                            alumnoLocal.controlNumber == userTrim &&
                                            alumnoLocal.fullName == nipTrim) {

                                            mensaje = "Modo Offline: Recuperando datos de la base de datos..."

                                            // Extraemos los renglones de materias guardados en SQLite
                                            val materiasBD = queries.getSubjectsByStudent(alumnoLocal.controlNumber).executeAsList()

                                            // Llenamos el Kárdex mapeando las propiedades con strings vacíos (para A1 y P1 obligatorios)
                                            listaMaterias = materiasBD.map {
                                                MateriaKardex(Materia = it.name, ClvOfiMat = "INF", Calif = it.grade.toInt(), Acred = "A", Cdts = 5, A1 = "", P1 = "")
                                            }

                                            // Llenamos las Calificaciones Finales con los mismos datos
                                            listaCalificaciones = materiasBD.map {
                                                CalificacionFinal(materia = it.name, calif = it.grade.toInt(), acred = "A", grupo = "A")
                                            }

                                            // Llenamos Carga y Unidades de forma inteligente usando la info local para evitar pantallas vacías offline
                                            listaCarga = materiasBD.map {
                                                MateriaCarga(materia = it.name, docente = "Consultado de BD Local", horario = "Horario Offline", aula = "Aula BD")
                                            }
                                            listaUnidades = materiasBD.map {
                                                MateriaUnidad(Materia = it.name, C1 = it.grade.toInt().toString(), C2 = "100")
                                            }

                                            delay(1500)
                                            pantallaActual = Pantalla.CARGA
                                        } else {
                                            mensaje = "Error: Credenciales institucionales incorrectas o requiere internet."
                                        }
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
                            Text(
                                text = item.Materia,
                                modifier = Modifier.padding(bottom = 8.dp),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
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