/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 10:42:09
* @Last Modified by:   兰舰
* @Last Modified time: 2024-11-15 10:42:09
* @Copyright aPaaS-front-team. All rights reserved.
*/
import { request } from '@/utils/service'
import type * as Login from './types/login'
import config from '@/config'

function commonFunc<T, K>(interfaceName: string, data: T, method: string) {
  return request<K>({
    url: config.URL + config.gateWay + 'oortcloud-sso/' + interfaceName,
    method: method,
    data: data
  })
}

/** 获取登录验证码 */
export function loginSSO(data: Login.ILoginRequestData) {
  return commonFunc<Login.ILoginRequestData, Login.LoginResponseData>('sso/v1/login', data, 'post')
}

/** 验证token */
export function verifyToken(data: Login.IVerifyTokenData) {
  return commonFunc<Login.IVerifyTokenData, Login.LoginResponseData>('sso/v1/verifyToken', data, 'post')
}
/** 登出 */
export function logout(data: Login.IVerifyTokenData) {
  return commonFunc<Login.IVerifyTokenData, Login.LoginResponseData>('sso/v1/logout', data, 'post')
}
// 获取用户信息
export function getUserInfo(data:any) {
  return commonFunc('sso/v1/getUserInfo', data, 'post')
}
// 部门列表
export function deptlist(data:any) {
  return commonFunc('sso/v1/deptlist', data, 'post')
}
// 部门和用户树
export function getdeptuser(data:any) {
  return commonFunc('sso/v1/getdeptuser', data, 'post')
}
