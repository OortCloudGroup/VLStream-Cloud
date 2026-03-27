/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 10:46:10
 * @Last Modified by: 兰舰
 * @Last Modified time: 2025-09-18 17:00:51
* @Copyright aPaaS-front-team. All rights reserved.
*/
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

// 底图列表
export function mlsBaseMapList(data: any) {
  return commonFunc('/mapServer/mlsBaseMap/page', data, 'get')
}

// 获取 GeoJSON数据
export function getGeoJSONData(data: any) {
  return commonFunc('/mapServer/mlsFeatures/getGeoJson', data, 'post')
}

// 分页获取GeoJSON 数据
export function getGeoJSONDataPage(data: any) {
  return commonFunc('/mapServer/mlsFeatures/getGeoJsonPage', data, 'post')
}

// 要素-列表
export function mlsFeaturesCategoryList(data: any) {
  return commonFunc('/mapServer/mlsFeatures/category/list', data, 'get')
}

// 获取逆地址解析
export function geocoderAddress(data: any) {
  return commonFunc('/mapServer/mlsBaseMap/geocoder', data, 'get')
}

// 获取搜索的周边
export function getSearchAround(data: any) {
  return commonFunc('/mapServer/mlsBaseMap/search', data, 'get')
}
