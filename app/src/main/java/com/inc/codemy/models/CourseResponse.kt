package com.inc.codemy.models

import kotlinx.serialization.Serializable

@Serializable
data class CourseResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val iconUrl: String?,
    val isActive: Boolean
)