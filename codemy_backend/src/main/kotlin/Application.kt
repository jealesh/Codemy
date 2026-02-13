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

// Exposed только для /register
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import tables.Users
import tables.UserStats

// Модели
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

// ← Модель для ответа статистики (обязательно @Serializable)
@Serializable
data class UserStatsResponse(
    val total_xp: Long,
    val level: Int,
    val weekly_xp: Int,
    val last_reset_week: String? = null,
    val updated_at: String? = null
)

@Serializable
data class UserProfileResponse(
    val full_name: String = "",
    val username: String = "",
    val total_xp: Long = 0,
    val level: Int = 1,
    val weekly_xp: Int = 0,
    val last_reset_week: String? = null,
    val updated_at: String? = null
)

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

fun Application.module() {
    DatabaseFactory.connect()

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
                        it[passwordHash] = request.password
                        it[fullName] = request.fullName
                        it[age] = request.age
                    }
                    insertStatement[Users.id].value
                }

                // ← Автоматически создаём статистику для нового пользователя
                transaction {
                    UserStats.insert {
                        it[userId] = newUserId
                        it[totalXp] = 0
                        it[level] = 1
                        it[weeklyXp] = 0
                        it[lastResetWeek] = null
                        it[updatedAt] = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC)
                    }
                }

                call.respond(
                    HttpStatusCode.Created,
                    RegisterResponse("User successfully created", newUserId)
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

        // ← Только один маршрут для статистики (JDBC)
        get("/user/profile/{userId}") {
            val userId = call.parameters["userId"]?.toLongOrNull() ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Неверный userId")
            )

            try {
                DatabaseFactory.getConnection().use { conn ->
                    val stmt = conn.prepareStatement("""
                SELECT 
                    u.full_name, u.username,
                    us.total_xp, us.level, us.weekly_xp, us.last_reset_week, us.updated_at
                FROM app.users u
                LEFT JOIN app.user_stats us ON u.id = us.user_id
                WHERE u.id = ?
            """.trimIndent())

                    stmt.setLong(1, userId)

                    val rs = stmt.executeQuery()

                    if (!rs.next()) {
                        call.respond(
                            HttpStatusCode.NotFound,
                            mapOf("error" to "Пользователь не найден")
                        )
                        return@get
                    }

                    val response = UserProfileResponse(
                        full_name = rs.getString("full_name") ?: "",
                        username = rs.getString("username") ?: "",
                        total_xp = rs.getLong("total_xp"),
                        level = rs.getInt("level"),
                        weekly_xp = rs.getInt("weekly_xp"),
                        last_reset_week = rs.getDate("last_reset_week")?.toString(),
                        updated_at = rs.getTimestamp("updated_at")?.toString()
                    )

                    call.respond(response)  // ← теперь объект data class
                }
            } catch (e: Exception) {
                println("Profile error for user $userId: ${e.message}")
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Ошибка сервера: ${e.message}")
                )
            }
        }
    }
}