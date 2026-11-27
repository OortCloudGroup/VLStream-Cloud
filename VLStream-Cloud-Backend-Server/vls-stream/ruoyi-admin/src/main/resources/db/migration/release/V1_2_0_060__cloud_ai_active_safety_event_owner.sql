-- Keep the active-safety identifier numeric in both modes; single mode uses no=0.
-- The discriminator prevents one event from having two independently mutable owners.
SET @cloud_sink_column = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name='vls_cloud_ai_event' AND column_name='event_sink');
SET @cloud_sink_ddl = IF(@cloud_sink_column=0,
    'ALTER TABLE vls_cloud_ai_event ADD COLUMN event_sink varchar(16) NOT NULL DEFAULT ''VLS'' COMMENT ''VLS multi-tenant or LOCAL single-tenant event owner''',
    'SELECT 1');
PREPARE cloud_sink_stmt FROM @cloud_sink_ddl;
EXECUTE cloud_sink_stmt;
DEALLOCATE PREPARE cloud_sink_stmt;

-- Correct only pre-existing single-tenant cloud detections. Multi-tenant events start
-- with is_report=0 and keep an outbox row after delivery; they must not be rerouted.
INSERT INTO oort_task_event
    (id,no,tenant_id,uuid,name,data,status,item,client,mod_type,mod_status,
     device_id,device_name,device_tag,work_order_status,created_at,updated_at,deleted_at)
SELECT CAST(c.event_id AS CHAR),0,c.tenant_id,LEFT(e.report_device,40),LEFT(e.event_type,20),
    JSON_OBJECT('describe',COALESCE(e.event_desc,''),
        'point',JSON_OBJECT('address',COALESCE(e.report_location,'')),
        'pics',JSON_ARRAY(CONCAT('vls-media://',c.annotated_media_id),CONCAT('vls-media://',c.original_media_id)),
        'send_pics',JSON_ARRAY(),
        'video',IF(c.video_media_id IS NULL,JSON_ARRAY(),JSON_ARRAY(CONCAT('vls-media://',c.video_media_id))),
        'cloud_ai_event_id',CAST(c.event_id AS CHAR),
        'mqtt_message_id',e.mqtt_message_id,'device_event_id',c.id,'media_id',c.annotated_media_id,
        'mqtt_payload',CAST(c.detail_json AS JSON),'event_time',DATE_FORMAT(e.report_time,'%Y-%m-%d %H:%i:%s')),
    IF(e.event_status IN ('completed','closed'),1,2),LEFT(e.event_type,40),'camera',2,
    IF(e.event_status='pending',0,IF(e.event_status='closed',3,1)),LEFT(e.report_device,40),
    LEFT(COALESCE(JSON_UNQUOTE(JSON_EXTRACT(c.detail_json,'$.cameraName')),e.report_device),20),'',0,
    e.report_time,NOW(),IF(e.is_deleted=0,0,UNIX_TIMESTAMP()*1000)
FROM vls_cloud_ai_event c JOIN vls_event_management e ON e.id=c.event_id AND e.tenant_id=c.tenant_id
WHERE c.event_sink='VLS' AND e.is_report=1
    AND NOT EXISTS (SELECT 1 FROM vls_event_report_outbox o WHERE o.event_id=e.id)
    AND NOT EXISTS (SELECT 1 FROM oort_task_event l WHERE l.id=CAST(c.event_id AS CHAR) COLLATE utf8mb4_general_ci AND l.no=0);

UPDATE vls_cloud_ai_event c
JOIN oort_task_event l ON l.id=CAST(c.event_id AS CHAR) COLLATE utf8mb4_general_ci AND l.no=0 AND l.tenant_id=c.tenant_id
JOIN vls_event_management e ON e.id=c.event_id AND e.tenant_id=c.tenant_id
SET c.event_sink='LOCAL'
WHERE c.event_sink='VLS' AND e.is_report=1
    AND JSON_UNQUOTE(JSON_EXTRACT(l.data,'$.cloud_ai_event_id'))=CAST(c.event_id AS CHAR)
    AND NOT EXISTS (SELECT 1 FROM vls_event_report_outbox o WHERE o.event_id=e.id);

-- Retain the earlier row for audit, but remove it from the independently editable view.
UPDATE vls_event_management e JOIN vls_cloud_ai_event c ON c.event_id=e.id AND c.tenant_id=e.tenant_id
SET e.is_deleted=1 WHERE c.event_sink='LOCAL' AND e.is_report=1
    AND NOT EXISTS (SELECT 1 FROM vls_event_report_outbox o WHERE o.event_id=e.id);
