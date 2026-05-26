-- Migration: Add avatar_url column to users table
-- Если поле avatar_url было удалено, эта миграция восстановит его

DO $$
BEGIN
    -- Проверяем, существует ли колонка avatar_url
    IF NOT EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_schema = 'app' 
        AND table_name = 'users' 
        AND column_name = 'avatar_url'
    ) THEN
        -- Добавляем колонку avatar_url
        ALTER TABLE app.users 
        ADD COLUMN avatar_url VARCHAR(255) NULL;
        
        RAISE NOTICE 'Колонка avatar_url добавлена в таблицу app.users';
    ELSE
        RAISE NOTICE 'Колонка avatar_url уже существует в таблице app.users';
    END IF;
END $$;
