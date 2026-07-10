import { authRequest } from '../utils/request.js'

// 使用 SpringBlade 自带认证接口获取 accessToken。
export function loginUser(params) {
  return authRequest.post('/blade-auth/token', null, {
    params,
    skipTokenAuth: true,
    useBladeClientAuth: true
  })
}

// 通过当前 Authorization token 获取后端系统用户信息。
export function getUserInfo() {
  return authRequest.get('/blade-system/user/info')
}

// 校验 token 时复用当前用户接口，避免依赖旧统一用户中心。
export function verifyToken() {
  return getUserInfo()
}

// 调用 SpringBlade 登出接口，当前后端为预留逻辑。
export function logoutUser() {
  return authRequest.post('/blade-auth/logout')
}
