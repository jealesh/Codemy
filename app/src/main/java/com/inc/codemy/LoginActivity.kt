package com.inc.codemy

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.inc.codemy.models.LoginRequest
import com.inc.codemy.models.LoginResponse
import com.inc.codemy.network.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : ComponentActivity() {

    private lateinit var inputLoginOrEmail: EditText
    private lateinit var inputPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegister: Button
    private lateinit var tvMessage: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Подставь свои реальные ID из XML
        inputLoginOrEmail = findViewById(R.id.inputLoginOrEmail)
        inputPassword = findViewById(R.id.inputPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegister = findViewById(R.id.btnRegister)
        tvMessage = findViewById(R.id.tvMessage)

        btnLogin.setOnClickListener {
            loginUser()
        }

        btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun loginUser() {
        val loginOrEmail = inputLoginOrEmail.text.toString().trim()
        val password = inputPassword.text.toString().trim()

        if (loginOrEmail.isEmpty() || password.isEmpty()) {
            showError("Заполните все поля")
            return
        }

        val request = LoginRequest(loginOrEmail, password)

        // Показываем, что идёт загрузка (опционально)
        tvMessage.text = "Проверка..."
        tvMessage.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.apiService.login(request)
                }

                if (response.userId != null) {
                    // Успех — переходим на экран выбора уроков
                    showSuccess("Вход успешен! Добро пожаловать")
                    startActivity(Intent(this@LoginActivity, MainScreenActivity::class.java))
                    finish() // закрываем экран логина
                } else {
                    // Ошибка от сервера
                    when (response.message) {
                        "Пользователь не найден" -> showError("Неверный логин или email")
                        "Неверный пароль" -> showError("Неверный пароль")
                        else -> showError(response.message)
                    }
                }
            } catch (e: Exception) {
                // Сетевая ошибка или что-то сломалось
                showError("Не удалось подключиться к серверу: ${e.localizedMessage ?: "Неизвестная ошибка"}")
            }
        }
    }

    private fun showError(message: String) {
        tvMessage.text = message
        tvMessage.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
    }

    private fun showSuccess(message: String) {
        tvMessage.text = message
        tvMessage.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
    }
}