package com.inc.codemy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.inc.codemy.models.ExerciseCompletionRequest
import com.inc.codemy.network.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TheoryFragment : Fragment() {

    companion object {
        private const val ARG_TEXT = "text"
        private const val ARG_POSITION = "position"
        private const val ARG_EXERCISE_ID = "exercise_id"
        private const val ARG_LESSON_ID = "lesson_id"
        private const val ARG_USER_ID = "user_id"
        private const val ARG_IS_COMPLETED = "is_completed"

        fun newInstance(text: String, position: Int = -1, exerciseId: Long = -1L, lessonId: Long = -1L, userId: Long = -1L, isCompleted: Boolean = false) =
            TheoryFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TEXT, text)
                    putInt(ARG_POSITION, position)
                    putLong(ARG_EXERCISE_ID, exerciseId)
                    putLong(ARG_LESSON_ID, lessonId)
                    putLong(ARG_USER_ID, userId)
                    putBoolean(ARG_IS_COMPLETED, isCompleted)
                }
            }
    }

    private var isCompleted = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_theory, container, false)
        view.findViewById<TextView>(R.id.textTheory).text = arguments?.getString(ARG_TEXT) ?: ""

        val position = arguments?.getInt(ARG_POSITION) ?: -1
        val exerciseId = arguments?.getLong(ARG_EXERCISE_ID) ?: -1L
        val lessonId = arguments?.getLong(ARG_LESSON_ID) ?: -1L
        val userId = arguments?.getLong(ARG_USER_ID) ?: -1L
        val isAlreadyCompleted = arguments?.getBoolean("isCompleted") ?: false

        val btnNext = view.findViewById<Button>(R.id.btnNextTheory)

        // Если теория уже завершена - скрываем кнопку
        if (isAlreadyCompleted) {
            btnNext.visibility = View.GONE
            isCompleted = true
        } else {
            btnNext.visibility = View.VISIBLE
        }

        // Кнопка "Перейти к следующему шагу" - пользователь подтверждает что прочитал
        btnNext.setOnClickListener {
            if (!isCompleted && position >= 0) {
                isCompleted = true
                (activity as? LessonActivity)?.let { activity ->
                    activity.tabColors[position] = true
                    activity.adapter.setCompleted(position, true)
                }
                saveTheoryProgress(userId, exerciseId, lessonId)
                btnNext.visibility = View.GONE
                // Автоматически переходим к следующей карточке
                (activity as? LessonActivity)?.goToNextCard()
            }
        }

        return view
    }

    private fun saveTheoryProgress(userId: Long, exerciseId: Long, lessonId: Long) {
        if (userId <= 0 || exerciseId <= 0 || lessonId <= 0) return

        CoroutineScope(Dispatchers.Main).launch {
            try {
                withContext(Dispatchers.IO) {
                    ApiClient.apiService.completeTheory(
                        ExerciseCompletionRequest(userId, exerciseId, lessonId, "theory", true)
                    )
                }
            } catch (e: Exception) {
                // Тихо игнорируем ошибку сохранения теории
            }
        }
    }
}