import axios from 'axios'
import { getModelHubAccessToken } from '@/utils/modelHubAuth'

/**
 * AI 模型接口（apaas-deployment-service）
 * 路径：{platform}/bus/apaas-deployment-service/aiModel/v1/*
 */
const DEPLOYMENT_BASE = import.meta.env.DEV
  ? '/bus/apaas-deployment-service'
  : 'https://workup-dev.myoumuamua.com:6433/bus/apaas-deployment-service'

const MODEL_HUB_HEADERS = {
  'Content-Type': 'application/json',
  accept: 'application/json, text/plain, */*',
  requesttype: 'app',
  appid: '08e3168bd56a4e75ae3d5dee63db0657',
  secretkey: '32e3ca224aa741fbb1362d33070bca2f'
}

function createRequest(accessToken) {
  const token = accessToken || getModelHubAccessToken()
  const tenantId =
    sessionStorage.getItem('modelHubTenantId') ||
    localStorage.getItem('modelHubTenantId') ||
    ''

  const instance = axios.create({
    baseURL: DEPLOYMENT_BASE,
    timeout: 30000,
    headers: {
      ...MODEL_HUB_HEADERS,
      ...(token ? { accesstoken: token } : {}),
      ...(tenantId ? { tenantid: tenantId } : {})
    }
  })

  instance.interceptors.response.use(
    (response) => response.data,
    (error) => Promise.reject(error)
  )

  return instance
}

function withToken(data = {}) {
  return {
    accessToken: data.accessToken || getModelHubAccessToken(),
    ...data
  }
}

/** 获取模型列表 POST /aiModel/v1/list */
export function getAiModelList(data = {}) {
  const request = createRequest(data.accessToken)
  return request.post('/aiModel/v1/list', withToken({
    page: data.page || data.pageNum || 1,
    pageNum: data.pageNum || data.page || 1,
    pageSize: data.pageSize || data.pagesize || 12,
    pagesize: data.pagesize || data.pageSize || 12,
    status: data.status !== undefined ? data.status : 0,
    keyword: data.keyword || data.name || ''
  }))
}

/** 获取模型详情 POST /aiModel/v1/detail */
export function getAiModelDetail(data = {}) {
  const request = createRequest(data.accessToken)
  return request.post('/aiModel/v1/detail', withToken({
    uid: data.uid
  }))
}

/** 创建模型 POST /aiModel/v1/add */
export function addAiModel(data = {}) {
  const request = createRequest(data.accessToken)
  return request.post('/aiModel/v1/add', withToken({
    name: data.name,
    alias: data.alias,
    model_type: data.model_type,
    description: data.description || '',
    file_path: data.file_path
  }))
}

/** 编辑模型 POST /aiModel/v1/edit */
export function editAiModel(data = {}) {
  const request = createRequest(data.accessToken)
  return request.post('/aiModel/v1/edit', withToken({
    uid: data.uid,
    name: data.name,
    alias: data.alias,
    model_type: data.model_type,
    description: data.description || '',
    file_path: data.file_path
  }))
}

/** 删除模型 POST /aiModel/v1/delete */
export function deleteAiModel(data = {}) {
  const request = createRequest(data.accessToken)
  return request.post('/aiModel/v1/delete', withToken({
    uid: data.uid
  }))
}
