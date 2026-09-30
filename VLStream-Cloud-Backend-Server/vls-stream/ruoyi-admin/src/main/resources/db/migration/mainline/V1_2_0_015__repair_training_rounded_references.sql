-- Repair only the five verified pending tasks created by the legacy Number(ID) frontend.
-- Both destination records and tenant ownership must exist; reruns are no-ops.
UPDATE vls_algorithm_training t
INNER JOIN vls_algorithm a ON a.id = 2098276259682451458
    AND BINARY a.tenant_id = BINARY t.tenant_id AND a.is_deleted = 0
INNER JOIN vls_algorithm_annotation d ON d.id = 2098181727083167745
    AND BINARY d.tenant_id = BINARY t.tenant_id AND d.is_deleted = 0
SET t.algorithm_id = a.id, t.dataset_id = d.id
WHERE t.tenant_id = '000000' AND t.is_deleted = 0 AND t.train_status = 'pending'
    AND t.algorithm_id = 2098276259682451500
    AND t.dataset_id = 2098181727083167700
    AND t.id IN (2098371387210203137, 2098366221929021442, 2098358119859093506,
                 2098276964761731074, 2098276360295415809);
