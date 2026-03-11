-- Migration: Add streak columns to user_stats table
-- Date: 2026-03-11

-- Add current streak column
ALTER TABLE app.user_stats 
ADD COLUMN IF NOT EXISTS streak_current INTEGER DEFAULT 0;

-- Add max streak column
ALTER TABLE app.user_stats 
ADD COLUMN IF NOT EXISTS streak_max INTEGER DEFAULT 0;

-- Add last activity date column for streak tracking
ALTER TABLE app.user_stats 
ADD COLUMN IF NOT EXISTS last_activity_date DATE;

-- Set default values for existing records
UPDATE app.user_stats 
SET streak_current = 0, 
    streak_max = 0 
WHERE streak_current IS NULL OR streak_max IS NULL;
