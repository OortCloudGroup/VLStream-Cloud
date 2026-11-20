CREATE TABLE IF NOT EXISTS vls_cloud_ai_grant (
    id varchar(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    tenant_id varchar(64) COLLATE utf8mb4_bin NOT NULL,
    actor_id varchar(128) COLLATE utf8mb4_bin NOT NULL,
    token_cipher text NOT NULL,
    source_json longtext NOT NULL,
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id), KEY idx_cloud_ai_grant_owner (tenant_id,actor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='云端场景摄像机授权';

CREATE TABLE IF NOT EXISTS vls_cloud_ai_scene (
    id varchar(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    tenant_id varchar(64) COLLATE utf8mb4_bin NOT NULL,
    actor_id varchar(128) COLLATE utf8mb4_bin NOT NULL,
    name varchar(100) NOT NULL,
    description varchar(1000) NULL,
    config_json longtext NOT NULL,
    revision int NOT NULL DEFAULT 1,
    enabled tinyint NOT NULL DEFAULT 1,
    deleted tinyint NOT NULL DEFAULT 0,
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY(id), KEY idx_cloud_ai_scene_owner(tenant_id,actor_id,deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='云端检测场景';

CREATE TABLE IF NOT EXISTS vls_cloud_ai_task (
    id varchar(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    tenant_id varchar(64) COLLATE utf8mb4_bin NOT NULL,
    actor_id varchar(128) COLLATE utf8mb4_bin NOT NULL,
    scene_id varchar(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    name varchar(100) NOT NULL,
    schedule_json text NOT NULL,
    enabled tinyint NOT NULL DEFAULT 1,
    next_start bigint NULL,
    next_end bigint NULL,
    revision int NOT NULL DEFAULT 1,
    deleted tinyint NOT NULL DEFAULT 0,
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY(id), KEY idx_cloud_ai_task_due(enabled,deleted,next_start),
    KEY idx_cloud_ai_task_scene(tenant_id,scene_id,deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='云端场景时间配置';

CREATE TABLE IF NOT EXISTS vls_cloud_ai_run (
    id varchar(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    tenant_id varchar(64) COLLATE utf8mb4_bin NOT NULL,
    actor_id varchar(128) COLLATE utf8mb4_bin NOT NULL,
    task_id varchar(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    scene_id varchar(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    window_key varchar(40) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    planned_start bigint NOT NULL,
    planned_end bigint NULL,
    duration_seconds int NULL,
    snapshot_json longtext NOT NULL,
    node_id bigint NOT NULL,
    run_dir varchar(600) NULL,
    state varchar(24) NOT NULL DEFAULT 'QUEUED',
    message varchar(600) NULL,
    detail_json longtext NULL,
    cancel_requested tinyint NOT NULL DEFAULT 0,
    dispatched tinyint NOT NULL DEFAULT 0,
    started_at bigint NULL,
    ended_at bigint NULL,
    heartbeat_at bigint NULL,
    lease_owner varchar(36) NULL,
    lease_until bigint NULL,
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY(id), UNIQUE KEY uk_cloud_ai_window(task_id,window_key),
    KEY idx_cloud_ai_run_worker(state,lease_until), KEY idx_cloud_ai_run_scene(tenant_id,scene_id,create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='云端检测独立运行与状态';

CREATE TABLE IF NOT EXISTS vls_cloud_ai_event (
    id varchar(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    tenant_id varchar(64) COLLATE utf8mb4_bin NOT NULL,
    run_id varchar(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    scene_id varchar(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    event_id bigint NOT NULL,
    camera_key varchar(36) NOT NULL,
    model_id varchar(32) NOT NULL,
    detected_at bigint NOT NULL,
    detail_json longtext NOT NULL,
    media_state varchar(24) NOT NULL DEFAULT 'PENDING',
    original_media_id varchar(36) NULL,
    annotated_media_id varchar(36) NULL,
    video_media_id varchar(36) NULL,
    PRIMARY KEY(id), UNIQUE KEY uk_cloud_ai_event(event_id),
    KEY idx_cloud_ai_event_run(tenant_id,run_id,detected_at),
    KEY idx_cloud_ai_event_scene(tenant_id,scene_id,detected_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能分析关联主动安全的同一事件';

-- Old scenes never had a GPU/model execution contract. Preserve their names and input for explicit completion.
INSERT INTO vls_cloud_ai_scene(id,tenant_id,actor_id,name,description,config_json,enabled,create_time)
SELECT CONCAT('legacy-',s.id), COALESCE(NULLIF(s.tenant_id,''),'000000'), COALESCE(CAST(s.create_user AS CHAR),'legacy'),
       LEFT(COALESCE(s.name,'未命名场景'),100), LEFT(s.description,1000),
       JSON_OBJECT('legacyCameras',s.cameras,'legacyCron',s.cron_expression,'cameras',JSON_ARRAY(),'models',JSON_ARRAY()), 0,
       COALESCE(s.create_time,CURRENT_TIMESTAMP)
FROM vls_scene_governance s
WHERE s.is_deleted=0 AND NOT EXISTS (SELECT 1 FROM vls_cloud_ai_scene n WHERE n.id=CONCAT('legacy-',s.id));
