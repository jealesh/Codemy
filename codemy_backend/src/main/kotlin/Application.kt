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
import org.jetbrains.exposed.sql.insert
import java.sql.Connection
import org.jetbrains.exposed.sql.transactions.transaction
import tables.*
import java.time.OffsetDateTime
import java.time.ZoneOffset

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

@Serializable
data class CourseResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val iconUrl: String?,
    val isActive: Boolean
)

@Serializable
data class LessonResponse(
    val id: Long,
    val title: String,
    val orderIndex: Int,
    val estimatedMinutes: Int,
    val xpReward: Int,
    val progress: Int?,
    val completed: Boolean,
    val content: List<LessonSection> = emptyList()
)

@Serializable
data class LessonSection(
    val type: String,           // "theory", "oral_code", "programming", "matching"
    val text: String,           // основной текст / вопрос
    val correctAnswer: String? = null,  // правильный ответ (для задач)
    val options: List<String>? = null,   // варианты для matching
    val id: Long? = null,                // ID упражнения в БД
    val xpReward: Int? = null            // XP награда за упражнение
)

@Serializable
data class LeaderboardUserResponse(
    val userId: Long,
    val username: String,
    val fullName: String?,
    val weeklyXp: Int,
    val rank: Int,
    val avatarUrl: String?
)

@Serializable
data class LeaderboardResponse(
    val weekStart: String,
    val daysRemaining: Int,
    val users: List<LeaderboardUserResponse>,
    val userRank: LeaderboardUserResponse?
)

// ────────────────────────────────────────────────
// Модели для синхронизации прогресса
// ────────────────────────────────────────────────

/**
 * XP награды за типы упражнений
 */
object XPRewards {
    const val ORAL_CODE = 3        // Устная задача
    const val MATCHING = 2         // Соотношение
    const val PROGRAMMING = 5      // Программирование
    const val THEORY = 0           // Теория (без XP)
}

@Serializable
data class ExerciseCompletionRequest(
    val userId: Long,
    val exerciseId: Long,
    val lessonId: Long,
    val exerciseType: String,  // "oral_code", "matching", "programming"
    val isCorrect: Boolean
)

@Serializable
data class TheoryCompletionRequest(
    val userId: Long,
    val exerciseId: Long,
    val lessonId: Long
)

@Serializable
data class ExerciseCompletionResponse(
    val success: Boolean,
    val xpEarned: Int,
    val message: String,
    val totalXp: Long,
    val dailyXp: Int,
    val dailyGoal: Int,
    val alreadyCompleted: Boolean = false
)

@Serializable
data class LessonProgressUpdateRequest(
    val userId: Long,
    val lessonId: Long,
    val progress: Int,          // 0-100
    val isCompleted: Boolean
)

@Serializable
data class LessonProgressResponse(
    val lessonId: Long,
    val progress: Int,
    val isCompleted: Boolean,
    val exercisesCompleted: Int,
    val totalExercises: Int
)

@Serializable
data class UserProgressResponse(
    val userId: Long,
    val totalXp: Long,
    val level: Int,
    val dailyXp: Int,
    val dailyGoal: Int,
    val dailyProgress: Float,
    val lessonsCompleted: Int,
    val exercisesCompleted: Int
)

@Serializable
data class ExerciseProgressResponse(
    val lessonId: Long,
    val completedExerciseIds: List<Long>
)

@Serializable
data class DailyGoalResponse(
    val goal_xp: Int,
    val current_xp: Int,
    val is_completed: Boolean
)

@Serializable
data class DailyGoalUpdateRequest(
    val user_id: Long,
    val goal_xp: Int
)

@Serializable
data class DailyActivityResponse(
    val user_id: Long,
    val date: String,
    val xp_earned: Int,
    val lessons_completed: Int,
    val exercises_completed: Int,
    val streak_active: Int
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

                // Автоматически создаём статистику для нового пользователя
                transaction {
                    UserStats.insert {
                        it[userId] = newUserId
                        it[totalXp] = 0L
                        it[level] = 1
                        it[weeklyXp] = 0
                        it[lastResetWeek] = null
                        it[updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
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

        get("/user/profile/{userId}") {
            val userId = call.parameters["userId"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Неверный userId"))

            try {
                DatabaseFactory.getConnection().use { conn ->
                    val stmt = conn.prepareStatement("""
                        SELECT u.full_name, u.username, us.total_xp, us.level, us.weekly_xp,
                               us.last_reset_week, us.updated_at
                        FROM app.users u
                        LEFT JOIN app.user_stats us ON u.id = us.user_id
                        WHERE u.id = ?
                    """.trimIndent())
                    stmt.setLong(1, userId)
                    val rs = stmt.executeQuery()

                    if (!rs.next()) {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Пользователь не найден"))
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

                    call.respond(response)
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

        // ────────────────────────────────────────────────
        // Теперь без Exposed where/eq — чистый SQL
        get("/courses") {
            try {
                DatabaseFactory.getConnection().use { conn ->
                    val stmt = conn.prepareStatement("""
                        SELECT id, name, description, icon_url, is_active 
                        FROM app.courses 
                        WHERE is_active = true
                        ORDER BY name
                    """.trimIndent())

                    val rs = stmt.executeQuery()
                    val courses = mutableListOf<CourseResponse>()

                    while (rs.next()) {
                        courses.add(
                            CourseResponse(
                                id = rs.getLong("id"),
                                name = rs.getString("name"),
                                description = rs.getString("description"),
                                iconUrl = rs.getString("icon_url"),
                                isActive = rs.getBoolean("is_active")
                            )
                        )
                    }

                    call.respond(HttpStatusCode.OK, courses)
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "unknown error"))
                )
            }
        }

        get("/courses/{courseId}/lessons") {
            val courseId = call.parameters["courseId"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid courseId"))

            val userId = call.request.queryParameters["userId"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "userId required in query"))

            try {
                DatabaseFactory.getConnection().use { conn ->
                    val stmt = conn.prepareStatement("""
                        SELECT l.id, l.title, l.order_index, l.estimated_minutes, l.xp_reward,
                               ulp.progress, ulp.completed_at
                        FROM app.lessons l
                        LEFT JOIN app.user_lesson_progress ulp 
                            ON l.id = ulp.lesson_id AND ulp.user_id = ?
                        WHERE l.course_id = ?
                        ORDER BY l.order_index ASC
                    """.trimIndent())

                    stmt.setLong(1, userId)
                    stmt.setLong(2, courseId)

                    val rs = stmt.executeQuery()
                    val lessons = mutableListOf<LessonResponse>()

                    while (rs.next()) {
                        lessons.add(
                            LessonResponse(
                                id = rs.getLong("id"),
                                title = rs.getString("title"),
                                orderIndex = rs.getInt("order_index"),
                                estimatedMinutes = rs.getInt("estimated_minutes"),
                                xpReward = rs.getInt("xp_reward"),
                                progress = if (rs.wasNull()) 0 else rs.getInt("progress"),
                                completed = !rs.wasNull() && rs.getTimestamp("completed_at") != null
                            )
                        )
                    }

                    // Если нет записей прогресса — возвращаем уроки с progress=0
                    call.respond(HttpStatusCode.OK, lessons)
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "unknown"))
                )
            }
        }
        get("/lessons/{lessonId}/content") {
            val lessonId = call.parameters["lessonId"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid lessonId"))

            val userId = call.request.queryParameters["userId"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "userId required"))

            try {
                DatabaseFactory.getConnection().use { conn ->
                    // Урок
                    val lessonStmt = conn.prepareStatement("""
                SELECT l.id, l.title, l.order_index, l.estimated_minutes, l.xp_reward,
                       ulp.progress, (ulp.completed_at IS NOT NULL) AS completed
                FROM app.lessons l
                LEFT JOIN app.user_lesson_progress ulp ON l.id = ulp.lesson_id AND ulp.user_id = ?
                WHERE l.id = ?
            """.trimIndent())

                    lessonStmt.setLong(1, userId)
                    lessonStmt.setLong(2, lessonId)

                    val lessonRs = lessonStmt.executeQuery()
                    if (!lessonRs.next()) {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Урок не найден"))
                        return@use
                    }

                    // Упражнения
                    val exerciseStmt = conn.prepareStatement("""
                SELECT id, "order", type, text, correct_answer, options, xp_reward
                FROM app.exercise
                WHERE lesson_id = ?
                ORDER BY "order" ASC
            """.trimIndent())

                    exerciseStmt.setLong(1, lessonId)
                    val exerciseRs = exerciseStmt.executeQuery()

                    val content = mutableListOf<LessonSection>()
                    while (exerciseRs.next()) {
                        content.add(
                            LessonSection(
                                id = exerciseRs.getLong("id"),
                                type = exerciseRs.getString("type"),
                                text = exerciseRs.getString("text"),
                                correctAnswer = exerciseRs.getString("correct_answer"),
                                options = if (exerciseRs.getString("options") != null) {
                                    Json.decodeFromString(exerciseRs.getString("options"))
                                } else null,
                                xpReward = exerciseRs.getInt("xp_reward")
                            )
                        )
                    }

                    val lesson = LessonResponse(
                        id = lessonRs.getLong("id"),
                        title = lessonRs.getString("title"),
                        orderIndex = lessonRs.getInt("order_index"),
                        estimatedMinutes = lessonRs.getInt("estimated_minutes"),
                        xpReward = lessonRs.getInt("xp_reward"),
                        progress = if (lessonRs.wasNull()) 0 else lessonRs.getInt("progress"),
                        completed = lessonRs.getBoolean("completed"),
                        content = content
                    )

                    call.respond(HttpStatusCode.OK, lesson)
                }
            } catch (e: Exception) {
                println("Ошибка в /lessons/$lessonId/content: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // ────────────────────────────────────────────────
        // Leaderboard API
        get("/leaderboard/weekly") {
            val userId = call.request.queryParameters["userId"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "userId required"))

            try {
                DatabaseFactory.getConnection().use { conn ->
                    // Получаем текущую неделю (понедельник)
                    val now = java.time.OffsetDateTime.now()
                    val monday = now.with(java.time.DayOfWeek.MONDAY).toLocalDate()
                    val nextMonday = monday.plusWeeks(1)
                    val daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(now.toLocalDate(), nextMonday).toInt()

                    // Топ пользователей за текущую неделю
                    val stmt = conn.prepareStatement("""
                        SELECT u.id, u.username, u.full_name, COALESCE(us.weekly_xp, 0) as weekly_xp,
                               u.avatar_url,
                               RANK() OVER (ORDER BY COALESCE(us.weekly_xp, 0) DESC) as rank
                        FROM app.users u
                        LEFT JOIN app.user_stats us ON u.id = us.user_id
                        ORDER BY weekly_xp DESC
                        LIMIT 100
                    """.trimIndent())

                    val rs = stmt.executeQuery()
                    val users = mutableListOf<LeaderboardUserResponse>()

                    while (rs.next()) {
                        users.add(
                            LeaderboardUserResponse(
                                userId = rs.getLong("id"),
                                username = rs.getString("username"),
                                fullName = rs.getString("full_name"),
                                weeklyXp = rs.getInt("weekly_xp"),
                                rank = rs.getInt("rank"),
                                avatarUrl = rs.getString("avatar_url")
                            )
                        )
                    }

                    // Позиция текущего пользователя
                    var userRank: LeaderboardUserResponse? = null
                    val userStmt = conn.prepareStatement("""
                        SELECT u.id, u.username, u.full_name, COALESCE(us.weekly_xp, 0) as weekly_xp,
                               u.avatar_url,
                               (SELECT COUNT(DISTINCT us2.weekly_xp) + 1
                                FROM app.user_stats us2
                                WHERE us2.weekly_xp > COALESCE(us.weekly_xp, 0)) as rank
                        FROM app.users u
                        LEFT JOIN app.user_stats us ON u.id = us.user_id
                        WHERE u.id = ?
                    """.trimIndent())
                    userStmt.setLong(1, userId)
                    val userRs = userStmt.executeQuery()

                    if (userRs.next()) {
                        userRank = LeaderboardUserResponse(
                            userId = userRs.getLong("id"),
                            username = userRs.getString("username"),
                            fullName = userRs.getString("full_name"),
                            weeklyXp = userRs.getInt("weekly_xp"),
                            rank = userRs.getInt("rank"),
                            avatarUrl = userRs.getString("avatar_url")
                        )
                    }

                    call.respond(
                        HttpStatusCode.OK,
                        LeaderboardResponse(
                            weekStart = monday.toString(),
                            daysRemaining = daysRemaining,
                            users = users,
                            userRank = userRank
                        )
                    )
                }
            } catch (e: Exception) {
                println("Ошибка в /leaderboard/weekly: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // ────────────────────────────────────────────────
        // Add XP endpoint
        post("/user-stats/add-xp") {
            val userId = call.request.queryParameters["userId"]?.toLongOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "userId required"))

            val xpAmount = call.request.queryParameters["xpAmount"]?.toIntOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "xpAmount required"))

            try {
                DatabaseFactory.getConnection().use { conn ->
                    // Обновляем total_xp и weekly_xp
                    conn.prepareStatement("""
                        UPDATE app.user_stats
                        SET total_xp = total_xp + ?,
                            weekly_xp = weekly_xp + ?,
                            updated_at = ?
                        WHERE user_id = ?
                    """.trimIndent()).use { stmt ->
                        stmt.setInt(1, xpAmount)
                        stmt.setInt(2, xpAmount)
                        stmt.setTimestamp(3, java.sql.Timestamp(System.currentTimeMillis()))
                        stmt.setLong(4, userId)
                        stmt.executeUpdate()
                    }

                    call.respond(HttpStatusCode.OK, mapOf("message" to "XP added successfully"))
                }
            } catch (e: Exception) {
                println("Ошибка в /user-stats/add-xp: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // ────────────────────────────────────────────────
        // Завершение упражнения с начислением XP (синхронизация)
        // ────────────────────────────────────────────────
        post("/exercise/complete") {
            try {
                val request = call.receive<ExerciseCompletionRequest>()

                DatabaseFactory.getConnection().use { conn ->
                    // Проверяем, не завершено ли уже это упражнение
                    val checkStmt = conn.prepareStatement("""
                        SELECT is_completed, xp_earned FROM app.exercise_progress
                        WHERE user_id = ? AND exercise_id = ?
                    """.trimIndent())
                    checkStmt.setLong(1, request.userId)
                    checkStmt.setLong(2, request.exerciseId)
                    val checkRs = checkStmt.executeQuery()

                    if (checkRs.next() && checkRs.getBoolean("is_completed")) {
                        // Уже завершено - XP не начисляем повторно
                        call.respond(
                            HttpStatusCode.OK,
                            ExerciseCompletionResponse(
                                success = false,
                                xpEarned = 0,
                                message = "Упражнение уже завершено",
                                totalXp = 0,
                                dailyXp = 0,
                                dailyGoal = 0,
                                alreadyCompleted = true
                            )
                        )
                        return@post
                    }

                    // Определяем XP награду по типу упражнения
                    val xpReward = when (request.exerciseType.lowercase()) {
                        "oral_code" -> XPRewards.ORAL_CODE      // 3 XP
                        "matching" -> XPRewards.MATCHING        // 2 XP
                        "programming" -> XPRewards.PROGRAMMING  // 5 XP
                        else -> 0
                    }

                    // Если ответ неверный - просто записываем попытку без XP
                    val xpToEarn = if (request.isCorrect) xpReward else 0

                    // Вставляем или обновляем прогресс упражнения
                    conn.prepareStatement("""
                        INSERT INTO app.exercise_progress (user_id, exercise_id, lesson_id, is_completed, xp_earned, attempts_count, last_attempt_at, completed_at)
                        VALUES (?, ?, ?, ?, ?, 1, CURRENT_TIMESTAMP, ${if (request.isCorrect) "CURRENT_TIMESTAMP" else "NULL"})
                        ON CONFLICT (user_id, exercise_id) DO UPDATE SET
                            is_completed = ${if (request.isCorrect) "true" else "exercise_progress.is_completed"},
                            xp_earned = GREATEST(exercise_progress.xp_earned, ?),
                            attempts_count = exercise_progress.attempts_count + 1,
                            last_attempt_at = CURRENT_TIMESTAMP,
                            completed_at = ${if (request.isCorrect) "CURRENT_TIMESTAMP" else "exercise_progress.completed_at"}
                    """.trimIndent()).use { stmt ->
                        stmt.setLong(1, request.userId)
                        stmt.setLong(2, request.exerciseId)
                        stmt.setLong(3, request.lessonId)
                        stmt.setBoolean(4, request.isCorrect)
                        stmt.setInt(5, xpToEarn)
                        stmt.setInt(6, xpToEarn)
                        stmt.executeUpdate()
                    }

                    // Начисляем XP в user_stats только если ответ правильный
                    var totalXp: Long = 0
                    if (request.isCorrect && xpReward > 0) {
                        conn.prepareStatement("""
                            UPDATE app.user_stats
                            SET total_xp = total_xp + ?,
                                weekly_xp = weekly_xp + ?,
                                updated_at = CURRENT_TIMESTAMP
                            WHERE user_id = ?
                        """.trimIndent()).use { stmt ->
                            stmt.setInt(1, xpReward)
                            stmt.setInt(2, xpReward)
                            stmt.setLong(3, request.userId)
                            stmt.executeUpdate()
                        }

                        // Получаем обновлённый total_xp
                        val statsStmt = conn.prepareStatement("""
                            SELECT total_xp FROM app.user_stats WHERE user_id = ?
                        """.trimIndent())
                        statsStmt.setLong(1, request.userId)
                        val statsRs = statsStmt.executeQuery()
                        if (statsRs.next()) {
                            totalXp = statsRs.getLong("total_xp")
                        }
                    }

                    // Обновляем user_daily_activity
                    val today = java.time.LocalDate.now()
                    var dailyXp = 0
                    var dailyGoal = 20
                    if (request.isCorrect && xpReward > 0) {
                        conn.prepareStatement("""
                            INSERT INTO app.user_daily_activity (user_id, date, xp_earned, exercises_completed, daily_goal)
                            VALUES (?, ?, ?, 1, 20)
                            ON CONFLICT (user_id, date) DO UPDATE SET
                                xp_earned = user_daily_activity.xp_earned + ?,
                                exercises_completed = user_daily_activity.exercises_completed + 1
                        """.trimIndent()).use { stmt ->
                            stmt.setLong(1, request.userId)
                            stmt.setDate(2, java.sql.Date.valueOf(today))
                            stmt.setInt(3, xpReward)
                            stmt.setInt(4, xpReward)
                            stmt.executeUpdate()
                        }

                        // Получаем дневной прогресс
                        val dailyStmt = conn.prepareStatement("""
                            SELECT xp_earned, daily_goal FROM app.user_daily_activity
                            WHERE user_id = ? AND date = ?
                        """.trimIndent())
                        dailyStmt.setLong(1, request.userId)
                        dailyStmt.setDate(2, java.sql.Date.valueOf(today))
                        val dailyRs = dailyStmt.executeQuery()
                        if (dailyRs.next()) {
                            dailyXp = dailyRs.getInt("xp_earned")
                            dailyGoal = dailyRs.getInt("daily_goal")
                        }
                    }

                    call.respond(
                        HttpStatusCode.OK,
                        ExerciseCompletionResponse(
                            success = request.isCorrect,
                            xpEarned = xpToEarn,
                            message = if (request.isCorrect) "Упражнение завершено! +$xpToEarn XP" else "Попытка записана",
                            totalXp = totalXp,
                            dailyXp = dailyXp,
                            dailyGoal = dailyGoal,
                            alreadyCompleted = false
                        )
                    )
                }
            } catch (e: Exception) {
                println("Ошибка в /exercise/complete: ${e.message}")
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // ────────────────────────────────────────────────
        // Завершение теории (без XP)
        // ────────────────────────────────────────────────
        post("/theory/complete") {
            try {
                val request = call.receive<TheoryCompletionRequest>()

                DatabaseFactory.getConnection().use { conn ->
                    // Проверяем, не завершена ли уже эта теория
                    val checkStmt = conn.prepareStatement("""
                        SELECT is_completed FROM app.exercise_progress
                        WHERE user_id = ? AND exercise_id = ?
                    """.trimIndent())
                    checkStmt.setLong(1, request.userId)
                    checkStmt.setLong(2, request.exerciseId)
                    val checkRs = checkStmt.executeQuery()

                    if (checkRs.next() && checkRs.getBoolean("is_completed")) {
                        // Уже завершена
                        call.respond(
                            HttpStatusCode.OK,
                            ExerciseCompletionResponse(
                                success = true,
                                xpEarned = 0,
                                message = "Теория уже прочитана",
                                totalXp = 0,
                                dailyXp = 0,
                                dailyGoal = 0,
                                alreadyCompleted = true
                            )
                        )
                        return@post
                    }

                    // Вставляем или обновляем прогресс теории
                    conn.prepareStatement("""
                        INSERT INTO app.exercise_progress (user_id, exercise_id, lesson_id, is_completed, xp_earned, attempts_count, last_attempt_at, completed_at)
                        VALUES (?, ?, ?, true, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        ON CONFLICT (user_id, exercise_id) DO UPDATE SET
                            is_completed = true,
                            attempts_count = exercise_progress.attempts_count + 1,
                            last_attempt_at = CURRENT_TIMESTAMP,
                            completed_at = CURRENT_TIMESTAMP
                    """.trimIndent()).use { stmt ->
                        stmt.setLong(1, request.userId)
                        stmt.setLong(2, request.exerciseId)
                        stmt.setLong(3, request.lessonId)
                        stmt.executeUpdate()
                    }

                    call.respond(
                        HttpStatusCode.OK,
                        ExerciseCompletionResponse(
                            success = true,
                            xpEarned = 0,
                            message = "Теория прочитана",
                            totalXp = 0,
                            dailyXp = 0,
                            dailyGoal = 0,
                            alreadyCompleted = false
                        )
                    )
                }
            } catch (e: Exception) {
                println("Ошибка в /theory/complete: ${e.message}")
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // ────────────────────────────────────────────────
        // Обновление прогресса урока
        // ────────────────────────────────────────────────
        post("/lesson/progress") {
            try {
                val request = call.receive<LessonProgressUpdateRequest>()

                DatabaseFactory.getConnection().use { conn ->
                    // Вставляем или обновляем прогресс урока
                    conn.prepareStatement("""
                        INSERT INTO app.user_lesson_progress (user_id, lesson_id, progress, completed_at, last_attempt_at, attempts_count)
                        VALUES (?, ?, ?, ${if (request.isCompleted) "CURRENT_TIMESTAMP" else "NULL"}, CURRENT_TIMESTAMP, 1)
                        ON CONFLICT (user_id, lesson_id) DO UPDATE SET
                            progress = GREATEST(user_lesson_progress.progress, ?),
                            completed_at = ${if (request.isCompleted) "CURRENT_TIMESTAMP" else "user_lesson_progress.completed_at"},
                            last_attempt_at = CURRENT_TIMESTAMP,
                            attempts_count = user_lesson_progress.attempts_count + 1
                    """.trimIndent()).use { stmt ->
                        stmt.setLong(1, request.userId)
                        stmt.setLong(2, request.lessonId)
                        stmt.setInt(3, request.progress)
                        stmt.setInt(4, request.progress)
                        stmt.executeUpdate()
                    }

                    call.respond(HttpStatusCode.OK, mapOf("message" to "Прогресс обновлён"))
                }
            } catch (e: Exception) {
                println("Ошибка в /lesson/progress: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // ────────────────────────────────────────────────
        // Получение прогресса урока
        // ────────────────────────────────────────────────
        get("/lesson/progress") {
            val userId = call.request.queryParameters["userId"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "userId required"))

            val lessonId = call.request.queryParameters["lessonId"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "lessonId required"))

            try {
                DatabaseFactory.getConnection().use { conn ->
                    // Получаем прогресс урока
                    val progressStmt = conn.prepareStatement("""
                        SELECT progress, (completed_at IS NOT NULL) as is_completed
                        FROM app.user_lesson_progress
                        WHERE user_id = ? AND lesson_id = ?
                    """.trimIndent())
                    progressStmt.setLong(1, userId)
                    progressStmt.setLong(2, lessonId)
                    val progressRs = progressStmt.executeQuery()

                    var progress = 0
                    var isCompleted = false

                    if (progressRs.next()) {
                        progress = progressRs.getInt("progress")
                        isCompleted = progressRs.getBoolean("is_completed")
                    }

                    // Считаем выполненные упражнения в уроке
                    val exercisesStmt = conn.prepareStatement("""
                        SELECT COUNT(*) as total, COUNT(CASE WHEN ep.is_completed THEN 1 END) as completed
                        FROM app.exercise e
                        LEFT JOIN app.exercise_progress ep ON e.id = ep.exercise_id AND ep.user_id = ?
                        WHERE e.lesson_id = ?
                    """.trimIndent())
                    exercisesStmt.setLong(1, userId)
                    exercisesStmt.setLong(2, lessonId)
                    val exercisesRs = exercisesStmt.executeQuery()

                    var totalExercises = 0
                    var exercisesCompleted = 0

                    if (exercisesRs.next()) {
                        totalExercises = exercisesRs.getInt("total")
                        exercisesCompleted = exercisesRs.getInt("completed")
                    }

                    call.respond(
                        HttpStatusCode.OK,
                        LessonProgressResponse(
                            lessonId = lessonId,
                            progress = progress,
                            isCompleted = isCompleted,
                            exercisesCompleted = exercisesCompleted,
                            totalExercises = totalExercises
                        )
                    )
                }
            } catch (e: Exception) {
                println("Ошибка в /lesson/progress: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // ────────────────────────────────────────────────
        // Получение списка выполненных упражнений в уроке
        // ────────────────────────────────────────────────
        get("/lesson/exercises-progress") {
            val userId = call.request.queryParameters["userId"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "userId required"))

            val lessonId = call.request.queryParameters["lessonId"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "lessonId required"))

            try {
                DatabaseFactory.getConnection().use { conn ->
                    // Получаем ID выполненных упражнений
                    val stmt = conn.prepareStatement("""
                        SELECT exercise_id FROM app.exercise_progress
                        WHERE user_id = ? AND lesson_id = ? AND is_completed = true
                    """.trimIndent())
                    stmt.setLong(1, userId)
                    stmt.setLong(2, lessonId)
                    val rs = stmt.executeQuery()

                    val completedExerciseIds = mutableListOf<Long>()
                    while (rs.next()) {
                        completedExerciseIds.add(rs.getLong("exercise_id"))
                    }

                    call.respond(
                        HttpStatusCode.OK,
                        ExerciseProgressResponse(
                            lessonId = lessonId,
                            completedExerciseIds = completedExerciseIds
                        )
                    )
                }
            } catch (e: Exception) {
                println("Ошибка в /lesson/exercises-progress: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // ────────────────────────────────────────────────
        // Общий прогресс пользователя
        // ────────────────────────────────────────────────
        get("/user/progress") {
            val userId = call.request.queryParameters["userId"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "userId required"))

            try {
                DatabaseFactory.getConnection().use { conn ->
                    val today = java.time.LocalDate.now()

                    // Получаем общую статистику
                    val statsStmt = conn.prepareStatement("""
                        SELECT total_xp, level FROM app.user_stats WHERE user_id = ?
                    """.trimIndent())
                    statsStmt.setLong(1, userId)
                    val statsRs = statsStmt.executeQuery()

                    var totalXp: Long = 0
                    var level = 1

                    if (statsRs.next()) {
                        totalXp = statsRs.getLong("total_xp")
                        level = statsRs.getInt("level")
                    }

                    // Получаем дневной прогресс
                    var dailyXp = 0
                    var dailyGoal = 20
                    val dailyStmt = conn.prepareStatement("""
                        SELECT xp_earned, daily_goal FROM app.user_daily_activity
                        WHERE user_id = ? AND date = ?
                    """.trimIndent())
                    dailyStmt.setLong(1, userId)
                    dailyStmt.setDate(2, java.sql.Date.valueOf(today))
                    val dailyRs = dailyStmt.executeQuery()

                    if (dailyRs.next()) {
                        dailyXp = dailyRs.getInt("xp_earned")
                        dailyGoal = dailyRs.getInt("daily_goal")
                    }

                    val dailyProgress = if (dailyGoal > 0) dailyXp.toFloat() / dailyGoal else 0f

                    // Считаем завершённые уроки
                    var lessonsCompleted = 0
                    val lessonsStmt = conn.prepareStatement("""
                        SELECT COUNT(*) FROM app.user_lesson_progress
                        WHERE user_id = ? AND completed_at IS NOT NULL
                    """.trimIndent())
                    lessonsStmt.setLong(1, userId)
                    val lessonsRs = lessonsStmt.executeQuery()
                    if (lessonsRs.next()) {
                        lessonsCompleted = lessonsRs.getInt(1)
                    }

                    // Считаем завершённые упражнения
                    var exercisesCompleted = 0
                    val exercisesStmt = conn.prepareStatement("""
                        SELECT COUNT(*) FROM app.exercise_progress
                        WHERE user_id = ? AND is_completed = true
                    """.trimIndent())
                    exercisesStmt.setLong(1, userId)
                    val exercisesRs = exercisesStmt.executeQuery()
                    if (exercisesRs.next()) {
                        exercisesCompleted = exercisesRs.getInt(1)
                    }

                    call.respond(
                        HttpStatusCode.OK,
                        UserProgressResponse(
                            userId = userId,
                            totalXp = totalXp,
                            level = level,
                            dailyXp = dailyXp,
                            dailyGoal = dailyGoal,
                            dailyProgress = dailyProgress,
                            lessonsCompleted = lessonsCompleted,
                            exercisesCompleted = exercisesCompleted
                        )
                    )
                }
            } catch (e: Exception) {
                println("Ошибка в /user/progress: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // ────────────────────────────────────────────────
        // Daily Goal API
        // ────────────────────────────────────────────────

        /**
         * Получить дневную цель и прогресс пользователя
         */
        get("/daily-goal/{userId}") {
            val userId = call.parameters["userId"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "userId required"))

            try {
                DatabaseFactory.getConnection().use { conn ->
                    val today = java.time.LocalDate.now()

                    // Получаем дневную цель из user_stats
                    val statsStmt = conn.prepareStatement("""
                        SELECT daily_goal FROM app.user_stats WHERE user_id = ?
                    """.trimIndent())
                    statsStmt.setLong(1, userId)
                    val statsRs = statsStmt.executeQuery()

                    var dailyGoal = 20 // значение по умолчанию
                    if (statsRs.next()) {
                        dailyGoal = statsRs.getInt("daily_goal")
                    }

                    // Получаем текущий XP за день
                    var currentXp = 0
                    val activityStmt = conn.prepareStatement("""
                        SELECT xp_earned FROM app.user_daily_activity
                        WHERE user_id = ? AND date = ?
                    """.trimIndent())
                    activityStmt.setLong(1, userId)
                    activityStmt.setDate(2, java.sql.Date.valueOf(today))
                    val activityRs = activityStmt.executeQuery()

                    if (activityRs.next()) {
                        currentXp = activityRs.getInt("xp_earned")
                    }

                    call.respond(
                        HttpStatusCode.OK,
                        DailyGoalResponse(
                            goal_xp = dailyGoal,
                            current_xp = currentXp,
                            is_completed = currentXp >= dailyGoal
                        )
                    )
                }
            } catch (e: Exception) {
                println("Ошибка в /daily-goal/{userId}: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        /**
         * Обновить дневную цель пользователя
         */
        post("/daily-goal/update") {
            try {
                val request = call.receive<DailyGoalUpdateRequest>()

                if (request.goal_xp < 10 || request.goal_xp > 200) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Цель должна быть от 10 до 200 XP")
                    )
                }

                DatabaseFactory.getConnection().use { conn ->
                    // Обновляем цель в user_stats
                    conn.prepareStatement("""
                        UPDATE app.user_stats
                        SET daily_goal = ?
                        WHERE user_id = ?
                    """.trimIndent()).use { stmt ->
                        stmt.setInt(1, request.goal_xp)
                        stmt.setLong(2, request.user_id)
                        stmt.executeUpdate()
                    }

                    // Возвращаем обновлённый прогресс
                    val today = java.time.LocalDate.now()
                    var currentXp = 0
                    val activityStmt = conn.prepareStatement("""
                        SELECT xp_earned FROM app.user_daily_activity
                        WHERE user_id = ? AND date = ?
                    """.trimIndent())
                    activityStmt.setLong(1, request.user_id)
                    activityStmt.setDate(2, java.sql.Date.valueOf(today))
                    val activityRs = activityStmt.executeQuery()

                    if (activityRs.next()) {
                        currentXp = activityRs.getInt("xp_earned")
                    }

                    call.respond(
                        HttpStatusCode.OK,
                        DailyGoalResponse(
                            goal_xp = request.goal_xp,
                            current_xp = currentXp,
                            is_completed = currentXp >= request.goal_xp
                        )
                    )
                }
            } catch (e: Exception) {
                println("Ошибка в /daily-goal/update: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        /**
         * Получить дневную активность пользователя
         */
        get("/daily-activity/{userId}") {
            val userId = call.parameters["userId"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "userId required"))

            try {
                DatabaseFactory.getConnection().use { conn ->
                    val today = java.time.LocalDate.now()

                    val stmt = conn.prepareStatement("""
                        SELECT user_id, date, xp_earned, lessons_completed, exercises_completed, streak_active
                        FROM app.user_daily_activity
                        WHERE user_id = ? AND date = ?
                    """.trimIndent())
                    stmt.setLong(1, userId)
                    stmt.setDate(2, java.sql.Date.valueOf(today))
                    val rs = stmt.executeQuery()

                    if (rs.next()) {
                        call.respond(
                            HttpStatusCode.OK,
                            DailyActivityResponse(
                                user_id = rs.getLong("user_id"),
                                date = rs.getDate("date").toString(),
                                xp_earned = rs.getInt("xp_earned"),
                                lessons_completed = rs.getInt("lessons_completed"),
                                exercises_completed = rs.getInt("exercises_completed"),
                                streak_active = rs.getInt("streak_active")
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.OK,
                            DailyActivityResponse(
                                user_id = userId,
                                date = today.toString(),
                                xp_earned = 0,
                                lessons_completed = 0,
                                exercises_completed = 0,
                                streak_active = 0
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                println("Ошибка в /daily-activity/{userId}: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }
    }
}