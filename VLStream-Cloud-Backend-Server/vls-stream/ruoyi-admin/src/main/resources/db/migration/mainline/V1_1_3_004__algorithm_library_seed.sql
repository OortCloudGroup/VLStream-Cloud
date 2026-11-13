-- Seed the default business algorithm libraries and their algorithms.
--
-- The source list contains business categories, descriptions and algorithms.
-- Categories are represented by algorithm repositories; algorithms are linked
-- to the repository for the fixed root tenant used by the initial data.
-- Model files are intentionally left NULL because the source list does not
-- provide deployable PT/ONNX paths.

INSERT INTO `vls_algorithm_repository`
    (`tenant_id`, `name`, `algorithm_count`, `repository_type`, `remark`,
     `create_user`, `create_dept`, `create_time`, `update_user`, `update_time`,
     `status`, `is_deleted`)
SELECT '000000', '智慧工地', 0, 'basic',
       '施工现场全天候智能监管，从人工巡查升级为智能预警',
       NULL, NULL, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, 1, 0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM `vls_algorithm_repository`
    WHERE `tenant_id` = '000000'
      AND `name` = '智慧工地'
      AND `is_deleted` = 0
);

INSERT INTO `vls_algorithm_repository`
    (`tenant_id`, `name`, `algorithm_count`, `repository_type`, `remark`,
     `create_user`, `create_dept`, `create_time`, `update_user`, `update_time`,
     `status`, `is_deleted`)
SELECT '000000', '智慧水利', 0, 'basic',
       '看水位、更看懂水域风险，视频与雨量、水位、水质数据协同，全面感知水域态势，提前预警风险，守护水域安全。',
       NULL, NULL, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, 1, 0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM `vls_algorithm_repository`
    WHERE `tenant_id` = '000000'
      AND `name` = '智慧水利'
      AND `is_deleted` = 0
);

INSERT INTO `vls_algorithm_repository`
    (`tenant_id`, `name`, `algorithm_count`, `repository_type`, `remark`,
     `create_user`, `create_dept`, `create_time`, `update_user`, `update_time`,
     `status`, `is_deleted`)
SELECT '000000', '污水处理', 0, 'basic',
       'AI看守处理流程，异常自动发现，助力无人值守',
       NULL, NULL, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, 1, 0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM `vls_algorithm_repository`
    WHERE `tenant_id` = '000000'
      AND `name` = '污水处理'
      AND `is_deleted` = 0
);

INSERT INTO `vls_algorithm_repository`
    (`tenant_id`, `name`, `algorithm_count`, `repository_type`, `remark`,
     `create_user`, `create_dept`, `create_time`, `update_user`, `update_time`,
     `status`, `is_deleted`)
SELECT '000000', '智慧城管', 0, 'basic',
       '城市问题自动发现，从被动巡查转向主动发现',
       NULL, NULL, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, 1, 0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM `vls_algorithm_repository`
    WHERE `tenant_id` = '000000'
      AND `name` = '智慧城管'
      AND `is_deleted` = 0
);

INSERT INTO `vls_algorithm_repository`
    (`tenant_id`, `name`, `algorithm_count`, `repository_type`, `remark`,
     `create_user`, `create_dept`, `create_time`, `update_user`, `update_time`,
     `status`, `is_deleted`)
SELECT '000000', '智慧社区园区', 0, 'basic',
       '老旧监控也能智能升级，前端设备利旧，提升社区治理效率',
       NULL, NULL, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, 1, 0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM `vls_algorithm_repository`
    WHERE `tenant_id` = '000000'
      AND `name` = '智慧社区园区'
      AND `is_deleted` = 0
);

INSERT INTO `vls_algorithm_repository`
    (`tenant_id`, `name`, `algorithm_count`, `repository_type`, `remark`,
     `create_user`, `create_dept`, `create_time`, `update_user`, `update_time`,
     `status`, `is_deleted`)
SELECT '000000', '智慧校园', 0, 'basic',
       '从校园周界到行为安全，全天候感知风险，提升校园安全管理效率',
       NULL, NULL, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, 1, 0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM `vls_algorithm_repository`
    WHERE `tenant_id` = '000000'
      AND `name` = '智慧校园'
      AND `is_deleted` = 0
);

INSERT INTO `vls_algorithm_repository`
    (`tenant_id`, `name`, `algorithm_count`, `repository_type`, `remark`,
     `create_user`, `create_dept`, `create_time`, `update_user`, `update_time`,
     `status`, `is_deleted`)
SELECT '000000', '危化安全生产', 0, 'basic',
       'AI守护高风险作业现场，发现违规自动预警，降低监管压力',
       NULL, NULL, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, 1, 0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM `vls_algorithm_repository`
    WHERE `tenant_id` = '000000'
      AND `name` = '危化安全生产'
      AND `is_deleted` = 0
);

INSERT INTO `vls_algorithm_repository`
    (`tenant_id`, `name`, `algorithm_count`, `repository_type`, `remark`,
     `create_user`, `create_dept`, `create_time`, `update_user`, `update_time`,
     `status`, `is_deleted`)
SELECT '000000', '明厨亮灶', 0, 'basic',
       '后厨违规，AI主动提醒，食品安全与环境卫生协同监管',
       NULL, NULL, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, 1, 0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM `vls_algorithm_repository`
    WHERE `tenant_id` = '000000'
      AND `name` = '明厨亮灶'
      AND `is_deleted` = 0
);

INSERT INTO `vls_algorithm_repository`
    (`tenant_id`, `name`, `algorithm_count`, `repository_type`, `remark`,
     `create_user`, `create_dept`, `create_time`, `update_user`, `update_time`,
     `status`, `is_deleted`)
SELECT '000000', '加油站安全监管', 0, 'basic',
       '高风险行为及时发现，重点区域全天候智能监测',
       NULL, NULL, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, 1, 0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM `vls_algorithm_repository`
    WHERE `tenant_id` = '000000'
      AND `name` = '加油站安全监管'
      AND `is_deleted` = 0
);

SET @vls_repo_smart_construction = (
    SELECT MIN(`id`) FROM `vls_algorithm_repository`
    WHERE `tenant_id` = '000000' AND `name` = '智慧工地' AND `is_deleted` = 0
);
SET @vls_repo_smart_water = (
    SELECT MIN(`id`) FROM `vls_algorithm_repository`
    WHERE `tenant_id` = '000000' AND `name` = '智慧水利' AND `is_deleted` = 0
);
SET @vls_repo_wastewater = (
    SELECT MIN(`id`) FROM `vls_algorithm_repository`
    WHERE `tenant_id` = '000000' AND `name` = '污水处理' AND `is_deleted` = 0
);
SET @vls_repo_city_management = (
    SELECT MIN(`id`) FROM `vls_algorithm_repository`
    WHERE `tenant_id` = '000000' AND `name` = '智慧城管' AND `is_deleted` = 0
);
SET @vls_repo_community_park = (
    SELECT MIN(`id`) FROM `vls_algorithm_repository`
    WHERE `tenant_id` = '000000' AND `name` = '智慧社区园区' AND `is_deleted` = 0
);
SET @vls_repo_smart_campus = (
    SELECT MIN(`id`) FROM `vls_algorithm_repository`
    WHERE `tenant_id` = '000000' AND `name` = '智慧校园' AND `is_deleted` = 0
);
SET @vls_repo_hazardous_production = (
    SELECT MIN(`id`) FROM `vls_algorithm_repository`
    WHERE `tenant_id` = '000000' AND `name` = '危化安全生产' AND `is_deleted` = 0
);
SET @vls_repo_open_kitchen = (
    SELECT MIN(`id`) FROM `vls_algorithm_repository`
    WHERE `tenant_id` = '000000' AND `name` = '明厨亮灶' AND `is_deleted` = 0
);
SET @vls_repo_gas_station = (
    SELECT MIN(`id`) FROM `vls_algorithm_repository`
    WHERE `tenant_id` = '000000' AND `name` = '加油站安全监管' AND `is_deleted` = 0
);

INSERT INTO `vls_algorithm`
    (`tenant_id`, `repository_id`, `name`, `category`, `description`, `image_url`,
     `pt_model_file_path`, `onnx_model_file_path`, `config_params`, `input_format`,
     `output_format`, `gpu_required`, `is_system`, `create_user`, `create_dept`,
     `create_time`, `update_user`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_smart_construction, '安全帽检测', 'detect', NULL, NULL,
       NULL, NULL, NULL, 'image', 'json', 0, 1, NULL, NULL, CURRENT_TIMESTAMP,
       NULL, CURRENT_TIMESTAMP, 1, 0
FROM DUAL
WHERE @vls_repo_smart_construction IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `vls_algorithm`
      WHERE `repository_id` = @vls_repo_smart_construction AND `name` = '安全帽检测'
  );

INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_smart_construction, '安全带检测', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_smart_construction IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_smart_construction AND `name` = '安全带检测');
INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_smart_construction, '吊笼超员', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_smart_construction IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_smart_construction AND `name` = '吊笼超员');
INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_smart_construction, '区域入侵', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_smart_construction IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_smart_construction AND `name` = '区域入侵');
INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_smart_construction, '物品遗留', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_smart_construction IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_smart_construction AND `name` = '物品遗留');
INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_smart_construction, '烟火检测', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_smart_construction IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_smart_construction AND `name` = '烟火检测');

INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_smart_water, '水尺识别', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_smart_water IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_smart_water AND `name` = '水尺识别');
INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_smart_water, '排水检测', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_smart_water IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_smart_water AND `name` = '排水检测');
INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_smart_water, '漂浮物检测', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_smart_water IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_smart_water AND `name` = '漂浮物检测');
INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_smart_water, '游泳检测', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_smart_water IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_smart_water AND `name` = '游泳检测');
INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_smart_water, '钓鱼检测', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_smart_water IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_smart_water AND `name` = '钓鱼检测');
INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_smart_water, '区域入侵', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_smart_water IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_smart_water AND `name` = '区域入侵');

INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_wastewater, '跑泥检测', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_wastewater IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_wastewater AND `name` = '跑泥检测');
INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_wastewater, '断泥检测', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_wastewater IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_wastewater AND `name` = '断泥检测');
INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_wastewater, '污泥满溢', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_wastewater IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_wastewater AND `name` = '污泥满溢');
INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_wastewater, '水质表识别', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_wastewater IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_wastewater AND `name` = '水质表识别');
INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_wastewater, '排药量检测', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_wastewater IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_wastewater AND `name` = '排药量检测');

INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_city_management, '店外经营', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_city_management IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_city_management AND `name` = '店外经营');
INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_city_management, '占道经营', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_city_management IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_city_management AND `name` = '占道经营');
INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_city_management, '杂物堆放', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_city_management IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_city_management AND `name` = '杂物堆放');
INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_city_management, '暴露垃圾', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_city_management IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_city_management AND `name` = '暴露垃圾');
INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_city_management, '道路积水', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_city_management IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_city_management AND `name` = '道路积水');
INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_city_management, '户外广告', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_city_management IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_city_management AND `name` = '户外广告');

INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_community_park, '人脸识别', 'faceDetect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_community_park IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_community_park AND `name` = '人脸识别');
INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_community_park, '车辆违停', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_community_park IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_community_park AND `name` = '车辆违停');
INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_community_park, '电动车进电梯', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_community_park IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_community_park AND `name` = '电动车进电梯');
INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_community_park, '消防通道占用', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_community_park IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_community_park AND `name` = '消防通道占用');
INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_community_park, '高空抛物', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_community_park IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_community_park AND `name` = '高空抛物');
INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_community_park, '烟火检测', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_community_park IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_community_park AND `name` = '烟火检测');

INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_smart_campus, '人脸识别', 'faceDetect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_smart_campus IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_smart_campus AND `name` = '人脸识别');
INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_smart_campus, '人员徘徊', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_smart_campus IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_smart_campus AND `name` = '人员徘徊');
INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_smart_campus, '翻越围墙', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_smart_campus IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_smart_campus AND `name` = '翻越围墙');
INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_smart_campus, '异常聚集', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_smart_campus IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_smart_campus AND `name` = '异常聚集');
INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_smart_campus, '奔跑摔倒', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_smart_campus IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_smart_campus AND `name` = '奔跑摔倒');
INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_smart_campus, '烟火检测', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_smart_campus IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_smart_campus AND `name` = '烟火检测');

INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_hazardous_production, '安全帽检测', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_hazardous_production IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_hazardous_production AND `name` = '安全帽检测');
INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_hazardous_production, '非正规抽烟', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_hazardous_production IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_hazardous_production AND `name` = '非正规抽烟');
INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_hazardous_production, '接打电话', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_hazardous_production IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_hazardous_production AND `name` = '接打电话');
INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_hazardous_production, '烟火检测', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_hazardous_production IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_hazardous_production AND `name` = '烟火检测');
INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_hazardous_production, '人员脱岗', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_hazardous_production IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_hazardous_production AND `name` = '人员脱岗');
INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_hazardous_production, '区域入侵', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_hazardous_production IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_hazardous_production AND `name` = '区域入侵');

INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_open_kitchen, '厨师帽检测', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_open_kitchen IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_open_kitchen AND `name` = '厨师帽检测');
INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_open_kitchen, '厨师服检测', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_open_kitchen IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_open_kitchen AND `name` = '厨师服检测');
INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_open_kitchen, '口罩检测', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_open_kitchen IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_open_kitchen AND `name` = '口罩检测');
INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_open_kitchen, '手套检测', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_open_kitchen IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_open_kitchen AND `name` = '手套检测');
INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_open_kitchen, '垃圾桶未盖', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_open_kitchen IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_open_kitchen AND `name` = '垃圾桶未盖');
INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_open_kitchen, '鼠患检测', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_open_kitchen IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_open_kitchen AND `name` = '鼠患检测');

INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_gas_station, '违规抽烟', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_gas_station IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_gas_station AND `name` = '违规抽烟');
INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_gas_station, '接打电话', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_gas_station IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_gas_station AND `name` = '接打电话');
INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_gas_station, '烟火检测', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_gas_station IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_gas_station AND `name` = '烟火检测');
INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_gas_station, '液体泄漏', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_gas_station IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_gas_station AND `name` = '液体泄漏');
INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_gas_station, '卸油管异常', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_gas_station IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_gas_station AND `name` = '卸油管异常');
INSERT INTO `vls_algorithm` (`tenant_id`, `repository_id`, `name`, `category`, `input_format`, `output_format`, `gpu_required`, `is_system`, `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_gas_station, '消防设施检测', 'detect', 'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0 FROM DUAL
WHERE @vls_repo_gas_station IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `vls_algorithm` WHERE `repository_id` = @vls_repo_gas_station AND `name` = '消防设施检测');

UPDATE `vls_algorithm_repository` r
SET `algorithm_count` = (
    SELECT COUNT(*)
    FROM `vls_algorithm` AS a
    WHERE a.`repository_id` = r.`id`
      AND a.`is_deleted` = 0
)
WHERE r.`id` IN (
    @vls_repo_smart_construction,
    @vls_repo_smart_water,
    @vls_repo_wastewater,
    @vls_repo_city_management,
    @vls_repo_community_park,
    @vls_repo_smart_campus,
    @vls_repo_hazardous_production,
    @vls_repo_open_kitchen,
    @vls_repo_gas_station
);
