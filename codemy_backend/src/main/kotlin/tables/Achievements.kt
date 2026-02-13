package tables

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone
import org.jetbrains.exposed.sql.ReferenceOption

object Achievements : LongIdTable("app.achievements") {
    val code        = varchar("code", 50).uniqueIndex()
    val name        = varchar("name", 100)
    val description = text("description").nullable()
    val iconUrl     = varchar("icon_url", 255).nullable()
    val xpBonus     = integer("xp_bonus").default(0)
}