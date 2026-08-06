-- Add the missing smart city management algorithms from the business catalog.
-- The first six entries were seeded by V1_1_3_004. The catalog repeats
-- "暴露垃圾" and "道路积水" in more than one business group; the existing
-- algorithm is retained and its description records both groups.

SET @vls_repo_city_management = (
    SELECT MIN(`id`)
    FROM `vls_algorithm_repository`
    WHERE `tenant_id` = '000000'
      AND `name` = '智慧城管'
      AND `is_deleted` = 0
);

INSERT INTO `vls_algorithm`
    (`tenant_id`, `repository_id`, `name`, `category`, `description`,
     `input_format`, `output_format`, `gpu_required`, `is_system`,
     `create_time`, `update_time`, `status`, `is_deleted`)
SELECT '000000', @vls_repo_city_management, seed.`name`, 'detect', seed.`business_group`,
       'image', 'json', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0
FROM (
    SELECT '无照游商' AS `name`, '街面秩序' AS `business_group`
    UNION ALL SELECT '流浪乞讨人员', '街面秩序'
    UNION ALL SELECT '机动车违停', '街面秩序'
    UNION ALL SELECT '空调外机低挂', '市容环境'
    UNION ALL SELECT '废弃家具', '市容环境'
    UNION ALL SELECT '道路不洁', '市容环境'
    UNION ALL SELECT '橱窗张贴', '市容环境'
    UNION ALL SELECT '气模拱门', '市容环境'
    UNION ALL SELECT '绿地脏乱', '市容环境'
    UNION ALL SELECT '擅自饲养家禽家畜', '市容环境'
    UNION ALL SELECT '不规范垃圾桶', '市容环境'
    UNION ALL SELECT '积存垃圾渣土', '市容环境'
    UNION ALL SELECT '露天烧烤', '市容环境'
    UNION ALL SELECT '动物尸体', '市容环境'
    UNION ALL SELECT '垃圾箱满溢', '市容环境'
    UNION ALL SELECT '私搭乱建', '市容环境'
    UNION ALL SELECT '违规接坡', '市容环境'
    UNION ALL SELECT '道路破损', '市容环境'
    UNION ALL SELECT '水域不洁', '市容环境'
    UNION ALL SELECT '非装饰性树挂', '市容环境'
    UNION ALL SELECT '道路污水', '市容环境'
    UNION ALL SELECT '道路遗撒', '市容环境'
    UNION ALL SELECT '绿化弃料', '市容环境'
    UNION ALL SELECT '焚烧垃圾树叶', '市容环境'
    UNION ALL SELECT '擅自架设管线杆线设施', '市容环境'
    UNION ALL SELECT '违规撑伞', '市容环境'
    UNION ALL SELECT '打包垃圾', '市容环境'
    UNION ALL SELECT '乱堆物堆料', '市容环境'
    UNION ALL SELECT '沿街晾挂', '市容环境'
    UNION ALL SELECT '路面塌陷', '突发事件'
    UNION ALL SELECT '违规牌匾', '宣传广告'
    UNION ALL SELECT '违规标语宣传品', '宣传广告'
    UNION ALL SELECT '违规户外广告', '宣传广告'
    UNION ALL SELECT '街头散发广告', '宣传广告'
    UNION ALL SELECT '非法小广告', '宣传广告'
    UNION ALL SELECT '防撞桶异常', '交通设施'
    UNION ALL SELECT '便道桩异常', '交通设施'
    UNION ALL SELECT '柔性隔离体异常', '交通设施'
    UNION ALL SELECT '施工占道', '施工管理'
    UNION ALL SELECT '施工废弃料', '施工管理'
    UNION ALL SELECT '城市黄土裸露', '施工管理'
    UNION ALL SELECT '安全帽佩戴', '施工管理'
    UNION ALL SELECT '工地物料乱堆', '施工管理'
    UNION ALL SELECT '雨水篦子堵塞', '公用设施'
    UNION ALL SELECT '井盖异常', '公用设施'
    UNION ALL SELECT '雨水篦子破损', '公用设施'
    UNION ALL SELECT '电力设施异常', '公用设施'
    UNION ALL SELECT '消防设施异常', '公用设施'
    UNION ALL SELECT '牌匾破损', '市容环境设施'
    UNION ALL SELECT '宣传栏破损', '市容环境设施'
    UNION ALL SELECT '吸烟视频', '街面行为'
    UNION ALL SELECT '牵狗绳', '街面行为'
    UNION ALL SELECT '口罩佩戴', '街面行为'
    UNION ALL SELECT '街面人员聚集', '街面行为'
    UNION ALL SELECT '街面人员倒地', '街面行为'
    UNION ALL SELECT '街面肢体冲突', '街面行为'
    UNION ALL SELECT '街面物品遗留', '街面行为'
    UNION ALL SELECT '街面快速移动目标', '街面行为'
) AS seed
WHERE @vls_repo_city_management IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM `vls_algorithm` AS existing
      WHERE existing.`repository_id` = @vls_repo_city_management
        AND existing.`name` = seed.`name`
        AND existing.`is_deleted` = 0
  );

UPDATE `vls_algorithm`
SET `description` = CASE `name`
    WHEN '店外经营' THEN '街面秩序'
    WHEN '占道经营' THEN '街面秩序'
    WHEN '杂物堆放' THEN '街面秩序'
    WHEN '暴露垃圾' THEN '街面秩序、市容环境'
    WHEN '道路积水' THEN '街面秩序、突发事件'
    WHEN '户外广告' THEN '街面秩序'
    ELSE `description`
END
WHERE `repository_id` = @vls_repo_city_management
  AND `is_deleted` = 0
  AND `name` IN ('店外经营', '占道经营', '杂物堆放', '暴露垃圾', '道路积水', '户外广告')
  AND (`description` IS NULL OR `description` = '');

UPDATE `vls_algorithm_repository` AS repository
SET repository.`algorithm_count` = (
    SELECT COUNT(*)
    FROM `vls_algorithm` AS algorithm
    WHERE algorithm.`repository_id` = repository.`id`
      AND algorithm.`is_deleted` = 0
)
WHERE repository.`id` = @vls_repo_city_management;
