import axios from 'axios'
import { getModelHubAccessToken } from '@/utils/modelHubAuth'

/**
 * Model Hub SSO 地址
 * 正确路径：{platform}/bus/apaas-sso/sso/v1/getUserInfo
 */
const PLATFORM_SSO_BASE = import.meta.env.DEV
  ? '/bus/apaas-sso'
  : 'https://workup-dev.myoumuamua.com:6433/bus/apaas-sso'

const MODEL_HUB_HEADERS = {
  'Content-Type': 'application/json',
  accept: 'application/json, text/plain, */*',
  requesttype: 'app',
  appid: '08e3168bd56a4e75ae3d5dee63db0657',
  secretkey: '32e3ca224aa741fbb1362d33070bca2f'
}

function createModelHubRequest(accessToken) {
  const token = accessToken || getModelHubAccessToken()
  const tenantId =
    sessionStorage.getItem('modelHubTenantId') ||
    localStorage.getItem('modelHubTenantId') ||
    ''

  const headers = {
    ...MODEL_HUB_HEADERS,
    ...(token ? { accesstoken: token } : {}),
    ...(tenantId ? { tenantid: tenantId } : {})
  }

  const instance = axios.create({
    baseURL: PLATFORM_SSO_BASE,
    timeout: 15000,
    headers
  })

  instance.interceptors.response.use(
    (response) => response.data,
    (error) => Promise.reject(error)
  )

  return instance
}

/** 获取用户信息 */
export function getModelHubUserInfo(data = {}) {
  const accessToken = data.accessToken || getModelHubAccessToken()
  const request = createModelHubRequest(accessToken)
  return request.post('/sso/v1/getUserInfo', {
    accessToken,
    desensitize: data.desensitize !== undefined ? data.desensitize : true
  })
}

/** 编辑用户信息 */
export function editModelHubUser(data = {}) {
  const accessToken = data.accessToken || getModelHubAccessToken()
  const request = createModelHubRequest(accessToken)
  return request.post('/sso/v1/userEdit', {
    ...data,
    accessToken
  })
}
