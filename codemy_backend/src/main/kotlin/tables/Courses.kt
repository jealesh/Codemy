package tables

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone
import org.jetbrains.exposed.sql.ReferenceOption

object Courses : LongIdTable("app.courses") {
    val name      = varchar("name", 50).uniqueIndex()
    val isActive  = bool("is_active").default(true)
    val createdAt = timestampWithTimeZone("created_at")
}