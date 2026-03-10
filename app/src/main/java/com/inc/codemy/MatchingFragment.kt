package com.inc.codemy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.inc.codemy.utils.ProgressSyncManager

class MatchingFragment : Fragment() {

    companion object {
        private const val ARG_QUESTION = "question"
        private const val ARG_OPTIONS = "options"
        private const val ARG_CORRECT = "correct"
        private const val ARG_USER_ID = "user_id"
        private const val ARG_EXERCISE_ID = "exercise_id"
        private const val ARG_LESSON_ID = "lesson_id"
        private const val ARG_POSITION = "position"
        const val EXERCISE_TYPE = "matching"
        const val XP_REWARD = 2

        fun newInstance(
            question: String,
            options: List<String>?,
            correctAnswer: String?,
            userId: Long = 1L,
            exerciseId: Long = 0L,
            lessonId: Long = 0L,
            position: Int = -1,
            isCompleted: Boolean = false
        ) = MatchingFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_QUESTION, question)
                putStringArrayList(ARG_OPTIONS, ArrayList(options ?: emptyList()))
                putString(ARG_CORRECT, correctAnswer)
                putLong(ARG_USER_ID, userId)
                putLong(ARG_EXERCISE_ID, exerciseId)
                putLong(ARG_LESSON_ID, lessonId)
                putInt(ARG_POSITION, position)
                putBoolean("isCompleted", isCompleted)
            }
        }
    }

    private var selectedPosition = -1
    private var isSolvedCorrectly = false
    private var xpAwarded = false
    private val optionViews = mutableListOf<View>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_matching, container, false)

        val question = arguments?.getString(ARG_QUESTION) ?: ""
        val options = arguments?.getStringArrayList(ARG_OPTIONS) ?: arrayListOf()
        val correct = arguments?.getString(ARG_CORRECT) ?: ""
        val position = arguments?.getInt(ARG_POSITION) ?: -1
        val isCompleted = arguments?.getBoolean("isCompleted") ?: false

        view.findViewById<TextView>(R.id.textQuestion).text = question

        val optionsContainer = view.findViewById<LinearLayout>(R.id.optionsContainer)
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
            textResult.visibility = View.GONE
            textResult.text = ""
            selectedPosition = -1
            isSolvedCorrectly = false
            xpAwarded = false
            // Отмечаем как выполненное в адаптере и обновляем цвет
            (activity as? LessonActivity)?.let { activity ->
                activity.adapter.setCompleted(position, true)
                activity.tabColors[position] = true
            }
            // Разблокируем выбор
            for (optionView in optionViews) {
                optionView.isEnabled = true
                optionView.alpha = 1.0f
                optionView.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_option_unselected)
                val optionText = optionView.findViewById<TextView>(android.R.id.text1)
                optionText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            }
        }

        // Создаем кастомные RadioButton-подобные view
        options.forEachIndexed { index, option ->
            val optionView = createOptionView(option, index)
            optionViews.add(optionView)
            
            optionView.setOnClickListener {
                // Если кликнули на уже выбранный - ничего не делаем (только один выбор)
                if (selectedPosition == index) return@setOnClickListener

                // Сбрасываем выделение с предыдущего
                if (selectedPosition != -1) {
                    val previousView = optionViews[selectedPosition]
                    previousView.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_option_unselected)
                    val previousText = previousView.findViewById<TextView>(android.R.id.text1)
                    previousText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                }

                // Выделяем текущий
                optionView.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_option_selected)
                val currentText = optionView.findViewById<TextView>(android.R.id.text1)
                currentText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))

                selectedPosition = index
            }

            optionsContainer.addView(optionView)

            // Добавляем отступ между опциями
            if (index < options.size - 1) {
                val spacer = View(context)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    resources.getDimensionPixelSize(R.dimen.spacing_s)
                )
                spacer.layoutParams = params
                optionsContainer.addView(spacer)
            }
        }

        btnCheck.setOnClickListener {
            if (selectedPosition == -1) {
                Toast.makeText(context, "Выберите вариант ответа!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedText = options[selectedPosition]
            textResult.visibility = View.VISIBLE

            if (selectedText == correct) {
                textResult.text = "Правильно!\n\nВы выбрали: $selectedText"
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
            // Сбрасываем выделение
            if (selectedPosition != -1) {
                val previousView = optionViews[selectedPosition]
                previousView.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_option_unselected)
                val previousText = previousView.findViewById<TextView>(android.R.id.text1)
                previousText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            }
            selectedPosition = -1

            // Сбрасываем результат
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

    private fun createOptionView(text: String, position: Int): LinearLayout {
        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.HORIZONTAL
        layout.setPadding(20, 20, 20, 20)
        layout.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_option_unselected)
        layout.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        // TextView для текста опции
        val textView = TextView(requireContext())
        textView.id = android.R.id.text1
        textView.text = text
        textView.textSize = 16f
        textView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
        val typeface = requireContext().resources.getFont(R.font.rubik)
        textView.typeface = typeface
        textView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        layout.addView(textView)
        return layout
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
