/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import axios, { type AxiosInstance, type AxiosRequestConfig } from 'axios'

// AxiosRequestConfiginterface metadataproperty
interface ExtendedAxiosRequestConfig extends AxiosRequestConfig {
  metadata?: {
    startTime: number;
  };
  returnFullResponse?: boolean;
}
import { message } from '@/utils/resetMessage'
import { get } from 'lodash-es'
import { getToken } from './cache/cookies'
import Config from '@/config'
import { useErrorMsgStoreHook } from '@/store/modules/useErrorMsg'
import { ElMessageBox } from 'element-plus'
import { applyAuthHeaders, applyPlatformGatewayHeaders, getStoredToken, handleSessionFailureAsync } from '@/utils/request'
 //
const whiteApi = ['/menu/v1/myAuth','sso/v1/getUserList', 'sso/v1/getDeptUser']
function includeApi(url) {
  return whiteApi.some(item=> { return url.includes(item)})
}
const noWarningApi = ['im-api']
function shouldShowWarningByApi(url = '') {
  return !noWarningApi.some(item => url.includes(item))
}

/* * instance */
function createService() {
 // : n only , in n , only
  let  repeatArr = new Map<string, number>()
  let timer: number | NodeJS.Timeout | null = 0
  let duration = 800 //
  const store = useErrorMsgStoreHook()
  // Axios instance
  const service = axios.create()
  //
  service.interceptors.request.use(
    (config: AxiosRequestConfig) => {
      const nowTime = new Date().getTime()
      // console.log(' Process in , after ',repeatArr.get(`${config.method}-${config.url}`) && (nowTime - repeatArr.get(`${config.method}-${config.url}`)) < 1000,repeatArr.get(`${config.method}-${config.url}`), `${config.url}`, !includeApi(`${config.url}`))
      //   console.log(repeatArr)
      const existingTime = repeatArr.get(`${config.method}-${config.url}`);
      if (existingTime !== undefined && (nowTime -  existingTime) < duration  && !includeApi(`${config.url}`)) {
        if (timer) {
          window.clearTimeout(timer as number)
        }
        timer = setTimeout(() => {
          repeatArr.set(`${config.method}-${config.url}`, nowTime)
        }, 1000) as NodeJS.Timeout;
        console.warn('request to fast', `${config.url}`)
      }
      repeatArr.set(`${config.method}-${config.url}`, nowTime);
      (config as ExtendedAxiosRequestConfig).metadata = {
        startTime: new Date().getTime(),
      };
      // axios 1.x
      const headers = config.headers as Record<string, any>;
      // FormData to multipart/form-data
      if (config.data instanceof FormData && headers) {
        delete headers['Content-Type']
        delete headers['content-type']
      }
      if(headers?.tenantid && !config.url?.includes('sso/v1/login')
        && !config.url?.includes('/sso/v1/getCaptcha')
        && !config.url?.includes('sso/v2/getLoginCode')
        &&!config.url?.includes('sso/v1/verifyToken') ) {
        // if sessionStoage id and ,
        if(window.localStorage.getItem('recentlyLoginTenantId') !== headers?.tenantid) {
          ElMessageBox.alert('当前登录账号身份已变更，请刷新后重试', '提示', {
            confirmButtonText: '确定',
            callback: () => {
              location.reload()
            },
          })
          return Promise.reject({
            isInterceptorDetour: true, // Custom , in
            message: '当前登录账号身份已变更，请刷新后重试'
          });
        }
      }
      applyAuthHeaders(config)
      if (String(config.url || '').includes('/bus/apaas-vls-server')) {
        applyPlatformGatewayHeaders(config)
      }
      return config
    },
    // failed
    (error) =>{
      store.addErrorMsg({
        msg: error.message,interfaceName: error.config.url,
        startTime: error.config.metadata.startTime, endTime: new Date().getTime()
      })
      return Promise.reject(error)
    }
  )
  // ( )
  service.interceptors.response.use(
    async (response) => {
      const sessionError = await handleSessionFailureAsync(response)
      if (sessionError) return Promise.reject(sessionError)
      // console.log(apiData)
      // if to 200, interface successfully, data
      // and after interface respone
      if (response.status === 200) {
        if (response.data.code !== 200) {
            if (response.data.msg){
              const url = response.config?.url || ''
              // Check current whether in page
              if(location.hash.substring(1) === '') {
                (message as any).warning(response.data.msg)
              } else {
                  if (shouldShowWarningByApi(response.config?.url || '')) {
                    (message as any).warning(response.data.msg)
                  }
              }
              store.addErrorMsg({
                msg: response.data.msg,interfaceName: response.config.url,
                startTime:  (response.config as ExtendedAxiosRequestConfig).metadata!.startTime, endTime: new Date().getTime()
              })
            }
        }
      }
      if ((response.config as ExtendedAxiosRequestConfig).returnFullResponse) {
        return response
      }
      return response.data
    },
    async (error) => {
      if (axios.isCancel(error)) return Promise.reject(error)
      const sessionError = await handleSessionFailureAsync(error)
      if (sessionError) return Promise.reject(sessionError)
      const httpStatusCode = error.response?.status
      let errMessage = error.message
      const errorBody = error.response?.data
      if(errorBody && errorBody.code ) {
        errMessage = errorBody.code +  errorBody.msg
        errorBody.msg = errorBody.msg || errorBody.message || error.message
        errorBody.msg && (message as any).error(errorBody.msg)
        store.addErrorMsg({
          msg: errorBody.msg,interfaceName: error.config.url,
          startTime:  (error.config as ExtendedAxiosRequestConfig).metadata!.startTime, endTime: new Date().getTime()
        })
      } else if(errMessage) {
        (message as any).error(errMessage)
      }
      if(httpStatusCode === 404) {
        return Promise.reject(errMessage)
      }
      //
      if(httpStatusCode === 400) {
        return errorBody
      }
      if(httpStatusCode === 500) {
        return errorBody
      }
      if (error.message.includes('timeout')) {
        return Promise.reject('连接 超时')
      }
      return Promise.reject("系统繁忙，请示稍后再试")
    }
  )
  return service
}

/* * method */
function createRequestFunction(service: AxiosInstance) {
  return function <T>(config: AxiosRequestConfig): Promise<T> {
      const configDefault = {
      headers: {
        // Token
        'Content-Type': get(config, 'headers.Content-Type', 'application/json'),
        ...Config.headers,
        accesstoken: getStoredToken() || getToken()
      },
      timeout: config.timeout || 10 * 1000,
      data: {}
    }
    const mergedConfig = Object.assign(configDefault, config)
    mergedConfig.headers = {
      ...configDefault.headers,
      ...(config.headers as Record<string, any> || {})
    }
    applyAuthHeaders(mergedConfig)
    if (String(mergedConfig.url || '').includes('/bus/apaas-vls-server')) {
      applyPlatformGatewayHeaders(mergedConfig)
    }
    return service(mergedConfig).then()
  }
}

/* * instance */
export const service = createService()
/* * method */
export const request = createRequestFunction(service)
