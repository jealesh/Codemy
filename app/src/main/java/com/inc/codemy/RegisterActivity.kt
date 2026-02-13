package com.inc.codemy

import android.os.Bundle
import android.content.Intent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.inc.codemy.models.RegisterRequest
import com.inc.codemy.models.RegisterResponse
import com.inc.codemy.network.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterActivity : ComponentActivity() {
    private lateinit var inputName: EditText
    private lateinit var inputLogin: EditText
    private lateinit var inputAge: EditText
    private lateinit var inputEmail: EditText
    private lateinit var inputPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var btnLoginBack: Button
    private lateinit var tvMessage: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Находим все элементы по ID из XML
        inputName = findViewById(R.id.inputName)
        inputLogin = findViewById(R.id.inputLogin)
        inputAge = findViewById(R.id.inputAge)
        inputEmail = findViewById(R.id.inputEmail)
        inputPassword = findViewById(R.id.inputPassword)
        btnRegister = findViewById(R.id.btnRegister)
        btnLoginBack = findViewById(R.id.btnLoginBack)
        tvMessage = findViewById(R.id.tvMessage) // Если у тебя нет TextView для сообщений, добавь его в XML

        // Если TextView для сообщений нет — можно использовать Toast или добавить в XML
        // tvMessage = TextView(this) // временно, если нет

        // Обработчик кнопки "Зарегистрироваться"
        btnRegister.setOnClickListener {
            registerUser()
        }

        // Кнопка "Уже зарегистрированы?" — можно перейти на экран логина
        btnLoginBack.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            // finish() — если хочешь закрыть экран регистрации после перехода
            // overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right) — анимация перехода (опционально)
        }
    }

    private fun registerUser() {
        val name = inputName.text.toString().trim()
        val login = inputLogin.text.toString().trim()
        val ageStr = inputAge.text.toString().trim()
        val email = inputEmail.text.toString().trim()
        val password = inputPassword.text.toString().trim()

        // Простая валидация
        if (name.isEmpty() || login.isEmpty() || ageStr.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showMessage("Заполните все поля", isError = true)
            return
        }

        val age = ageStr.toIntOrNull() ?: run {
            showMessage("Возраст должен быть числом", isError = true)
            return
        }

        val request = RegisterRequest(
            fullName = name,
            username = login,
            age = age,
            email = email,
            password = password
        )

        // Запрос в фоне
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.apiService.register(request)
                }

                showMessage(response.message, isError = false)

                // Можно перейти на главный экран после успеха
                // startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
                // finish()

            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: "Неизвестная ошибка"
                showMessage("Ошибка: $errorMsg", isError = true)
            }
        }
    }

    private fun showMessage(text: String, isError: Boolean) {
        // Если есть TextView для сообщений — используем его
        if (::tvMessage.isInitialized) {
            tvMessage.text = text
            tvMessage.setTextColor(
                ContextCompat.getColor(
                    this,
                    if (isError) android.R.color.holo_red_dark else android.R.color.holo_green_dark
                )
            )
        } else {
            // Если TextView нет — показываем Toast
            Toast.makeText(this, text, Toast.LENGTH_LONG).show()
        }
    }
}