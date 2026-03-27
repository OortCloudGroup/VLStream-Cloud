/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 10:46:45
* @Last Modified by:   兰舰
* @Last Modified time: 2024-11-15 10:46:45
* @Copyright aPaaS-front-team. All rights reserved.
*/
import { request } from '@/utils/service'
import config from '@/config'
// import config from '@/config'

function commonFunc<T, K>(interfaceName: string, data: T, method: string, type = true) {
  let params = type ? { params: data } : { data: data }
  return request<K>({
    // url: 'http://192.168.88.56:8099' + interfaceName,
    url: config.URL + config.gateWay + 'oortcloud-qrcode' + interfaceName,
    method: method,
    ...params
  })
}
// function commonFuncBlob<T, K>(interfaceName: string, data: T, method: string) {
//   return request<K>({
//     url: config.URL + config.gateWay + 'oortcloud-qrcode' + interfaceName,
//     method: method,
//     params: data,
//     responseType: 'blob'
//   })
// }

// 二维码扫码登录相关接口
/* 查询二维码登录状态 */
export function checkLoginStatus(data) {
  return commonFunc('/qrCode/scanCodeLogin/checkLoginStatus', data, 'get')
}

/* 获取/刷新登录二维码 */
export function getLoginQrCode(data) {
  return commonFunc('/qrCode/scanCodeLogin/getLoginQrCode', data, 'post', false)
}

// 二维码扫码统计
/* 分页查询扫码历史 */
export function scanPage(data: any) {
  return commonFunc('/qrCode/scan/page/' + data, '', 'get')
}

// 统计某个日期范围内二维码每一天被扫描数量统计
export function scanStatisticsCount(data: any, data1:any) {
  return commonFunc('/qrCode/scan/statisticsCount/' + data, data1, 'get')
}

// 二维码被扫描数量统计(总数、近七日、昨日、今日)
export function scanStatisticsCountByDate(data: any) {
  return commonFunc('/qrCode/scan/statisticsCountByDate/' + data, '', 'get')
}

// 统计某个日期范围内二维码每一天被扫描数量统计
export function scanStatisticsTypeCount(data: any, data1:any) {
  return commonFunc('/qrCode/scan/statisticsTypeCount/' + data, data1, 'get')
}

// 二维码被扫描数量统计(总数、近七日、昨日、今日)
export function scanStatisticsTypeCountByDate(data: any) {
  return commonFunc('/qrCode/scan/statisticsCountTypeByDate/' + data, '', 'get')
}

// 二维码操作
/* 根据文本生成二维码图片 */
export function operateCreat(data: any) {
  return commonFunc('/qrCode/operate/creat', data, 'post')
}

/* 识别二维码 */
export function operateRecognize(data: any) {
  return commonFunc('/qrCode/operate/recognize', data, 'get')
}

/* 发布二维码 */
export function operateRegisterQrCode(data: any) {
  return commonFunc('/qrCode/operate/registerQrCode', data, 'post', false)
}

// 二维码管理
/* 批量删除类型 */
export function qrCodeManager(data:any) {
  return commonFunc('/qrCode/manager/' + data, '', 'delete')
}

/* 分页查询二维码类型 */
export function managerQueryPageList(data: any) {
  return commonFunc('/qrCode/manager/queryPageList', data, 'get')
}

// 二维码类型管理
/* 批量删除类型 */
export function typeTypeIds(data:any) {
  return commonFunc('/qrCode/type/' + data, '', 'delete')
}

// 分页查询二维码类型
export function typeQueryPageList(data: any) {
  return commonFunc('/qrCode/type/queryPageList', data, 'get')
}

// 新增/修改类型
export function typeSave(data: any) {
  return commonFunc('/qrCode/type/save', data, 'post', false)
}

// test
export function typeTest(data: any) {
  return commonFunc('/qrCode/type/test', data, 'post')
}

// 二维码统计面板
/* 总扫码数、总二维码数、昨日扫码数、近7日扫码数、总类型数统计 */
export function getDisplayData() {
  return commonFunc('/qrCode/display/getDisplayData', '', 'get')
}

/* 二维码扫码占比统计 */
export function getQrScanPercent1(data: any) {
  return commonFunc('/qrCode/display/getQrScanPercent', data, 'get')
}

/* 今日扫码数统计，以小时分组 */
export function getTodayScanData() {
  return commonFunc('/qrCode/display/getTodayScanData', '', 'get')
}

// /* 二维码扫码前六统计 */
export function getTopSixScanData(data: any) {
  return commonFunc('/qrCode/display/getTopSixScanData', data, 'get')
}

/* 类型扫码前六统计 */
export function getTopSixScanTypeData(data: any) {
  return commonFunc('/qrCode/display/getTopSixScanTypeData', data, 'get')
}
