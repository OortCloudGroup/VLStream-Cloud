import request from '@/utils/request'
import {
  mapBladeMenuToRuoyi,
  mapRuoyiMenu,
  toBladeList,
  toStringIds
} from './ruoyiCompat'

/**
 * 获取所有菜单与按钮扁平列表 (前端可自行转换为树)
 * @param {Object} params 查询过滤参数
 * @returns {Promise} 返回请求响应的 Promise 对象
 */
export function getMenuList(params) {
  const ruoyiParams = {
    ...params,
    menuName: params?.name || params?.menuName,
    perms: params?.code || params?.perms
  }
  return request({ url: '/system/menu/list', method: 'get', params: ruoyiParams })
    .then((response) => toBladeList(response, mapRuoyiMenu))
}

/**
 * 仅获取菜单分类的列表 (不含按钮)
 * @param {Object} params 查询过滤参数
 * @returns {Promise} 返回请求响应的 Promise 对象
 */
export function getMenuOnlyList(params) {
  return getMenuList(params)
}

/**
 * 获取菜单树形结构数据
 * @param {Object} params 查询过滤参数
 * @returns {Promise} 返回请求响应的 Promise 对象
 */
export function getMenuTree(params) {
  return request({ url: '/system/menu/treeselect', method: 'get', params })
    .then((response) => ({
      ...response,
      success: response?.code === 200,
      data: response?.data || []
    }))
}

/**
 * 获取给角色授权所用的完整权限树结构 (包含菜单树、数据权限树、接口权限树)
 * @param {Object} params 查询过滤参数
 * @returns {Promise} 返回权限授权树的 Promise 对象
 */
export function getGrantTree(params) {
  return request({ url: '/system/menu/treeselect', method: 'get', params })
    .then((response) => ({
      ...response,
      success: response?.code === 200,
      data: {
        menu: response?.data || [],
        dataScope: [],
        apiScope: []
      }
    }))
}

/**
 * 获取指定角色当前拥有的已选中权限ID列表
 * @param {String} roleIds 逗号分隔的角色ID
 * @returns {Promise} 返回已选中的 key 列表
 */
export function getRoleTreeKeys(roleIds) {
  const roleId = toStringIds(roleIds)[0]
  return request({ url: `/system/menu/roleMenuTreeselect/${encodeURIComponent(roleId)}`, method: 'get' })
    .then((response) => ({
      ...response,
      success: response?.code === 200,
      data: {
        menu: response?.data?.checkedKeys || [],
        dataScope: [],
        apiScope: []
      }
    }))
}

/**
 * 新增或更新菜单、按钮项
 * @param {Object} data 菜单实体数据
 * @returns {Promise} 返回操作结果的 Promise 对象
 */
export function submitMenu(data) {
  const payload = mapBladeMenuToRuoyi(data)
  return request({
    url: '/system/menu',
    method: payload.menuId ? 'put' : 'post',
    data: payload
  })
}

/**
 * 批量或单个删除菜单
 * @param {String} ids 逗号分隔的菜单ID字符串
 * @returns {Promise} 返回操作结果的 Promise 对象
 */
export function removeMenus(ids) {
  const menuIds = toStringIds(ids)
  if (menuIds.length <= 1) {
    return request({ url: `/system/menu/${encodeURIComponent(menuIds[0] || ids)}`, method: 'delete' })
  }
  return Promise.all(menuIds.map((menuId) => request({ url: `/system/menu/${encodeURIComponent(menuId)}`, method: 'delete' })))
    .then(() => ({ code: 200, success: true, msg: '操作成功', data: true }))
}
