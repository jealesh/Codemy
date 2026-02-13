package com.inc.codemy.models

import kotlinx.serialization.Serializable
@Serializable
data class LoginResponse(
    val message: String,
    val userId: Long? = null
)