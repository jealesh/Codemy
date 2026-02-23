package com.inc.codemy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class ProgrammingFragment : Fragment() {

    companion object {
        private const val ARG_QUESTION = "question"
        private const val ARG_ANSWER = "answer"

        fun newInstance(question: String, correctAnswer: String?): ProgrammingFragment {
            val fragment = ProgrammingFragment()
            fragment.arguments = Bundle().apply {
                putString(ARG_QUESTION, question)
                putString(ARG_ANSWER, correctAnswer)
            }
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_programming, container, false)

        val question = arguments?.getString(ARG_QUESTION) ?: ""
        val correct = arguments?.getString(ARG_ANSWER) ?: ""

        view.findViewById<TextView>(R.id.textQuestion)?.text = question

        val input = view.findViewById<EditText>(R.id.inputCode)
        val btnRun = view.findViewById<Button>(R.id.btnRun)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmit)

        // Запуск кода (простой вывод)
        btnRun.setOnClickListener {
            val code = input.text.toString()
            try {
                val process = Runtime.getRuntime().exec("python -c \"$code\"")
                val output = process.inputStream.bufferedReader().readText()
                val error = process.errorStream.bufferedReader().readText()

                val result = if (error.isNotEmpty()) "Ошибка:\n$error" else "Вывод:\n$output"
                Toast.makeText(requireContext(), result, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Не удалось запустить Python: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        // Отправка на проверку
        btnSubmit.setOnClickListener {
            val code = input.text.toString().trim()
            if (code.equals(correct.trim(), ignoreCase = true)) {
                Toast.makeText(requireContext(), "Правильно! +XP", Toast.LENGTH_SHORT).show()
                // Здесь позже отправим прогресс на сервер
            } else {
                Toast.makeText(requireContext(), "Неправильно. Правильный код:\n$correct", Toast.LENGTH_LONG).show()
            }
        }

        return view
    }
}