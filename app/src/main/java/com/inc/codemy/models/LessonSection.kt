package com.inc.codemy.models

import kotlinx.serialization.Serializable

@Serializable
data class LessonSection(
    val id: Long? = null,       // ID упражнения
    val type: String,           // "theory", "oral_code", "programming", "matching"
    val text: String,           // основной текст / вопрос
    val correctAnswer: String? = null,  // правильный ответ (для задач)
    val options: List<String>? = null,   // варианты для matching
    val stdin: String? = null,          // входные данные для programming
    val expectedOutput: String? = null  // ожидаемый вывод для programming
)