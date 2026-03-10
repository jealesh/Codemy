package com.inc.codemy.models

import kotlinx.serialization.Serializable

@Serializable
data class ExerciseCompletionRequest(
    val userId: Long,
    val exerciseId: Long,
    val lessonId: Long,
    val exerciseType: String,  // "oral_code", "matching", "programming"
    val isCorrect: Boolean
)

@Serializable
data class ExerciseCompletionResponse(
    val success: Boolean,
    val xpEarned: Int,
    val message: String,
    val totalXp: Long,
    val dailyXp: Int,
    val dailyGoal: Int,
    val alreadyCompleted: Boolean = false
)

@Serializable
data class LessonProgressUpdateRequest(
    val userId: Long,
    val lessonId: Long,
    val progress: Int,          // 0-100
    val isCompleted: Boolean
)

@Serializable
data class LessonProgressResponse(
    val lessonId: Long,
    val progress: Int,
    val isCompleted: Boolean,
    val exercisesCompleted: Int,
    val totalExercises: Int
)

@Serializable
data class UserProgressResponse(
    val userId: Long,
    val totalXp: Long,
    val level: Int,
    val dailyXp: Int,
    val dailyGoal: Int,
    val dailyProgress: Float,
    val lessonsCompleted: Int,
    val exercisesCompleted: Int
)

@Serializable
data class ExerciseProgressResponse(
    val lessonId: Long,
    val completedExerciseIds: List<Long>
)
