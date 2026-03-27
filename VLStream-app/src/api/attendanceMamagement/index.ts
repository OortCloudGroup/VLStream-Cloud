import { request } from '@/utils/service'
import config from '@/config'
function commonFunc<T, K>(interfaceName: string, data: T, method: string, isParams = false) {
  let params = method === 'get' || method === 'delete' || method === 'GET' || method === 'DELETE' ? { params: data } : { data: data }
  if (isParams) {
    params = { params: data }
  }
  return request<K>({
    url: config.URL + config.gateWay + 'apaas-map-server' + interfaceName,
    method: method,
    ...params
  })
}

function commonFunc2<T, K>(interfaceName: string, data: T, method: string, isParams = false) {
  let params = method === 'get' || method === 'delete' || method === 'GET' || method === 'DELETE' ? { params: data } : { data: data }
  if (isParams) {
    params = { params: data }
  }
  return request<K>({
    url: config.URL + config.gateWay + 'apaas-sso' + interfaceName,
    method: method,
    ...params
  })
}

function commonFunc3<T, K>(interfaceName: string, data: T, method: string, isParams = false) {
  let params = method === 'get' || method === 'delete' || method === 'GET' || method === 'DELETE' ? { params: data } : { data: data }
  if (isParams) {
    params = { params: data }
  }
  return request<K>({
    url: config.URL + config.gateWay + 'zc-smartcity-server/admin-api' + interfaceName,
    method: method,
    ...params
  })
}

function commonFunc4<T, K>(interfaceName: string, data: T, method: string, isParams = false) {
  let params = method === 'get' || method === 'delete' || method === 'GET' || method === 'DELETE' ? { params: data } : { data: data }
  if (isParams) {
    params = { params: data }
  }
  return request<K>({
    url: config.URL + config.gateWay + 'zc-smartcity-server/app-api' + interfaceName,
    method: method,
    ...params
  })
}

// 获取规则树列表
export const getCategoryTree = (data) => {
  return commonFunc('/mapServer/mlsFeatures/category/tree', data, 'get')
}

// 获取现场动态
export const getCategoryList = (data) => {
  return commonFunc('/mapServer/mlsFeatures/category/list', data, 'get')
}

// 现场动态地图标点
export const getGeoJsonInfo = (data) => {
  return commonFunc('/mapServer/mlsFeatures/getGeoJson', data, 'post')
}

// 部门应用榜
export function appDept(data) {
  return commonFunc2('/rank/v1/app.dept', data, 'post')
}

// 用户应用榜
export function appUser(data) {
  return commonFunc2('/rank/v1/app.user', data, 'post')
}

// 获取用户的积分明细
export function scoreDetailList(data) {
  return commonFunc2('/rank/v1/score.detailList', data, 'post')
}

// 获得园林养护任务
export function getGreeneryManagementTask(data) {
  return commonFunc3('/smartCity/greenery-management-task/get', data, 'get')
}

// 获得园林巡查巡查任务
export function getGardenInspectionTask(data) {
  return commonFunc3('/smartCity/garden-inspection-task/get', data, 'get')
}

// 获得园林养护应急事件处理
export function getGreeneryManagementEmergencyIncident(data) {
  return commonFunc3('/smartCity/greenery-management-emergency-incident/get', data, 'get')
}

// 获得问题整改进度
export function getProblemCorrectionProgress(data) {
  return commonFunc3('/smartCity/problem-correction-progress/get', data, 'get')
}

// 获得问题整改进度分页
export function pageProblemCorrectionProgress(data) {
  return commonFunc3('/smartCity/problem-correction-progress/page', data, 'get')
}

// 创建问题整改信息
export function createProblemCorrectionProgress(data) {
  return commonFunc4('/smartCity/problem-correction-progress/create', data, 'post')
}

// 获取个人总积分
export function getPersonalTotalScore(data) {
  return commonFunc2('/rank/v1/getPersonalTotalScore', data, 'post')
}

