package tables

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone
import org.jetbrains.exposed.sql.ReferenceOption

object UserLessonProgress : LongIdTable("app.user_lesson_progress") {
    val userId       = long("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val lessonId     = long("lesson_id").references(Lessons.id, onDelete = ReferenceOption.CASCADE)
    val progress     = integer("progress").default(0)
    val completedAt  = timestampWithTimeZone("completed_at").nullable()
    val lastAttemptAt = timestampWithTimeZone("last_attempt_at")
    val attempts     = integer("attempts").default(0)

    init {
        uniqueIndex(userId, lessonId)
    }
}