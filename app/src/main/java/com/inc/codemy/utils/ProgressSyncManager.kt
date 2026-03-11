package com.inc.codemy.utils

import android.content.Context
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.inc.codemy.models.CourseResponse
import com.inc.codemy.models.DailyGoalResponse
import com.inc.codemy.models.ExerciseCompletionRequest
import com.inc.codemy.models.ExerciseCompletionResponse
import com.inc.codemy.models.LessonProgressUpdateRequest
import com.inc.codemy.models.LessonResponse
import com.inc.codemy.models.UserProfileResponse
import com.inc.codemy.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Менеджер для синхронизации прогресса с сервером + кэширование данных
 */
object ProgressSyncManager {

    /**
     * Синхронизировать завершение упражнения с начислением XP
     */
    suspend fun syncExerciseCompletionAsync(
        userId: Long,
        exerciseId: Long,
        lessonId: Long,
        exerciseType: String,
        isCorrect: Boolean
    ): ExerciseCompletionResponse {
        if (exerciseId <= 0) {
            val xpReward = getXPReward(exerciseType)
            return ExerciseCompletionResponse(
                success = true,
                xpEarned = xpReward,
                message = "+$xpReward XP",
                totalXp = 0,
                dailyXp = 0,
                dailyGoal = 0,
                alreadyCompleted = false
            )
        }

        return try {
            val request = ExerciseCompletionRequest(
                userId = userId,
                exerciseId = exerciseId,
                lessonId = lessonId,
                exerciseType = exerciseType,
                isCorrect = isCorrect
            )
            ApiClient.apiService.completeExercise(request)
        } catch (e: Exception) {
            ExerciseCompletionResponse(
                success = false,
                xpEarned = 0,
                message = "Ошибка",
                totalXp = 0,
                dailyXp = 0,
                dailyGoal = 0,
                alreadyCompleted = false
            )
        }
    }

    /**
     * Обновить прогресс урока
     */
    suspend fun updateLessonProgressAsync(
        userId: Long,
        lessonId: Long,
        progress: Int,
        isCompleted: Boolean
    ): Boolean {
        return try {
            val request = LessonProgressUpdateRequest(
                userId = userId,
                lessonId = lessonId,
                progress = progress,
                isCompleted = isCompleted
            )
            ApiClient.apiService.updateLessonProgress(request)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Получить XP награду для типа упражнения
     */
    fun getXPReward(exerciseType: String): Int {
        return when (exerciseType.lowercase()) {
            "oral_code" -> 3
            "matching" -> 2
            "programming" -> 5
            else -> 0
        }
    }

    /**
     * Получить тип упражнения из типа секции
     */
    fun getExerciseType(sectionType: String): String {
        return when (sectionType.lowercase()) {
            "oral_code" -> "oral_code"
            "matching" -> "matching"
            "programming" -> "programming"
            else -> "theory"
        }
    }
}

/**
 * In-memory кэш для часто используемых данных
 * Использует ConcurrentHashMap для потокобезопасности
 */
object DataCache {

    data class CacheEntry<T>(
        val data: T,
        val timestamp: Long,
        val ttlMs: Long
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() - timestamp > ttlMs
    }

    // Используем star projection для хранения разных типов
    private val cache = ConcurrentHashMap<String, CacheEntry<*>>()

    /**
     * Получить данные из кэша или загрузить через loader
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun <T> getOrLoad(
        key: String,
        ttlMs: Long = 60_000L, // 1 минута по умолчанию
        loader: suspend () -> T
    ): T {
        val entry = cache[key] as? CacheEntry<T>
        if (entry != null && !entry.isExpired()) {
            return entry.data
        }

        val data = loader()
        cache[key] = CacheEntry(data, System.currentTimeMillis(), ttlMs)
        return data
    }

    /**
     * Положить данные в кэш
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> put(key: String, data: T, ttlMs: Long = 60_000L) {
        cache[key] = CacheEntry(data, System.currentTimeMillis(), ttlMs) as CacheEntry<*>
    }

    /**
     * Получить данные из кэша (null если нет или истёк срок)
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): T? {
        val entry = cache[key]
        if (entry == null || entry.isExpired()) {
            cache.remove(key)
            return null
        }
        return (entry as? CacheEntry<T>)?.data
    }

    /**
     * Удалить данные из кэша
     */
    fun remove(key: String) {
        cache.remove(key)
    }

    /**
     * Очистить весь кэш
     */
    fun clear() {
        cache.clear()
    }

    /**
     * Очистить просроченные записи
     */
    fun cleanupExpired() {
        val now = System.currentTimeMillis()
        cache.entries.removeAll { it.value.isExpired() }
    }
}

/**
 * Кэш для пользовательских данных
 */
object UserDataCache {

    private val dailyGoals = ConcurrentHashMap<Long, DailyGoalResponse>()
    private val userProfiles = ConcurrentHashMap<Long, UserProfileResponse>()
    private val courses = mutableListOf<CourseResponse>()
    private val lessons = ConcurrentHashMap<Long, List<LessonResponse>>()

    fun getDailyGoal(userId: Long): DailyGoalResponse? = dailyGoals[userId]
    fun putDailyGoal(userId: Long, data: DailyGoalResponse) { dailyGoals[userId] = data }
    fun removeDailyGoal(userId: Long) { dailyGoals.remove(userId) }

    fun getUserProfile(userId: Long): UserProfileResponse? = userProfiles[userId]
    fun putUserProfile(userId: Long, data: UserProfileResponse) { userProfiles[userId] = data }
    fun removeUserProfile(userId: Long) { userProfiles.remove(userId) }

    fun getCourses(): List<CourseResponse> = courses.toList()
    fun putCourses(data: List<CourseResponse>) {
        courses.clear()
        courses.addAll(data)
    }
    fun clearCourses() { courses.clear() }

    fun getLessons(courseId: Long): List<LessonResponse>? = lessons[courseId]
    fun putLessons(courseId: Long, data: List<LessonResponse>) { lessons[courseId] = data }
    fun removeLessons(courseId: Long) { lessons.remove(courseId) }

    fun clear() {
        dailyGoals.clear()
        userProfiles.clear()
        courses.clear()
        lessons.clear()
    }
}
