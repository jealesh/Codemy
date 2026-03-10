package com.inc.codemy

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.inc.codemy.models.LessonSection

interface CompletionListener {
    fun onExerciseCompleted(position: Int, isCompleted: Boolean)
}

class LessonSectionsPagerAdapter(
    fa: FragmentActivity,
    private val sections: List<LessonSection>,
    private val userId: Long = 1L,
    private val lessonId: Long = 0L,
    private val completedExerciseIds: Set<Long> = emptySet()
) : FragmentStateAdapter(fa) {

    private val completionStatus = BooleanArray(sections.size) { false }
    private var completionListener: CompletionListener? = null

    fun setCompletionListener(listener: CompletionListener?) {
        completionListener = listener
    }

    fun setCompleted(position: Int, isCompleted: Boolean) {
        if (position in 0 until sections.size) {
            completionStatus[position] = isCompleted
            completionListener?.onExerciseCompleted(position, isCompleted)
        }
    }

    fun isCompleted(position: Int): Boolean {
        return if (position in 0 until sections.size) completionStatus[position] else false
    }

    override fun getItemCount(): Int = sections.size

    override fun createFragment(position: Int): Fragment {
        val section = sections[position]
        val exerciseId = section.id ?: 0L
        // Проверяем, выполнено ли упражнение (только для задач, не для теории)
        val isCompleted = completedExerciseIds.contains(exerciseId)
        
        return when (section.type) {
            "theory" -> TheoryFragment.newInstance(section.text, position, exerciseId, lessonId, userId)
            "oral_code" -> OralCodeFragment.newInstance(
                section.text,
                section.correctAnswer,
                userId,
                exerciseId,
                lessonId,
                position,
                isCompleted
            )
            "programming" -> ProgrammingFragment.newInstance(
                section.text,
                section.correctAnswer,
                section.stdin,
                section.expectedOutput,
                userId,
                exerciseId,
                lessonId,
                position,
                isCompleted
            )
            "matching" -> MatchingFragment.newInstance(
                section.text,
                section.options,
                section.correctAnswer,
                userId,
                exerciseId,
                lessonId,
                position,
                isCompleted
            )
            else -> TheoryFragment.newInstance("Неизвестный тип: ${section.type}", position, exerciseId, lessonId, userId)
        }
    }
}