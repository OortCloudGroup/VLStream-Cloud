-- ZIP imports previously counted boxes as annotated images. Recompute from active images.
-- Match the image eligibility used by countActiveImages/countDistinctAnnotatedImages.
UPDATE vls_algorithm_annotation AS project
LEFT JOIN (
    SELECT image.annotation_id, COUNT(*) AS total_count,
           SUM(CASE WHEN EXISTS (
               SELECT 1 FROM vls_annotation_instance AS instance
               WHERE instance.annotation_id = image.annotation_id
                 AND instance.image_id = image.id AND instance.is_deleted = 0
           ) THEN 1 ELSE 0 END) AS annotated_count
    FROM vls_annotation_image AS image
    WHERE image.is_deleted = 0 AND image.media_type = 'image'
      AND image.quality_status != 'excluded'
    GROUP BY image.annotation_id
) AS counts ON counts.annotation_id = project.id
SET project.total_count = COALESCE(counts.total_count, 0),
    project.annotated_count = COALESCE(counts.annotated_count, 0),
    project.progress = CASE WHEN COALESCE(counts.total_count, 0) = 0 THEN 0
        ELSE FLOOR(counts.annotated_count * 100 / counts.total_count) END,
    project.annotation_status = CASE
        WHEN COALESCE(counts.annotated_count, 0) = 0 THEN 'none'
        WHEN counts.annotated_count = counts.total_count THEN 'completed'
        ELSE 'partial' END
WHERE project.is_deleted = 0;
