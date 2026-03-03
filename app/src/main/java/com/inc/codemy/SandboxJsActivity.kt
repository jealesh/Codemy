package com.inc.codemy

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class SandboxJsActivity : AppCompatActivity() {

    private lateinit var languageSpinner: Spinner
    private lateinit var inputCode: EditText
    private lateinit var inputStdin: EditText
    private lateinit var btnRunCode: Button
    private lateinit var webViewJS: WebView
    private lateinit var outputResult: TextView
    private lateinit var scrollOutput: ScrollView
    private var isJSReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sandbox_js)

        languageSpinner = findViewById(R.id.languageSpinner)
        inputCode = findViewById(R.id.inputCode)
        inputStdin = findViewById(R.id.inputStdin)
        btnRunCode = findViewById(R.id.btnRunCode)
        webViewJS = findViewById(R.id.webViewJS)
        outputResult = findViewById(R.id.outputResult)
        scrollOutput = findViewById(R.id.scrollOutput)

        // Spinner с языками
        val languages = arrayOf("Python", "JavaScript")
        languageSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        languageSpinner.setSelection(1) // по умолчанию JS

        // Переход на Python при выборе
        languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val lang = languages[position]
                if (lang == "Python") {
                    startActivity(Intent(this@SandboxJsActivity, SandboxActivity::class.java))
                    overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Оптимизация WebView для быстрой загрузки
        webViewJS.settings.javaScriptEnabled = true
        webViewJS.settings.domStorageEnabled = true
        webViewJS.settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
        webViewJS.settings.javaScriptCanOpenWindowsAutomatically = true
        webViewJS.settings.setGeolocationEnabled(false)
        
        // Отключаем лишние функции для производительности
        webViewJS.settings.loadsImagesAutomatically = false
        webViewJS.settings.blockNetworkImage = true
        
        webViewJS.addJavascriptInterface(JSConsoleBridge(), "AndroidConsole")
        webViewJS.webChromeClient = WebChromeClient()
        webViewJS.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isJSReady = true
                outputResult.text = "✅ JavaScript готов к работе!"
                outputResult.setTextColor(ContextCompat.getColor(this@SandboxJsActivity, android.R.color.holo_green_light))
            }
        }
        webViewJS.loadUrl("file:///android_res/raw/js_runner.html")

        // Нижняя навигация
        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, MainScreenActivity::class.java))
            finish()
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }

        findViewById<LinearLayout>(R.id.navSandbox).setOnClickListener {
            // Уже в песочнице, ничего не делаем
        }

        findViewById<LinearLayout>(R.id.navTrophy).setOnClickListener {
            startActivity(Intent(this, LeagueActivity::class.java))
            finish()
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }

        findViewById<LinearLayout>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }

        btnRunCode.setOnClickListener {
            val code = inputCode.text.toString().trim()
            var stdin = inputStdin.text.toString().trim()
            
            if (code.isEmpty()) {
                outputResult.text = "❗ Напиши код!"
                outputResult.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
                return@setOnClickListener
            }

            if (!isJSReady) {
                outputResult.text = "⏳ JavaScript загружается...\nПодождите несколько секунд и попробуйте снова."
                outputResult.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_light))
                return@setOnClickListener
            }

            // Экранируем специальные символы для JavaScript
            stdin = stdin.replace("\\", "\\\\")
                .replace("`", "\\`")
                .replace("$", "\\$")

            // Выполняем JS с stdin
            outputResult.text = "⏳ Выполняется...\nКод:\n$code\n\nВходные данные:\n$stdin\n\n--- Результат ---"
            outputResult.setTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_light))

            webViewJS.evaluateJavascript("runJSWithInput(`$code`, `$stdin`)", { result ->
                // Результат выводится через console.log → JSConsoleBridge
                scrollOutput.post {
                    scrollOutput.fullScroll(View.FOCUS_DOWN)
                }
            })
        }
    }

    private inner class JSConsoleBridge {
        @JavascriptInterface
        fun log(message: String) {
            runOnUiThread {
                val isError = message.contains("ERROR") || message.contains("ошибка") || message.contains("Error") || message.contains("ReferenceError")
                outputResult.setTextColor(
                    ContextCompat.getColor(
                        this@SandboxJsActivity,
                        if (isError) android.R.color.holo_red_light else android.R.color.holo_green_light
                    )
                )

                if (outputResult.text.contains("Выполняется") || outputResult.text.contains("готов")) {
                    outputResult.text = message
                } else {
                    outputResult.append("\n$message")
                }
                scrollOutput.fullScroll(View.FOCUS_DOWN)
            }
        }
    }

    override fun onBackPressed() {
        startActivity(Intent(this, MainScreenActivity::class.java))
        finish()
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }
}
