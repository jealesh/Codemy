-- Migration: Add stored function for XP synchronization
-- Run this in your PostgreSQL database

-- Функция для завершения упражнения и начисления XP
-- Возвращает: (success, xp_earned, total_xp, daily_xp, daily_goal, already_completed, streak_current, streak_max)
CREATE OR REPLACE FUNCTION app.complete_exercise(
    p_user_id BIGINT,
    p_exercise_id BIGINT,
    p_lesson_id BIGINT,
    p_is_correct BOOLEAN
)
RETURNS TABLE (
    success BOOLEAN,
    xp_earned INTEGER,
    total_xp BIGINT,
    daily_xp INTEGER,
    daily_goal INTEGER,
    already_completed BOOLEAN,
    streak_current INTEGER,
    streak_max INTEGER
) AS $$
DECLARE
    v_xp_reward INTEGER;
    v_xp_to_earn INTEGER;
    v_total_xp BIGINT;
    v_daily_xp INTEGER;
    v_daily_goal INTEGER;
    v_already_completed BOOLEAN;
    v_current_streak INTEGER;
    v_max_streak INTEGER;
    v_today DATE;
    v_yesterday DATE;
    v_last_activity DATE;
    v_progress INTEGER;
BEGIN
    -- Получаем XP награду за упражнение из БД
    SELECT xp_reward INTO v_xp_reward
    FROM app.exercise
    WHERE id = p_exercise_id;

    v_xp_reward := COALESCE(v_xp_reward, 0);
    v_xp_to_earn := CASE WHEN p_is_correct THEN v_xp_reward ELSE 0 END;
    v_today := CURRENT_DATE;
    v_yesterday := v_today - INTERVAL '1 day';

    -- Проверяем, не завершено ли уже упражнение
    SELECT is_completed INTO v_already_completed
    FROM app.exercise_progress
    WHERE user_id = p_user_id AND exercise_id = p_exercise_id;

    IF v_already_completed = true THEN
        -- Уже завершено - возвращаем без начисления XP
        SELECT total_xp, daily_goal, streak_current, streak_max
        INTO v_total_xp, v_daily_goal, v_current_streak, v_max_streak
        FROM app.user_stats
        WHERE user_id = p_user_id;

        RETURN QUERY SELECT
            false AS success,
            0 AS xp_earned,
            COALESCE(v_total_xp, 0) AS total_xp,
            COALESCE(v_daily_xp, 0) AS daily_xp,
            COALESCE(v_daily_goal, 20) AS daily_goal,
            true AS already_completed,
            COALESCE(v_current_streak, 0) AS streak_current,
            COALESCE(v_max_streak, 0) AS streak_max;
        RETURN;
    END IF;

    -- Вставляем или обновляем прогресс упражнения
    INSERT INTO app.exercise_progress (
        user_id, exercise_id, lesson_id, is_completed, xp_earned,
        attempts_count, last_attempt_at, completed_at
    ) VALUES (
        p_user_id, p_exercise_id, p_lesson_id, p_is_correct, v_xp_to_earn,
        1, CURRENT_TIMESTAMP, CASE WHEN p_is_correct THEN CURRENT_TIMESTAMP ELSE NULL END
    )
    ON CONFLICT (user_id, exercise_id) DO UPDATE SET
        is_completed = CASE WHEN p_is_correct THEN true ELSE exercise_progress.is_completed END,
        xp_earned = GREATEST(exercise_progress.xp_earned, v_xp_to_earn),
        attempts_count = exercise_progress.attempts_count + 1,
        last_attempt_at = CURRENT_TIMESTAMP,
        completed_at = CASE WHEN p_is_correct THEN CURRENT_TIMESTAMP ELSE exercise_progress.completed_at END;

    -- Начисляем XP в user_stats только если ответ правильный
    IF p_is_correct AND v_xp_reward > 0 THEN
        -- Получаем daily_goal
        SELECT daily_goal INTO v_daily_goal
        FROM app.user_stats
        WHERE user_id = p_user_id;
        v_daily_goal := COALESCE(v_daily_goal, 20);

        -- Обновляем или создаём запись в user_stats
        INSERT INTO app.user_stats (
            user_id, total_xp, weekly_xp, daily_goal, updated_at
        ) VALUES (
            p_user_id, v_xp_to_earn, v_xp_to_earn, v_daily_goal, CURRENT_TIMESTAMP
        )
        ON CONFLICT (user_id) DO UPDATE SET
            total_xp = app.user_stats.total_xp + v_xp_to_earn,
            weekly_xp = app.user_stats.weekly_xp + v_xp_to_earn,
            daily_goal = EXCLUDED.daily_goal,
            updated_at = CURRENT_TIMESTAMP
        RETURNING total_xp INTO v_total_xp;

        -- Обновляем стрик
        SELECT last_activity_date, streak_current, streak_max
        INTO v_last_activity, v_current_streak, v_max_streak
        FROM app.user_stats
        WHERE user_id = p_user_id;

        -- Логика стрика
        IF v_last_activity IS NULL THEN
            v_current_streak := 1;
            v_max_streak := 1;
        ELSIF v_last_activity = v_today THEN
            -- Уже была активность сегодня - не обновляем
            NULL;
        ELSIF v_last_activity = v_yesterday THEN
            -- Активность была вчера - продолжаем стрик
            v_current_streak := v_current_streak + 1;
            IF v_current_streak > v_max_streak THEN
                v_max_streak := v_current_streak;
            END IF;
        ELSE
            -- Пропустили день - сбрасываем стрик
            v_current_streak := 1;
            IF v_max_streak < 1 THEN
                v_max_streak := 1;
            END IF;
        END IF;

        -- Обновляем стрик в user_stats
        UPDATE app.user_stats
        SET streak_current = v_current_streak,
            streak_max = v_max_streak,
            last_activity_date = v_today
        WHERE user_id = p_user_id;

        -- Обновляем user_daily_activity
        INSERT INTO app.user_daily_activity (
            user_id, date, xp_earned, exercises_completed, lessons_completed,
            streak_active, daily_goal
        ) VALUES (
            p_user_id, v_today, v_xp_to_earn, 1, 0,
            v_current_streak, v_daily_goal
        )
        ON CONFLICT (user_id, date) DO UPDATE SET
            xp_earned = app.user_daily_activity.xp_earned + v_xp_to_earn,
            exercises_completed = app.user_daily_activity.exercises_completed + 1,
            daily_goal = EXCLUDED.daily_goal
        RETURNING xp_earned INTO v_daily_xp;

        -- Вычисляем прогресс урока
        SELECT
            COUNT(CASE WHEN ep.is_completed = true THEN 1 END) * 100 / NULLIF(COUNT(*), 0)
        INTO v_progress
        FROM app.exercise e
        LEFT JOIN app.exercise_progress ep
            ON e.id = ep.exercise_id AND ep.user_id = p_user_id
        WHERE e.lesson_id = p_lesson_id;

        v_progress := COALESCE(v_progress, 0);

        -- Обновляем прогресс урока
        INSERT INTO app.user_lesson_progress (
            user_id, lesson_id, progress, last_attempt_at
        ) VALUES (
            p_user_id, p_lesson_id, v_progress, CURRENT_TIMESTAMP
        )
        ON CONFLICT (user_id, lesson_id) DO UPDATE SET
            progress = EXCLUDED.progress,
            last_attempt_at = CURRENT_TIMESTAMP;

    ELSE
        -- Если ответ неверный или XP=0, просто получаем текущий total_xp
        SELECT total_xp, daily_goal INTO v_total_xp, v_daily_goal
        FROM app.user_stats
        WHERE user_id = p_user_id;
        v_total_xp := COALESCE(v_total_xp, 0);
        v_daily_goal := COALESCE(v_daily_goal, 20);
        v_daily_xp := 0;
        v_current_streak := 0;
        v_max_streak := 0;
    END IF;

    RETURN QUERY SELECT
        p_is_correct AS success,
        v_xp_to_earn AS xp_earned,
        v_total_xp AS total_xp,
        COALESCE(v_daily_xp, 0) AS daily_xp,
        v_daily_goal AS daily_goal,
        false AS already_completed,
        COALESCE(v_current_streak, 0) AS streak_current,
        COALESCE(v_max_streak, 0) AS streak_max;
END;
$$ LANGUAGE plpgsql;

-- Функция для завершения теории (без XP)
CREATE OR REPLACE FUNCTION app.complete_theory(
    p_user_id BIGINT,
    p_exercise_id BIGINT,
    p_lesson_id BIGINT
)
RETURNS TABLE (
    success BOOLEAN,
    xp_earned INTEGER,
    total_xp BIGINT,
    daily_xp INTEGER,
    daily_goal INTEGER,
    already_completed BOOLEAN,
    streak_current INTEGER,
    streak_max INTEGER
) AS $$
DECLARE
    v_already_completed BOOLEAN;
    v_total_xp BIGINT;
    v_daily_goal INTEGER;
    v_progress INTEGER;
BEGIN
    -- Проверяем, не завершена ли уже теория
    SELECT is_completed INTO v_already_completed
    FROM app.exercise_progress
    WHERE user_id = p_user_id AND exercise_id = p_exercise_id;

    IF v_already_completed = true THEN
        SELECT total_xp, daily_goal, streak_current, streak_max
        INTO v_total_xp, v_daily_goal, v_current_streak, v_max_streak
        FROM app.user_stats
        WHERE user_id = p_user_id;

        RETURN QUERY SELECT
            true AS success,
            0 AS xp_earned,
            COALESCE(v_total_xp, 0) AS total_xp,
            0 AS daily_xp,
            COALESCE(v_daily_goal, 20) AS daily_goal,
            true AS already_completed,
            COALESCE(v_current_streak, 0) AS streak_current,
            COALESCE(v_max_streak, 0) AS streak_max;
        RETURN;
    END IF;

    -- Вставляем прогресс теории
    INSERT INTO app.exercise_progress (
        user_id, exercise_id, lesson_id, is_completed, xp_earned,
        attempts_count, last_attempt_at, completed_at
    ) VALUES (
        p_user_id, p_exercise_id, p_lesson_id, true, 0,
        1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    )
    ON CONFLICT (user_id, exercise_id) DO UPDATE SET
        is_completed = true,
        attempts_count = exercise_progress.attempts_count + 1,
        last_attempt_at = CURRENT_TIMESTAMP,
        completed_at = CURRENT_TIMESTAMP;

    -- Вычисляем прогресс урока
    SELECT
        COUNT(CASE WHEN ep.is_completed = true THEN 1 END) * 100 / NULLIF(COUNT(*), 0)
    INTO v_progress
    FROM app.exercise e
    LEFT JOIN app.exercise_progress ep
        ON e.id = ep.exercise_id AND ep.user_id = p_user_id
    WHERE e.lesson_id = p_lesson_id;

    v_progress := COALESCE(v_progress, 0);

    -- Обновляем прогресс урока
    INSERT INTO app.user_lesson_progress (
        user_id, lesson_id, progress, last_attempt_at
    ) VALUES (
        p_user_id, p_lesson_id, v_progress, CURRENT_TIMESTAMP
    )
    ON CONFLICT (user_id, lesson_id) DO UPDATE SET
        progress = EXCLUDED.progress,
        last_attempt_at = CURRENT_TIMESTAMP;

    -- Получаем текущий XP
    SELECT total_xp, daily_goal, streak_current, streak_max
    INTO v_total_xp, v_daily_goal, v_current_streak, v_max_streak
    FROM app.user_stats
    WHERE user_id = p_user_id;

    RETURN QUERY SELECT
        true AS success,
        0 AS xp_earned,
        COALESCE(v_total_xp, 0) AS total_xp,
        0 AS daily_xp,
        COALESCE(v_daily_goal, 20) AS daily_goal,
        false AS already_completed,
        COALESCE(v_current_streak, 0) AS streak_current,
        COALESCE(v_max_streak, 0) AS streak_max;
END;
$$ LANGUAGE plpgsql;

-- Verify functions exist
SELECT routine_name, routine_type
FROM information_schema.routines
WHERE routine_schema = 'app'
AND routine_name IN ('complete_exercise', 'complete_theory');
