package tables

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone
import org.jetbrains.exposed.sql.ReferenceOption

object Lessons : LongIdTable("app.lessons") {
    val courseId         = long("course_id").references(Courses.id, onDelete = ReferenceOption.CASCADE)
    val title            = varchar("title", 150)
    val orderIndex       = integer("order_index")
    val contentJson      = text("content_json").nullable()
    val estimatedMinutes = integer("estimated_minutes").default(10)
    val xpReward         = integer("xp_reward").default(10)

    init {
        uniqueIndex(courseId, title)
        uniqueIndex(courseId, orderIndex)
    }
}