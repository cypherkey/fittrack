-- Rename performed_at -> started_at (nullable), add ended_at + completed, drop session duration_seconds.
CREATE TABLE workout_new (
    id                   TEXT PRIMARY KEY NOT NULL,
    user_id              TEXT NOT NULL REFERENCES app_user(id),
    started_at           TEXT,
    ended_at             TEXT,
    name                 TEXT,
    total_weight_lifted  REAL,
    difficulty           TEXT,
    notes                TEXT,
    source_template_id   TEXT REFERENCES workout_template(id) ON DELETE SET NULL,
    completed            INTEGER NOT NULL DEFAULT 0,
    created_at           TEXT NOT NULL,
    updated_at           TEXT NOT NULL,
    CHECK (completed IN (0, 1)),
    CHECK (difficulty IS NULL OR difficulty IN ('EASY', 'MEDIUM', 'HARD'))
);

INSERT INTO workout_new (
    id, user_id, started_at, ended_at, name, total_weight_lifted, difficulty, notes,
    source_template_id, completed, created_at, updated_at
)
SELECT
    id, user_id, performed_at, NULL, name, total_weight_lifted, difficulty, notes,
    source_template_id, 0, created_at, updated_at
FROM workout;

DROP TABLE workout;
ALTER TABLE workout_new RENAME TO workout;

CREATE INDEX idx_workout_user_started_at ON workout(user_id, started_at);
