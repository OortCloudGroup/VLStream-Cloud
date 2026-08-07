-- Ensure the standalone distribution has one usable basic algorithm repository.
-- This migration is idempotent and never overwrites existing algorithms.

INSERT INTO `vls_algorithm_repository`
  (`tenant_id`, `name`, `algorithm_count`, `repository_type`, `remark`, `create_time`, `status`, `is_deleted`)
SELECT
  '000000', 'Basic preset算法库', 0, 'basic', 'Built-in algorithm presets', NOW(), 1, 0
WHERE NOT EXISTS (
  SELECT 1
  FROM `vls_algorithm_repository`
  WHERE `repository_type` = 'basic' AND `is_deleted` = 0
);

SET @vlstream_basic_repository_id = (
  SELECT `id`
  FROM `vls_algorithm_repository`
  WHERE `repository_type` = 'basic' AND `is_deleted` = 0
  ORDER BY `id`
  LIMIT 1
);

INSERT INTO `vls_algorithm`
  (`tenant_id`, `repository_id`, `name`, `category`, `description`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `status`, `is_deleted`)
SELECT '000000', @vlstream_basic_repository_id, preset.name, preset.category, preset.description,
       'image,video', preset.output_format, 0, 1, NOW(), 1, 0
FROM (
  SELECT 'Object detection' AS name, 'detect' AS category, 'General object detection' AS description, 'bbox' AS output_format
  UNION ALL SELECT 'Instance segmentation', 'segment', 'Precise contours and defect segmentation', 'mask'
  UNION ALL SELECT 'Image classification', 'classify', 'Image-level classification', 'class'
  UNION ALL SELECT '关键点检测', 'pose', '人体姿态与动作关键点检测', 'keypoint'
  UNION ALL SELECT '旋转目标检测', 'obb', '遥感、航拍与文本目标检测', 'obb'
  UNION ALL SELECT '人脸识别', 'faceDetect', '人脸检测与识别', 'face'
  UNION ALL SELECT '行人检测', 'personDetect', '行人目标检测', 'bbox'
) AS preset
WHERE @vlstream_basic_repository_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `vls_algorithm` existing
    WHERE existing.`repository_id` = @vlstream_basic_repository_id
      AND existing.`category` = preset.category
      AND existing.`is_deleted` = 0
  );

UPDATE `vls_algorithm_repository` repository
SET repository.`algorithm_count` = (
  SELECT COUNT(*)
  FROM `vls_algorithm` algorithm
  WHERE algorithm.`repository_id` = repository.`id`
    AND algorithm.`is_deleted` = 0
)
WHERE repository.`id` = @vlstream_basic_repository_id;
