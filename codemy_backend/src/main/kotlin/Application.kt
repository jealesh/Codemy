package com.example

import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.sql.Connection

// Exposed
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq


// Твои таблицы
import tables.Users

// Если используешь другие сущности/таблицы — добавляй сюда же
@Serializable
data class RegisterRequest(
    val fullName: String,
    val username: String,
    val age: Int,
    val email: String,
    val password: String
)

@Serializable
data class RegisterResponse(
    val message: String,
    val userId: Long? = null
)

@Serializable
data class LoginRequest(
    val loginOrEmail: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val message: String,
    val userId: Long? = null
)

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

fun Application.module() {
    DatabaseFactory.connect()  // подключаемся к БД

    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        })
    }

    install(CORS) {
        anyHost()
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowCredentials = true
    }

    routing {
        get("/") {
            call.respondText("Server Codemy is working!")
        }

        post("/register") {
            try {
                val request = call.receive<RegisterRequest>()
                println("Registration received: $request")

                val newUserId = transaction {
                    val insertStatement = Users.insert {
                        it[username] = request.username
                        it[email] = request.email
                        it[passwordHash] = request.password   // потом добавим хеширование
                        it[fullName] = request.fullName
                        it[age] = request.age
                    }

                    insertStatement[Users.id].value   // получаем сгенерированный ID
                }

                call.respond(
                    HttpStatusCode.Created,
                    RegisterResponse(
                        message = "User successfully created",
                        userId = newUserId
                    )
                )
            } catch (e: Exception) {
                println("Registration error: ${e.message}")
                call.respond(
                    HttpStatusCode.BadRequest,
                    RegisterResponse("Error: ${e.message}")
                )
            }
        }
        post("/login") {
            try {
                val request = call.receive<LoginRequest>()

                DatabaseFactory.getConnection().use { conn ->
                    val stmt = conn.prepareStatement("""
                SELECT id, password_hash 
                FROM app.users 
                WHERE username = ? OR email = ?
                LIMIT 1
            """.trimIndent())

                    stmt.setString(1, request.loginOrEmail)
                    stmt.setString(2, request.loginOrEmail)

                    val rs = stmt.executeQuery()

                    if (!rs.next()) {
                        call.respond(
                            HttpStatusCode.NotFound,
                            LoginResponse("Пользователь не найден", null)
                        )
                        return@post
                    }

                    val userId = rs.getLong("id")
                    val storedPassword = rs.getString("password_hash")

                    if (storedPassword == request.password) {
                        call.respond(
                            HttpStatusCode.OK,
                            LoginResponse("Вход успешен", userId)
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            LoginResponse("Неверный пароль", null)
                        )
                    }
                }
            } catch (e: Exception) {
                println("Login error: ${e.message}")
                call.respond(
                    HttpStatusCode.BadRequest,
                    LoginResponse("Ошибка: ${e.message}", null)
                )
            }
        }
    }

}
