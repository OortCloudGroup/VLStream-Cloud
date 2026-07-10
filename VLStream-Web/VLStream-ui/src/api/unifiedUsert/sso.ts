/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 10:47:19
 * @Last Modified by: 兰舰
 * @Last Modified time: 2025-09-16 20:49:055
* @Copyright aPaaS-front-team. All rights reserved.
*/
import { request } from '@/utils/service'
import config from '@/config'

function commonFunc<T, K>(interfaceName: string, data: T, method = 'post') {
  return request<K>({
    url: config.URL + config.gateWay + 'apaas-sso/' + interfaceName,
    method: method,
    data: data
  })
}

// 清空最近联系人
export function usedDel(data) {
  return commonFunc('tag/v1/usedDel', data, 'post')
}

// 获取最近联系人
export function usedGet(data) {
  return commonFunc('tag/v1/usedGet', data, 'post')
}

// 获取用户信息
export function getUserList(data) {
  return commonFunc('sso/v1/getUserList', data, 'post')
}

// 获取部门信息
export function getDeptList(data) {
  return commonFunc('sso/v1/getDeptList', data, 'post')
}
// 部门和用户树
export function getDeptUser(data) {
  return commonFunc('sso/v1/getDeptUser', data)
}

// 用户列表
export function deptUserList(data) {
  return commonFunc('sso/v1/deptUserList', data)
}

// 上报最近联系人
export function usedSet(data) {
  return commonFunc('tag/v1/usedSet', data, 'post')
}

// 地址本关联用户列表
export function addressUserList(data) {
  return commonFunc('address/v1/tagUserList', data, 'post')
}

// 地址本列表
export function addressList(data) {
  return commonFunc('address/v1/tagList', data, 'post')
}

// 标签列表
export function tagList(data) {
  return commonFunc('tag/v1/tagList', data, 'post')
}

// 标签关联用户列表
export function tagUserList(data) {
  return commonFunc('tag/v1/tagUserList', data, 'post')
}

// 上传协议地址接口
export function agreementAdd(data) {
  return commonFunc('/sso/v1/agreementAdd', data, 'post')
}

// 编辑协议地址接口
export function agreementEdit(data) {
  return commonFunc('/sso/v1/agreementEdit', data, 'post')
}

// 查询协议地址接口
export function agreementList(data) {
  return commonFunc('/sso/v1/agreementList', data, 'post')
}

// 获取用户账号中心
export function userAccountCenter(data) {
  return commonFunc('sso/v1/userAccountCenter', data, 'post')
}

// 通过邀请码加入企业
export function joinByInviteCode(data) {
  return commonFunc('/sso/v1/joinTenantUser', data, 'POST')
}

// 多种方式的用户注册
export function userRegister(data) {
  return commonFunc('/sso/v1/userRegister', data, 'POST')
}

// 获取多端同登策略
export function policyGet(data) {
  return commonFunc('/session/v1/policyGet', data, 'POST')
}

// 保存多端同登策略
export function policySave(data) {
  return commonFunc('/session/v1/policySave', data, 'POST')
}

// 刷新token
export function refreshToken(data) {
  return request({
    url: '/sso/v1/refreshToken',
    method: 'POST',
    data
  })
}

// 清除登录错误次数限制
export function clearLoginerrtimes(data) {
  return commonFunc('/sso/v1/clearLoginerrtimes', data, 'POST')
}

// 获取自己的会话列表
export function myList(data) {
  return commonFunc('/session/v1/myList', data, 'POST')
}

// 踢出自己其他登录设备
export function kickMyOther(data) {
  return commonFunc('/session/v1/kickMyOther', data, 'POST')
}

// 用户场景行业列表
export function getMyIndustryList(data) {
  return commonFunc('/userSceneManage/v1/getMyIndustryList', data)
}

// 用户场景行业-选中
export function getLastIndustry(data) {
  return commonFunc('/userSceneManage/v1/getLastIndustry', data)
}

// 用户场景行业-选中
export function saveLastIndustry(data) {
  return commonFunc('/userSceneManage/v1/saveLastIndustry', data)
}

// 查询当前租户密码校验规则
export function getPasswordPolicy(data) {
  return commonFunc('/passwordPolicy/v1/get', data, 'POST')
}

// 按当前租户策略校验密码复杂度
export function validatePasswordPolicy(data) {
  return commonFunc('/passwordPolicy/v1/validate', data, 'POST')
}
