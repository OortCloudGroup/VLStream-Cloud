import { request } from '@/utils/service'
// import { getToken } from '@/utils/cache/cookies'
import config from '@/config'

function commonFuncC<T, K>(interfaceName: string, data: T, method: string, isParams = false) {
  let params = method === 'get' || method === 'delete' || method === 'GET' || method === 'DELETE' ? { params: data } : { data: data }
  if (isParams) {
    params = { params: data }
  }
  return request<K>({
    url: config.URL + config.gateWay + 'apaas-unified-msg' + interfaceName,
    method: method,
    ...params
  })
}

function commonFunc2<T, K>(interfaceName: string, data: T, method: string) {
  return request<K>({
    url: config.URL + config.gateWay + 'apaas-unified-msg' + interfaceName,
    method: method,
    data: data,
    responseType: 'blob'
  })
}

/* 通道费用统计 */
export function ductCostStatis(data: any) {
  return commonFuncC('/home/v1/duct/cost/statis', data, 'post')
}

/* 通道使用统计 */
export function ductUseStatis(data: any) {
  return commonFuncC('/home/v1/duct/use/statis', data, 'post')
}

/* 看板-消息费用情况*/
export function msgcoststatis(data: any) {
  return commonFuncC('/home/v1/msgcoststatis', data, 'post')
}

/* 看板-消息发送情况*/
export function msgsendstatis(data: any) {
  return commonFuncC('/home/v1/msgsendstatis', data, 'post')
}

/* 看板-统计发送消息跳转 查看的接口 */
export function msgsendlist(data: any) {
  return commonFuncC('/msg/v1/msgsendlist', data, 'post')
}

/* 推送记录-消息列表详情接口*/
export function msgListInfo(data: any) {
  return commonFuncC('/msg/v1/list/info', data, 'post')
}

/* 数据列表 */
export function homeFirst(data: any) {
  return commonFuncC('/home/v1/index/first', data, 'post')
}

/* 数据看板 发送渠道统计/发送状态 */
export function homeTwo(data: any) {
  return commonFuncC('/home/v1/index/two', data, 'post')
}

/* 数据看板曲线图 */
export function homeThree(data: any) {
  return commonFuncC('/home/v1/index/three', data, 'post')
}

/* 报表费用-导出*/
export function msg_cost_export(data: any) {
  return commonFunc2('/home/v1/msg_cost_export', data, 'post')
}

/* 消息发送情况-导出*/
export function msg_send_export(data: any) {
  return commonFunc2('/home/v1/msg_send_export', data, 'post')
}
