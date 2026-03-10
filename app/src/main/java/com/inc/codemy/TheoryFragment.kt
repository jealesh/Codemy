package com.inc.codemy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

        fun newInstance(text: String, position: Int = -1, exerciseId: Long = 0L, lessonId: Long = 0L, userId: Long = 1L) =
            TheoryFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TEXT, text)
                    putInt(ARG_POSITION, position)
                    putLong(ARG_EXERCISE_ID, exerciseId)
                    putLong(ARG_LESSON_ID, lessonId)
                    putLong(ARG_USER_ID, userId)
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_theory, container, false)
        view.findViewById<TextView>(R.id.textTheory).text = arguments?.getString(ARG_TEXT) ?: ""

        // Теория считается прочитанной, когда фрагмент показан
        val position = arguments?.getInt(ARG_POSITION) ?: -1
        val exerciseId = arguments?.getLong(ARG_EXERCISE_ID) ?: 0L
        val lessonId = arguments?.getLong(ARG_LESSON_ID) ?: 0L
        val userId = arguments?.getLong(ARG_USER_ID) ?: 1L

        if (position >= 0) {
            (activity as? LessonActivity)?.let { activity ->
                // Проверяем, не отмечено ли уже в tabColors - это главный источник истины
                if (activity.tabColors[position] != true) {
                    // Ещё не отмечено - помечаем
                    activity.tabColors[position] = true
                    activity.adapter.setCompleted(position, true)
                    // Сохраняем прогресс в БД
                    saveTheoryProgress(userId, exerciseId, lessonId)
                }
            }
        }

        return view
    }

    private fun saveTheoryProgress(userId: Long, exerciseId: Long, lessonId: Long) {
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