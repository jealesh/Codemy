package com.inc.codemy.models

import kotlinx.serialization.Serializable

@Serializable
data class LeaderboardUserResponse(
    val userId: Long,
    val username: String,
    val fullName: String?,
    val weeklyXp: Int,
    val rank: Int,
    val avatarUrl: String?
)

@Serializable
data class LeaderboardResponse(
    val weekStart: String,
    val daysRemaining: Int,
    val users: List<LeaderboardUserResponse>,
    val userRank: LeaderboardUserResponse?
)
