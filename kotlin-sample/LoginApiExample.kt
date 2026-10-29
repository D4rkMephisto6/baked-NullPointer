// Ejemplo ilustrativo para shared/commonMain de Kotlin Multiplatform con Ktor.
// No forma parte de una app completa. Ajusta dependencias/serialización de tu proyecto KMP.
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class LoginPayload(val employeeCode: String, val pin: String)

@Serializable
data class LoginUser(val userId: Long, val employeeCode: String, val fullName: String, val role: String)

@Serializable
data class LoginResult(val token: String, val tokenType: String, val expiresInSeconds: Long, val user: LoginUser)

val posHttpClient = HttpClient {
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
}

suspend fun loginMozo(baseUrl: String, employeeCode: String, pin: String): LoginResult =
    posHttpClient.post("$baseUrl/api/v1/auth/login") {
        contentType(ContentType.Application.Json)
        setBody(LoginPayload(employeeCode, pin))
    }.body()

// PC/Postman: baseUrl = "http://localhost:8080"
// Emulador Android: baseUrl = "http://10.0.2.2:8080"
// Después de login, envía: Authorization: Bearer <result.token>
