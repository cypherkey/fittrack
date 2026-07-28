CREATE TABLE app_user (
    id              TEXT PRIMARY KEY NOT NULL,
    email           TEXT,
    username        TEXT,
    password_hash   TEXT,
    display_name    TEXT NOT NULL,
    google_subject  TEXT,
    avatar_url      TEXT,
    created_at      TEXT NOT NULL,
    updated_at      TEXT NOT NULL
);

CREATE UNIQUE INDEX uk_app_user_email ON app_user(email) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX uk_app_user_username ON app_user(username) WHERE username IS NOT NULL;
CREATE UNIQUE INDEX uk_app_user_google_subject ON app_user(google_subject) WHERE google_subject IS NOT NULL;

CREATE TABLE equipment (
    id   TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL UNIQUE
);

CREATE TABLE muscle (
    id   TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL UNIQUE
);

CREATE TABLE image (
    id       TEXT PRIMARY KEY NOT NULL,
    path     TEXT NOT NULL UNIQUE,
    alt_text TEXT
);

CREATE TABLE exercise (
    id                   TEXT PRIMARY KEY NOT NULL,
    name                 TEXT NOT NULL,
    force                TEXT,
    level                TEXT NOT NULL,
    mechanic             TEXT,
    equipment_id         TEXT REFERENCES equipment(id),
    instructions         TEXT NOT NULL DEFAULT '',
    category             TEXT,
    tracked_parameters   INTEGER NOT NULL DEFAULT 0,
    is_custom            INTEGER NOT NULL DEFAULT 0,
    added_by             TEXT REFERENCES app_user(id),
    CHECK (level IN ('BEGINNER', 'INTERMEDIATE', 'EXPERT')),
    CHECK (mechanic IS NULL OR mechanic IN ('COMPOUND', 'ISOLATION')),
    CHECK (is_custom IN (0, 1)),
    CHECK (
        (is_custom = 0 AND added_by IS NULL)
        OR (is_custom = 1 AND added_by IS NOT NULL)
    )
);

CREATE TABLE exercise_has_muscle (
    exercise_id TEXT NOT NULL REFERENCES exercise(id) ON DELETE CASCADE,
    muscle_id   TEXT NOT NULL REFERENCES muscle(id),
    is_primary  INTEGER NOT NULL,
    PRIMARY KEY (exercise_id, muscle_id),
    CHECK (is_primary IN (0, 1))
);

CREATE TABLE exercise_has_image (
    exercise_id TEXT NOT NULL REFERENCES exercise(id) ON DELETE CASCADE,
    image_id    TEXT NOT NULL REFERENCES image(id),
    sort_order  INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (exercise_id, image_id)
);

CREATE TABLE workout_template (
    id                   TEXT PRIMARY KEY NOT NULL,
    user_id              TEXT NOT NULL REFERENCES app_user(id),
    name                 TEXT,
    duration_seconds     INTEGER,
    total_weight_lifted  REAL,
    difficulty           TEXT,
    notes                TEXT,
    visibility           TEXT NOT NULL DEFAULT 'PRIVATE',
    created_at           TEXT NOT NULL,
    updated_at           TEXT NOT NULL,
    CHECK (difficulty IS NULL OR difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    CHECK (visibility IN ('PRIVATE', 'PUBLIC'))
);

CREATE TABLE template_set (
    id                   TEXT PRIMARY KEY NOT NULL,
    workout_template_id  TEXT NOT NULL REFERENCES workout_template(id) ON DELETE CASCADE,
    exercise_id          TEXT NOT NULL REFERENCES exercise(id),
    set_number           INTEGER NOT NULL,
    reps                 INTEGER,
    weight_kg            REAL,
    duration_seconds     INTEGER,
    distance_meters      REAL,
    rpe                  TEXT,
    notes                TEXT,
    UNIQUE (workout_template_id, set_number),
    CHECK (rpe IS NULL OR rpe IN ('EASY', 'CHALLENGING', 'HARD'))
);

CREATE TABLE workout (
    id                   TEXT PRIMARY KEY NOT NULL,
    user_id              TEXT NOT NULL REFERENCES app_user(id),
    performed_at         TEXT NOT NULL,
    name                 TEXT,
    duration_seconds     INTEGER,
    total_weight_lifted  REAL,
    difficulty           TEXT,
    notes                TEXT,
    source_template_id   TEXT REFERENCES workout_template(id) ON DELETE SET NULL,
    created_at           TEXT NOT NULL,
    updated_at           TEXT NOT NULL,
    CHECK (difficulty IS NULL OR difficulty IN ('EASY', 'MEDIUM', 'HARD'))
);

CREATE INDEX idx_workout_user_performed_at ON workout(user_id, performed_at);

CREATE TABLE workout_set (
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
    notes             TEXT,
    UNIQUE (workout_id, set_number),
    CHECK (completed IN (0, 1)),
    CHECK (rpe IS NULL OR rpe IN ('EASY', 'CHALLENGING', 'HARD'))
);
