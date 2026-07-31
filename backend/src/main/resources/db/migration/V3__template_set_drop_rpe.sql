-- RPE is workout-logging only. SQLite cannot DROP COLUMN when a CHECK references it,
-- so rebuild template_set without rpe.
CREATE TABLE template_set_new (
    id                   TEXT PRIMARY KEY NOT NULL,
    workout_template_id  TEXT NOT NULL REFERENCES workout_template(id) ON DELETE CASCADE,
    exercise_id          TEXT NOT NULL REFERENCES exercise(id),
    set_number           INTEGER NOT NULL,
    reps                 INTEGER,
    weight_kg            REAL,
    duration_seconds     INTEGER,
    distance_meters      REAL,
    notes                TEXT,
    UNIQUE (workout_template_id, set_number)
);

INSERT INTO template_set_new (
    id, workout_template_id, exercise_id, set_number, reps, weight_kg, duration_seconds, distance_meters, notes
)
SELECT
    id, workout_template_id, exercise_id, set_number, reps, weight_kg, duration_seconds, distance_meters, notes
FROM template_set;

DROP TABLE template_set;
ALTER TABLE template_set_new RENAME TO template_set;
