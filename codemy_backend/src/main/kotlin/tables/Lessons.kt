package tables

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone
import org.jetbrains.exposed.sql.ReferenceOption

object Lessons : LongIdTable("app.lessons") {
    val courseId   = long("course_id").references(Courses.id, onDelete = ReferenceOption.CASCADE)
    val title      = varchar("title", 150)
    val orderIndex = integer("order_index")

    init {
        uniqueIndex(courseId, title)
        uniqueIndex(courseId, orderIndex)
    }
}