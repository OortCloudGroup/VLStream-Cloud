-- Correct the already-applied 044 display seed through a new forward migration.
-- The approved company tenant 000000 is the existing VM company P100 management tenant.
-- No account, SSH username, credential, host trust, runtime path or ready evidence is configured.
ALTER TABLE vls_company_compute_offer ADD COLUMN node_id BIGINT NULL;
ALTER TABLE vls_company_compute_offer ADD CONSTRAINT uk_company_compute_node UNIQUE (node_id);

INSERT INTO vls_compute_node
    (id,tenant_id,name,provider,host,port,username,auth_mode,password_cipher,host_key,
     python_path,work_dir,gpu_index,enabled,probe_state,resource_scope,server_group,gpu_model,
     billing_type,points_per_hour,power_state,capacity_state,provider_instance_id,control_mode,
     provider_product,provider_gpu_amount,management_owner_subject,scheduling_mode,lifecycle_message)
VALUES
    (792022144331380001,'000000','OortCloud RTX 6000D','autodl','connect.weste.seetacloud.com',33138,
     '','UNCONFIGURED',NULL,NULL,'','',0,0,'UNTESTED','PLATFORM','autodl-pro-7920a22db144',
     'NVIDIA RTX 6000D 84GB','POINTS',NULL,'UNKNOWN','UNKNOWN','pro-7920a22db144','MANUAL',
     'AUTODL_PRO',1,'approved-company-pro-7920a22db144','INSTANCE_EXCLUSIVE',
     '待配置SSH训练身份；收费使用尚未开通');

UPDATE vls_company_compute_offer
SET node_id=792022144331380001,display_name='OortCloud RTX 6000D',updated_at=CURRENT_TIMESTAMP
WHERE offer_code='autodl-pro-7920a22db144' AND provider='autodl' AND provider_instance_id='pro-7920a22db144';

ALTER TABLE vls_company_compute_offer ADD CONSTRAINT fk_company_compute_node
    FOREIGN KEY (node_id) REFERENCES vls_compute_node(id);
