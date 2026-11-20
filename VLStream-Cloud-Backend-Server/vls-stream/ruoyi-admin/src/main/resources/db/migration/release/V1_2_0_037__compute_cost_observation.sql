-- Provider raw prices/quotes are observations only, not activated actual hourly costs.
ALTER TABLE vls_compute_node ADD COLUMN provider_cost_json TEXT NULL;
