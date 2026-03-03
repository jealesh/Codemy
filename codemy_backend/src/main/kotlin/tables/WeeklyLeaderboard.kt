package tables

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone
import org.jetbrains.exposed.sql.ReferenceOption

object WeeklyLeaderboard : LongIdTable("app.weekly_leaderboard") {
    val userId = long("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val weekStart = date("week_start")
    val xpEarned = integer("xp_earned").default(0)
    val rank = integer("rank").nullable()
    val updatedAt = timestampWithTimeZone("updated_at")
    
    init {
        uniqueIndex("user_week_unique", userId, weekStart)
    }
}
