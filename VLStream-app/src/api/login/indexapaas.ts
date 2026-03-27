/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 10:45:18
 * @Last Modified by: 兰舰
 * @Last Modified time: 2025-01-22 21:39:22
* @Copyright aPaaS-front-team. All rights reserved.
*/
import { request } from '@/utils/service'
import type * as Login from './types/login'
import config from '@/config'

function commonFunc<T, K>(interfaceName: string, data: T, method: string) {
  return request<K>({
    url: config.URL + config.gateWay + 'apaas-sso/' + interfaceName,
    method: method,
    data: data
  })
}

/** 获取临时登录凭证 */
export function getLoginCode(data: Login.ILoginRequestData) {
  return commonFunc('sso/v1/getLoginCode', data, 'post')
}

// 获取租户列表
export function getUserTenants(data:any) {
  return commonFunc('sso/v1/getUserTenants', data, 'post')
}

// 根据条件或用户列表
export function getUserList(data:any) {
  return commonFunc('sso/v1/getUserList', data, 'post')
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

// 检测是否需要验证码
export function getCaptcha(data:any) {
  return commonFunc('sso/v1/getCaptcha', data, 'post')
}

// 获取租户信息
export function getTenant(data:any) {
  return commonFunc('sso/v1/getTenant', data, 'post')
}
// 获取租户列表
export function getTenantList(data:any) {
  return commonFunc('sso/v1/getTenantList', data, 'post')
}

// 根据短语查询租户id
export function getTenantIdByPhrase(data:any) {
  return commonFunc('sso/v1/getTenantIdByPhrase', data, 'post')
}

// 字典管理

// 根据标记获取字典列表
export function dictListByTag(data:any) {
  return commonFunc('dict/v1/getdicts', data, 'post')
}

// 字典列表
export function dictList(data:any) {
  return commonFunc('dict/v1/list', data, 'post')
}

// 字典详情
export function dictInfo(data:any) {
  return commonFunc('dict/v1/info', data, 'post')
}

// 字典删除
export function dictDel(data:any) {
  return commonFunc('dict/v1/del', data, 'post')
}

//  修改/添加字典
export function dictSave(data:any) {
  return commonFunc('dict/v1/save', data, 'post')
}

// 字典排序
export function dictSort(data:any) {
  return commonFunc('dict/v1/sort', data, 'post')
}

// 设置初始化状态
export function initSetStatus(data) {
  return commonFunc('dict/v1/initSetStatus', data, 'post')
}

// 修改密码
export function resetPassword(data) {
  return commonFunc('sso/v1/resetPassword', data, 'post')
}

// 获取会话设置
export function sessionSettingGet(data) {
  return commonFunc('session/v1/settingGet', data, 'post')
}

// 保存会话设置
export function sessionSettingSet(data) {
  return commonFunc('session/v1/settingSet', data, 'post')
}

// 会话列表
export function sessionList(data) {
  return commonFunc('session/v1/list', data, 'post')
}

// 会话踢出
export function sessionKick(data) {
  return commonFunc('session/v1/kick', data, 'post')
}

// 平台配置 保存配置
export function frontConfigSet(data) {
  return commonFunc('frontConf/v1/set', data, 'post')
}

// 获取微信授权的URL
export function getWXQRCodeURL(data) {
  return commonFunc('sso/v1/weChatReturnAuthUrl', data, 'post')
}

// 获取钉钉授权URL
export function getDingTalkAuthURL(data) {
  return commonFunc('sso/v1/dingReturnAuthUrl', data, 'post')
}

// 登录发送手机验证码
export function sendLoginSmsCode(data) {
  return commonFunc('sso/v1/sendLoginSmsCode', data, 'post')
}

// 常用意见列表
export function myOpinionList(data) {
  return commonFunc('sso/v1/myOpinionList', data, 'post')
}
