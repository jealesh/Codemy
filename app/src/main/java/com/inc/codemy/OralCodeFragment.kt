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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_oral_code, container, false)

        val question = arguments?.getString(ARG_QUESTION) ?: ""
        val correct = arguments?.getString(ARG_ANSWER) ?: ""

        view.findViewById<TextView>(R.id.textQuestion).text = question

        val input = view.findViewById<EditText>(R.id.inputAnswer)
        view.findViewById<Button>(R.id.btnCheck).setOnClickListener {
            val answer = input.text.toString().trim()
            if (answer.equals(correct.trim(), ignoreCase = true)) {
                Toast.makeText(context, "Правильно!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Неправильно. Ответ: $correct", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }
}