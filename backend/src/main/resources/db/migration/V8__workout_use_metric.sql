ALTER TABLE workout ADD COLUMN use_metric INTEGER NOT NULL DEFAULT 1;

UPDATE workout
SET use_metric = (
    SELECT u.use_metric FROM app_user u WHERE u.id = workout.user_id
)
WHERE EXISTS (
    SELECT 1 FROM app_user u WHERE u.id = workout.user_id
);
