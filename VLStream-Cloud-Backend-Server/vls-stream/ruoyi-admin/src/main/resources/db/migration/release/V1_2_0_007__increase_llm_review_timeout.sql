ALTER TABLE `vls_llm_provider`
    MODIFY COLUMN `timeout_seconds` int NOT NULL DEFAULT 120 COMMENT 'Request timeout in seconds';

-- Raise rows that still use the original 30-second default; other configured values are preserved.
UPDATE `vls_llm_provider`
SET `timeout_seconds` = 120
WHERE `timeout_seconds` = 30;
