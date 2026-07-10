/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 10:45:18
 * @Last Modified by: 兰舰
 * @Last Modified time: 2025-09-28 20:30:51
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

/** 获取临时登录凭证 */
export function getLoginCodeV2(data: Login.ILoginRequestData) {
  return commonFunc('sso/v2/getLoginCode', data, 'post')
}

/** 双子登录 */
export function fastLogin(data: Login.ILoginRequestData) {
  return commonFunc('sso/v1/fastLogin', data, 'post')
}

// 获取租户列表
export function getUserTenants(data:any) {
  return commonFunc('sso/v1/getUserTenants', data, 'post')
}

// 获取租户树形列表
export function getUserTenantsTree(data:any) {
  return commonFunc('sso/v1/getUserTenantsTree', data, 'post')
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

// 获取Google授权URL
export function getGoogleAuthURL(data) {
  return commonFunc('sso/v1/returnAuthUrl', data, 'post')
}

// // 登录发送手机验证码
// export function sendLoginSmsCode(data) {
//   return commonFunc('sso/v1/sendLoginSmsCode', data, 'post')
// }

// 短信验证码发送(用于登陆前发送验证码)
export function sendLoginSmsCode(data) {
  return commonFunc('sso/v1/sendLoginSmsCode', data, 'post')
}

// 发送用于登录的邮箱验证码(用户未登录状态下调用)
export function sendLoginEmailCode(data) {
  return commonFunc('sso/v1/sendLoginEmailCode', data, 'post')
}

// 获取用户其他登录方式,返回已认证的账号列表
export function getUserOtherLogin(data) {
  return commonFunc('sso/v1/getUserOtherLogin', data, 'post')
}

// 生成验证码
export function generateCaptcha(data) {
  return commonFunc('sso/v1/generateCaptcha', data, 'post')
}

// 重置会话设置为默认值
export function settingReset(data) {
  return commonFunc('session/v1/settingReset', data, 'post')
}

// 检查当前用户账号是否需要手机号绑定
export function checkNeedBindPhone(data) {
  return commonFunc('sso/v1/checkNeedBindPhone', data, 'post')
}

// 三方账号绑定手机号登录
export function thirdBindPhoneLogin(data) {
  return commonFunc('sso/v1/thirdBindPhoneLogin', data, 'post')
}

