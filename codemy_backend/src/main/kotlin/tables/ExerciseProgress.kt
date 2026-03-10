package tables

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone
import org.jetbrains.exposed.sql.ReferenceOption

/**
 * Прогресс пользователя по каждому упражнению
 * Предотвращает повторное получение XP за одно и то же упражнение
 */
object ExerciseProgress : LongIdTable("app.exercise_progress") {
    val userId        = long("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val exerciseId    = long("exercise_id").references(Exercise.id, onDelete = ReferenceOption.CASCADE)
    val lessonId      = long("lesson_id").references(Lessons.id, onDelete = ReferenceOption.CASCADE)
    val isCompleted   = bool("is_completed").default(false)     // Выполнено ли
    val xpEarned      = integer("xp_earned").default(0)         // Сколько XP получено
    val attemptsCount = integer("attempts_count").default(0)    // Количество попыток
    val lastAttemptAt = timestampWithTimeZone("last_attempt_at").nullable()
    val completedAt   = timestampWithTimeZone("completed_at").nullable()
    
    init {
        uniqueIndex(userId, exerciseId)  // Уникальная запись на упражнение
        index("user_lesson_idx", false, userId, lessonId)  // Индекс для поиска по уроку
    }
}
