package com.inc.codemy

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class SandboxJsActivity : AppCompatActivity() {

    private lateinit var languageSpinner: Spinner
    private lateinit var inputCode: EditText
    private lateinit var btnRunCode: Button
    private lateinit var webViewJS: WebView
    private lateinit var outputResult: TextView
    private lateinit var scrollOutput: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sandbox_js)

        languageSpinner = findViewById(R.id.languageSpinner)
        inputCode = findViewById(R.id.inputCode)
        btnRunCode = findViewById(R.id.btnRunCode)
        webViewJS = findViewById(R.id.webViewJS)
        outputResult = findViewById(R.id.outputResult)
        scrollOutput = findViewById(R.id.scrollOutput)

        // Spinner с обоими языками
        val languages = arrayOf("Python", "JavaScript")
        languageSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        languageSpinner.setSelection(1) // по умолчанию JS

        // Переход на Python при выборе
        languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val lang = languages[position]

                if (lang == "Python") {
                    startActivity(Intent(this@SandboxJsActivity, SandboxActivity::class.java))
                    return
                }

                // Если JavaScript — остаёмся здесь
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Настройка WebView
        webViewJS.settings.javaScriptEnabled = true
        webViewJS.settings.domStorageEnabled = true
        webViewJS.webChromeClient = WebChromeClient()

        // Перехват console.log
        webViewJS.addJavascriptInterface(ConsoleBridge(), "AndroidConsole")

        // Загружаем JS-раннер
        webViewJS.loadUrl("file:///android_res/raw/js_runner.html")

        btnRunCode.setOnClickListener {
            val code = inputCode.text.toString().trim()
            if (code.isEmpty()) {
                outputResult.text = "Напиши код!"
                outputResult.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
                return@setOnClickListener
            }

            outputResult.text = ""
            outputResult.append("Выполняется...")

            runJavaScript(code)
        }

        // Навигация
        findViewById<TextView>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, MainScreenActivity::class.java))
            finish()
        }

        findViewById<TextView>(R.id.navSandbox).setOnClickListener {
            startActivity(Intent(this, SandboxActivity::class.java))
            finish()
        }

        findViewById<TextView>(R.id.navTrophy).setOnClickListener {
            startActivity(Intent(this, LeagueActivity::class.java))
            finish()
        }

        findViewById<TextView>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
    }

    private inner class ConsoleBridge {
        @JavascriptInterface
        fun log(message: String) {
            runOnUiThread {
                if (message.startsWith("ERROR:") || message.contains("JS ошибка")) {
                    outputResult.setTextColor(ContextCompat.getColor(this@SandboxJsActivity, android.R.color.holo_red_light))
                } else {
                    outputResult.setTextColor(ContextCompat.getColor(this@SandboxJsActivity, android.R.color.holo_green_light))
                }

                outputResult.append("\n$message")
                scrollOutput.post { scrollOutput.fullScroll(View.FOCUS_DOWN) }
            }
        }
    }

    private fun runJavaScript(code: String) {
        outputResult.text = ""  // чистим перед запуском
        outputResult.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light)) // по умолчанию зелёный

        webViewJS.evaluateJavascript("runJS(`$code`)", { result ->
            // Вывод уже добавлен через console.log → AndroidConsole.log

            // Проверяем, была ли ошибка
            val text = outputResult.text.toString()
            if (text.contains("ERROR") || text.contains("JS ошибка")) {
                outputResult.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
            } else {
                outputResult.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
            }

            scrollOutput.post { scrollOutput.fullScroll(View.FOCUS_DOWN) }
            webViewJS.visibility = View.GONE
        })
    }
}