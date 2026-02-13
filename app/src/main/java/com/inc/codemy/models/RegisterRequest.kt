package com.inc.codemy.models

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val fullName: String,
    val username: String,
    val age: Int,
    val email: String,
    val password: String
)