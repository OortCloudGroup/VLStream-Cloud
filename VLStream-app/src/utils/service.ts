/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 11:05:50
 * @Last Modified by: 兰舰
 * @Last Modified time: 2025-09-26 14:18:12
* @Copyright aPaaS-front-team. All rights reserved.
*/
import axios, { type AxiosInstance, type AxiosRequestConfig } from 'axios'
import { get } from 'lodash-es'
import { getToken, removeToken } from './cache'
import Config from '@/config'
import useGoWhere from '@/hooks/useGoWhere'
import { showToast } from 'vant'
// 节流白名单
const whiteApi = ['/menu/v1/myAuth', 'sso/v1/getUserList', 'sso/v1/getDeptUser']
function includeApi(url) {
  return whiteApi.some(item => { return url.includes(item) })
}

/** 创建请求实例 */
function createService() {
  // 节流: n 秒内只运行一次，若在n 秒内重复触发，只有一次生效
  let repeatArr = new Map()
  let timer = 0
  let duration = 800 // 间隔时间
  // 创建一个 Axios 实例
  const service = axios.create()
  // 请求拦截
  service.interceptors.request.use(
    (config) => {
      const nowTime = new Date().getTime()
      // console.log('节流处理中，稍后再试',repeatArr.get(`${config.method}-${config.url}`) && (nowTime -  repeatArr.get(`${config.method}-${config.url}`)) < 1000,repeatArr.get(`${config.method}-${config.url}`), `${config.url}`,  !includeApi(`${config.url}`))
      //   console.log(repeatArr)
      if (repeatArr.get(`${config.method}-${config.url}`) && (nowTime - repeatArr.get(`${config.method}-${config.url}`)) < duration && !includeApi(`${config.url}`)) {
        if (timer) {
          window.clearTimeout(timer)
        }
        timer = setTimeout(() => {
          repeatArr.set(`${config.method}-${config.url}`, nowTime)
        }, 1000)
        console.warn('request to fast', `${config.url}`)
      }
      repeatArr.set(`${config.method}-${config.url}`, nowTime)
      config.metadata = {
        startTime: new Date().getTime()
      }
      return config
    },
    // 发送失败
    (error) => {
      return Promise.reject(error)
    }
  )
  // 响应拦截（可根据具体业务作出相应的调整）
  service.interceptors.response.use(
    (response) => {
      // console.log(apiData)
      // 如果返回的状态码为200，说明接口请求成功，可以正常拿到数据
      // 否则的话抛出错误 结合自身业务和后台返回的接口状态约定写respone拦截器
      if (response.status === 200) {
        if (response.data.code !== 200) {
          if (response.data?.msg === '无效的accesstoken') {
            removeToken()
            location.reload()
          } else {
            if (response.data.msg) showToast(response.data.msg)
          }
        }
      }
      return response.data
    },
    (error) => {
      const httpStatusCode = error.response?.status
      let errMessage = error.message
      const errorBody = error.response?.data
      if (errorBody && errorBody.code) {
        errMessage = errorBody.code + errorBody.msg
        errorBody.msg = errorBody.msg || errorBody.message
        errorBody.msg && showToast(errorBody.msg)
        if (errorBody.msg?.includes('accessToken无效') || errorBody.msg?.includes('无效的accesstoken') || errorBody.msg?.includes('token失效')) {
          useGoWhere().goWhere()
        }
      } else if (errMessage) {
        showToast(errMessage)
      }
      if (httpStatusCode === 404) {
        return Promise.reject(errMessage)
      }
      // 以下两个状态码的返回错误
      if (httpStatusCode === 400) {
        return errorBody
      }
      if (httpStatusCode === 500) {
        return errorBody
      }
      if (error.message.includes('timeout')) {
        return Promise.reject('连接 超时')
      }
      return Promise.reject('系统繁忙，请示稍后再试')
    }
  )
  return service
}

/** 创建请求方法 */
function createRequestFunction(service: AxiosInstance) {
  return function <T>(config: AxiosRequestConfig): Promise<T> {
    const configDefault = {
      headers: {
        // 携带 Token
        'Content-Type': get(config, 'headers.Content-Type', 'application/json'),
        ...Config.headers,
        'AccessToken': getToken()
      },
      timeout: 10 * 1000,
      data: {}
    }
    return service(Object.assign(configDefault, config)).then()
  }
}

/** 用于网络请求的实例 */
export const service = createService()
/** 用于网络请求的方法 */
export const request = createRequestFunction(service)
