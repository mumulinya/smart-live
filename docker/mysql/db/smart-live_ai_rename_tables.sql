SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

SET @db_name = DATABASE();

-- Rename user-side AI tables when old names still exist.
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.tables
            WHERE table_schema = @db_name
              AND table_name = 'session'
        ),
        'RENAME TABLE `session` TO `user_ai_session`',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.tables
            WHERE table_schema = @db_name
              AND table_name = 'message'
        ),
        'RENAME TABLE `message` TO `user_ai_message`',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Rename merchant-side AI tables when old names still exist.
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.tables
            WHERE table_schema = @db_name
              AND table_name = 'ai_merchant_session'
        ),
        'RENAME TABLE `ai_merchant_session` TO `merchant_ai_session`',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.tables
            WHERE table_schema = @db_name
              AND table_name = 'ai_merchant_message'
        ),
        'RENAME TABLE `ai_merchant_message` TO `merchant_ai_message`',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET FOREIGN_KEY_CHECKS = 1;
