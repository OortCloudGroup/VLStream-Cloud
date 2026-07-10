import request from '@/utils/request'

// 标签管理 API

/**
 * 分页查询标签信息
 * @param {Object} params - 查询参数
 * @param {number} params.current - 当前�?
 * @param {number} params.size - 每页大小
 * @param {string} params.keyword - 搜索关键�?
 * @param {string} params.categoryType - 标签大类
 * @param {number} params.level - 标签层级
 * @param {number} params.parentId - 父级ID
 */
export function getTagManagementPage(params) {
  return request({
    url: '/vlsTagManagement/page',
    method: 'get',
    params
  })
}

/**
 * 获取标签树形结构（用于左侧导航）
 */
export function getTagTree() {
  return request({
    url: '/vlsTagManagement/tree',
    method: 'get'
  })
}

/**
 * 获取标签详情
 * @param {number} id - 标签ID
 */
export function getTagById(id) {
  return request({
    url: `/vlsTagManagement/${id}`,
    method: 'get'
  })
}

/**
 * 创建标签
 * @param {Object} data - 标签数据
 */
export function createTag(data) {
  return request({
    url: '/vlsTagManagement',
    method: 'post',
    data
  })
}

/**
 * 更新标签
 * @param {number} id - 标签ID
 * @param {Object} data - 更新数据
 */
export function updateTag(id, data) {
  return request({
    url: `/vlsTagManagement/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除标签
 * @param {number} id - 标签ID
 */
export function deleteTag(id) {
  return request({
    url: `/vlsTagManagement/${id}`,
    method: 'delete'
  })
}

/**
 * 批量删除标签
 * @param {Array} ids - 标签ID数组
 */
export function batchDeleteTags(ids) {
  return request({
    url: '/vlsTagManagement/batch',
    method: 'delete',
    data: ids
  })
}

/**
 * 获取标签统计信息
 */
export function getTagStatistics() {
  return request({
    url: '/vlsTagManagement/statistics',
    method: 'get'
  })
}

/**
 * 获取标签的使用统计
 * @param {number} id - 标签ID
 */
export function getTagUsageStats(id) {
  return request({
    url: `/vlsTagManagement/${id}/stats`,
    method: 'get'
  })
}

/**
 * 检查标签名称是否重复
 * @param {string} tagName - 标签名称
 * @param {number} parentId - 父级ID（可选）
 * @param {number} excludeId - 排除的ID（可选，用于编辑时验证）
 */
export function checkTagNameDuplicate(tagName, parentId = null, excludeId = null) {
  const params = { tagName }
  if (parentId !== null) params.parentId = parentId
  if (excludeId !== null) params.excludeId = excludeId
  
  return request({
    url: '/vlsTagManagement/check-name',
    method: 'get',
    params
  })
}

/**
 * 获取标签的所有子标签
 * @param {number} parentId - 父级ID
 */
export function getChildTags(parentId) {
  return request({
    url: `/vlsTagManagement/${parentId}/children`,
    method: 'get'
  })
}

/**
 * 移动标签到新的父级
 * @param {number} id - 标签ID
 * @param {number} newParentId - 新的父级ID
 */
export function moveTag(id, newParentId) {
  return request({
    url: `/vlsTagManagement/${id}/move`,
    method: 'put',
    data: { newParentId }
  })
}

// 设备标签关联 API

/**
 * 获取设备的所有标签
 * @param {number} deviceId - 设备ID
 */
export function getDeviceTags(deviceId) {
  return request({
    url: `/api/device-tag-relation/device/${deviceId}/tags`,
    method: 'get'
  })
}

/**
 * 获取标签关联的所有设备
 * @param {number} tagId - 标签ID
 */
export function getTagDevices(tagId) {
  return request({
    url: `/api/device-tag-relation/tag/${tagId}/devices`,
    method: 'get'
  })
}

/**
 * 为设备添加标签
 * @param {number} deviceId - 设备ID
 * @param {number} tagId - 标签ID
 * @param {string} createdBy - 创建者
 */
export function addDeviceTag(deviceId, tagId, createdBy = 'system') {
  return request({
    url: `/api/device-tag-relation/device/${deviceId}/tag/${tagId}`,
    method: 'post',
    params: { createdBy }
  })
}

/**
 * 批量为设备添加标签
 * @param {number} deviceId - 设备ID
 * @param {Array} tagIds - 标签ID数组
 * @param {string} createdBy - 创建者
 */
export function addDeviceTags(deviceId, tagIds, createdBy = 'system') {
  return request({
    url: `/api/device-tag-relation/device/${deviceId}/tags`,
    method: 'post',
    data: tagIds,
    params: { createdBy }
  })
}

/**
 * 移除设备标签
 * @param {number} deviceId - 设备ID
 * @param {number} tagId - 标签ID
 */
export function removeDeviceTag(deviceId, tagId) {
  return request({
    url: `/api/device-tag-relation/device/${deviceId}/tag/${tagId}`,
    method: 'delete'
  })
}

/**
 * 批量移除设备标签
 * @param {number} deviceId - 设备ID
 * @param {Array} tagIds - 标签ID数组
 */
export function removeDeviceTags(deviceId, tagIds) {
  return request({
    url: `/api/device-tag-relation/device/${deviceId}/tags`,
    method: 'delete',
    data: tagIds
  })
}

/**
 * 更新设备的所有标签
 * @param {number} deviceId - 设备ID
 * @param {Array} tagIds - 标签ID数组
 * @param {string} createdBy - 创建者
 */
export function updateDeviceTags(deviceId, tagIds, createdBy = 'system') {
  return request({
    url: `/api/device-tag-relation/device/${deviceId}/tags`,
    method: 'put',
    data: tagIds,
    params: { createdBy }
  })
}

/**
 * 删除设备的所有标签关联
 * @param {number} deviceId - 设备ID
 */
export function removeAllDeviceTags(deviceId) {
  return request({
    url: `/api/device-tag-relation/device/${deviceId}/all-tags`,
    method: 'delete'
  })
}

/**
 * 删除标签的所有设备关联
 * @param {number} tagId - 标签ID
 */
export function removeAllTagDevices(tagId) {
  return request({
    url: `/api/device-tag-relation/tag/${tagId}/all-devices`,
    method: 'delete'
  })
}

/**
 * 检查设备标签关联是否存在
 * @param {number} deviceId - 设备ID
 * @param {number} tagId - 标签ID
 */
export function checkDeviceTagRelation(deviceId, tagId) {
  return request({
    url: '/api/device-tag-relation/check-relation',
    method: 'get',
    params: {
      deviceId,
      tagId
    }
  })
} 
