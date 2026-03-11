package com.example

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection
import tables.*  // твои таблицы

object DatabaseFactory {
    private lateinit var dataSource: HikariDataSource

    fun connect() {
        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:postgresql://localhost:5432/codemy"
            username = "codemy_app"
            password = "CodemySecure2026!"
            driverClassName = "org.postgresql.Driver"
            
            // Настройки пула соединений
            minimumIdle = 5
            maximumPoolSize = 20
            idleTimeout = 300000
            maxLifetime = 1800000
            connectionTimeout = 30000
            poolName = "CodemyHikariPool"
            
            // Настройки производительности
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
            addDataSourceProperty("useServerPrepStmts", "true")
        }
        
        dataSource = HikariDataSource(config)
        
        val database = Database.connect(dataSource)

        transaction(database) {
            // Самое важное — устанавливаем схему public для всего этого соединения
            exec("SET search_path TO app;")

            // Теперь создаём таблицы
            SchemaUtils.createMissingTablesAndColumns(
                Users, Courses, Lessons, Exercise, ExerciseProgress,
                UserLessonProgress, UserDailyActivity, UserStats, Achievements, UserAchievements
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

            println("PostgreSQL is connected with HikariCP connection pool.")
        }
    }

    fun getConnection(): Connection {
        return dataSource.connection
    }
    
    fun close() {
        if (::dataSource.isInitialized) {
            dataSource.close()
        }
    }
}
