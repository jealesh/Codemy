package tables

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone
import org.jetbrains.exposed.sql.ReferenceOption

object UserAchievements : LongIdTable("app.user_achievements") {
    val userId        = long("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val achievementId = long("achievement_id").references(Achievements.id)
    val unlockedAt    = timestampWithTimeZone("unlocked_at")

    init {
        uniqueIndex(userId, achievementId)
    }
}