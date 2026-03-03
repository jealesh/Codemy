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

class OralCodeFragment : Fragment() {

    companion object {
        private const val ARG_QUESTION = "question"
        private const val ARG_ANSWER = "answer"

        fun newInstance(question: String, correctAnswer: String?) = OralCodeFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_QUESTION, question)
                putString(ARG_ANSWER, correctAnswer)
            }
        }
    }

    private var isSolvedCorrectly = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_oral_code, container, false)

        val question = arguments?.getString(ARG_QUESTION) ?: ""
        val correct = arguments?.getString(ARG_ANSWER) ?: ""

        view.findViewById<TextView>(R.id.textQuestion).text = question

        val input = view.findViewById<EditText>(R.id.inputAnswer)
        val btnCheck = view.findViewById<Button>(R.id.btnCheck)
        val btnNext = view.findViewById<Button>(R.id.btnNext)
        val btnRetry = view.findViewById<Button>(R.id.btnRetry)
        val textResult = view.findViewById<TextView>(R.id.textResult)

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
}