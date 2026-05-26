package com.inc.codemy.models

import kotlinx.serialization.Serializable

@Serializable
data class CourseResponse(
    val id: Long,
    val name: String,
    val isActive: Boolean
)