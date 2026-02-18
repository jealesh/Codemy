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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.inc.codemy.models.CourseResponse
import com.inc.codemy.models.LessonResponse
import com.inc.codemy.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainScreenActivity : AppCompatActivity() {

    private lateinit var courseSpinner: Spinner
    private lateinit var textSectionTitle: TextView
    private lateinit var lessonsRecycler: RecyclerView
    private lateinit var btnStartTraining: Button
    private lateinit var progressDailyGoal: ProgressBar
    private lateinit var textDailyGoal: TextView

    private var dynamicCourses: List<CourseResponse> = emptyList()
    private var selectedCourseId: Long = -1L
    private val userId: Long = 1L // TODO: брать из SharedPreferences после логина

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_screen)

        // Инициализация View
        courseSpinner = findViewById(R.id.courseSpinner)
        textSectionTitle = findViewById(R.id.textSectionTitle)
        lessonsRecycler = findViewById(R.id.lessonsRecycler)
        btnStartTraining = findViewById(R.id.btnStartTraining)
        progressDailyGoal = findViewById(R.id.progressDailyGoal)
        textDailyGoal = findViewById(R.id.textDailyGoal)

        // Настройка RecyclerView
        lessonsRecycler.layoutManager = LinearLayoutManager(this)
        lessonsRecycler.setHasFixedSize(true)
        lessonsRecycler.adapter = LessonsAdapter(emptyList())

        // Загрузка курсов из API
        loadCoursesFromApi()

        // Кнопка "Начать тренировку" — открывает следующий урок
        btnStartTraining.setOnClickListener {
            val adapter = lessonsRecycler.adapter as? LessonsAdapter
            val lessons = adapter?.currentLessons ?: emptyList()   // ← исправлено: currentLessons вместо lessons
            val nextIndex = findNextAvailableLessonIndex(lessons)

            if (nextIndex >= 0) {
                val intent = Intent(this, PythonCourseActivity::class.java)
                intent.putExtra("START_LESSON_INDEX", nextIndex)
                intent.putExtra("SELECTED_COURSE", courseSpinner.selectedItem?.toString() ?: "Курс")
                startActivity(intent)
            } else {
                Toast.makeText(this, "Все уроки завершены!", Toast.LENGTH_SHORT).show()
            }
        }

        // Нижняя навигация
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

        // Временный захардкод ежедневной цели (потом из API / БД)
        progressDailyGoal.progress = 45
        textDailyGoal.text = "Цель дня: 9 / 20 XP"
    }

    private fun loadCoursesFromApi() {
        lifecycleScope.launch {
            try {
                val fetchedCourses = withContext(Dispatchers.IO) {
                    ApiClient.apiService.getCourses()
                }

                dynamicCourses = fetchedCourses

                if (dynamicCourses.isEmpty()) {
                    Toast.makeText(this@MainScreenActivity, "Нет доступных курсов", Toast.LENGTH_LONG).show()
                    return@launch
                }

                val courseNames = dynamicCourses.map { it.name }
                val adapter = ArrayAdapter(
                    this@MainScreenActivity,
                    android.R.layout.simple_spinner_item,
                    courseNames
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                courseSpinner.adapter = adapter

                // Выбираем первый курс по умолчанию
                courseSpinner.setSelection(0)
                selectedCourseId = dynamicCourses[0].id
                textSectionTitle.text = "Обучение · ${dynamicCourses[0].name}"
                loadLessonsForCourse(selectedCourseId)

                // Обработчик смены курса
                courseSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                        if (position >= dynamicCourses.size) return
                        val selectedCourse = dynamicCourses[position]
                        selectedCourseId = selectedCourse.id
                        textSectionTitle.text = "Обучение · ${selectedCourse.name}"
                        loadLessonsForCourse(selectedCourseId)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {}
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@MainScreenActivity,
                    "Ошибка загрузки курсов: ${e.localizedMessage ?: "Неизвестная ошибка"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun loadLessonsForCourse(courseId: Long) {
        lifecycleScope.launch {
            try {
                val lessonsResponse = withContext(Dispatchers.IO) {
                    ApiClient.apiService.getLessons(courseId, userId)
                }

                val lessonsForAdapter = lessonsResponse.map { response ->
                    Lesson(
                        title = response.title,
                        progress = response.progress ?: 0
                    )
                }

                (lessonsRecycler.adapter as LessonsAdapter).updateLessons(lessonsForAdapter)

            } catch (e: Exception) {
                Toast.makeText(
                    this@MainScreenActivity,
                    "Ошибка загрузки уроков: ${e.localizedMessage ?: "Неизвестная ошибка"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun findNextAvailableLessonIndex(lessons: List<Lesson>): Int {
        lessons.forEachIndexed { index, lesson ->
            if (lesson.progress < 100) return index
        }
        return -1
    }
}