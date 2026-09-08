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
import { getToken, removeToken, getRefreshToken, setRefreshToken } from './cache/cookies'
import Config from '@/config'
import { useErrorMsgStoreHook } from '@/store/modules/useErrorMsg'
import useGoWhere  from '@/hooks/useGoWhere'
import { ElMessageBox } from 'element-plus'
import { refreshToken } from '@/api/system/localAuth'
import { applyAuthHeaders, applyPlatformGatewayHeaders, getStoredToken } from '@/utils/request'
import {
  isMultiTenantMode,
  isPlatformTokenFailure,
  redirectToPlatformLogin
} from '@/utils/platformSession'


import { useUserStoreHook } from '@/store/modules/useraPaas'


// and new
let isRefreshing = false;
let requestsQueue: Array<{
  resolve: (value: any) => void,
  reject: (reason?: any) => void,
  config: AxiosRequestConfig
}> = [];

// token new Process method
const handleTokenRefresh = (originalResponse: any, originalConfig?: AxiosRequestConfig) => {
  return new Promise((resolve, reject) => {
    // if in new token, current
    if (isRefreshing) {
      requestsQueue.push({ resolve, reject, config: originalConfig || originalResponse.config });
      return;
    }

    isRefreshing = true;

    // new token Get new
    const refreshTokenValue = getRefreshToken();
    let params = { refreshToken: '' }
    if (!!refreshTokenValue) {
      params.refreshToken = refreshTokenValue;
    }
    refreshToken(params)
      .then((res: any) => {
        const refreshRes = res as { code: number, data: { accessToken: string, refreshToken?: string }, msg: string };
        if (refreshRes.code === 200 && refreshRes.data && refreshRes.data.accessToken) {
          // new new token
          if(refreshRes.data.refreshToken) {
            setRefreshToken(refreshRes.data.refreshToken);
          }
          // new token
          const setToken = useUserStoreHook().setStoreToken
          setToken(refreshRes.data.accessToken);
          // new in token
          const updateTokenInRequest = (requestConfig: AxiosRequestConfig) => {
            // axios 1.x
            const headers = requestConfig.headers as Record<string, any> || {};
            // new in accesstoken
            requestConfig.headers = { ...headers, accesstoken: refreshRes.data.accessToken };
            // if URL in tokenparameter, also need to new
            if (requestConfig.params) {
              requestConfig.params = { ...requestConfig.params };
              if (requestConfig.params.accessToken) {
                requestConfig.params.accessToken = refreshRes.data.accessToken;
              }
            }
            // if URL in accessTokenQuery parameter, also need to new
            if (requestConfig.url && requestConfig.url.includes('accessToken=')) {
              requestConfig.url = requestConfig.url.replace(/accessToken=[^&]*/g, `accessToken=${encodeURIComponent(refreshRes.data.accessToken)}`);
            }
            if(requestConfig.data) {
              try {
                // if is data ( is JSON )
                if (typeof requestConfig.data === 'string') {
                  let tempData = JSON.parse(requestConfig.data);
                  if (tempData.accessToken) {
                    tempData.accessToken = res.data.accessToken;
                    requestConfig.data = JSON.stringify(tempData);
                  }
                } else if (typeof requestConfig.data === 'object') {
                  // if is object data
                  requestConfig.data = { ...requestConfig.data };
                  if (requestConfig.data.accessToken) {
                    requestConfig.data.accessToken = res.data.accessToken;
                  }
                }
              } catch (e) {
                // if Parse JSONfailed,
                console.warn('Failed to parse request data:', e);
              }
            }
            console.log('---00', requestConfig)
            return requestConfig;
          };

          // new in
          const tokenRefreshed = () => {
            requestsQueue.forEach(request => {
              const newRequest = { ...request.config };
              updateTokenInRequest(newRequest);
              service(newRequest).then(resp => {
                request.resolve(resp);
              }).catch(err => {
                request.reject(err);
              });
            });
            requestsQueue = [];
          };

          tokenRefreshed();

          // new current
          if (originalConfig) {
            // Process
            updateTokenInRequest(originalConfig);
            service(originalConfig).then(resp => {
              resolve(resp);
            }).catch(err => {
              reject(err);
            });
          } else {
            // successfully
            const originalRequest = originalResponse.config;
            updateTokenInRequest(originalRequest);
            service(originalRequest).then(resp => {
              resolve(resp);
            }).catch(err => {
              reject(err);
            });
          }
        } else {
          // new failed,
          removeToken();
          useGoWhere().goWhere();
          reject(originalResponse);
        }
      })
      .catch(error => {
        // new failed,
        removeToken();
        useGoWhere().goWhere();
        reject(error);
      })
      .finally(() => {
        isRefreshing = false;
      });
  });
};
 //
const whiteApi = ['/menu/v1/myAuth','sso/v1/getUserList', 'sso/v1/getDeptUser']
function includeApi(url) {
  return whiteApi.some(item=> { return url.includes(item)})
}
const noWarningApi = ['im-api']
function shouldShowWarningByApi(url = '') {
  return !noWarningApi.some(item => url.includes(item))
}

/**
 * Check whether to already item after 、only taskinterface.
 */
function isLocalTaskApi(url = '') {
  return /^\/?task\//.test(url)
}

function isDirectPlatformApi(url = '') {
  try {
    const parsed = new URL(String(url), window.location.origin)
    return parsed.pathname.startsWith('/bus/apaas-')
      && !parsed.pathname.startsWith('/bus/apaas-vls-server')
  } catch (error) {
    return false
  }
}

function rejectExpiredPlatformSession(originalResponse: any) {
  redirectToPlatformLogin()
  return Promise.reject({
    isInterceptorDetour: true,
    message: originalResponse?.data?.msg || originalResponse?.response?.data?.msg || '平台登录信息已过期'
  })
}

/**
 * finish will , old SSO new interface .
 */
function rejectExpiredLocalSession(originalResponse: any) {
  removeToken()
  useGoWhere().goWhere()
  return Promise.reject({
    isInterceptorDetour: true,
    message: originalResponse?.data?.msg || originalResponse?.response?.data?.msg || '当前用户登录信息已过期'
  })
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
    (response) => {
      console.log('response-----------------', response)
      // console.log(apiData)
      // if to 200, interface successfully, data
      // and after interface respone
      if (response.status === 200) {
        if (response.data.code !== 200) {
          if (isMultiTenantMode() && isDirectPlatformApi(response.config?.url || '')
            && isPlatformTokenFailure(response.data)) {
            return rejectExpiredPlatformSession(response)
          }
          // Check response.data.code === 4004 then new token
          if (response.data.code === 4004) {
            if (isLocalTaskApi(response.config?.url || '')) {
              return rejectExpiredLocalSession(response)
            }
            return handleTokenRefresh(response);
          }
          // new interface and msginterface Process
          if(response.data?.code === 4444 || response.data?.msg === '无效的accesstoken') {
            // new failed,
            removeToken();
            useGoWhere().goWhere();
            return Promise.reject({
              isInterceptorDetour: true, // Custom , in
              message: '当前用户登录信息已过期'
            });
          } else {
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
      }
      if ((response.config as ExtendedAxiosRequestConfig).returnFullResponse) {
        return response
      }
      return response.data
    },
    (error) => {
      const httpStatusCode = error.response?.status
      let errMessage = error.message
      const errorBody = error.response?.data
      if(errorBody && errorBody.code ) {
        if (isMultiTenantMode() && isDirectPlatformApi(error.config?.url || '')
          && isPlatformTokenFailure(errorBody)) {
          return rejectExpiredPlatformSession(error)
        }
        // whether need to new token
        if (errorBody.code === 4004) {
          if (isLocalTaskApi(error.config?.url || '')) {
            return rejectExpiredLocalSession(error)
          }
          return handleTokenRefresh(error, error.config);
        }
        errMessage = errorBody.code +  errorBody.msg
        errorBody.msg = errorBody.msg || errorBody.message
        errorBody.msg && (message as any).error(errorBody.msg)
        store.addErrorMsg({
          msg: errorBody.msg,interfaceName: error.config.url,
          startTime:  (error.config as ExtendedAxiosRequestConfig).metadata!.startTime, endTime: new Date().getTime()
        })
        if(errorBody.msg?.includes('accessToken无效') || errorBody.msg?.includes('无效的accesstoken') || errorBody.msg?.includes('token失效')) {
          useGoWhere().goWhere()
        }
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
