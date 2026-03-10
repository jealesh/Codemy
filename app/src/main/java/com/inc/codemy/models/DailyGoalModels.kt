package com.inc.codemy.models

import kotlinx.serialization.Serializable

@Serializable
data class DailyGoalResponse(
    val goal_xp: Int,
    val current_xp: Int,
    val is_completed: Boolean
)

@Serializable
data class DailyGoalUpdateRequest(
    val user_id: Long,
    val goal_xp: Int
)

@Serializable
data class DailyActivityResponse(
    val user_id: Long,
    val date: String,
    val xp_earned: Int,
    val lessons_completed: Int,
    val exercises_completed: Int,
    val streak_active: Int
)
