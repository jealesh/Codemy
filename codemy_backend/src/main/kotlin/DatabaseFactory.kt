package com.example

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection
import java.sql.DriverManager
import tables.*  // твои таблицы

object DatabaseFactory {
    fun connect() {
        val driver = "org.postgresql.Driver"
        val url = "jdbc:postgresql://localhost:5432/codemy"
        val user = "codemy_app"
        val password = "CodemySecure2026!"

        val database = Database.connect(url, driver, user, password)

        transaction(database) {
            // Самое важное — устанавливаем схему public для всего этого соединения
            exec("SET search_path TO app;")

            // Теперь создаём таблицы
            SchemaUtils.createMissingTablesAndColumns(
                Users, Courses, Lessons, UserLessonProgress,
                UserDailyActivity, UserStats, Achievements, UserAchievements
            )

            // ALTER дефолтных значений
            exec(
                """
            DO $$
            BEGIN
                ALTER TABLE users ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;
                ALTER TABLE users ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;
                ALTER TABLE courses ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;
                ALTER TABLE user_lesson_progress ALTER COLUMN last_attempt_at SET DEFAULT CURRENT_TIMESTAMP;
                ALTER TABLE user_stats ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;
                ALTER TABLE user_achievements ALTER COLUMN unlocked_at SET DEFAULT CURRENT_TIMESTAMP;
            EXCEPTION WHEN others THEN
                -- если колонка уже имеет дефолт — просто игнорируем
            END $$;
        """.trimIndent()
            )

            println("PostgreSQL is connected. The public schema is installed. Tables are created/updated.")
        }
    }

    fun getConnection(): java.sql.Connection {
        Class.forName("org.postgresql.Driver")
        return java.sql.DriverManager.getConnection(
            "jdbc:postgresql://localhost:5432/codemy",
            "codemy_app",           // или "postgres"
            "CodemySecure2026!"     // твой реальный пароль
        )
    }
}
