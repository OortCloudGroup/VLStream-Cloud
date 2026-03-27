import { request } from '@/utils/service'
import config from '@/config'

function commonFuncC<T, K>(interfaceName: string, data: T, method: string, isParams = false) {
  let params = method === 'get' || method === 'delete' || method === 'GET' || method === 'DELETE' ? { params: data } : { data: data }
  if (isParams) {
    params = { params: data }
  }
  return request<K>({
    url: config.URL + config.gateWay + 'apaas-pay-userdefine/pay-userdefine' + interfaceName,
    method: method,
    ...params
  })
}

function commonFuncC1<T, K>(interfaceName: string, data: T, method: string, isParams = false) {
  let params = method === 'get' || method === 'delete' || method === 'GET' || method === 'DELETE' ? { params: data } : { data: data }
  if (isParams) {
    params = { params: data }
  }
  return request<K>({
    url: config.URL + config.gateWay + 'apaas-pay/pay' + interfaceName,
    method: method,
    ...params
  })
}

/* 收银台订单创建*/
export function lakalaCreateOrder(data: any) {
  return commonFuncC('/uPay/scanCode', data, 'post')
}

/* 收银台订单 退款*/
export function lakalaRefundOrder(data: any) {
  return commonFuncC1('/uPay/refund', data, 'POST')
}
