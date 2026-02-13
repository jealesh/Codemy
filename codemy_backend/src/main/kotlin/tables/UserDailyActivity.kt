package tables

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone
import org.jetbrains.exposed.sql.ReferenceOption

object UserDailyActivity : LongIdTable("app.user_daily_activity") {
    val userId           = long("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val date             = date("date")
    val xpEarned         = integer("xp_earned").default(0)
    val lessonsCompleted = integer("lessons_completed").default(0)
    val streakActive     = integer("streak_active").default(0)

    init {
        uniqueIndex(userId, date)
    }
}