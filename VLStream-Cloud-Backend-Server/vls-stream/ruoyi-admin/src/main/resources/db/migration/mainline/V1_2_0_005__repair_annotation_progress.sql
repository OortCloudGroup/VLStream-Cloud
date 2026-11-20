UPDATE `vls_algorithm_annotation` AS annotation_project
LEFT JOIN (
    SELECT `annotation_id`, COUNT(DISTINCT `image_id`) AS `annotated_count`
    FROM `vls_annotation_instance`
    WHERE `is_deleted` = 0
    GROUP BY `annotation_id`
) AS annotation_summary
    ON annotation_summary.`annotation_id` = annotation_project.`id`
LEFT JOIN (
    SELECT `annotation_id`, COUNT(*) AS `total_count`
    FROM `vls_annotation_image`
    WHERE `is_deleted` = 0
    GROUP BY `annotation_id`
) AS image_summary
    ON image_summary.`annotation_id` = annotation_project.`id`
SET annotation_project.`total_count` = COALESCE(image_summary.`total_count`, 0),
    annotation_project.`annotated_count` = COALESCE(annotation_summary.`annotated_count`, 0),
    annotation_project.`progress` = CASE
        WHEN COALESCE(image_summary.`total_count`, 0) = 0 THEN 0
        ELSE LEAST(
            100,
            FLOOR(
                COALESCE(annotation_summary.`annotated_count`, 0) * 100
                / image_summary.`total_count`
            )
        )
    END,
    annotation_project.`annotation_status` = CASE
        WHEN COALESCE(image_summary.`total_count`, 0) = 0 THEN 'none'
        WHEN COALESCE(annotation_summary.`annotated_count`, 0) = 0 THEN 'none'
        WHEN COALESCE(annotation_summary.`annotated_count`, 0) >= image_summary.`total_count`
            THEN 'completed'
        ELSE 'partial'
    END
WHERE annotation_project.`is_deleted` = 0;
