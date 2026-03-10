package com.inc.codemy.utils

import android.content.Context
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.inc.codemy.models.ExerciseCompletionRequest
import com.inc.codemy.models.ExerciseCompletionResponse
import com.inc.codemy.models.LessonProgressUpdateRequest
import com.inc.codemy.models.XPRewards
import com.inc.codemy.network.ApiClient
import kotlinx.coroutines.launch

/**
 * Менеджер для синхронизации прогресса с сервером
 */
object ProgressSyncManager {

    /**
     * Синхронизировать завершение упражнения с начислением XP
     * @param fragment Fragment для lifecycleScope
     * @param userId ID пользователя
     * @param exerciseId ID упражнения
     * @param lessonId ID урока
     * @param exerciseType Тип упражнения: "oral_code", "matching", "programming"
     * @param isCorrect Правильный ли ответ
     * @param onResult Callback с результатом
     */
    fun syncExerciseCompletion(
        fragment: Fragment,
        userId: Long,
        exerciseId: Long,
        lessonId: Long,
        exerciseType: String,
        isCorrect: Boolean,
        onResult: (ExerciseCompletionResponse) -> Unit
    ) {
        if (exerciseId <= 0) {
            // Если exerciseId не передан, просто показываем сообщение
            val xpReward = getXPReward(exerciseType)
            Toast.makeText(fragment.context, "+$xpReward XP", Toast.LENGTH_SHORT).show()
            onResult(ExerciseCompletionResponse(
                success = true,
                xpEarned = xpReward,
                message = "+$xpReward XP",
                totalXp = 0,
                dailyXp = 0,
                dailyGoal = 0,
                alreadyCompleted = false
            ))
            return
        }

        fragment.lifecycleScope.launch {
            try {
                val request = ExerciseCompletionRequest(
                    userId = userId,
                    exerciseId = exerciseId,
                    lessonId = lessonId,
                    exerciseType = exerciseType,
                    isCorrect = isCorrect
                )
                val response = ApiClient.apiService.completeExercise(request)
                onResult(response)
            } catch (e: Exception) {
                Toast.makeText(fragment.context, "Ошибка синхронизации: ${e.message}", Toast.LENGTH_SHORT).show()
                onResult(ExerciseCompletionResponse(
                    success = false,
                    xpEarned = 0,
                    message = "Ошибка",
                    totalXp = 0,
                    dailyXp = 0,
                    dailyGoal = 0,
                    alreadyCompleted = false
                ))
            }
        }
    }

    /**
     * Обновить прогресс урока
     */
    fun updateLessonProgress(
        fragment: Fragment,
        userId: Long,
        lessonId: Long,
        progress: Int,
        isCompleted: Boolean,
        onResult: (Boolean) -> Unit
    ) {
        fragment.lifecycleScope.launch {
            try {
                val request = LessonProgressUpdateRequest(
                    userId = userId,
                    lessonId = lessonId,
                    progress = progress,
                    isCompleted = isCompleted
                )
                ApiClient.apiService.updateLessonProgress(request)
                onResult(true)
            } catch (e: Exception) {
                Toast.makeText(fragment.context, "Ошибка обновления прогресса", Toast.LENGTH_SHORT).show()
                onResult(false)
            }
        }
    }

    /**
     * Получить XP награду для типа упражнения
     */
    fun getXPReward(exerciseType: String): Int {
        return when (exerciseType.lowercase()) {
            "oral_code" -> XPRewards.ORAL_CODE      // 3 XP
            "matching" -> XPRewards.MATCHING        // 2 XP
            "programming" -> XPRewards.PROGRAMMING  // 5 XP
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
