package com.inc.codemy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_matching, container, false)

        val question = arguments?.getString(ARG_QUESTION) ?: ""
        val options = arguments?.getStringArrayList(ARG_OPTIONS) ?: arrayListOf()
        val correct = arguments?.getString(ARG_CORRECT) ?: ""

        view.findViewById<TextView>(R.id.textQuestion).text = question

        val radioGroup = view.findViewById<RadioGroup>(R.id.radioGroupOptions)
        options.forEachIndexed { index, option ->
            val radio = RadioButton(context)
            radio.text = option
            radio.id = index
            radioGroup.addView(radio)
        }

        view.findViewById<Button>(R.id.btnCheck).setOnClickListener {
            val selectedId = radioGroup.checkedRadioButtonId
            if (selectedId == -1) {
                Toast.makeText(context, "Выберите вариант!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedText = options[selectedId]
            if (selectedText == correct) {
                Toast.makeText(context, "Правильно!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Неправильно. Правильный ответ: $correct", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }
}