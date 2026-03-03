package com.inc.codemy

data class LeagueUser(
    val name: String,
    val weeklyXP: Int,
    val rank: Int = 0,
    val isCurrentUser: Boolean = false
)
