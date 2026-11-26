-- Business rule: a new member gets every page and operation except the system
-- management module. DefaultMemberMenuSyncService grants each active menu outside
-- the system directory to every tenant's default member role (vls_member) once,
-- at startup and after menu changes. This table records which menus were already
-- offered to which role, so a menu an administrator later revokes from the role
-- is not granted again, while menus added afterwards are still granted.
CREATE TABLE IF NOT EXISTS vls_default_member_menu_sync (
    role_id VARCHAR(64) NOT NULL COMMENT 'default member role (vls_member)',
    menu_id BIGINT NOT NULL COMMENT 'menu already offered to the role',
    tenant_id VARCHAR(64) NOT NULL COMMENT 'tenant of the role',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='default member menu grants already applied';
