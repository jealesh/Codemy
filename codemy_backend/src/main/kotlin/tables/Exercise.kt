package tables

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ReferenceOption

/**
 * Таблица упражнений/заданий в уроке
 * Каждое упражнение имеет свой тип и награду в XP
 */
object Exercise : LongIdTable("app.exercise") {
    val lessonId       = long("lesson_id").references(Lessons.id, onDelete = ReferenceOption.CASCADE)
    val orderIndex     = integer("order_index")  // Порядок в уроке
    val type           = varchar("type", 50)     // "theory", "oral_code", "matching", "programming"
    val text           = text("text")            // Текст задания/вопрос
    val correctAnswer  = varchar("correct_answer", 1000).nullable()
    val options        = text("options").nullable()  // JSON array для matching
    val xpReward       = integer("xp_reward").default(0)  // XP за выполнение
    
    init {
        index(customIndexName = "idx_exercise_lesson_id", columns = arrayOf(lessonId))
        uniqueIndex(lessonId, orderIndex)
    }
}
