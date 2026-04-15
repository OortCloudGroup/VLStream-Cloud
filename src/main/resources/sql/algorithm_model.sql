-- ===================================================
-- 算法模型表结构和测试数据
-- ===================================================

-- 创建算法模型表
CREATE TABLE `algorithm_model` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '模型ID',
  `model_name` varchar(100) NOT NULL COMMENT '模型名称',
  `algorithm_id` bigint NOT NULL COMMENT '算法ID',
  `training_id` bigint DEFAULT NULL COMMENT '训练任务ID',
  `version` varchar(20) NOT NULL COMMENT '模型版本',
  `model_format` varchar(20) DEFAULT NULL COMMENT '模型格式：ONNX,PyTorch,TensorFlow',
  `model_size` varchar(20) DEFAULT NULL COMMENT '模型大小',
  `model_path` varchar(500) NOT NULL COMMENT '模型文件路径',
  `accuracy` decimal(5,2) DEFAULT NULL COMMENT '模型准确率',
  `status` varchar(20) NOT NULL DEFAULT 'draft' COMMENT '状态：draft-草稿,testing-测试中,published-已发布',
  `description` text COMMENT '模型描述',
  `download_count` int DEFAULT '0' COMMENT '下载次数',
  `deploy_count` int DEFAULT '0' COMMENT '部署次数',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  PRIMARY KEY (`id`),
  KEY `idx_algorithm_id` (`algorithm_id`),
  KEY `idx_training_id` (`training_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_by` (`created_by`),
  KEY `idx_created_time` (`created_time`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=10000 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='算法模型表';

-- 插入测试数据
INSERT INTO `algorithm_model` (
  `id`, `model_name`, `algorithm_id`, `training_id`, `version`, `model_format`, 
  `model_size`, `model_path`, `accuracy`, `status`, `description`, 
  `download_count`, `deploy_count`, `publish_time`, `created_by`, 
  `created_time`, `updated_time`, `deleted`
) VALUES 
(10001, 'YOLOv5目标检测模型', 1, 1001, 'v1.0.0', 'ONNX', '245MB', '/models/yolov5/v1.0.0/yolov5s.onnx', 0.89, 'published', '基于YOLOv5的实时目标检测模型，适用于交通监控场景', 156, 23, '2024-01-15 10:30:00', 1, '2024-01-10 09:00:00', '2024-01-15 10:30:00', 0),

(10002, 'ResNet50图像分类模型', 2, 1002, 'v2.1.0', 'PyTorch', '98MB', '/models/resnet50/v2.1.0/resnet50.pth', 0.92, 'published', '基于ResNet50的图像分类模型，支持1000种物体识别', 89, 12, '2024-01-20 14:20:00', 1, '2024-01-18 11:30:00', '2024-01-20 14:20:00', 0),

(10003, 'BERT文本情感分析模型', 3, 1003, 'v1.2.0', 'TensorFlow', '420MB', '/models/bert/v1.2.0/bert_sentiment.h5', 0.94, 'published', '基于BERT的中文情感分析模型，准确率高达94%', 234, 45, '2024-02-01 16:45:00', 2, '2024-01-25 13:15:00', '2024-02-01 16:45:00', 0),

(10004, 'UNet图像分割模型', 4, 1004, 'v1.0.0', 'ONNX', '156MB', '/models/unet/v1.0.0/unet.onnx', 0.87, 'testing', '基于UNet的医疗图像分割模型，专用于肺部CT影像分析', 23, 3, NULL, 2, '2024-02-05 08:20:00', '2024-02-05 15:30:00', 0),

(10005, 'Transformer翻译模型', 5, 1005, 'v3.0.0', 'PyTorch', '680MB', '/models/transformer/v3.0.0/transformer.pth', 0.91, 'published', '基于Transformer的中英文翻译模型，支持实时翻译', 167, 28, '2024-02-10 11:00:00', 3, '2024-02-08 10:45:00', '2024-02-10 11:00:00', 0),

(10006, 'MobileNet轻量级检测模型', 1, 1006, 'v1.1.0', 'TensorFlow', '45MB', '/models/mobilenet/v1.1.0/mobilenet.tflite', 0.83, 'published', '基于MobileNet的轻量级目标检测模型，适用于移动设备', 78, 15, '2024-02-15 09:30:00', 1, '2024-02-12 14:00:00', '2024-02-15 09:30:00', 0),

(10007, 'LSTM时序预测模型', 6, 1007, 'v2.0.0', 'ONNX', '89MB', '/models/lstm/v2.0.0/lstm_forecast.onnx', 0.88, 'draft', '基于LSTM的时间序列预测模型，用于股价和天气预测', 0, 0, NULL, 3, '2024-02-18 16:20:00', '2024-02-18 16:20:00', 0),

(10008, 'GAN图像生成模型', 7, 1008, 'v1.0.0', 'PyTorch', '320MB', '/models/gan/v1.0.0/gan_generator.pth', 0.85, 'testing', '基于GAN的图像生成模型，可生成高质量的人脸图像', 12, 1, NULL, 4, '2024-02-20 12:10:00', '2024-02-20 18:45:00', 0),

(10009, 'DeepLab语义分割模型', 8, 1009, 'v2.2.0', 'TensorFlow', '280MB', '/models/deeplab/v2.2.0/deeplab.pb', 0.90, 'published', '基于DeepLabv3+的语义分割模型，适用于自动驾驶场景', 145, 22, '2024-02-25 14:15:00', 2, '2024-02-22 11:30:00', '2024-02-25 14:15:00', 0),

(10010, 'XGBoost决策树模型', 9, 1010, 'v1.3.0', 'ONNX', '67MB', '/models/xgboost/v1.3.0/xgboost.onnx', 0.86, 'published', '基于XGBoost的决策树模型，用于金融风险评估', 95, 18, '2024-03-01 10:40:00', 4, '2024-02-28 09:20:00', '2024-03-01 10:40:00', 0),

(10011, 'YOLO改进版检测模型', 1, 1011, 'v1.0.1', 'ONNX', '260MB', '/models/yolo_improved/v1.0.1/yolo_improved.onnx', 0.91, 'draft', '改进版YOLO目标检测模型，提升了小目标检测能力', 0, 0, NULL, 1, '2024-03-05 15:30:00', '2024-03-05 15:30:00', 0),

(10012, 'ResNet18轻量级分类模型', 2, 1012, 'v1.0.0', 'PyTorch', '45MB', '/models/resnet18/v1.0.0/resnet18.pth', 0.84, 'testing', '基于ResNet18的轻量级图像分类模型，适用于边缘计算', 5, 0, NULL, 2, '2024-03-08 11:45:00', '2024-03-08 17:20:00', 0),

(10013, 'OCR文字识别模型', 10, 1013, 'v2.0.0', 'ONNX', '156MB', '/models/ocr/v2.0.0/ocr_crnn.onnx', 0.93, 'published', '基于CRNN的OCR文字识别模型，支持中英文混合识别', 178, 31, '2024-03-10 13:20:00', 3, '2024-03-08 10:15:00', '2024-03-10 13:20:00', 0),

(10014, 'VGG16特征提取模型', 11, 1014, 'v1.0.0', 'TensorFlow', '520MB', '/models/vgg16/v1.0.0/vgg16.h5', 0.88, 'published', '基于VGG16的特征提取模型，用于图像相似度计算', 67, 9, '2024-03-12 16:30:00', 4, '2024-03-10 14:25:00', '2024-03-12 16:30:00', 0),

(10015, 'Faster R-CNN目标检测模型', 12, 1015, 'v1.5.0', 'PyTorch', '340MB', '/models/faster_rcnn/v1.5.0/faster_rcnn.pth', 0.87, 'testing', '基于Faster R-CNN的目标检测模型，检测精度高', 18, 2, NULL, 1, '2024-03-15 09:40:00', '2024-03-15 16:10:00', 0);

-- 性能优化建议
-- 1. 对于高频查询的字段添加索引
-- 2. 对于大文件路径字段考虑使用TEXT类型
-- 3. 定期清理已删除的记录
-- 4. 对模型文件进行压缩存储
-- 5. 考虑使用分区表提高查询性能

-- 查询示例
-- 查询已发布的模型
SELECT * FROM algorithm_model WHERE status = 'published' AND deleted = 0;

-- 查询某个算法的所有模型版本
SELECT * FROM algorithm_model WHERE algorithm_id = 1 AND deleted = 0 ORDER BY created_time DESC;

-- 查询热门模型（按下载次数排序）
SELECT * FROM algorithm_model WHERE status = 'published' AND deleted = 0 ORDER BY download_count DESC LIMIT 10;

-- 查询模型统计信息
SELECT 
    COUNT(*) as total_count,
    SUM(CASE WHEN status = 'draft' THEN 1 ELSE 0 END) as draft_count,
    SUM(CASE WHEN status = 'testing' THEN 1 ELSE 0 END) as testing_count,
    SUM(CASE WHEN status = 'published' THEN 1 ELSE 0 END) as published_count,
    SUM(download_count) as total_download_count,
    SUM(deploy_count) as total_deploy_count,
    AVG(accuracy) as avg_accuracy
FROM algorithm_model 
WHERE deleted = 0; 