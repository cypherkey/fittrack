ALTER TABLE app_user ADD COLUMN admin INTEGER NOT NULL DEFAULT 0;

UPDATE app_user SET admin = 1 WHERE username = 'admin';
