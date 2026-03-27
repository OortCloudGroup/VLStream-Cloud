import { request } from '@/utils/service'
import config from '@/config'
import { userCenterVerifyToken } from './userCenter'

// 公共请求函数：统一URL拼接、入参处理、请求方式，泛型支持类型约束
function commonFunc<T = any, K = any>(interfaceName: string, data: T, method = 'post') {
  return request<K>({
    url: config.URL + config.gateWay + 'vls-server/' + interfaceName,
    method: method,
    data: data
  })
}

// 验证token（保留原跨模块调用逻辑，不修改原有接口依赖）
export function verifyToken(data) {
  return userCenterVerifyToken(data)
}

// 获取用户信息
export function getUserInfo(data) {
  return commonFunc('/auth/getUserInfo', data)
}

// 用户登录
export function loginUser(data) {
  return commonFunc('/auth/login', data)
}
