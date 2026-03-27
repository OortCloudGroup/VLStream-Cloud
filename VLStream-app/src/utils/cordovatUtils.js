import Config from '@/config'
export const getAccessToken = () => {
  return window.androidFunUtils.getToken()
}
export const getGateWay = () => {
  return window.androidFunUtils.getGateway()
}

export const getAppInfo = async() => {
  return new Promise((resolve) => {
    if (Config.appId) {
      if (window.global.is_cordova) {
      // 使用安卓接口获取应用信息
        window.androidFunUtils.getAppInfo().then(res => {
          try {
            res = JSON.parse(res)
            // 保存包名
            Config.appInfo = res
            Config.apppackage = res.apppackage
            Config.appId = res.app_id
            Config.secretKey = res.app_secret
            resolve(true)
          } catch (e) {
            resolve(false)
          }
        }, () => {
          resolve(false)
        // alert('获取应用信息失败_回调')
        })
      } else {
        resolve(false)
      }
    } else {
      resolve(false)
    }
  })
}

import { useUserStoreHook } from '@/store/modules/useraPaas'
import { idbysecretdetail } from '@/api/admin_platform'
export const getAppInfoFromHttp = async(params = {}) => {
  const appid = params.appid || Config.appId
  const appsecret = params.appsecret || Config.secretKey
  return new Promise((resolve, reject) => {
    const store = useUserStoreHook()
    const params = {
      accessToken: store.token,
      app_id: appid,
      app_secret: appsecret,
      uuid: store.userInfo.userId
    }
    idbysecretdetail(params).then(res => {
      if (res.code === 200) {
        Config.appInfo = res.data
        Config.apppackage = res.data.apppackage
        resolve(res)
      } else {
        reject()
      }
    })
  })
}
