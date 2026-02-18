package com.inc.codemy

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class SandboxActivity : AppCompatActivity() {

    private lateinit var languageSpinner: Spinner
    private lateinit var inputCode: EditText
    private lateinit var btnRunCode: Button
    private lateinit var webViewPyodide: WebView
    private lateinit var outputResult: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sandbox)

        languageSpinner = findViewById(R.id.languageSpinner)
        inputCode = findViewById(R.id.inputCode)
        btnRunCode = findViewById(R.id.btnRunCode)
        webViewPyodide = findViewById(R.id.webViewPyodide)
        outputResult = findViewById(R.id.outputResult)

        // Настройка Spinner
        val languages = arrayOf("Python", "JavaScript")
        languageSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)

        // Настройка WebView для Pyodide
        webViewPyodide.settings.javaScriptEnabled = true
        webViewPyodide.settings.domStorageEnabled = true
        webViewPyodide.loadUrl("file:///android_res/raw/pyodide_runner.html")

        languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val lang = languages[position]

                if (lang == "JavaScript") {
                    startActivity(Intent(this@SandboxActivity, SandboxJsActivity::class.java))
                    // НЕ добавляем finish() — чтобы можно было вернуться назад
                    return
                }

                // Если Python — остаёмся здесь
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Кнопка "Выполнить" — только для Python (JS теперь в отдельной активности)
        btnRunCode.setOnClickListener {
            val code = inputCode.text.toString().trim()
            val lang = languageSpinner.selectedItem.toString()

            if (code.isEmpty()) {
                outputResult.text = "Напиши код!"
                outputResult.setTextColor(
                    ContextCompat.getColor(
                        this,
                        android.R.color.holo_red_light
                    )
                )
                return@setOnClickListener
            }

            when (lang) {
                "Python" -> runPython(code)
                "JavaScript" -> {
                    // Эта ветка больше не нужна, т.к. переход происходит в спиннере
                    // Но оставляем на всякий случай
                    startActivity(Intent(this, SandboxJsActivity::class.java))
                    finish()
                }
            }
        }

        // Навигация
        findViewById<TextView>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, MainScreenActivity::class.java))
        }

        findViewById<TextView>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        findViewById<TextView>(R.id.navTrophy).setOnClickListener {
            startActivity(Intent(this, LeagueActivity::class.java))
        }
    }

    private fun runPython(code: String) {
        webViewPyodide.visibility = View.VISIBLE
        findViewById<ScrollView>(R.id.scrollOutput).visibility = View.GONE

        webViewPyodide.evaluateJavascript("runPython(`$code`)", { result ->
            val cleanResult = result
                ?.replace("\"", "")
                ?.replace("\\n", "\n")
                ?.replace("\\r", "") ?: "Нет вывода"

            outputResult.text = cleanResult
            outputResult.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
            findViewById<ScrollView>(R.id.scrollOutput).visibility = View.VISIBLE
            webViewPyodide.visibility = View.GONE
        })
    }
}