-- Approved company supply is catalogued separately from executable nodes.
-- No SSH credentials, endpoint, tenant/owner, price, wallet rule or runnable node is seeded.
CREATE TABLE IF NOT EXISTS vls_company_compute_offer (
    offer_code VARCHAR(64) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    gpu_model VARCHAR(100) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    provider_product VARCHAR(32) NOT NULL,
    provider_instance_id VARCHAR(64) NOT NULL,
    published TINYINT NOT NULL DEFAULT 0,
    created_by VARCHAR(128) NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (offer_code),
    UNIQUE KEY uk_company_compute_supplier (provider, provider_instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- User explicitly approved publication as company PAID supply, pending billing/VM access acceptance.
-- This is visibility only. Rental, power, probe, queue and training remain unavailable.
INSERT INTO vls_company_compute_offer
    (offer_code,display_name,gpu_model,provider,provider_product,provider_instance_id,published,created_by,updated_by)
SELECT 'autodl-pro-7920a22db144','公司 RTX 6000D（收费待开通）','NVIDIA RTX 6000D 84GB',
       'autodl','AUTODL_PRO','pro-7920a22db144',1,'approved-deployment','approved-deployment'
WHERE NOT EXISTS (SELECT 1 FROM vls_company_compute_offer WHERE offer_code='autodl-pro-7920a22db144');
