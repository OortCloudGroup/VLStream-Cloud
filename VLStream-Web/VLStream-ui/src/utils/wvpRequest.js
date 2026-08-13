import axios from 'axios'
import { ElMessage } from 'element-plus'
import { saveAs } from 'file-saver'
import { getStoredToken } from '@/utils/request'

export const getWvpBaseURL = () => {
  const configuredBaseURL = import.meta.env.VITE_WVP_API_BASE_URL
  if (configuredBaseURL) return configuredBaseURL
  return import.meta.env.DEV ? '/wvp-api' : '/bus/wvp-server'
}

export const getWvpToken = () => {
  const urlParams = new URLSearchParams(window.location.search)
  return urlParams.get('wvpAccessToken')
    || sessionStorage.getItem('wvpAccessToken')
    || localStorage.getItem('wvpAccessToken')
    || getStoredToken()
}

const wvpRequest = axios.create({
  baseURL: getWvpBaseURL(),
  timeout: 1000000,
  headers: {
    'Content-Type': 'application/json;charset=utf-8'
  }
})

const applyWvpHeaders = (config) => {
  const token = getWvpToken()
  if (token) config.headers.accessToken = token.replace(/^Bearer\s+/i, '')

  config.headers.requestType = 'app'
  config.headers['X-WVP-Auth-Source'] = 'vlstream'
  return config
}

wvpRequest.interceptors.request.use(applyWvpHeaders)

wvpRequest.interceptors.response.use(
  (response) => {
    if (response.config.responseType === 'blob' || response.config.responseType === 'arraybuffer') {
      return response.data
    }

    const data = response.data
    const code = data?.code ?? 200
    if (code === 200) {
      if (data && Object.prototype.hasOwnProperty.call(data, 'total')) {
        data.total = Number(data.total)
      }
      return data
    }

    const message = data?.msg || `WVP 接口返回异常（${code}）`
    ElMessage.error(message)
    return Promise.reject(new Error(message))
  },
  (error) => {
    const message = error.response?.data?.msg
      || (error.code === 'ECONNABORTED' ? 'WVP 接口请求超时' : 'WVP 服务连接失败')
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export async function downloadWvp(url, params, filename, config = {}) {
  const data = await wvpRequest.post(url, params, {
    responseType: 'blob',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    ...config
  })
  saveAs(new Blob([data]), filename)
}

export default wvpRequest
