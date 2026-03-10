package com.inc.codemy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.inc.codemy.utils.ProgressSyncManager

class OralCodeFragment : Fragment() {

    companion object {
        private const val ARG_QUESTION = "question"
        private const val ARG_ANSWER = "answer"
        private const val ARG_USER_ID = "user_id"
        private const val ARG_EXERCISE_ID = "exercise_id"
        private const val ARG_LESSON_ID = "lesson_id"
        private const val ARG_POSITION = "position"
        const val EXERCISE_TYPE = "oral_code"
        const val XP_REWARD = 3

        fun newInstance(
            question: String,
            correctAnswer: String?,
            userId: Long = 1L,
            exerciseId: Long = 0L,
            lessonId: Long = 0L,
            position: Int = -1,
            isCompleted: Boolean = false
        ) = OralCodeFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_QUESTION, question)
                putString(ARG_ANSWER, correctAnswer)
                putLong(ARG_USER_ID, userId)
                putLong(ARG_EXERCISE_ID, exerciseId)
                putLong(ARG_LESSON_ID, lessonId)
                putInt(ARG_POSITION, position)
                putBoolean("isCompleted", isCompleted)
            }
        }
    }

    private var isSolvedCorrectly = false
    private var xpAwarded = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_oral_code, container, false)

        val question = arguments?.getString(ARG_QUESTION) ?: ""
        val correct = arguments?.getString(ARG_ANSWER) ?: ""
        val position = arguments?.getInt(ARG_POSITION) ?: -1
        val isCompleted = arguments?.getBoolean("isCompleted") ?: false

        view.findViewById<TextView>(R.id.textQuestion).text = question

        val input = view.findViewById<EditText>(R.id.inputAnswer)
        val btnCheck = view.findViewById<Button>(R.id.btnCheck)
        val btnNext = view.findViewById<Button>(R.id.btnNext)
        val btnRetry = view.findViewById<Button>(R.id.btnRetry)
        val textResult = view.findViewById<TextView>(R.id.textResult)
        val textXpReward = view.findViewById<TextView>(R.id.textXpReward)

        // Если упражнение уже выполнено - показываем состояние завершения
        // Но позволяем решить повторно
        if (isCompleted) {
            btnCheck.visibility = View.VISIBLE
            btnNext.visibility = View.GONE
            btnRetry.visibility = View.GONE
            input.isEnabled = true
            textResult.visibility = View.GONE
            textResult.text = ""
            // Отмечаем как выполненное в адаптере и обновляем цвет
            (activity as? LessonActivity)?.let { activity ->
                activity.adapter.setCompleted(position, true)
                activity.tabColors[position] = true
            }
        }

        btnCheck.setOnClickListener {
            val answer = input.text.toString().trim()

            if (answer.isEmpty()) {
                Toast.makeText(context, "Введите ответ!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            textResult.visibility = View.VISIBLE

            if (answer.equals(correct.trim(), ignoreCase = true)) {
                textResult.text = "Правильно!\n\nВаш ответ: $answer"
                textResult.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_light))
                textResult.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_result_success)
                isSolvedCorrectly = true
                btnCheck.visibility = View.GONE
                btnNext.visibility = View.VISIBLE
                btnRetry.visibility = View.VISIBLE

                // Отмечаем упражнение как выполненное
                if (position >= 0) {
                    (activity as? LessonActivity)?.adapter?.setCompleted(position, true)
                }

                // Синхронизируем прогресс и начисляем XP
                if (!xpAwarded) {
                    syncProgress(isCorrect = true)
                    xpAwarded = true
                }
            } else {
                textResult.text = "Неправильно.\n\nПопробуйте ещё раз!"
                textResult.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_light))
                textResult.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_result_error)
                btnCheck.isEnabled = false
                btnCheck.alpha = 0.5f
                btnRetry.visibility = View.VISIBLE
                btnNext.visibility = View.GONE
                
                // Записываем попытку без XP
                syncProgress(isCorrect = false)
            }
        }

        btnNext.setOnClickListener {
            // Переход на следующий шаг
            (activity as? LessonActivity)?.goToNextCard()
        }

        btnRetry.setOnClickListener {
            // Сбрасываем всё для новой попытки
            input.text?.clear()
            textResult.visibility = View.GONE
            textResult.text = ""
            isSolvedCorrectly = false
            btnCheck.visibility = View.VISIBLE
            btnCheck.isEnabled = true
            btnCheck.alpha = 1.0f
            btnNext.visibility = View.GONE
            btnRetry.visibility = View.GONE
        }

        return view
    }

    private fun syncProgress(isCorrect: Boolean) {
        val userId = arguments?.getLong(ARG_USER_ID) ?: 1L
        val exerciseId = arguments?.getLong(ARG_EXERCISE_ID) ?: 0L
        val lessonId = arguments?.getLong(ARG_LESSON_ID) ?: 0L

        ProgressSyncManager.syncExerciseCompletion(
            fragment = this,
            userId = userId,
            exerciseId = exerciseId,
            lessonId = lessonId,
            exerciseType = EXERCISE_TYPE,
            isCorrect = isCorrect
        ) { response ->
            if (response.success && isCorrect) {
                if (response.alreadyCompleted) {
                    Toast.makeText(context, "Упражнение уже завершено", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, response.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}