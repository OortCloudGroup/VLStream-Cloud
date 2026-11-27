ALTER TABLE vls_algorithm_repository
    ADD COLUMN shared_catalog_id BIGINT NULL COMMENT 'Canonical system catalog; self for the canonical row',
    ADD INDEX idx_shared_algorithm_catalog (shared_catalog_id, is_deleted);

ALTER TABLE vls_algorithm
    ADD COLUMN catalog_seed TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Read-only algorithm installed by the system';

-- Map the official catalogs and their initialized tenant copies without changing existing IDs.
UPDATE vls_algorithm_repository AS repository
JOIN (
    SELECT MIN(id) AS id, name
    FROM vls_algorithm_repository
    WHERE tenant_id = '000000' AND is_deleted = 0 AND repository_type = 'basic'
      AND create_user IS NULL AND name IN (
          '智慧工地', '智慧水利', '污水处理', '智慧城管', '智慧社区园区',
          '智慧校园', '危化安全生产', '明厨亮灶', '加油站安全监管'
      )
    GROUP BY name
) AS canonical ON CAST(repository.name AS BINARY) = CAST(canonical.name AS BINARY)
SET repository.shared_catalog_id = canonical.id
WHERE repository.is_deleted = 0 AND repository.repository_type = 'basic'
  AND repository.create_user IS NULL;

UPDATE vls_algorithm AS algorithm
JOIN vls_algorithm_repository AS repository ON repository.id = algorithm.repository_id
    AND repository.tenant_id = algorithm.tenant_id
SET algorithm.catalog_seed = 1
WHERE repository.shared_catalog_id = repository.id AND algorithm.tenant_id = '000000'
  AND algorithm.is_deleted = 0 AND algorithm.is_system = 1
  AND algorithm.create_user IS NULL AND algorithm.create_dept IS NULL;

UPDATE vls_algorithm AS algorithm
JOIN vls_algorithm_repository AS repository ON repository.id = algorithm.repository_id
    AND repository.tenant_id = algorithm.tenant_id
JOIN vls_algorithm AS seed ON seed.repository_id = repository.shared_catalog_id
    AND seed.tenant_id = '000000' AND seed.catalog_seed = 1
    AND CAST(algorithm.name AS BINARY) = CAST(seed.name AS BINARY)
SET algorithm.catalog_seed = 1
WHERE repository.shared_catalog_id <> repository.id
  AND algorithm.is_deleted = 0 AND algorithm.is_system = 1
  AND algorithm.create_user IS NULL AND algorithm.create_dept IS NULL;
