package com.inc.codemy.models

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val loginOrEmail: String,
    val password: String
)