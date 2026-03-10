package tables

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone
import org.jetbrains.exposed.sql.ReferenceOption

/**
 * Прогресс пользователя по уроку (разделам курса)
 * Отслеживает общий прогресс по уроку
 */
object UserLessonProgress : LongIdTable("app.user_lesson_progress") {
    val userId        = long("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val lessonId      = long("lesson_id").references(Lessons.id, onDelete = ReferenceOption.CASCADE)
    val progress      = integer("progress").default(0)       // Процент выполнения (0-100)
    val completedAt   = timestampWithTimeZone("completed_at").nullable()  // Когда завершён
    val lastAttemptAt = timestampWithTimeZone("last_attempt_at")  // Последняя попытка
    val attemptsCount = integer("attempts_count").default(0)  // Количество попыток
    
    init {
        uniqueIndex(userId, lessonId)
    }
}