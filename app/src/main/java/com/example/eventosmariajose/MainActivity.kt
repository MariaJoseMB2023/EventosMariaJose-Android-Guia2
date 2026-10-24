
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eventosmariajose.ui.theme.EventosMariaJoseTheme
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

// MODELOS

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

// API RETROFIT

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

// CONEXIÓN AL BACKEND

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

// LÓGICA DE LA APLICACIÓN

class EventosViewModel : ViewModel() {

    var correo by mutableStateOf("mariajose.prueba@example.com")
    var clave by mutableStateOf("")
    var mensaje by mutableStateOf("")
    var token by mutableStateOf("")
    var eventos by mutableStateOf<List<Map<String, Any?>>>(emptyList())
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

                mensaje = "Bienvenida, ${respuesta.nombre}\nToken: $token"

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

// PANTALLA PRINCIPAL

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContent {

            EventosMariaJoseTheme {

                val vm: EventosViewModel = viewModel()

                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        Text(
                            text = "Sistema de Eventos",
                            style = MaterialTheme.typography.headlineMedium
                        )

                        Text(
                            text = "Guía 2 - Login y eventos autenticados"
                        )

                        OutlinedTextField(
                            value = vm.correo,
                            onValueChange = { vm.correo = it },
                            label = { Text("Correo") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = vm.clave,
                            onValueChange = { vm.clave = it },
                            label = { Text("Contraseña") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = { vm.iniciarSesion() },
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

                        Text(text = vm.mensaje)

                        if (vm.sesionIniciada) {

                            HorizontalDivider()

                            Text(
                                text = "Eventos disponibles",
                                style = MaterialTheme.typography.titleLarge
                            )

                            if (vm.eventosCargados && vm.eventos.isEmpty()) {

                                Text("No hay eventos registrados.")
                            }

                            LazyColumn {

                                items(vm.eventos) { evento ->

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {

                                        Column(
                                            modifier = Modifier.padding(12.dp)
                                        ) {

                                            val nombreEvento =
                                                evento["nombre"]
                                                    ?: evento["titulo"]
                                                    ?: "Sin nombre"

                                            Text(
                                                text = "Evento: $nombreEvento"
                                            )

                                            Text(
                                                text = "ID: ${evento["id"] ?: "-"}"
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
