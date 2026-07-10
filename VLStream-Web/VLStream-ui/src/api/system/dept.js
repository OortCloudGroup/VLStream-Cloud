import request from '@/utils/request'
import {
  mapBladeDeptToRuoyi,
  mapRuoyiDept,
  toBladeList,
  toStringIds
} from './ruoyiCompat'

/**
 * 获取部门列表
 * @param {Object} params 过滤参数，如 deptName, fullName, tenantId 等
 * @returns {Promise} 返回部门扁平列表数据
 */
export function getDeptList(params) {
  const ruoyiParams = {
    ...params,
    deptName: params?.deptName || params?.fullName
  }
  return request({ url: '/system/dept/list', method: 'get', params: ruoyiParams })
    .then((response) => toBladeList(response, mapRuoyiDept))
}

/**
 * 获取部门树形结构数据
 * @param {Object} params 可包含 tenantId 等过滤参数
 * @returns {Promise} 返回部门树形数据
 */
export function getDeptTree(params) {
  return request({ url: '/system/dept/list', method: 'get', params })
    .then((response) => toBladeList(response, mapRuoyiDept))
}

/**
 * 新增或修改部门数据
 * @param {Object} data 部门实体对象
 * @returns {Promise} 返回请求响应的 Promise 对象
 */
export function submitDept(data) {
  const payload = mapBladeDeptToRuoyi(data)
  return request({
    url: '/system/dept',
    method: payload.deptId ? 'put' : 'post',
    data: payload
  })
}

/**
 * 批量或单个删除部门
 * @param {String} ids 逗号分隔的部门ID
 * @returns {Promise} 返回请求响应的 Promise 对象
 */
export function removeDepts(ids) {
  const deptIds = toStringIds(ids)
  if (deptIds.length <= 1) {
    return request({ url: `/system/dept/${encodeURIComponent(deptIds[0] || ids)}`, method: 'delete' })
  }
  return Promise.all(deptIds.map((deptId) => request({ url: `/system/dept/${encodeURIComponent(deptId)}`, method: 'delete' })))
    .then(() => ({ code: 200, success: true, msg: '操作成功', data: true }))
}
