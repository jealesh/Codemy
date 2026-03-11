-- Migration script to add streak columns if they don't exist
-- Run this in your PostgreSQL database

-- Add streak_current column
DO $$ 
BEGIN 
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'app' 
        AND table_name = 'user_stats' 
        AND column_name = 'streak_current'
    ) THEN
        ALTER TABLE app.user_stats ADD COLUMN streak_current INTEGER DEFAULT 0;
    END IF;
END $$;

-- Add streak_max column
DO $$ 
BEGIN 
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'app' 
        AND table_name = 'user_stats' 
        AND column_name = 'streak_max'
    ) THEN
        ALTER TABLE app.user_stats ADD COLUMN streak_max INTEGER DEFAULT 0;
    END IF;
END $$;

-- Add last_activity_date column
DO $$ 
BEGIN 
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'app' 
        AND table_name = 'user_stats' 
        AND column_name = 'last_activity_date'
    ) THEN
        ALTER TABLE app.user_stats ADD COLUMN last_activity_date DATE;
    END IF;
END $$;

-- Verify columns exist
SELECT column_name, data_type, column_default 
FROM information_schema.columns 
WHERE table_schema = 'app' 
AND table_name = 'user_stats' 
AND column_name IN ('streak_current', 'streak_max', 'last_activity_date')
ORDER BY ordinal_position;
