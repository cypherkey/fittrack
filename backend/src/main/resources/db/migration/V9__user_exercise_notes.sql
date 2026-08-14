CREATE TABLE user_exercise_notes (
    id           TEXT PRIMARY KEY NOT NULL,
    user_id      TEXT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    exercise_id  TEXT NOT NULL REFERENCES exercise(id) ON DELETE CASCADE,
    notes        TEXT,
    UNIQUE (user_id, exercise_id)
);

CREATE INDEX idx_user_exercise_notes_user ON user_exercise_notes(user_id);
CREATE INDEX idx_user_exercise_notes_exercise ON user_exercise_notes(exercise_id);

-- Migrate existing workout_set notes into per-user exercise notes (one row per user+exercise; prefer longest notes)
INSERT INTO user_exercise_notes (id, user_id, exercise_id, notes)
SELECT
    lower(hex(randomblob(4))) || '-' ||
    lower(hex(randomblob(2))) || '-' ||
    '4' || substr(lower(hex(randomblob(2))), 2) || '-' ||
    substr('89ab', abs(random()) % 4 + 1, 1) || substr(lower(hex(randomblob(2))), 2) || '-' ||
    lower(hex(randomblob(6))),
    w.user_id,
    ws.exercise_id,
    (
        SELECT ws2.notes
        FROM workout_set ws2
        JOIN workout w2 ON w2.id = ws2.workout_id
        WHERE w2.user_id = w.user_id
          AND ws2.exercise_id = ws.exercise_id
          AND ws2.notes IS NOT NULL
          AND trim(ws2.notes) != ''
        ORDER BY length(ws2.notes) DESC, w2.updated_at DESC
        LIMIT 1
    )
FROM workout_set ws
JOIN workout w ON w.id = ws.workout_id
WHERE ws.notes IS NOT NULL AND trim(ws.notes) != ''
GROUP BY w.user_id, ws.exercise_id;

-- Drop notes from workout_set (rebuild; SQLite-safe)
CREATE TABLE workout_set_new (
    id                TEXT PRIMARY KEY NOT NULL,
    workout_id        TEXT NOT NULL REFERENCES workout(id) ON DELETE CASCADE,
    exercise_id       TEXT NOT NULL REFERENCES exercise(id),
    set_number        INTEGER NOT NULL,
    reps              INTEGER,
    weight_kg         REAL,
    duration_seconds  INTEGER,
    distance_meters   REAL,
    completed         INTEGER NOT NULL DEFAULT 1,
    rpe               TEXT,
    UNIQUE (workout_id, set_number),
    CHECK (completed IN (0, 1)),
    CHECK (rpe IS NULL OR rpe IN ('EASY', 'CHALLENGING', 'HARD'))
);

INSERT INTO workout_set_new (
    id, workout_id, exercise_id, set_number, reps, weight_kg, duration_seconds, distance_meters, completed, rpe
)
SELECT
    id, workout_id, exercise_id, set_number, reps, weight_kg, duration_seconds, distance_meters, completed, rpe
FROM workout_set;

DROP TABLE workout_set;
ALTER TABLE workout_set_new RENAME TO workout_set;