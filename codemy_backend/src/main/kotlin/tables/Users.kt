package tables

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone
import org.jetbrains.exposed.sql.ReferenceOption

object Users : LongIdTable("app.users") {
    val username     = varchar("username", 50).uniqueIndex()
    val email        = varchar("email", 120).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val fullName     = varchar("full_name", 100).nullable()
    val age          = integer("age").nullable()
    val avatarUrl    = varchar("avatar_url", 255).nullable()
    val createdAt    = timestampWithTimeZone("created_at")
    val updatedAt    = timestampWithTimeZone("updated_at")
}