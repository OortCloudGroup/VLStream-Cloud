/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 11:05:50
 * @Last Modified by: 兰舰
 * @Last Modified time: 2025-07-24 15:37:45
* @Copyright aPaaS-front-team. All rights reserved.
*/
import axios, { type AxiosInstance, type AxiosRequestConfig } from 'axios'

// 扩展AxiosRequestConfig接口以包含metadata属性
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
import { refreshToken } from '@/api/unifiedUsert/sso'
import { applyAuthHeaders, getStoredToken } from '@/utils/request'


import { useUserStoreHook } from '@/store/modules/useraPaas'


// 定义请求队列和刷新状态
let isRefreshing = false;
let requestsQueue: Array<{
  resolve: (value: any) => void,
  reject: (reason?: any) => void,
  config: AxiosRequestConfig
}> = [];

// 通用的token刷新处理方法
const handleTokenRefresh = (originalResponse: any, originalConfig?: AxiosRequestConfig) => {
  return new Promise((resolve, reject) => {
    // 如果正在刷新token，则将当前请求加入队列
    if (isRefreshing) {
      requestsQueue.push({ resolve, reject, config: originalConfig || originalResponse.config });
      return;
    }

    isRefreshing = true;

    // 使用刷新token来获取新的访问令牌
    const refreshTokenValue = getRefreshToken();
    let params = { refreshToken: '' }
    if (!!refreshTokenValue) {
      params.refreshToken = refreshTokenValue;
    }
    refreshToken(params)
      .then((res: any) => {
        const refreshRes = res as { code: number, data: { accessToken: string, refreshToken?: string }, msg: string };
        if (refreshRes.code === 200 && refreshRes.data && refreshRes.data.accessToken) {
          // 更新刷新token
          if(refreshRes.data.refreshToken) {
            setRefreshToken(refreshRes.data.refreshToken);
          }
          // 更新token
          const setToken = useUserStoreHook().setStoreToken
          setToken(refreshRes.data.accessToken);
          // 创建一个通用函数来更新请求中的token
          const updateTokenInRequest = (requestConfig: AxiosRequestConfig) => {
            // 添加类型断言以兼容 axios 1.x
            const headers = requestConfig.headers as Record<string, any> || {};
            // 更新请求头中的AccessToken
            requestConfig.headers = { ...headers, 'AccessToken': refreshRes.data.accessToken };
            // 如果URL中有token参数，也需要更新
            if (requestConfig.params) {
              requestConfig.params = { ...requestConfig.params };
              if (requestConfig.params.accessToken) {
                requestConfig.params.accessToken = refreshRes.data.accessToken;
              }
            }
            // 如果URL中包含accessToken查询参数，也需要更新
            if (requestConfig.url && requestConfig.url.includes('accessToken=')) {
              requestConfig.url = requestConfig.url.replace(/accessToken=[^&]*/g, `accessToken=${encodeURIComponent(refreshRes.data.accessToken)}`);
            }
            if(requestConfig.data) {
              try {
                // 如果是字符串格式的数据（通常是JSON字符串）
                if (typeof requestConfig.data === 'string') {
                  let tempData = JSON.parse(requestConfig.data);
                  if (tempData.accessToken) {
                    tempData.accessToken = res.data.accessToken;
                    requestConfig.data = JSON.stringify(tempData);
                  }
                } else if (typeof requestConfig.data === 'object') {
                  // 如果是对象格式的数据
                  requestConfig.data = { ...requestConfig.data };
                  if (requestConfig.data.accessToken) {
                    requestConfig.data.accessToken = res.data.accessToken;
                  }
                }
              } catch (e) {
                // 如果解析JSON失败，忽略错误
                console.warn('Failed to parse request data:', e);
              }
            }
            console.log('---00', requestConfig)
            return requestConfig;
          };

          // 重新发送队列中的请求
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

          // 重新发送当前请求
          if (originalConfig) {
            // 来自错误处理的请求
            updateTokenInRequest(originalConfig);
            service(originalConfig).then(resp => {
              resolve(resp);
            }).catch(err => {
              reject(err);
            });
          } else {
            // 来自成功响应的请求
            const originalRequest = originalResponse.config;
            updateTokenInRequest(originalRequest);
            service(originalRequest).then(resp => {
              resolve(resp);
            }).catch(err => {
              reject(err);
            });
          }
        } else {
          // 刷新失败，跳转到登录页
          removeToken();
          useGoWhere().goWhere();
          reject(originalResponse);
        }
      })
      .catch(error => {
        // 刷新失败，跳转到登录页
        removeToken();
        useGoWhere().goWhere();
        reject(error);
      })
      .finally(() => {
        isRefreshing = false;
      });
  });
};
 // 节流白名单
const whiteApi = ['/menu/v1/myAuth','sso/v1/getUserList', 'sso/v1/getDeptUser']
function includeApi(url) {
  return whiteApi.some(item=> { return url.includes(item)})
}
const noWarningApi = ['im-api']
function shouldShowWarningByApi(url = '') {
  return !noWarningApi.some(item => url.includes(item))
}

/** 创建请求实例 */
function createService() {
 // 节流: n 秒内只运行一次，若在n 秒内重复触发，只有一次生效
  let  repeatArr = new Map<string, number>()
  let timer: number | NodeJS.Timeout | null = 0
  let duration = 800 // 间隔时间
  const store = useErrorMsgStoreHook()
  // 创建一个 Axios 实例
  const service = axios.create()
  // 请求拦截
  service.interceptors.request.use(
    (config: AxiosRequestConfig) => {
      const nowTime = new Date().getTime()
      // console.log('节流处理中，稍后再试',repeatArr.get(`${config.method}-${config.url}`) && (nowTime -  repeatArr.get(`${config.method}-${config.url}`)) < 1000,repeatArr.get(`${config.method}-${config.url}`), `${config.url}`,  !includeApi(`${config.url}`))
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
      // 添加类型断言以兼容 axios 1.x
      const headers = config.headers as Record<string, any>;
      // FormData 上传须为 multipart/form-data
      if (config.data instanceof FormData && headers) {
        delete headers['Content-Type']
        delete headers['content-type']
      }
      if(headers?.tenantid && !config.url?.includes('sso/v1/login')
        && !config.url?.includes('/sso/v1/getCaptcha')
        && !config.url?.includes('sso/v2/getLoginCode')
        &&!config.url?.includes('sso/v1/verifyToken') ) {
        // 如果 sessionStoage 的 租户id和请求的不不一致，说切换了租户
        if(window.localStorage.getItem('recentlyLoginTenantId') !== headers?.tenantid) {
          ElMessageBox.alert('当前登录账号身份已变更，请刷新后重试', '提示', {
            confirmButtonText: '确定',
            callback: () => {
              location.reload()
            },
          })
          return Promise.reject({
            isInterceptorDetour: true, // 自定义标识，方便在响应拦截器里区分
            message: '当前登录账号身份已变更，请刷新后重试'
          });
        }
      }
      const token = applyAuthHeaders(config)
      if (token && headers) {
        headers.AccessToken = token
      }
      return config
    },
    // 发送失败
    (error) =>{
      store.addErrorMsg({
        msg: error.message,interfaceName: error.config.url,
        startTime: error.config.metadata.startTime, endTime: new Date().getTime()
      })
      return Promise.reject(error)
    }
  )
  // 响应拦截（可根据具体业务作出相应的调整）
  service.interceptors.response.use(
    (response) => {
      console.log('response-----------------', response)
      // console.log(apiData)
      // 如果返回的状态码为200，说明接口请求成功，可以正常拿到数据
      // 否则的话抛出错误 结合自身业务和后台返回的接口状态约定写respone拦截器
      if (response.status === 200) {
        if (response.data.code !== 200) {
          // 判断 response.data.code  === 4004  就去调用刷新token
          if (response.data.code === 4004) {
            return handleTokenRefresh(response);
          }
          // 针对刷新接口  和 其他 msg接口的处理
          if(response.data?.code === 4444 || response.data?.msg === '无效的accesstoken') {
            // 刷新失败，跳转到登录页
            removeToken();
            useGoWhere().goWhere();
            return Promise.reject({
              isInterceptorDetour: true, // 自定义标识，方便在响应拦截器里区分
              message: '当前用户登录信息已过期'
            });
          } else {
            if (response.data.msg){
              const url = response.config?.url || ''
              // 判断当前是否在登陆页面
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
         // 检查是否需要刷新token
        if (errorBody.code === 4004) {
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
      // 以下两个状态码的返回错误
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

/** 创建请求方法 */
function createRequestFunction(service: AxiosInstance) {
  return function <T>(config: AxiosRequestConfig): Promise<T> {
      const configDefault = {
      headers: {
        // 携带 Token
        'Content-Type': get(config, 'headers.Content-Type', 'application/json'),
        ...Config.headers,
        'AccessToken': getStoredToken() || getToken()
      },
      timeout: config.timeout || 10 * 1000,
      data: {}
    }
    const mergedConfig = Object.assign(configDefault, config)
    mergedConfig.headers = {
      ...configDefault.headers,
      ...(config.headers as Record<string, any> || {})
    }
    const token = applyAuthHeaders(mergedConfig)
    if (token) {
      mergedConfig.headers.AccessToken = token
    }
    return service(mergedConfig).then()
  }
}

/** 用于网络请求的实例 */
export const service = createService()
/** 用于网络请求的方法 */
export const request = createRequestFunction(service)
