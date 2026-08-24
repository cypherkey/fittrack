CREATE TABLE appuser_favorite_exercise (
    user_id     TEXT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    exercise_id TEXT NOT NULL REFERENCES exercise(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, exercise_id)
);

CREATE INDEX idx_appuser_favorite_exercise_exercise ON appuser_favorite_exercise(exercise_id);