package com.inc.codemy

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class SandboxActivity : AppCompatActivity() {

    private lateinit var languageSpinner: Spinner
    private lateinit var inputCode: EditText
    private lateinit var inputStdin: EditText
    private lateinit var btnRunCode: Button
    private lateinit var webViewPyodide: WebView
    private lateinit var outputResult: TextView
    private lateinit var scrollOutput: ScrollView
    private var isPyodideReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sandbox)

        languageSpinner = findViewById(R.id.languageSpinner)
        inputCode = findViewById(R.id.inputCode)
        inputStdin = findViewById(R.id.inputStdin)
        btnRunCode = findViewById(R.id.btnRunCode)
        webViewPyodide = findViewById(R.id.webViewPyodide)
        outputResult = findViewById(R.id.outputResult)
        scrollOutput = findViewById(R.id.scrollOutput)

        // Настройка Spinner
        val languages = arrayOf("Python", "JavaScript")
        languageSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)

        // Оптимизация WebView для быстрой загрузки
        webViewPyodide.settings.javaScriptEnabled = true
        webViewPyodide.settings.domStorageEnabled = true
        webViewPyodide.settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
        webViewPyodide.settings.javaScriptCanOpenWindowsAutomatically = true
        webViewPyodide.settings.setGeolocationEnabled(false)
        
        // Отключаем лишние функции для производительности
        webViewPyodide.settings.loadsImagesAutomatically = false
        webViewPyodide.settings.blockNetworkImage = true
        
        webViewPyodide.webChromeClient = WebChromeClient()
        webViewPyodide.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isPyodideReady = true
                outputResult.text = "✅ Python готов к работе!"
                outputResult.setTextColor(ContextCompat.getColor(this@SandboxActivity, android.R.color.holo_green_light))
            }
        }
        webViewPyodide.loadUrl("file:///android_res/raw/pyodide_runner.html")

        languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val lang = languages[position]
                if (lang == "JavaScript") {
                    startActivity(Intent(this@SandboxActivity, SandboxJsActivity::class.java))
                    overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

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

        // Кнопка "Выполнить"
        btnRunCode.setOnClickListener {
            val code = inputCode.text.toString().trim()
            val stdin = inputStdin.text.toString().trim()

            if (code.isEmpty()) {
                outputResult.text = "❗ Напиши код!"
                outputResult.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
                return@setOnClickListener
            }

            if (!isPyodideReady) {
                outputResult.text = "⏳ Python загружается...\nПодождите несколько секунд и попробуйте снова."
                outputResult.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_light))
                return@setOnClickListener
            }

            // Выполняем Python с stdin
            outputResult.text = "⏳ Выполняется..."
            outputResult.setTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_light))

            webViewPyodide.evaluateJavascript("runPythonWithInput(`$code`, `$stdin`)", { result ->
                val cleanResult = result
                    ?.replace("\"", "")
                    ?.replace("\\n", "\n")
                    ?.replace("\\r", "") ?: "Нет вывода"

                outputResult.text = cleanResult
                outputResult.setTextColor(
                    ContextCompat.getColor(this, android.R.color.holo_green_light)
                )
                scrollOutput.fullScroll(View.FOCUS_DOWN)
            })
        }
    }

    override fun onBackPressed() {
        startActivity(Intent(this, MainScreenActivity::class.java))
        finish()
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }
}
