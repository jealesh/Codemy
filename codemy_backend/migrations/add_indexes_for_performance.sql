-- Индексы для ускорения запросов
-- Выполнить в PostgreSQL

-- Индексы для user_stats (частые запросы по user_id)
CREATE INDEX IF NOT EXISTS idx_user_stats_user_id ON app.user_stats(user_id);

-- Индексы для user_daily_activity (запросы по user_id + date)
CREATE INDEX IF NOT EXISTS idx_user_daily_activity_user_date ON app.user_daily_activity(user_id, date DESC);

-- Индексы для exercise_progress (запросы по user_id + exercise_id)
CREATE INDEX IF NOT EXISTS idx_exercise_progress_user_exercise ON app.exercise_progress(user_id, exercise_id);
CREATE INDEX IF NOT EXISTS idx_exercise_progress_user_lesson ON app.exercise_progress(user_id, lesson_id);

-- Индексы для user_lesson_progress (запросы по user_id + lesson_id)
CREATE INDEX IF NOT EXISTS idx_user_lesson_progress_user_lesson ON app.user_lesson_progress(user_id, lesson_id);

-- Индексы для users (поиск по username/email при логине)
CREATE INDEX IF NOT EXISTS idx_users_username ON app.users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON app.users(email);

-- Индексы для lessons (запросы по course_id)
CREATE INDEX IF NOT EXISTS idx_lessons_course_id ON app.lessons(course_id);

-- Индексы для exercise (запросы по lesson_id)
CREATE INDEX IF NOT EXISTS idx_exercise_lesson_id ON app.exercise(lesson_id);

-- Индексы для leaderboard (сортировка по weekly_xp)
CREATE INDEX IF NOT EXISTS idx_user_stats_weekly_xp ON app.user_stats(weekly_xp DESC);

-- Индексы для streak (сортировка по streak_current)
CREATE INDEX IF NOT EXISTS idx_user_stats_streak ON app.user_stats(streak_current DESC);

-- Проверка созданных индексов
SELECT 
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE schemaname = 'app'
ORDER BY tablename, indexname;
