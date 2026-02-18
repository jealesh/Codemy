package com.inc.codemy.models

import kotlinx.serialization.Serializable

@Serializable
data class LessonResponse(
    val id: Long,
    val title: String,
    val orderIndex: Int,
    val estimatedMinutes: Int,
    val xpReward: Int,
    val progress: Int? = 0,  // Прогресс пользователя (0-100)
    val completed: Boolean = false
)