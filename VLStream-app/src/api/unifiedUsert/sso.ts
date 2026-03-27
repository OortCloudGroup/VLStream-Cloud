/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 10:47:19
* @Last Modified by:   兰舰
* @Last Modified time: 2024-11-15 10:47:19
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

// 根据userid获取用户详情
export function getUserInfoByUserId(data) {
  return commonFunc('sso/v1/getUserInfoByUserId', data, 'post')
}
