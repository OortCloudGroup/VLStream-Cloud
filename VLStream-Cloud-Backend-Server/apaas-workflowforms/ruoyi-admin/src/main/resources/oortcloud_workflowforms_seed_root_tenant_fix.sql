SET NAMES utf8mb4;

-- Repair seed data generated before the fixed root tenant id was known.
-- This is safe to run more than once.
UPDATE `wf_form_synthesis`
SET `tenant_id` = '0e391fd7-1033-4f09-88c0-187582fee462'
WHERE `tenant_id` IN ('24cdafd7-528a-4514-92ff-76f474fb8781', '0b3c0131-f4be-11ef-8496-6cf6da435cd3')
  AND `category_name` = '系统审批'
  AND COALESCE(`type`, '') = '0'
  AND COALESCE(`del_flag`, '0') = '0';

UPDATE `wf_form`
SET `tenant_id` = '0e391fd7-1033-4f09-88c0-187582fee462'
WHERE `tenant_id` IN ('24cdafd7-528a-4514-92ff-76f474fb8781', '0b3c0131-f4be-11ef-8496-6cf6da435cd3')
  AND `category_id` = '1971042914271039489'
  AND COALESCE(`type`, '') = '0'
  AND `form_name` IN ('租户审批', '企业主体审批', '部门审批', '用户审批', '用户权限提升申请审批', '租户认证审批');
