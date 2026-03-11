package com.inc.codemy

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.inc.codemy.Lesson
import com.inc.codemy.models.CourseResponse
import com.inc.codemy.models.DailyGoalResponse
import com.inc.codemy.models.DailyGoalUpdateRequest
import com.inc.codemy.network.ApiClient
import com.inc.codemy.utils.UserDataCache
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
    private var userId: Long = -1L

    private var dailyGoalXp: Int = 20
    private var currentDailyXp: Int = 0

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

        // Получаем userId из SharedPreferences
        val sharedPref = getSharedPreferences("user_data", MODE_PRIVATE)
        userId = sharedPref.getLong("user_id", -1L)
        if (userId == -1L) {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show()
            return
        }

        // Настройка RecyclerView
        lessonsRecycler.layoutManager = LinearLayoutManager(this)
        lessonsRecycler.setHasFixedSize(true)
        lessonsRecycler.adapter = LessonsAdapter()

        // Загрузка курсов из API
        loadCoursesFromApi()

        // Кнопка "Начать тренировку" - открывает следующий урок
        btnStartTraining.setOnClickListener {
            val adapter = lessonsRecycler.adapter as? LessonsAdapter
            val lessons = adapter?.currentLessons ?: emptyList()

            val nextIndex = findNextAvailableLessonIndex(lessons)
            if (nextIndex >= 0) {
                val nextLesson = lessons[nextIndex]
                val intent = Intent(this, LessonActivity::class.java)
                intent.putExtra("LESSON_ID", nextLesson.id)
                intent.putExtra("LESSON_TITLE", nextLesson.title)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Все уроки завершены!", Toast.LENGTH_SHORT).show()
            }
        }

        // Нижняя навигация
        setupBottomNavigation()

        // Клик на карточку цели дня - открываем диалог настройки
        findViewById<LinearLayout>(R.id.dailyGoalCard).setOnClickListener {
            showDailyGoalDialog()
        }

        // Загружаем ежедневную цель
        loadDailyGoal()
    }

    private fun setupBottomNavigation() {
        findViewById<LinearLayout>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }

        findViewById<LinearLayout>(R.id.navSandbox).setOnClickListener {
            startActivity(Intent(this, SandboxActivity::class.java))
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }

        findViewById<LinearLayout>(R.id.navTrophy).setOnClickListener {
            startActivity(Intent(this, LeagueActivity::class.java))
        }
    }

    /**
     * Загрузка ежедневной цели с сервера
     */
    private fun loadDailyGoal() {
        // Проверяем кэш сначала
        val cachedGoal = UserDataCache.getDailyGoal(userId)
        if (cachedGoal != null) {
            dailyGoalXp = cachedGoal.goal_xp
            currentDailyXp = cachedGoal.current_xp
            updateDailyGoalUI()
            return
        }

        lifecycleScope.launch {
            try {
                val goal = withContext(Dispatchers.IO) {
                    ApiClient.apiService.getDailyGoal(userId)
                }

                dailyGoalXp = goal.goal_xp
                currentDailyXp = goal.current_xp
                UserDataCache.putDailyGoal(userId, goal) // Кэшируем
                updateDailyGoalUI()

            } catch (e: Exception) {
                // Используем значения по умолчанию при ошибке
                dailyGoalXp = 20
                currentDailyXp = 0
                updateDailyGoalUI()
            }
        }
    }

    /**
     * Обновление UI ежедневной цели
     */
    private fun updateDailyGoalUI() {
        progressDailyGoal.max = dailyGoalXp
        progressDailyGoal.progress = currentDailyXp

        val percentage = (currentDailyXp.toFloat() / dailyGoalXp * 100).toInt()
        textDailyGoal.text = "$currentDailyXp / $dailyGoalXp XP ($percentage%)"

        // Меняем цвет прогресс бара при выполнении цели
        progressDailyGoal.progressDrawable.setTint(
            getColor(
                if (currentDailyXp >= dailyGoalXp) R.color.colorSuccess
                else R.color.colorAccent
            )
        )
    }

    /**
     * Показ диалога установки ежедневной цели
     */
    private fun showDailyGoalDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_daily_goal)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(true)

        val textGoalValue = dialog.findViewById<TextView>(R.id.textGoalValue)
        val seekBarGoal = dialog.findViewById<SeekBar>(R.id.seekBarGoal)
        val btnIncreaseGoal = dialog.findViewById<Button>(R.id.btnIncreaseGoal)
        val btnDecreaseGoal = dialog.findViewById<Button>(R.id.btnDecreaseGoal)
        val btnSaveGoal = dialog.findViewById<Button>(R.id.btnSaveGoal)
        val btnCancelGoal = dialog.findViewById<Button>(R.id.btnCancelGoal)

        // Устанавливаем текущее значение
        var tempGoal = dailyGoalXp
        textGoalValue.text = "$tempGoal XP"
        seekBarGoal.progress = tempGoal

        // Обновление значения при изменении SeekBar
        seekBarGoal.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tempGoal = progress
                textGoalValue.text = "$tempGoal XP"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Кнопка "+"
        btnIncreaseGoal.setOnClickListener {
            if (tempGoal < 200) {
                tempGoal = (tempGoal + 5).coerceAtMost(200)
                textGoalValue.text = "$tempGoal XP"
                seekBarGoal.progress = tempGoal
            }
        }

        // Кнопка "−"
        btnDecreaseGoal.setOnClickListener {
            if (tempGoal > 10) {
                tempGoal = (tempGoal - 5).coerceAtLeast(10)
                textGoalValue.text = "$tempGoal XP"
                seekBarGoal.progress = tempGoal
            }
        }

        // Кнопка "Отмена"
        btnCancelGoal.setOnClickListener {
            dialog.dismiss()
        }

        // Кнопка "Сохранить"
        btnSaveGoal.setOnClickListener {
            if (tempGoal in 10..200) {
                saveDailyGoal(tempGoal)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Цель должна быть от 10 до 200 XP", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    /**
     * Сохранение новой ежедневной цели
     */
    private fun saveDailyGoal(newGoal: Int) {
        lifecycleScope.launch {
            try {
                val request = DailyGoalUpdateRequest(userId, newGoal)
                val response = withContext(Dispatchers.IO) {
                    ApiClient.apiService.updateDailyGoal(request)
                }

                dailyGoalXp = response.goal_xp
                currentDailyXp = response.current_xp
                UserDataCache.putDailyGoal(userId, response) // Обновляем кэш
                updateDailyGoalUI()

                Toast.makeText(this@MainScreenActivity, "Цель обновлена: $newGoal XP", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Toast.makeText(this@MainScreenActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadCoursesFromApi() {
        // Проверяем кэш сначала
        val cachedCourses = UserDataCache.getCourses()
        if (cachedCourses.isNotEmpty()) {
            dynamicCourses = cachedCourses
            setupCourseSpinner(cachedCourses)
            return
        }

        lifecycleScope.launch {
            try {
                val fetchedCourses = withContext(Dispatchers.IO) {
                    ApiClient.apiService.getCourses()
                }

                if (fetchedCourses.isEmpty()) {
                    Toast.makeText(this@MainScreenActivity, "Нет доступных курсов", Toast.LENGTH_LONG).show()
                    return@launch
                }

                dynamicCourses = fetchedCourses
                UserDataCache.putCourses(fetchedCourses) // Кэшируем
                setupCourseSpinner(fetchedCourses)

            } catch (e: Exception) {
                Toast.makeText(
                    this@MainScreenActivity,
                    "Ошибка загрузки курсов: ${e.localizedMessage ?: "Неизвестная ошибка"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setupCourseSpinner(courses: List<CourseResponse>) {
        val courseNames = courses.map { it.name }
        val adapter = ArrayAdapter(
            this@MainScreenActivity,
            android.R.layout.simple_spinner_item,
            courseNames
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        courseSpinner.adapter = adapter

        // Выбираем первый курс по умолчанию
        courseSpinner.setSelection(0)
        selectedCourseId = courses[0].id
        textSectionTitle.text = "Обучение · ${courses[0].name}"
        loadLessonsForCourse(selectedCourseId)

        // Обработчик смены курса
        courseSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (position >= courses.size) return
                val selectedCourse = courses[position]
                selectedCourseId = selectedCourse.id
                textSectionTitle.text = "Обучение · ${selectedCourse.name}"
                loadLessonsForCourse(selectedCourseId)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun loadLessonsForCourse(courseId: Long) {
        lifecycleScope.launch {
            try {
                val lessonsResponse = withContext(Dispatchers.IO) {
                    ApiClient.apiService.getLessons(courseId, userId)
                }

                // Логируем прогресс для отладки
                lessonsResponse.forEach { lesson ->
                    android.util.Log.d("MainScreenActivity", "Урок: ${lesson.title}, Прогресс: ${lesson.progress}")
                }

                UserDataCache.putLessons(courseId, lessonsResponse) // Кэшируем

                val lessonsForAdapter = lessonsResponse.map { response ->
                    Lesson(
                        id = response.id,
                        title = response.title,
                        progress = response.progress ?: 0
                    )
                }

                (lessonsRecycler.adapter as? LessonsAdapter)?.updateLessons(lessonsForAdapter)

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
