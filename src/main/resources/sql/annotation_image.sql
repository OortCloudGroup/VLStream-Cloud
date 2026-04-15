-- 标注图片表
CREATE TABLE IF NOT EXISTS annotation_image (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    dataset_id BIGINT NOT NULL COMMENT '数据集ID',
    file_name VARCHAR(255) NOT NULL COMMENT '文件名',
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_path VARCHAR(500) NOT NULL COMMENT '文件路径',
    file_url VARCHAR(500) COMMENT '文件访问URL',
    file_size BIGINT COMMENT '文件大小(字节)',
    mime_type VARCHAR(100) COMMENT 'MIME类型',
    width INT COMMENT '图片宽度',
    height INT COMMENT '图片高度',
    category VARCHAR(100) COMMENT '图片分类',
    annotation_data TEXT COMMENT '标注数据(JSON格式)',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态: PENDING-待标注, ANNOTATED-已标注, REVIEWED-已审核',
    tags VARCHAR(500) COMMENT '标签(逗号分隔)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by VARCHAR(100) COMMENT '创建人',
    update_by VARCHAR(100) COMMENT '更新人',
    
    INDEX idx_dataset_id (dataset_id),
    INDEX idx_status (status),
    INDEX idx_category (category),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标注图片表';

-- 插入示例数据
INSERT INTO annotation_image (dataset_id, file_name, original_name, file_path, file_url, file_size, mime_type, width, height, category, status) VALUES
(1, 'sample1.jpg', 'sample1.jpg', './data/images/sample1.jpg', '/api/files/images/sample1.jpg', 102400, 'image/jpeg', 1920, 1080, 'person', 'PENDING'),
(1, 'sample2.jpg', 'sample2.jpg', './data/images/sample2.jpg', '/api/files/images/sample2.jpg', 204800, 'image/jpeg', 1280, 720, 'vehicle', 'ANNOTATED'),
(2, 'sample3.png', 'sample3.png', './data/images/sample3.png', '/api/files/images/sample3.png', 153600, 'image/png', 800, 600, 'object', 'REVIEWED');




