package com.inc.codemy.models

import kotlinx.serialization.Serializable

@Serializable
data class UserProfileResponse(
    val full_name: String = "",
    val username: String = "",
    val total_xp: Long = 0,
    val level: Int = 1,
    val weekly_xp: Int = 0,
    val last_reset_week: String? = null,
    val updated_at: String? = null
)