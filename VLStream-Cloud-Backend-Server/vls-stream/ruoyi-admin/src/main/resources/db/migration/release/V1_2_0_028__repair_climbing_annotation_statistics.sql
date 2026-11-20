-- Recover verified derived statistics only. No images or annotation instances are modified.
UPDATE vls_algorithm_annotation p
JOIN (
    SELECT i.annotation_Id project_id, COUNT(*) total,
           SUM(EXISTS(SELECT 1 FROM vls_annotation_instance a
                      WHERE a.annotation_id=i.annotation_Id AND a.image_id=i.id AND a.is_deleted=0)) annotated
    FROM vls_annotation_image i
    WHERE i.annotation_Id=2105134734835503106 AND i.tenant_id='000000' AND i.is_deleted=0
    GROUP BY i.annotation_Id
) counts ON counts.project_id=p.id
SET p.total_count=counts.total, p.annotated_count=counts.annotated,
    p.progress=LEAST(100,FLOOR(counts.annotated*100/counts.total)),
    p.annotation_status=CASE WHEN counts.annotated=0 THEN 'none'
                            WHEN counts.annotated>=counts.total THEN 'completed' ELSE 'partial' END
WHERE p.id=2105134734835503106 AND p.tenant_id='000000' AND p.is_deleted=0;
