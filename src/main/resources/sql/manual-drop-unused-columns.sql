-- Manual cleanup for columns removed from the codebase.
-- Apply only after confirming the running database no longer depends on them.

ALTER TABLE post DROP COLUMN only_role;
ALTER TABLE chat_rooms DROP COLUMN last_message;
ALTER TABLE chat_participants DROP COLUMN is_favorite;
