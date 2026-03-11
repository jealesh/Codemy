package tables

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone
import org.jetbrains.exposed.sql.ReferenceOption

object UserStats : LongIdTable("app.user_stats") {
    val userId        = long("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE).uniqueIndex()
    val totalXp       = long("total_xp").default(0L)
    val level         = integer("level").default(1)
    val weeklyXp      = integer("weekly_xp").default(0)
    val dailyGoal     = integer("daily_goal").default(20)
    val lastResetWeek = date("last_reset_week").nullable()
    val streakCurrent = integer("streak_current").default(0)
    val streakMax     = integer("streak_max").default(0)
    val lastActivityDate = date("last_activity_date").nullable()
    val updatedAt     = timestampWithTimeZone("updated_at")
}