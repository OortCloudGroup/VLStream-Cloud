import { request } from '@/utils/service'
import config from '@/config'

function commonFunc<T = any, K = any>(interfaceName: string, data: T, method = 'post') {
  return request<K>({
    url: config.URL + config.gateWay + 'vls-server/' + interfaceName,
    method: method,
    data: data
  })
}

// 同步用户信息到本地
export function syncUserToLocal(userInfo) {
  return commonFunc('/api/user/sync', {
    userId: userInfo.userId,
    tenantId: userInfo.tenantId,
    loginId: userInfo.loginId,
    userName: userInfo.userName,
    loginTime: userInfo.loginTime,
    loginIP: userInfo.loginIP,
    loginType: userInfo.login_type,
    client: userInfo.client,
    accessToken: userInfo.accessToken
  })
}

// 获取本地用户信息
export function getLocalUserInfo(userId) {
  return commonFunc(`/api/user/${userId}`, {}, 'GET')
}

// 更新本地用户信息
export function updateLocalUserInfo(userId, userData) {
  return commonFunc(`/api/user/${userId}`, userData, 'PUT')
}

// 统一的API对象
export const userSyncApi = {
  syncUser: syncUserToLocal,
  getUserInfo: getLocalUserInfo,
  updateUser: updateLocalUserInfo
}
