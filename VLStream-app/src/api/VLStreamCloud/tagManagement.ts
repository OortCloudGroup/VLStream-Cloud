import { request } from '@/utils/service'
import config from '@/config'

function commonFunc<T = any, K = any>(interfaceName: string, data: T, method = 'post') {
  return request<K>({
    url: config.URL + config.gateWay + 'vls-server/' + interfaceName,
    method: method,
    data: data
  })
}

/**
 * 分页查询标签信息
 * @param {Object} params - 查询参数
 * @param {number} params.current - 当前页
 * @param {number} params.size - 每页大小
 * @param {string} params.keyword - 搜索关键词
 * @param {string} params.categoryType - 标签大类
 * @param {number} params.level - 标签层级
 * @param {number} params.parentId - 父级ID
 */
export function getTagManagementPage(params) {
  return commonFunc('/vlsTagManagement/page', params, 'GET')
}

/**
 * 获取标签树形结构（用于左侧导航）
 */
export function getTagTree() {
  return commonFunc('/vlsTagManagement/tree', {}, 'GET')
}

/**
 * 获取标签详情
 * @param {number} id - 标签ID
 */
export function getTagById(id) {
  return commonFunc(`/vlsTagManagement/${id}`, {}, 'GET')
}

/**
 * 创建标签
 * @param {Object} data - 标签数据
 */
export function createTag(data) {
  return commonFunc('/vlsTagManagement', data)
}

/**
 * 更新标签
 * @param {number} id - 标签ID
 * @param {Object} data - 更新数据
 */
export function updateTag(id, data) {
  return commonFunc(`/vlsTagManagement/${id}`, data, 'PUT')
}

/**
 * 删除标签
 * @param {number} id - 标签ID
 */
export function deleteTag(id) {
  return commonFunc(`/vlsTagManagement/${id}`, {}, 'DELETE')
}

/**
 * 批量删除标签
 * @param {Array} ids - 标签ID数组
 */
export function batchDeleteTags(ids) {
  return commonFunc('/vlsTagManagement/batch', ids, 'DELETE')
}

/**
 * 获取标签统计信息
 */
export function getTagStatistics() {
  return commonFunc('/vlsTagManagement/statistics', {}, 'GET')
}

/**
 * 获取标签的使用统计
 * @param {number} id - 标签ID
 */
export function getTagUsageStats(id) {
  return commonFunc(`/vlsTagManagement/${id}/stats`, {}, 'GET')
}

/**
 * 检查标签名称是否重复
 * @param {string} tagName - 标签名称
 * @param {number} parentId - 父级ID（可选）
 * @param {number} excludeId - 排除的ID（可选，用于编辑时验证）
 */
export function checkTagNameDuplicate(tagName: string, parentId: number | null = null, excludeId: number | null = null) {
  const params: { tagName: string; parentId?: number; excludeId?: number } = { tagName }
  if (parentId !== null) params.parentId = parentId
  if (excludeId !== null) params.excludeId = excludeId
  return commonFunc('/vlsTagManagement/check-name', params, 'GET')
}

/**
 * 获取标签的所有子标签
 * @param {number} parentId - 父级ID
 */
export function getChildTags(parentId) {
  return commonFunc(`/vlsTagManagement/${parentId}/children`, {}, 'GET')
}

/**
 * 移动标签到新的父级
 * @param {number} id - 标签ID
 * @param {number} newParentId - 新的父级ID
 */
export function moveTag(id, newParentId) {
  return commonFunc(`/vlsTagManagement/${id}/move`, { newParentId }, 'PUT')
}

// 设备标签关联 API
/**
 * 获取设备的所有标签
 * @param {number} deviceId - 设备ID
 */
export function getDeviceTags(deviceId) {
  return commonFunc(`/api/device-tag-relation/device/${deviceId}/tags`, {}, 'GET')
}

/**
 * 获取标签关联的所有设备
 * @param {number} tagId - 标签ID
 */
export function getTagDevices(tagId) {
  return commonFunc(`/api/device-tag-relation/tag/${tagId}/devices`, {}, 'GET')
}

/**
 * 为设备添加标签
 * @param {number} deviceId - 设备ID
 * @param {number} tagId - 标签ID
 * @param {string} createdBy - 创建者
 */
export function addDeviceTag(deviceId, tagId, createdBy = 'system') {
  return commonFunc(
    `/api/device-tag-relation/device/${deviceId}/tag/${tagId}`,
    { createdBy },
    'POST'
  )
}

/**
 * 批量为设备添加标签
 * @param {number} deviceId - 设备ID
 * @param {Array} tagIds - 标签ID数组
 * @param {string} createdBy - 创建者
 */
export function addDeviceTags(deviceId, tagIds, createdBy = 'system') {
  return commonFunc(
    `/api/device-tag-relation/device/${deviceId}/tags`,
    { tagIds, createdBy },
    'POST'
  )
}

/**
 * 移除设备标签
 * @param {number} deviceId - 设备ID
 * @param {number} tagId - 标签ID
 */
export function removeDeviceTag(deviceId, tagId) {
  return commonFunc(
    `/api/device-tag-relation/device/${deviceId}/tag/${tagId}`,
    {},
    'DELETE'
  )
}

/**
 * 批量移除设备标签
 * @param {number} deviceId - 设备ID
 * @param {Array} tagIds - 标签ID数组
 */
export function removeDeviceTags(deviceId, tagIds) {
  return commonFunc(
    `/api/device-tag-relation/device/${deviceId}/tags`,
    tagIds,
    'DELETE'
  )
}

/**
 * 更新设备的所有标签
 * @param {number} deviceId - 设备ID
 * @param {Array} tagIds - 标签ID数组
 * @param {string} createdBy - 创建者
 */
export function updateDeviceTags(deviceId, tagIds, createdBy = 'system') {
  return commonFunc(
    `/api/device-tag-relation/device/${deviceId}/tags`,
    { tagIds, createdBy },
    'PUT'
  )
}

/**
 * 删除设备的所有标签关联
 * @param {number} deviceId - 设备ID
 */
export function removeAllDeviceTags(deviceId) {
  return commonFunc(
    `/api/device-tag-relation/device/${deviceId}/all-tags`,
    {},
    'DELETE'
  )
}

/**
 * 删除标签的所有设备关联
 * @param {number} tagId - 标签ID
 */
export function removeAllTagDevices(tagId) {
  return commonFunc(
    `/api/device-tag-relation/tag/${tagId}/all-devices`,
    {},
    'DELETE'
  )
}

/**
 * 检查设备标签关联是否存在
 * @param {number} deviceId - 设备ID
 * @param {number} tagId - 标签ID
 */
export function checkDeviceTagRelation(deviceId, tagId) {
  return commonFunc(
    '/api/device-tag-relation/check-relation',
    { deviceId, tagId },
    'GET'
  )
}
