-- Manual migration for existing yiliao_patient databases.
-- The Docker init scripts only run when the MySQL data volume is first created.
USE yiliao_patient;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'patient_archive'
      AND COLUMN_NAME = 'id_card_hash'
);
SET @ddl := IF(
    @column_exists = 0,
    'ALTER TABLE patient_archive ADD COLUMN id_card_hash CHAR(64) NULL COMMENT ''身份证规范化值 SHA-256，不可逆'' AFTER id_card',
    'SELECT 1'
);
PREPARE add_column FROM @ddl;
EXECUTE add_column;
DEALLOCATE PREPARE add_column;

SET @index_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'patient_archive'
      AND INDEX_NAME = 'uk_id_card_hash'
);
SET @ddl := IF(
    @index_exists = 0,
    'ALTER TABLE patient_archive ADD UNIQUE KEY uk_id_card_hash (id_card_hash)',
    'SELECT 1'
);
PREPARE add_index FROM @ddl;
EXECUTE add_index;
DEALLOCATE PREPARE add_index;

-- Existing id_card values are AES-GCM ciphertext and cannot be hashed in SQL.
-- Existing rows therefore remain NULL until a trusted application-side backfill
-- is performed; NULL remains allowed by the unique index for legacy rows.
