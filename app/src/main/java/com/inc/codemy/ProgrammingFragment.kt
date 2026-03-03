package com.inc.codemy

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class ProgrammingFragment : Fragment() {

    companion object {
        private const val ARG_QUESTION = "question"
        private const val ARG_ANSWER = "answer"
        private const val ARG_STDIN = "stdin"
        private const val ARG_EXPECTED_OUTPUT = "expectedOutput"

        fun newInstance(
            question: String,
            correctAnswer: String?,
            stdin: String? = null,
            expectedOutput: String? = null
        ): ProgrammingFragment {
            val fragment = ProgrammingFragment()
            fragment.arguments = Bundle().apply {
                putString(ARG_QUESTION, question)
                putString(ARG_ANSWER, correctAnswer)
                putString(ARG_STDIN, stdin)
                putString(ARG_EXPECTED_OUTPUT, expectedOutput)
            }
            return fragment
        }
    }

    private var isTestPassed = false
    private var isPyodideReady = false
    private var isSolvedCorrectly = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_programming, container, false)

        val question = arguments?.getString(ARG_QUESTION) ?: ""
        val correct = arguments?.getString(ARG_ANSWER) ?: ""
        val expectedStdin = arguments?.getString(ARG_STDIN) ?: ""
        val expectedOutput = arguments?.getString(ARG_EXPECTED_OUTPUT) ?: ""

        view.findViewById<TextView>(R.id.textQuestion)?.text = question

        val inputCode = view.findViewById<EditText>(R.id.inputCode)
        val inputStdin = view.findViewById<EditText>(R.id.inputStdin)
        val btnRun = view.findViewById<Button>(R.id.btnRun)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmit)
        val btnNext = view.findViewById<Button>(R.id.btnNext)
        val btnRetry = view.findViewById<Button>(R.id.btnRetry)
        val textTestResult = view.findViewById<TextView>(R.id.textTestResult)
        val textTestLabel = view.findViewById<TextView>(R.id.textTestLabel)
        val textExpectedOutput = view.findViewById<TextView>(R.id.textExpectedOutput)
        val webView = view.findViewById<WebView>(R.id.webViewPyodide)

        // Показываем ожидаемый вывод если есть
        if (expectedOutput.isNotEmpty()) {
            textExpectedOutput.visibility = View.VISIBLE
            textExpectedOutput.text = "💡 Ожидаемый вывод:\n$expectedOutput"
        }

        // Предзаполняем stdin если есть
        if (expectedStdin.isNotEmpty()) {
            inputStdin.setText(expectedStdin)
        }

        // Настройка WebView для Pyodide
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isPyodideReady = true
            }
        }
        webView.loadUrl("file:///android_res/raw/pyodide_runner.html")

        // Кнопка "Тест" - запускаем код через Pyodide
        btnRun.setOnClickListener {
            val code = inputCode.text.toString()
            val stdin = inputStdin.text.toString()

            if (code.isEmpty()) {
                Toast.makeText(requireContext(), "Введите код!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isPyodideReady) {
                textTestLabel.visibility = View.VISIBLE
                textTestResult.visibility = View.VISIBLE
                textTestResult.text = "⏳ Python загружается...\nПодождите несколько секунд."
                textTestResult.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_orange_light))
                return@setOnClickListener
            }

            // Показываем результат теста
            textTestLabel.visibility = View.VISIBLE
            textTestResult.visibility = View.VISIBLE
            textTestResult.text = "⏳ Выполняется..."
            textTestResult.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))

            // Выполняем код через Pyodide
            webView.evaluateJavascript("runPythonWithInput(`$code`, `$stdin`)", { result ->
                val cleanResult = result
                    ?.replace("\"", "")
                    ?.replace("\\n", "\n")
                    ?.replace("\\r", "")
                    ?.trim() ?: ""

                textTestResult.text = "Вывод:\n$cleanResult"

                // Проверяем совпадение с ожидаемым выводом
                if (expectedOutput.isNotEmpty()) {
                    if (cleanResult == expectedOutput.trim()) {
                        isTestPassed = true
                        textTestResult.append("\n\nТест пройден!")
                        textTestResult.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_light))
                        textTestResult.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_result_success)
                        Toast.makeText(requireContext(), "Тест пройден!", Toast.LENGTH_SHORT).show()
                        btnRetry.visibility = View.GONE
                    } else {
                        isTestPassed = false
                        textTestResult.append("\n\nВывод не совпадает с ожидаемым")
                        textTestResult.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_light))
                        textTestResult.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_result_error)
                        Toast.makeText(requireContext(), "Вывод не совпадает", Toast.LENGTH_SHORT).show()
                        btnRetry.visibility = View.VISIBLE
                    }
                } else {
                    // Если нет ожидаемого вывода, просто показываем результат
                    textTestResult.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_light))
                    textTestResult.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_result_success)
                    isTestPassed = true
                    btnRetry.visibility = View.GONE
                }
            })
        }

        // Кнопка Retry - сбрасывает всё для новой попытки
        btnRetry.setOnClickListener {
            inputCode.text?.clear()
            if (expectedStdin.isNotEmpty()) {
                inputStdin.setText(expectedStdin)
            } else {
                inputStdin.text?.clear()
            }
            textTestLabel.visibility = View.GONE
            textTestResult.visibility = View.GONE
            textTestResult.text = ""
            isTestPassed = false
            isSolvedCorrectly = false
            btnRetry.visibility = View.GONE
            btnNext.visibility = View.GONE
            // Возвращаем кнопки теста и отправки
            btnRun.visibility = View.VISIBLE
            btnRun.isEnabled = true
            btnRun.alpha = 1.0f
            btnSubmit.visibility = View.VISIBLE
            btnSubmit.isEnabled = true
            btnSubmit.alpha = 1.0f
            btnSubmit.text = "Отправить"
        }

        // Кнопка Next - переход на следующий шаг
        btnNext.setOnClickListener {
            // Переход на следующий шаг - позже будет навигация
            Toast.makeText(requireContext(), "Переход на следующий шаг...", Toast.LENGTH_SHORT).show()
            // TODO: Добавить навигацию на следующую карточку
        }

        // Отправка на проверку
        btnSubmit.setOnClickListener {
            val code = inputCode.text.toString().trim()

            if (code.isEmpty()) {
                Toast.makeText(requireContext(), "Введите код!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (expectedOutput.isNotEmpty() && !isTestPassed) {
                Toast.makeText(
                    requireContext(),
                    "Сначала пройдите тест!\nСравните ваш вывод с ожидаемым.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            // Проверяем код на соответствие правильному ответу
            if (code.equals(correct.trim(), ignoreCase = true)) {
                Toast.makeText(requireContext(), "Правильно! +XP", Toast.LENGTH_SHORT).show()
                isSolvedCorrectly = true
                btnSubmit.isEnabled = false
                btnSubmit.alpha = 0.5f
                btnSubmit.text = "Выполнено"
                btnRun.isEnabled = false
                btnRun.alpha = 0.5f
                btnSubmit.visibility = View.GONE
                btnRun.visibility = View.GONE
                btnNext.visibility = View.VISIBLE
                btnRetry.visibility = View.VISIBLE
            } else {
                Toast.makeText(requireContext(), "Код не совпадает с правильным решением", Toast.LENGTH_LONG).show()
                btnRetry.visibility = View.VISIBLE
                btnNext.visibility = View.GONE
            }
        }

        btnNext.setOnClickListener {
            // Переход на следующий шаг
            (activity as? LessonActivity)?.goToNextCard()
        }

        return view
    }
}
