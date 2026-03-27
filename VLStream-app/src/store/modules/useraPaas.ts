/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 11:03:58
 * @Last Modified by: 兰舰
 * @Last Modified time: 2024-11-20 15:01:399
* @Copyright aPaaS-front-team. All rights reserved.
*/
import { ref } from 'vue'
import store from '@/store/index'
import { defineStore } from 'pinia'
import { getToken, removeToken, setToken } from '@/utils/cache'
import {
  loginSSO,
  verifyToken,
  logout,
  resetPassword,
  getLoginCode
} from '@/api/login/indexapaas'
import {
  IVerifyTokenData,
  UserInfo
} from '@/api/login/types/login'
import JSEncrypt from 'jsencrypt'
import config from '@/config/index'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(getToken() || '')
  const userInfo = ref<UserInfo>()
  const setUserInfo = (data) => {
    userInfo.value = { ...userInfo.value, ...data }
  }
  // 选人员
  const userListStore = ref<any>({})
  const addUser = (info) => {
    // 检测是否以及存在
    userListStore.value[info.user_id] = info
  }
  const tenantId = ref<string>(window.sessionStorage.getItem('tenantId') || '')

  /* 临时登录*/
  const getLogin = (loginData) => {
    return new Promise((resolve, reject) => {
      // 使用 公钥加密
      const publicKey =
        'MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAzd2eYqgC6A3CvQN982C7s3xo2zjL97b0lHkfcAFEy50YHg+g5QD6RbvZt0NTQVOoC5Vv867lp0UBrwAslNCjt92QyxKkGLU+10UClkCJsiHpxy/J7AOOpS0wMGao80uWN94EZEsP/FKFfGNz3lJcZRtg4TGCMgXQGlKxBcYJDor/zK/06asTBGp4IxvTDAKTuMP+u88y5nQTUpKPnPstwmiLfqZLOSE3y4kIA7VC98GxPY8SqLZ7b9HzLSNoZNXKvA9US7A2F8/A97b8tExXsGPOodMQzrLFVr840ZV2WgpxJHqobqYReGhGMN4JdjfGlUvHyhYaCyOcSWalWuLh18cDQkum8yUrw5Tki8r8VPDTEZhsOXcq46cMr4437HGYeDA2ib7TOArFq1d0DD9Z0DAsjNfhgqqIP9A9kXrs6JIRrkz82skco2WQ5NUdpLT3yaAiXTxmFaajQGVIhFG24VL8CTloRo3FZmy9vMlUseCKmhfCBFbhUG9r7HOuhkO+jY4yfItE8BIrClbkQBAzMBMTuRM84VQQ4MnYlbdT3uSt5Qw5WmGIxsAKk93o8Hyhrg/OX8FCwntw2h5AjGDGn/H5H0TDAp8vX0NJgh4xhOpNT8pshuX7W1vcqr42sqOjM/mbRPV3s+tT4ynY0xQLuqp0P7GW3fu0fT0/OeHPwcsCAwEAAQ=='
      const encrypt = new JSEncrypt()
      encrypt.setPublicKey(publicKey)
      const json = {
        loginId: loginData.loginId,
        password: loginData.password,
        timestamp: Math.floor(new Date().getTime() / 1000),
        client: 'pcweb'
      }
      if (loginData.code) {
        json.code = loginData.code
        json.captchaID = loginData.captchaID
      }
      console.log('json', json)
      const tjson = encrypt.encrypt(JSON.stringify(json)) as string
      getLoginCode({ userInfo: tjson })
        .then((res) => {
          resolve(res)
        })
        .catch((error) => {
          reject(error)
        })
    })
  }

  // apaas-sso登录
  const login = (loginData) => {
    return new Promise((resolve, reject) => {
      loginSSO(loginData)
        .then((res) => {
          setToken(res.data.accessToken)
          setTenantId(res.data.tenantId)
          token.value = res.data.accessToken
          userInfo.value = res.data
          resolve(res)
        })
        .catch((error) => {
          reject(error)
        })
    })
  }

  const resetPS = (data) => {
    return new Promise((resolve, reject) => {
      // 使用 公钥加密
      // const publicKey =
      //   'MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAwQ9LMVuWA26f+pc4cyiZbZRY+XzJ6B6sC9ZHRU5x3C8g5Cu1MlpZ3v8baD8r+aZOE9t5NnmSLFqcVIlO3DY+bX03188m59zZqWmLhzdKPWJ2ibH4AjCHH0OlJFUIew3qzXOdZw4nk+nBmrRV7XeU7a/K4SYI7bKQg2hn4N9giKdSztvZcjO21ZS2/JiQQfSh7vZDWMsU9RH7MGAkaSkmcOmM4TVA5ponhinnpcf2cJBs94hJgFjC3JagnzqpD8ZPpPG37Ozjz3sG1iOVtC3SSh7Ejxxm75N0wjSpcVmzIitUqOrEiVyo8XoALsGUW24oLBW+LLUGZ/TxwRgHiFSLe5gTaTM+wZNZFK31lyJiZv1HYSRMzmN5SgSp5kh/8pRW42T8mPcSx6NrvZXN3BZKdjkOJ4/eEAY8PlgwKs3vF0DQt5TPrnJIuOo5RIhtbojofe6tFCukr2Fv3k6lPFTbqWRVyK0SVYRAk+V+VLEyj5bouX1gCDvh2evP4+/4/ZGHty04gGHlWWClcjo7iUP9EeWo1IftyuD4fPtFl8sPm/By+/vz3/meavzWEjxL28kOSpTJWIVC2UeVgjMS/0e0s5DllJI3jtAG6AhQTNYrQTtJbtc7SFY6SYptZ+LLZ8kn2pAA1bZUOUCuCnDICLiglEFpPrSPQlWJyzN3WvU4bU0CAwEAAQ=='
      const publicKey =
        'MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAzd2eYqgC6A3CvQN982C7s3xo2zjL97b0lHkfcAFEy50YHg+g5QD6RbvZt0NTQVOoC5Vv867lp0UBrwAslNCjt92QyxKkGLU+10UClkCJsiHpxy/J7AOOpS0wMGao80uWN94EZEsP/FKFfGNz3lJcZRtg4TGCMgXQGlKxBcYJDor/zK/06asTBGp4IxvTDAKTuMP+u88y5nQTUpKPnPstwmiLfqZLOSE3y4kIA7VC98GxPY8SqLZ7b9HzLSNoZNXKvA9US7A2F8/A97b8tExXsGPOodMQzrLFVr840ZV2WgpxJHqobqYReGhGMN4JdjfGlUvHyhYaCyOcSWalWuLh18cDQkum8yUrw5Tki8r8VPDTEZhsOXcq46cMr4437HGYeDA2ib7TOArFq1d0DD9Z0DAsjNfhgqqIP9A9kXrs6JIRrkz82skco2WQ5NUdpLT3yaAiXTxmFaajQGVIhFG24VL8CTloRo3FZmy9vMlUseCKmhfCBFbhUG9r7HOuhkO+jY4yfItE8BIrClbkQBAzMBMTuRM84VQQ4MnYlbdT3uSt5Qw5WmGIxsAKk93o8Hyhrg/OX8FCwntw2h5AjGDGn/H5H0TDAp8vX0NJgh4xhOpNT8pshuX7W1vcqr42sqOjM/mbRPV3s+tT4ynY0xQLuqp0P7GW3fu0fT0/OeHPwcsCAwEAAQ=='
      const encrypt = new JSEncrypt()
      encrypt.setPublicKey(publicKey)
      const json = {
        oldPassword: data.oldPassword,
        password: data.password,
        timestamp: Math.floor(new Date().getTime() / 1000)
      }
      const tjson = encrypt.encrypt(JSON.stringify(json)) as string
      resetPassword({ accessToken: token.value, userInfo: tjson })
        .then((res) => {
          resolve(res)
        })
        .catch((error) => {
          reject(error)
        })
    })
  }

  const setTenantId = (data: string) => {
    tenantId.value = data
    window.sessionStorage.setItem('tenantId', data)
    setHeaders()
  }

  const setHeaders = () => {
    config.headers.tenantid = tenantId.value
  }

  const versionToken = () => {
    return new Promise((resolve, reject) => {
      if (!token.value) {
        token.value = getToken()
      }
      verifyToken({ accessToken: token.value })
        .then((res) => {
          setToken(res.data.accessToken)
          token.value = res.data.accessToken
          userInfo.value = { ...userInfo.value, ...res.data }
          // 切换租户时会把tenantId保存到本地，这时候会验证token, 不能拿验证token的返回的用户信息的租户id，应该获取本地缓存的. 若为空这获取token的
          if (!tenantId.value) {
            tenantId.value = window.sessionStorage.getItem('tenantId') || res.data.tenantId
          }
          setTenantId(tenantId.value)
          resolve(res)
        })
        .catch((error) => {
          reject(error)
        })
    })
  }

  const setStoreToken = (data: string) => {
    setToken(data)
    token.value = data
  }

  const getStoreToken = () => {
    return token.value
  }

  const removeStoreToken = () => {
    removeToken()
    token.value = ''
    userInfo.value = undefined
    tenantId.value = ''
    sessionStorage.clear()
  }

  const loginOut = (token: string) => {
    return new Promise((resolve, reject) => {
      const postJson: IVerifyTokenData = { accessToken: token }
      logout(postJson)
        .then((response) => {
          removeStoreToken()
          resolve(response)
        })
        .catch((error) => {
          reject(error)
        })
    })
  }

  const menuList = ref([])
  const setMenuList = (info) => {
    menuList.value = info
  }

  // 存储urlObj过来的传参
  const urlObj = ref(null)
  const setURLObj = (data) => {
    urlObj.value = data
  }

  const localRouter = ref([])
  const setLocalRouter = (data) => {
    localRouter.value = data
  }
  const getLocalRouter = () => {
    return localRouter.value
  }

  return {
    setUserInfo,
    getLogin,
    login,
    setStoreToken,
    token,
    userInfo,
    loginOut,
    versionToken,
    removeStoreToken,
    getStoreToken,
    addUser,
    userListStore,
    tenantId,
    setTenantId,
    setMenuList,
    menuList,
    resetPS,
    setURLObj,
    urlObj,
    setLocalRouter,
    getLocalRouter
  }
})

/** 在 setup 外使用 */
export function useUserStoreHook() {
  return useUserStore(store)
}
