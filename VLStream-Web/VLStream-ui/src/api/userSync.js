import request from '@/utils/request'

// 同步用户信息到本地
export function syncUserToLocal(userInfo) {
  return request.post('/api/user/sync', {
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
  return request.get(`/api/user/${userId}`)
}

// 更新本地用户信息
export function updateLocalUserInfo(userId, userData) {
  return request.put(`/api/user/${userId}`, userData)
}

// 统一的API对象
export const userSyncApi = {
  syncUser: syncUserToLocal,
  getUserInfo: getLocalUserInfo,
  updateUser: updateLocalUserInfo
} 