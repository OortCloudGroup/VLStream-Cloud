/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 10:46:24
* @Last Modified by:   兰舰
* @Last Modified time: 2024-11-15 10:46:24
* @Copyright aPaaS-front-team. All rights reserved.
*/
import { request } from '@/utils/service'
import config from '@/config'

function commonFunc<T, K>(interfaceName: string, data: T, method: string) {
  return request<K>({
    url: config.URL + config.gateWay + 'oortcloud-pay' + interfaceName,
    method: method,
    data: data
  })
}

function commonFunc1<T, K>(interfaceName: string, data: T, method: string, headerConf: any) {
  return request<K>({
    url: config.URL + config.gateWay + 'oortcloud-pay' + interfaceName,
    method: method,
    headers: {
      'AppId': headerConf.AppId,
      'MchId': headerConf.MchId,
      'Nonce': headerConf.Nonce,
      'Timestamp': headerConf.Timestamp,
      'Sign': headerConf.Sign
    },
    data: data
  })
}

// app
/* 绑定支付渠道 */
export function appBindChannel(data: any) {
  return commonFunc('/gateway/v1/app/bind_channel', data, 'post')
}

/* 更新绑定支付渠道 */
export function appUpChannel(data: any) {
  return commonFunc('/gateway/v1/app/up_channel', data, 'post')
}

/* 获取商户绑定的单条支付通道 */
export function appChannelConf(data: any) {
  return commonFunc('/gateway/v1/app/channel_conf', data, 'post')
}

/* 获取可选支付渠道 */
export function appChannelInfo(data: any) {
  return commonFunc('/gateway/v1/app/channel_info', data, 'post')
}

/* 获取支付渠道支付的方式 */
export function appChannelType(data: any) {
  return commonFunc('/gateway/v1/app/channel_type', data, 'post')
}

/* 获取商户绑定的支付通道 */
export function appPayChannel(data: any) {
  return commonFunc('/gateway/v1/app/pay_channel', data, 'post')
}

/* 获取秘钥 */
export function appSecretKey(data: any) {
  return commonFunc('/gateway/v1/app/secret_key', data, 'post')
}

/* 添加应用 */
export function appAdd(data: any) {
  return commonFunc('/gateway/v1/app/add', data, 'post')
}

/* 更新应用 */
export function appEdit(data: any) {
  return commonFunc('/gateway/v1/app/edit', data, 'post')
}

/* 更新应用 */
export function appInfo(data: any) {
  return commonFunc('/gateway/v1/app/info', data, 'post')
}

/* 应用列表 */
export function appList(data: any) {
  return commonFunc('/gateway/v1/app/list', data, 'post')
}

/* 删除应用 */
export function appDel(data: any) {
  return commonFunc('/gateway/v1/app/del', data, 'post')
}

// /* merchant */
/* 添加字典 */
export function merchantDicAdd(data: any) {
  return commonFunc('/gateway/v1/merchant/dictionary/add', data, 'post')
}

/* 获取字典 */
export function merchantDicList(data: any) {
  return commonFunc('/gateway/v1/merchant/dictionary/list', data, 'post')
}

/* 添加优惠卷规则 */
export function merchantCouponAdd(data: any) {
  return commonFunc('/gateway/v1/merchant/coupon/add', data, 'post')
}

/* 优惠卷列表 */
export function merchantCouponList(data: any) {
  return commonFunc('/gateway/v1/merchant/coupon/list', data, 'post')
}

/* 绑定分账用户 */
export function merchantDivideBind(data: any) {
  return commonFunc('/gateway/v1/merchant/divide/bind', data, 'post')
}

/* 解绑分账用户 */
export function merchantDivideDel(data: any) {
  return commonFunc('/gateway/v1/merchant/divide/unbind', data, 'post')
}

/* 修改分账用户列表 */
export function merchantDivideEdit(data: any) {
  return commonFunc('/gateway/v1/merchant/divide/edit', data, 'post')
}

/* 分账用户列表 */
export function merchantDivideList(data: any) {
  return commonFunc('/gateway/v1/merchant/divide/list', data, 'post')
}

/* 添加租户 */
export function merchantAdd(data: any) {
  return commonFunc('/gateway/v1/merchant/add', data, 'post')
}

/* 更新租户信息 */
export function merchantEdit(data: any) {
  return commonFunc('/gateway/v1/merchant/edit', data, 'post')
}

/* 获取租户列表 */
export function merchantList(data: any) {
  return commonFunc('/gateway/v1/merchant/list', data, 'post')
}

/* 获取租户信息 */
export function merchantInfo(data: any) {
  return commonFunc('/gateway/v1/merchant/info', data, 'post')
}

/* 添加会员 */
export function memberAdd(data: any) {
  return commonFunc('/gateway/v1/merchant/member/add', data, 'post')
}

/* 会员信息更新 */
export function memberEdit(data: any) {
  return commonFunc('/gateway/v1/merchant/member/edit', data, 'post')
}

// order
/* 订单详情 */
export function orderDetail(data: any) {
  return commonFunc('/gateway/v1/order/detail', data, 'post')
}

/* 订单列表 */
export function orderList(data: any) {
  return commonFunc('/gateway/v1/order/list', data, 'post')
}

/* 退款列表 */
export function refundList(data: any) {
  return commonFunc('/gateway/v1/order/refund/list', data, 'post')
}

/* 退款详情 */
export function refundDetail(data: any) {
  return commonFunc('/gateway/v1/order/refund/detail', data, 'post')
}

/* 订单统计 */
export function orderStats(data: any) {
  return commonFunc('/gateway/v1/order/stats', data, 'post')
}

// trade
/* 支付下单 */
export function TradePay(data: any) {
  return commonFunc('/gateway/v1/trade/pay', data, 'post')
}

/* 预下单 */
export function placeOrder(data: any, headerConf: any) {
  return commonFunc1('/gateway/v1/trade/place/order', data, 'post', headerConf)
}

/* 收银台数据 */
export function tradeCheckout(data: any) {
  return commonFunc('/gateway/v1/trade/checkout', data, 'post')
}

/* 收银台调起支付 */
export function orderPay(data: any) {
  return commonFunc('/gateway/v1/trade/order/pay', data, 'post')
}

/* 收银台订单查询*/
export function tradeQuery(data: any) {
  return commonFunc('/gateway/v1/trade/checkout/query', data, 'post')
}

/* 退款 */
export function tradeRefund(data: any, headerConf: any) {
  return commonFunc1('/gateway/v1/trade/refund', data, 'post', headerConf)
}

/* 退款查询 */
export function tradeRefundQuery(data: any, headerConf: any) {
  return commonFunc1('/gateway/v1/trade/refund/query', data, 'post', headerConf)
}

/* 退款查询 */
export function v1tradeQuery(data: any, headerConf: any) {
  return commonFunc1('/gateway/v1/trade/query', data, 'post', headerConf)
}
