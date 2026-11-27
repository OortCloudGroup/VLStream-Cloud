-- Preserve the task type selected when an external PT model is imported.
-- Existing training models remain NULL until their type is explicitly known.
ALTER TABLE vls_algorithm_model
    ADD COLUMN annotation_type VARCHAR(32) NULL COMMENT 'image_classification/object_detection/instance_segmentation/semantic_segmentation'
    AFTER model_format;
