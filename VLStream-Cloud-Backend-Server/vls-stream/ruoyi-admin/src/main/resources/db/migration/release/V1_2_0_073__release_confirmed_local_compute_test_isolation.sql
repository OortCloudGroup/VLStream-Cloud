-- One-time administrator acceptance of residual files from the confirmed local test.
-- This is NOT cleanup evidence and NOT a global admission/cleanup bypass.
-- Preserve all orders/jobs. A newer claim, active work or another instance is untouched.
UPDATE vls_compute_public_instance AS p
SET public_state = 'FREE', workspace_dirty = 0, owner_subject = NULL,
    claim_id = NULL, generation = generation + 1
WHERE p.instance_key = '0a611e964c40145a0b9feb71aa503d16b9f84875cd5f63c7bb1511a3809937a8'
  AND p.provider = 'autodl'
  AND p.provider_instance_id = 'pro-7920a22db144'
  AND p.public_state = 'ISOLATED' AND p.workspace_dirty = 1
  AND p.generation = 1
  AND p.owner_subject = '77f233bdcefd59b7667b31d4f22710712e468d42235aeea52e1b7510bfd921d7'
  AND p.claim_id = '1af85eb2-bd04-4450-9b5c-9712ad6c4565'
  AND EXISTS (
      SELECT 1 FROM vls_cloud_training_job j
      WHERE j.id = '84662308-64ae-482e-a5fa-bff789ffcbd8'
        AND j.public_instance_key = p.instance_key AND j.node_id = -6000
        AND j.job_state = 'COMPLETED'
  )
  AND NOT EXISTS (
      SELECT 1 FROM vls_compute_paid_run r
      WHERE r.active_instance_key = p.instance_key
         OR (r.node_id = -6000 AND (r.state IS NULL OR r.state NOT IN ('QUOTED','COMPLETED','CANCELLED')))
  )
  AND NOT EXISTS (
      SELECT 1 FROM vls_cloud_training_job j
      LEFT JOIN vls_compute_node n ON n.id = j.node_id
      WHERE (j.public_instance_key = p.instance_key OR n.public_instance_key = p.instance_key OR j.node_id = -6000)
        AND (j.job_state IS NULL OR j.job_state NOT IN ('COMPLETED','FAILED','CANCELLED'))
  )
  AND NOT EXISTS (
      SELECT 1 FROM vls_compute_node n
      WHERE (n.public_instance_key = p.instance_key OR n.id = -6000)
        AND (n.power_operation_id IS NOT NULL OR n.probe_state = 'CHECKING')
  );
