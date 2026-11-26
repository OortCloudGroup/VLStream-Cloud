CREATE TABLE vls_video_square_scene (
    tenant_id varchar(64) NOT NULL,
    owner_id varchar(64) NOT NULL,
    view_mode varchar(16) NOT NULL,
    revision int NOT NULL,
    prompt text NOT NULL,
    scene_json mediumtext NOT NULL,
    PRIMARY KEY (tenant_id, owner_id, view_mode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='Latest personal video square scene per view';
