package com.inc.codemy.models

import kotlinx.serialization.Serializable

@Serializable
data class UserStatsResponse(
    val total_xp: Long,
    val level: Int,
    val weekly_xp: Int? = null,
    val last_reset_week: String? = null,
    val updated_at: String? = null
)