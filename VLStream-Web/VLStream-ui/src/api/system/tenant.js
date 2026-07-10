import request from '@/utils/request'
import { SINGLE_TENANT_ID } from './ruoyiCompat'

/**
 * 分页查询租户列表
 * @param {Object} params 包含 current, size, tenantId, tenantName, contactNumber 等参数
 * @returns {Promise} 返回租户分页数据
 */
export function getTenantList(params) {
  return request({ url: '/blade-system/tenant/list', method: 'get', params })
}

/**
 * 获取用于下拉框选择的租户列表 (不分页)
 * @param {Object} params 过滤参数
 * @returns {Promise} 返回租户选项数据
 */
export function getTenantSelect(params) {
  return request({ url: '/blade-system/tenant/select', method: 'get', params })
}

/**
 * 新增或修改租户数据
 * @param {Object} data 租户实体数据
 * @returns {Promise} 返回请求响应的 Promise 对象
 */
export function submitTenant(data) {
  return Promise.resolve({
    code: 200,
    success: true,
    msg: '单租户模式已固定默认租户',
    data: { ...(data || {}), tenantId: SINGLE_TENANT_ID }
  })
}

/**
 * 批量或单个删除租户
 * @param {String} ids 逗号分隔的租户ID
 * @returns {Promise} 返回请求响应的 Promise 对象
 */
export function removeTenants(ids) {
  return Promise.resolve({
    code: 200,
    success: true,
    msg: '单租户模式不删除默认租户',
    data: true
  })
}
