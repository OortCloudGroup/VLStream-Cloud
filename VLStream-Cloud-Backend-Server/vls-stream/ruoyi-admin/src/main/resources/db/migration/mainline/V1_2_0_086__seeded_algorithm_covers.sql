-- Backfill covers for initialized libraries and their copies in existing tenants.
-- Only initialized rows with empty covers qualify. Asset keys are resolved by the frontend.
UPDATE vls_algorithm AS algorithm
JOIN vls_algorithm_repository AS repository
  ON repository.id = algorithm.repository_id
 AND repository.tenant_id = algorithm.tenant_id
JOIN (
    SELECT '智慧工地' AS repository_name, 'smart-construction' AS cover_directory
    UNION ALL SELECT '智慧水利', 'smart-water'
    UNION ALL SELECT '污水处理', 'wastewater'
    UNION ALL SELECT '智慧城管', 'city-management'
    UNION ALL SELECT '智慧社区园区', 'community-park'
    UNION ALL SELECT '智慧校园', 'smart-campus'
    UNION ALL SELECT '危化安全生产', 'hazardous-production'
    UNION ALL SELECT '明厨亮灶', 'open-kitchen'
    UNION ALL SELECT '加油站安全监管', 'gas-station'
) AS cover ON BINARY repository.name = BINARY cover.repository_name
SET algorithm.image_url = CONCAT('builtin://algorithm-covers/', cover.cover_directory, '/', algorithm.name, '.webp')
WHERE algorithm.is_system = 1
  AND algorithm.is_deleted = 0
  AND algorithm.create_user IS NULL
  AND repository.is_deleted = 0
  AND repository.create_user IS NULL
  AND repository.repository_type = 'basic'
  AND NULLIF(TRIM(algorithm.image_url), '') IS NULL
  AND CASE cover.cover_directory
      WHEN 'smart-construction' THEN BINARY algorithm.name IN (
          '区域入侵', '吊笼超员', '安全带检测', '安全帽检测',
          '烟火检测', '物品遗留'
      )
      WHEN 'smart-water' THEN BINARY algorithm.name IN (
          '区域入侵', '排水检测', '水尺识别', '游泳检测',
          '漂浮物检测', '钓鱼检测'
      )
      WHEN 'wastewater' THEN BINARY algorithm.name IN (
          '排药量检测', '断泥检测', '水质表识别', '污泥满溢',
          '跑泥检测'
      )
      WHEN 'city-management' THEN BINARY algorithm.name IN (
          '井盖异常', '便道桩异常', '口罩佩戴', '吸烟视频',
          '城市黄土裸露', '安全帽佩戴', '宣传栏破损', '工地物料乱堆',
          '施工占道', '施工废弃料', '柔性隔离体异常', '消防设施异常',
          '牌匾破损', '牵狗绳', '电力设施异常', '街面人员倒地',
          '街面人员聚集', '街面快速移动目标', '街面物品遗留', '街面肢体冲突',
          '防撞桶异常', '雨水篦子堵塞', '雨水篦子破损', '非法小广告'
      )
      WHEN 'community-park' THEN BINARY algorithm.name IN (
          '人脸识别', '消防通道占用', '烟火检测', '电动车进电梯',
          '车辆违停', '高空抛物'
      )
      WHEN 'smart-campus' THEN BINARY algorithm.name IN (
          '人员徘徊', '人脸识别', '奔跑摔倒', '异常聚集',
          '烟火检测', '翻越围墙'
      )
      WHEN 'hazardous-production' THEN BINARY algorithm.name IN (
          '人员脱岗', '区域入侵', '安全帽检测', '接打电话',
          '烟火检测', '非正规抽烟'
      )
      WHEN 'open-kitchen' THEN BINARY algorithm.name IN (
          '厨师帽检测', '厨师服检测', '口罩检测', '垃圾桶未盖',
          '手套检测', '鼠患检测'
      )
      WHEN 'gas-station' THEN BINARY algorithm.name IN (
          '卸油管异常', '接打电话', '消防设施检测', '液体泄漏',
          '烟火检测', '违规抽烟'
      )
      ELSE FALSE
  END;
