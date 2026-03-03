package com.inc.codemy

import android.graphics.drawable.Drawable
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

class MatchingFragment : Fragment() {

    companion object {
        private const val ARG_QUESTION = "question"
        private const val ARG_OPTIONS = "options"
        private const val ARG_CORRECT = "correct"

        fun newInstance(question: String, options: List<String>?, correctAnswer: String?) = MatchingFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_QUESTION, question)
                putStringArrayList(ARG_OPTIONS, ArrayList(options ?: emptyList()))
                putString(ARG_CORRECT, correctAnswer)
            }
        }
    }

    private var selectedPosition = -1
    private var isSolvedCorrectly = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_matching, container, false)

        val question = arguments?.getString(ARG_QUESTION) ?: ""
        val options = arguments?.getStringArrayList(ARG_OPTIONS) ?: arrayListOf()
        val correct = arguments?.getString(ARG_CORRECT) ?: ""

        view.findViewById<TextView>(R.id.textQuestion).text = question

        val optionsContainer = view.findViewById<LinearLayout>(R.id.optionsContainer)
        val btnCheck = view.findViewById<Button>(R.id.btnCheck)
        val btnNext = view.findViewById<Button>(R.id.btnNext)
        val btnRetry = view.findViewById<Button>(R.id.btnRetry)
        val textResult = view.findViewById<TextView>(R.id.textResult)

        // Создаем кастомные RadioButton-подобные view
        options.forEachIndexed { index, option ->
            val optionView = createOptionView(option, index)
            optionView.setOnClickListener {
                // Сбрасываем выделение с предыдущего
                if (selectedPosition != -1) {
                    val previousView = optionsContainer.getChildAt(selectedPosition)
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
            } else {
                textResult.text = "Неправильно.\n\nПопробуйте ещё раз!"
                textResult.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_light))
                textResult.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_result_error)
                btnCheck.isEnabled = false
                btnCheck.alpha = 0.5f
                btnRetry.visibility = View.VISIBLE
                btnNext.visibility = View.GONE
            }
        }

        btnNext.setOnClickListener {
            // Переход на следующий шаг
            (activity as? LessonActivity)?.goToNextCard()
        }

        btnRetry.setOnClickListener {
            // Сбрасываем выделение
            if (selectedPosition != -1) {
                val previousView = optionsContainer.getChildAt(selectedPosition)
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
}
