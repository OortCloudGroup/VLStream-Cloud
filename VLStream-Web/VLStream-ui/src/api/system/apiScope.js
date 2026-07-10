import request from '@/utils/request'

/**
 * 分页查询接口权限范围列表
 * @param {Object} params 包含 current, size, scopeName, resourceCode 等参数
 * @returns {Promise} 返回接口权限分页列表
 */
export function getApiScopeList(params) {
  return request({ url: '/blade-system/api-scope/list', method: 'get', params })
}

/**
 * 获取指定接口权限的详细信息
 * @param {Object} params 过滤参数，需包含 id
 * @returns {Promise} 返回接口权限详情的 Promise 对象
 */
export function getApiScopeDetail(params) {
  return Promise.resolve({ code: 200, success: true, msg: '操作成功', data: null, params })
}

/**
 * 创建或更新接口权限配置
 * @param {Object} data 接口权限配置对象
 * @returns {Promise} 返回操作结果的 Promise 对象
 */
export function submitApiScope(data) {
  return Promise.resolve({
    code: 500,
    success: false,
    msg: '当前后端未提供接口权限范围维护接口',
    data
  })
}

/**
 * 批量或单个删除接口权限配置
 * @param {String} ids 逗号分隔的接口权限ID
 * @returns {Promise} 返回操作结果的 Promise 对象
 */
export function removeApiScopes(ids) {
  return Promise.resolve({
    code: 500,
    success: false,
    msg: '当前后端未提供接口权限范围删除接口',
    data: ids
  })
}
