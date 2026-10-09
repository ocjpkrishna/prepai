-- V3 left usage_log.lesson_id without a constraint because lessons did not exist yet (row 6).
-- SET NULL keeps the usage row if a lesson is ever deleted, so the purge order of the modules does not matter.
ALTER TABLE usage_log
    ADD CONSTRAINT fk_usage_log_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(id) ON DELETE SET NULL;
