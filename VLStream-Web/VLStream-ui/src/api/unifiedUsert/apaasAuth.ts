/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 10:46:57
 * @Last Modified by: 兰舰
 * @Last Modified time: 2024-12-04 11:13:20
* @Copyright aPaaS-front-team. All rights reserved.
*/
import { request } from '@/utils/service'
import config from '@/config'

function commonFunc<T, K>(interfaceName: string, data: T, method = 'post') {
  return request<K>({
    url: config.URL + config.gateWay + 'apaas-auth' + interfaceName,
    method: method,
    data: data
  })
}

// 权限列表
export function authList(data) {
  return commonFunc('/admin/v1/authList', data)
}

// 配置权限是否需要校验
export function authSetValid(data) {
  return commonFunc('/admin/v1/authSetValid', data)
}

// 角色权限列表
export function roleAuth(data) {
  return commonFunc('/admin/v1/roleAuth', data)
}

// 角色关联权限
export function roleAuthLike(data) {
  return commonFunc('/admin/v1/roleAuthLike', data)
}

// 角色详情
export function roleInfo(data) {
  return commonFunc('/admin/v1/roleInfo', data)
}

// 角色列表
export function roleList(data) {
  return commonFunc('/admin/v1/roleList', data)
}

// 删除角色
export function roleRemove(data) {
  return commonFunc('/admin/v1/roleRemove', data)
}

// 保存角色
export function roleSave(data) {
  return commonFunc('/admin/v1/roleSave', data)
}

// 角色关联用户
export function roleUserLike(data) {
  return commonFunc('/admin/v1/roleUserLike', data)
}

// 角色关联用户列表
export function roleUserList(data) {
  return commonFunc('/admin/v1/roleUserList', data)
}

// 用户关联角色
export function userRoleLike(data) {
  return commonFunc('/admin/v1/userRoleLike', data)
}

// 用户取消关联角色
export function userRoleUnlike(data) {
  return commonFunc('/admin/v1/userRoleUnlike', data)
}

// 角色删除关联用户
export function roleUserUnlike(data) {
  return commonFunc('/admin/v1/roleUserUnlike', data)
}

// 服务详情
export function serviceInfo(data) {
  return commonFunc('/admin/v1/serviceInfo', data)
}

// 服务列表
export function serviceList(data) {
  return commonFunc('/admin/v1/serviceList', data)
}

// 用户角色
export function userRole(data) {
  return commonFunc('/admin/v1/userRole', data)
}

// menu
// 菜单详情
export function menuInfo(data) {
  return commonFunc('/menu/v1/menuInfo', data)
}

// 菜单列表
export function menuList(data) {
  return commonFunc('/menu/v1/menuList', data)
}

// 删除菜单
export function menuRemove(data) {
  return commonFunc('/menu/v1/menuRemove', data)
}

// 保存菜单
export function menuSave(data) {
  return commonFunc('/menu/v1/menuSave', data)
}

// 获取我的菜单
export function myAuth(data) {
  return commonFunc('/menu/v1/myAuth', data)
}

// 平台详情
export function platformInfo(data) {
  return commonFunc('/menu/v1/platformInfo', data)
}

// 平台列表
export function platformList(data) {
  return commonFunc('/menu/v1/platformList', data)
}

// 删除平台信息
export function platformRemove(data) {
  return commonFunc('/menu/v1/platformRemove', data)
}

// 保存平台信息
export function platformSave(data) {
  return commonFunc('/menu/v1/platformSave', data)
}

// 设置菜单初始化状态
export function platformSaveInitStatus(data) {
  return commonFunc('/menu/v1/platformSaveInitStatus', data)
}

// 菜单权限列表
export function roleAuthM(data) {
  return commonFunc('/menu/v1/roleAuth', data)
}

// 复制平台菜单到某个租户
export function platformCopyMenu(data) {
  return commonFunc('/menu/v1/platformCopy', data)
}

// 菜单权限关联
export function roleAuthLink(data) {
  return commonFunc('/menu/v1/roleAuthLink', data)
}

// 岗位权限列表
export function jobAuth(data) {
  return commonFunc('/admin/v1/jobAuth', data)
}

// 岗位列表
export function jobList(data) {
  return commonFunc('/admin/v1/jobList', data)
}

// 删除岗位
export function jobRemove(data) {
  return commonFunc('/admin/v1/jobRemove', data)
}

// 岗位关联角色
export function jobRoleLike(data) {
  return commonFunc('/admin/v1/jobRoleLike', data)
}

// 岗位角色列表
export function jobRoleList(data) {
  return commonFunc('/admin/v1/jobRoleList', data)
}

// 岗位取消角色关联
export function jobRoleUnlike(data) {
  return commonFunc('/admin/v1/jobRoleUnlike', data)
}

// 保存岗位
export function jobSave(data) {
  return commonFunc('/admin/v1/jobSave', data)
}

// 岗位关联用户
export function jobUserLike(data) {
  return commonFunc('/admin/v1/jobUserLike', data)
}

// 岗位删除关联用户
export function jobUserUnlike(data) {
  return commonFunc('/admin/v1/jobUserUnlike', data)
}

// 岗位详情
export function jobInfo(data) {
  return commonFunc('/admin/v1/jobInfo', data)
}

// 角色关联岗位
export function roleJobLike(data) {
  return commonFunc('/admin/v1/roleJobLike', data)
}

// 角色关联岗位列表
export function roleJobList(data) {
  return commonFunc('/admin/v1/roleJobList', data)
}

// 角色取消关联岗位
export function roleJobUnlike(data) {
  return commonFunc('/admin/v1/roleJobUnlike', data)
}

// 用户关联岗位
export function userJobLike(data) {
  return commonFunc('/admin/v1/userJobLike', data)
}

// 用户取消关联岗位
export function userJobUnlike(data) {
  return commonFunc('/admin/v1/userJobUnlike', data)
}

// 用户关联岗位列表
export function userJobList(data) {
  return commonFunc('/admin/v1/userJobList', data)
}

// 职位
// 职位权限列表
export function postAuth(data) {
  return commonFunc('/admin/v1/postAuth', data)
}

// 职位详情
export function postInfo(data) {
  return commonFunc('/admin/v1/postInfo', data)
}

// 获取职位列表
export function postList(data) {
  return commonFunc('/admin/v1/postList', data)
}

// 删除职位
export function postRemove(data) {
  return commonFunc('/admin/v1/postRemove', data)
}

// 职位关联角色
export function postRoleLike(data) {
  return commonFunc('/admin/v1/postRoleLike', data)
}

// 职位取消关联角色
export function postRoleUnlike(data) {
  return commonFunc('/admin/v1/postRoleUnlike', data)
}

// 职位角色列表
export function postRoleList(data) {
  return commonFunc('/admin/v1/postRoleList', data)
}

// 保存职位
export function postSave(data) {
  return commonFunc('/admin/v1/postSave', data)
}

// 职位关联用户
export function postUserLike(data) {
  return commonFunc('/admin/v1/postUserLike', data)
}

// 职位取消关联用户
export function postUserUnlike(data) {
  return commonFunc('/admin/v1/postUserUnlike', data)
}

// 职位用户列表
export function postUserList(data) {
  return commonFunc('/admin/v1/postUserList', data)
}

// 角色关联职位
export function rolePostLike(data) {
  return commonFunc('/admin/v1/rolePostLike', data)
}

// 角色取消关联职位
export function rolePostUnlike(data) {
  return commonFunc('/admin/v1/rolePostUnlike', data)
}

// 角色关联职位列表
export function rolePostList(data) {
  return commonFunc('/admin/v1/rolePostList', data)
}

// 用户关联职位
export function userPostLike(data) {
  return commonFunc('/admin/v1/userPostLike', data)
}

// 用户取消关联职位
export function userPostUnlike(data) {
  return commonFunc('/admin/v1/userPostUnlike', data)
}

// 用户职位列表
export function userPostList(data) {
  return commonFunc('/admin/v1/userPostList', data)
}

// 角色数据列表
export function roleDataList(data) {
  return commonFunc('/admin/v1/roleDataList', data)
}

export function roleDataLike(data) {
  return commonFunc('/admin/v1/roleDataLike', data)
}

export function roleDataUnlike(data) {
  return commonFunc('/admin/v1/roleDataUnlike', data)
}

// 功能权限添加
export function roleFunctionAdd(data) {
  return commonFunc('/admin/v1/roleFunctionAdd', data)
}

// 功能权限删除
export function roleFunctionDel(data) {
  return commonFunc('/admin/v1/roleFunctionDel', data)
}

// 功能权限编辑
export function roleFunctionEdit(data) {
  return commonFunc('/admin/v1/roleFunctionEdit', data)
}

// 功能权限详情
export function roleFunctionInfo(data) {
  return commonFunc('/admin/v1/roleFunctionInfo', data)
}

// 功能权限列表
export function roleFunctionList(data) {
  return commonFunc('/admin/v1/roleFunctionList', data)
}

// 角色关联应用
export function roleAppLike(data) {
  return commonFunc('/admin/v1/roleAppLike', data)
}

// 角色关联应用列表
export function roleAppList(data) {
  return commonFunc('/admin/v1/roleAppList', data)
}

// 角色取消关联应用
export function roleAppUnlike(data) {
  return commonFunc('/admin/v1/roleAppUnlike', data)
}

// 初始化添加
export function initAdd(data) {
  return commonFunc('/admin/v1/initAdd', data)
}

// 初始化删除
export function initDel(data) {
  return commonFunc('/admin/v1/initDel', data)
}

// 初始化编辑
export function initEdit(data) {
  return commonFunc('/admin/v1/initEdit', data)
}

// 初始化详情
export function initInfo(data) {
  return commonFunc('/admin/v1/initInfo', data)
}

// 初始化列表
export function initList(data) {
  return commonFunc('/admin/v1/initList', data)
}

// 初始化列表全部
export function initListAll(data) {
  return commonFunc('/admin/v1/initListAll', data)
}

// 初始化状态
export function initSetStatus(data) {
  return commonFunc('/admin/v1/initSetStatus', data)
}

// 获取初始化预览数据
export function initShow(data) {
  return commonFunc('/admin/v1/initShow', data)
}

// 开始初始化数据下级租户
export function initStart(data) {
  return commonFunc('/admin/v1/initStart', data)
}

// 获取角色数据表权限列表接口
export function roleDataTable(data) {
  return commonFunc('/admin/v1/roleDataTable', data)
}

// 角色关联数据表权限
export function roleDataTableLike(data) {
  return commonFunc('/admin/v1/roleDataTableLike', data)
}

// 获取数据表列表
export function dataTableList(data) {
  return commonFunc('/admin/v1/dataTableList', data)
}

// 获取数据表详情
export function dataTableInfo(data) {
  return commonFunc('/admin/v1/dataTableInfo', data)
}

