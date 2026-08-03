-- Blank titles are stored as NULL so multiple unnamed workouts remain allowed.
UPDATE workout SET name = NULL WHERE name IS NOT NULL AND trim(name) = '';

-- Disambiguate existing duplicates before adding the unique index (keep earliest by created_at).
UPDATE workout
SET name = name || ' [' || id || ']'
WHERE id IN (
    SELECT id FROM (
        SELECT w.id AS id,
               ROW_NUMBER() OVER (
                   PARTITION BY w.user_id, w.name
                   ORDER BY w.created_at ASC, w.id ASC
               ) AS rn
        FROM workout w
        WHERE w.name IS NOT NULL
    ) ranked
    WHERE rn > 1
);

CREATE UNIQUE INDEX idx_workout_user_name ON workout(user_id, name);