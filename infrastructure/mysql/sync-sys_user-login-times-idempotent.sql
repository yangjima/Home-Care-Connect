-- Idempotent: add login time columns + index only if missing (safe to re-run).
-- Usage: mysql -u... -p... homecare < sync-sys_user-login-times-idempotent.sql
-- Or: Get-Content ... | docker exec -i homecare-mysql mysql -uroot -p... homecare

USE homecare;

SET @db = DATABASE();

SET @need_first = (
    SELECT COUNT(*) = 0 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'first_login_at'
);
SET @sql_first = IF(
    @need_first,
    'ALTER TABLE sys_user ADD COLUMN first_login_at DATETIME NULL AFTER updated_at',
    'SELECT 1'
);
PREPARE stmt FROM @sql_first;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @need_last = (
    SELECT COUNT(*) = 0 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'last_login_at'
);
SET @sql_last = IF(
    @need_last,
    'ALTER TABLE sys_user ADD COLUMN last_login_at DATETIME NULL AFTER first_login_at',
    'SELECT 1'
);
PREPARE stmt2 FROM @sql_last;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

SET @need_idx = (
    SELECT COUNT(*) = 0 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'sys_user' AND INDEX_NAME = 'idx_last_login_at'
);
SET @sql_idx = IF(
    @need_idx,
    'CREATE INDEX idx_last_login_at ON sys_user (last_login_at)',
    'SELECT 1'
);
PREPARE stmt3 FROM @sql_idx;
EXECUTE stmt3;
DEALLOCATE PREPARE stmt3;

SELECT 'sys_user login columns sync done' AS result;
