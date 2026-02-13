package com.inc.codemy

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainScreenActivity : AppCompatActivity() {

    private lateinit var courseSpinner: Spinner
    private lateinit var textSectionTitle: TextView
    private lateinit var lessonsRecycler: RecyclerView
    private lateinit var btnStartTraining: Button
    private lateinit var progressDailyGoal: ProgressBar
    private lateinit var textDailyGoal: TextView

    // Пример данных для разных курсов (пока захардкодим)
    private val pythonLessons = listOf(
        Lesson("Ввод и вывод данных", 100),
        Lesson("Типы данных", 75),
        Lesson("Условный оператор", 30),
        Lesson("Циклы for и while", 0),
        Lesson("Строки", 0),
        Lesson("Списки", 0),
        Lesson("Функции", 0)
    )

    private val kotlinLessons = listOf(
        Lesson("Переменные и типы", 90),
        Lesson("Условия и when", 60),
        Lesson("Циклы", 20),
        Lesson("Функции", 0),
        Lesson("Классы и объекты", 0)
    )

    private var currentLessons = pythonLessons  // по умолчанию Python

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_screen)

        courseSpinner = findViewById(R.id.courseSpinner)
        textSectionTitle = findViewById(R.id.textSectionTitle)
        lessonsRecycler = findViewById(R.id.lessonsRecycler)
        btnStartTraining = findViewById(R.id.btnStartTraining)
        progressDailyGoal = findViewById(R.id.progressDailyGoal)
        textDailyGoal = findViewById(R.id.textDailyGoal)

        // Настройка Spinner
        val courses = arrayOf("Python", "Java", "C#")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, courses)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        courseSpinner.adapter = adapter

        // По умолчанию выбираем Python (позиция 0)
        courseSpinner.setSelection(0)

        // Обработчик выбора курса
        courseSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCourse = courses[position]
                textSectionTitle.text = "Обучение · $selectedCourse"

                // Меняем список уроков в зависимости от выбора
                currentLessons = when (selectedCourse) {
                    "Python" -> pythonLessons
                    "Kotlin" -> kotlinLessons
                    else -> pythonLessons  // для остальных пока Python
                }

                // Обновляем RecyclerView
                (lessonsRecycler.adapter as? LessonsAdapter)?.let { adapter ->
                    adapter.updateLessons(currentLessons)  // нужно будет добавить метод в адаптер
                } ?: run {
                    lessonsRecycler.adapter = LessonsAdapter(currentLessons)
                }

                Toast.makeText(this@MainScreenActivity, "Выбран курс: $selectedCourse", Toast.LENGTH_SHORT).show()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // ничего не делаем
            }
        }

        // Настройка RecyclerView
        lessonsRecycler.layoutManager = LinearLayoutManager(this)
        lessonsRecycler.setHasFixedSize(true)
        lessonsRecycler.adapter = LessonsAdapter(currentLessons)

        // Кнопка "Начать тренировку" → последний доступный урок текущего курса
        btnStartTraining.setOnClickListener {
            val nextIndex = findNextAvailableLessonIndex(currentLessons)
            if (nextIndex >= 0) {
                val intent = Intent(this, PythonCourseActivity::class.java)
                intent.putExtra("START_LESSON_INDEX", nextIndex)
                intent.putExtra("SELECTED_COURSE", courseSpinner.selectedItem.toString())
                startActivity(intent)
            } else {
                Toast.makeText(this, "Все уроки завершены!", Toast.LENGTH_SHORT).show()
            }
        }

        // Навигация (без изменений)
        findViewById<TextView>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
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

        // Ежедневный прогресс (захардкод)
        progressDailyGoal.progress = 45
        textDailyGoal.text = "Цель дня: 9 / 20 XP"
    }

    private fun findNextAvailableLessonIndex(lessons: List<Lesson>): Int {
        lessons.forEachIndexed { index, lesson ->
            if (lesson.progress < 100) return index
        }
        return -1
    }
}