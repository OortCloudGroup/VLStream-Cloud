-- Only a fingerprint is stored here. The encryption key lives in backend persistent storage.
CREATE TABLE vls_compute_key_registry (
    id TINYINT NOT NULL PRIMARY KEY,
    key_sha256 CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
