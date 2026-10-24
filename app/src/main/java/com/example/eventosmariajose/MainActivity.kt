
package com.example.eventosmariajose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eventosmariajose.local.EventoFavorito
import com.example.eventosmariajose.local.FavoritosViewModel
import com.example.eventosmariajose.ui.theme.EventosMariaJoseTheme
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

// ============================================
// MODELOS DEL BACKEND
// ============================================

data class LoginRequest(
    val correo: String,
    val clave: String
)

data class LoginResponse(
    val id: Long,
    val nombre: String,
    val correo: String,
    val rol: String,
    val token: String
)

// ============================================
// API RETROFIT
// ============================================

interface EventosApi {

    @POST("api/auth/login")
    suspend fun login(
        @Body datos: LoginRequest
    ): LoginResponse

    @GET("api/eventos/autenticado")
    suspend fun listarEventos(
        @Header("Authorization") authorization: String
    ): List<Map<String, Any?>>
}

// ============================================
// CONEXIÓN AL BACKEND
// ============================================

object ApiClient {

    private const val BASE_URL = "http://127.0.0.1:8080/"

    val api: EventosApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EventosApi::class.java)
    }
}

// ============================================
// VIEWMODEL LOGIN Y EVENTOS
// ============================================

class EventosViewModel : ViewModel() {

    var correo by mutableStateOf("mariajose.prueba@example.com")
    var clave by mutableStateOf("")
    var mensaje by mutableStateOf("")
    var token by mutableStateOf("")

    var eventos by mutableStateOf<List<Map<String, Any?>>>(
        emptyList()
    )

    var cargando by mutableStateOf(false)
    var sesionIniciada by mutableStateOf(false)
    var eventosCargados by mutableStateOf(false)

    fun iniciarSesion() {

        if (cargando) return

        viewModelScope.launch {

            cargando = true
            mensaje = "Conectando..."
            token = ""
            eventos = emptyList()
            sesionIniciada = false
            eventosCargados = false

            try {

                val respuesta = ApiClient.api.login(
                    LoginRequest(
                        correo = correo.trim(),
                        clave = clave
                    )
                )

                token = respuesta.token
                sesionIniciada = true
                mensaje = "Bienvenida, ${respuesta.nombre}"

                try {

                    eventos = ApiClient.api.listarEventos(
                        "Bearer $token"
                    )

                    eventosCargados = true

                } catch (e: Exception) {

                    mensaje += "\nError al cargar eventos: ${e.message}"
                }

            } catch (e: HttpException) {

                mensaje = if (e.code() == 401) {
                    "Credenciales incorrectas (401)"
                } else {
                    "Error HTTP ${e.code()}"
                }

            } catch (e: Exception) {

                mensaje = "Error de conexión: ${e.message}"

            } finally {

                cargando = false
            }
        }
    }
}

// ============================================
// CONVERTIR EVENTO REMOTO A FAVORITO LOCAL
// ============================================

fun convertirAFavorito(
    evento: Map<String, Any?>
): EventoFavorito? {

    val valorId = evento["id"]
        ?: evento["idEvento"]
        ?: evento["eventoId"]
        ?: evento["id_evento"]

    val id = when (valorId) {

        is Number -> valorId.toLong()

        is String -> valorId.toDoubleOrNull()?.toLong()

        else -> null

    } ?: return null

    return EventoFavorito(
        idEvento = id,
        nombre = (
                evento["nombre"]
                    ?: evento["titulo"]
                    ?: "Sin nombre"
                ).toString(),
        lugar = (
                evento["lugar"]
                    ?: "Sin lugar"
                ).toString(),
        fechaHora = (
                evento["fechaHora"]
                    ?: evento["fecha"]
                    ?: "Sin fecha"
                ).toString(),
        categoria = (
                evento["categoria"]
                    ?: "Sin categoría"
                ).toString(),
        estado = (
                evento["estado"]
                    ?: "Sin estado"
                ).toString()
    )
}

// ============================================
// PANTALLA DE FAVORITOS CON ROOM
// ============================================

@Composable
fun FavoritosScreen(
    favoritosViewModel: FavoritosViewModel
) {

    val favoritos by favoritosViewModel.favoritos.collectAsState()

    if (favoritos.isEmpty()) {

        Text("Todavía no tenés eventos favoritos.")

    } else {

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            items(
                items = favoritos,
                key = { it.idEvento }
            ) { favorito ->

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {

                        Text(
                            favorito.nombre,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text("ID: ${favorito.idEvento}")
                        Text("Lugar: ${favorito.lugar}")
                        Text("Fecha: ${favorito.fechaHora}")
                        Text("Categoría: ${favorito.categoria}")
                        Text("Estado: ${favorito.estado}")

                        Button(
                            onClick = {
                                favoritosViewModel.eliminar(favorito)
                            }
                        ) {

                            Text("Quitar de favoritos")
                        }
                    }
                }
            }
        }
    }
}

// ============================================
// MAIN ACTIVITY
// ============================================

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContent {

            EventosMariaJoseTheme {

                val vm: EventosViewModel = viewModel()

                val favoritosVm: FavoritosViewModel = viewModel()

                val favoritos by favoritosVm.favoritos.collectAsState()

                var mostrarFavoritos by rememberSaveable {
                    mutableStateOf(false)
                }

                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {

                        Text(
                            "Sistema de Eventos",
                            style = MaterialTheme.typography.headlineMedium
                        )

                        Text("Guía 3 - Favoritos con Room")

                        // NAVEGACIÓN

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {

                            Button(
                                onClick = {
                                    mostrarFavoritos = false
                                },
                                modifier = Modifier.weight(1f)
                            ) {

                                Text("Eventos")
                            }

                            Button(
                                onClick = {
                                    mostrarFavoritos = true
                                },
                                modifier = Modifier.weight(1f)
                            ) {

                                Text("Mis favoritos")
                            }
                        }

                        HorizontalDivider()

                        if (mostrarFavoritos) {

                            // PANTALLA LOCAL: ROOM

                            Text(
                                "Mis favoritos (${favoritos.size})",
                                style = MaterialTheme.typography.titleLarge
                            )

                            FavoritosScreen(favoritosVm)

                        } else {

                            // PANTALLA DE LOGIN

                            if (!vm.sesionIniciada) {

                                OutlinedTextField(
                                    value = vm.correo,
                                    onValueChange = {
                                        vm.correo = it
                                    },
                                    label = {
                                        Text("Correo")
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Email
                                    ),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = vm.clave,
                                    onValueChange = {
                                        vm.clave = it
                                    },
                                    label = {
                                        Text("Contraseña")
                                    },
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Button(
                                    onClick = {
                                        vm.iniciarSesion()
                                    },
                                    enabled = !vm.cargando,
                                    modifier = Modifier.fillMaxWidth()
                                ) {

                                    Text(
                                        if (vm.cargando) {
                                            "Conectando..."
                                        } else {
                                            "Iniciar sesión"
                                        }
                                    )
                                }
                            }

                            if (vm.mensaje.isNotBlank()) {

                                Text(vm.mensaje)
                            }

                            // LISTADO REMOTO

                            if (vm.sesionIniciada) {

                                Text(
                                    "Eventos disponibles",
                                    style = MaterialTheme.typography.titleLarge
                                )

                                if (
                                    vm.eventosCargados &&
                                    vm.eventos.isEmpty()
                                ) {

                                    Text("No hay eventos registrados.")
                                }

                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {

                                    items(vm.eventos) { evento ->

                                        val favorito = convertirAFavorito(evento)

                                        val guardado =
                                            favorito != null &&
                                                    favoritos.any {
                                                        it.idEvento == favorito.idEvento
                                                    }

                                        Card(
                                            modifier = Modifier.fillMaxWidth()
                                        ) {

                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {

                                                Text(
                                                    "Evento: ${
                                                        evento["nombre"]
                                                            ?: evento["titulo"]
                                                            ?: "Sin nombre"
                                                    }",
                                                    style = MaterialTheme.typography.titleMedium
                                                )

                                                Text(
                                                    "ID: ${
                                                        evento["id"]
                                                            ?: evento["idEvento"]
                                                            ?: evento["eventoId"]
                                                            ?: evento["id_evento"]
                                                            ?: "-"
                                                    }"
                                                )

                                                // DIAGNÓSTICO TEMPORAL
                                                // MUESTRA LOS DATOS REALES
                                                // QUE DEVUELVE SPRING BOOT

                                                Text(
                                                    "DATOS: $evento",
                                                    style = MaterialTheme.typography.bodySmall
                                                )

                                                // BOTÓN DE FAVORITOS

                                                if (favorito != null) {

                                                    Button(
                                                        onClick = {

                                                            if (guardado) {

                                                                favoritosVm.eliminar(favorito)

                                                            } else {

                                                                favoritosVm.guardar(favorito)
                                                            }
                                                        },
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {

                                                        Text(
                                                            if (guardado) {
                                                                "Quitar favorito"
                                                            } else {
                                                                "Guardar favorito"
                                                            }
                                                        )
                                                    }

                                                } else {

                                                    Text(
                                                        "Este evento no tiene ID válido.",
                                                        color = MaterialTheme.colorScheme.error
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
    }
}
