import { authRequest } from '@/utils/request'

const LOCAL_TENANT_ID = '000000'

/** 使用本项目 SpringBlade 认证端点登录，并兼容旧 store 需要的响应字段。 */
export async function loginSSO(data: Record<string, any>) {
  const response: any = await authRequest.post('/blade-auth/token', null, {
    params: {
      tenantId: LOCAL_TENANT_ID,
      username: data.username || data.loginId || data.account,
      password: data.password,
      grant_type: 'password',
      scope: 'all',
      type: 'account'
    },
    skipTokenAuth: true,
    useBladeClientAuth: true
  } as any)
  const payload = response?.data || response || {}
  const accessToken = payload.accessToken || payload.access_token || payload.token
  return {
    code: 200,
    success: true,
    data: {
      ...payload,
      accessToken,
      refreshToken: payload.refreshToken || payload.refresh_token || accessToken,
      tenantId: payload.tenantId || payload.tenant_id || LOCAL_TENANT_ID
    }
  }
}

/** 使用本地用户缓存校验当前请求头中的 token。 */
export function verifyToken() {
  return authRequest.post('/sso/v1/getUserInfo', {})
}

/** 获取当前本地登录用户信息。 */
export function getUserInfo() {
  return authRequest.post('/sso/v1/getUserInfo', {})
}

/** 获取本地单租户信息。 */
export function getUserTenants() {
  return authRequest.post('/sso/v1/getUserTenants', {})
}

/** 在本地鉴权体系内刷新当前 token。 */
export function refreshToken(data: Record<string, any>) {
  return authRequest.post('/sso/v1/refreshToken', data)
}

/** 注销当前本地会话。 */
export async function logout() {
  const response: any = await authRequest.post('/blade-auth/logout')
  return response?.code === undefined ? { code: 200, data: response } : response
}

/** 旧双阶段登录入口统一收敛到本地账号密码登录。 */
export function getLoginCodeV2(data: Record<string, any>) {
  return loginSSO(data)
}

/** 旧快速登录入口统一收敛到本地账号密码登录。 */
export function fastLogin(data: Record<string, any>) {
  return loginSSO(data)
}

/** 使用本地用户中心修改密码。 */
export function resetPassword(data: Record<string, any>) {
  return authRequest.put('/system/user/profile/updatePwd', null, {
    params: { oldPassword: data.oldPassword, newPassword: data.password }
  })
}
