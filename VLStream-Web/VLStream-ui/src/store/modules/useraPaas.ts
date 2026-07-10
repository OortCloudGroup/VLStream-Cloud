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
import { getToken, removeToken, setToken, setRefreshToken } from '@/utils/cache/cookies'
import {
  loginSSO,
  verifyToken,
  logout,
  resetPassword,
  // getLoginCode,
  getLoginCodeV2,
  fastLogin
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
    data.oort_uuid = data.user_id || data.userId
    data.oort_isadmin = data.isAdmin || data.is_admin
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
  // const getLogin = (loginData) => {
  //   return new Promise((resolve, reject) => {
  //     // 使用 公钥加密
  //     const publicKey =
  //       'MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAzd2eYqgC6A3CvQN982C7s3xo2zjL97b0lHkfcAFEy50YHg+g5QD6RbvZt0NTQVOoC5Vv867lp0UBrwAslNCjt92QyxKkGLU+10UClkCJsiHpxy/J7AOOpS0wMGao80uWN94EZEsP/FKFfGNz3lJcZRtg4TGCMgXQGlKxBcYJDor/zK/06asTBGp4IxvTDAKTuMP+u88y5nQTUpKPnPstwmiLfqZLOSE3y4kIA7VC98GxPY8SqLZ7b9HzLSNoZNXKvA9US7A2F8/A97b8tExXsGPOodMQzrLFVr840ZV2WgpxJHqobqYReGhGMN4JdjfGlUvHyhYaCyOcSWalWuLh18cDQkum8yUrw5Tki8r8VPDTEZhsOXcq46cMr4437HGYeDA2ib7TOArFq1d0DD9Z0DAsjNfhgqqIP9A9kXrs6JIRrkz82skco2WQ5NUdpLT3yaAiXTxmFaajQGVIhFG24VL8CTloRo3FZmy9vMlUseCKmhfCBFbhUG9r7HOuhkO+jY4yfItE8BIrClbkQBAzMBMTuRM84VQQ4MnYlbdT3uSt5Qw5WmGIxsAKk93o8Hyhrg/OX8FCwntw2h5AjGDGn/H5H0TDAp8vX0NJgh4xhOpNT8pshuX7W1vcqr42sqOjM/mbRPV3s+tT4ynY0xQLuqp0P7GW3fu0fT0/OeHPwcsCAwEAAQ=='
  //     const encrypt = new JSEncrypt()
  //     encrypt.setPublicKey(publicKey)
  //     const json = {
  //       loginId: loginData.loginId,
  //       password: loginData.password,
  //       timestamp: Math.floor(new Date().getTime() / 1000),
  //       client: 'pcweb'
  //     }
  //     if (loginData.code) {
  //       json.code = loginData.code
  //       json.captchaID = loginData.captchaID
  //     }
  //     console.log('json1', json)
  //     const tjson = encrypt.encrypt(JSON.stringify(json)) as string
  //     getLoginCode({ userInfo: tjson })
  //       .then((res) => {
  //         resolve(res)
  //       })
  //       .catch((error) => {
  //         reject(error)
  //       })
  //   })
  // }

  /* 临时登录2 - 支持多种登录方式 */
  const getLoginV2 = (loginData) => {
    return new Promise((resolve, reject) => {
      // 使用公钥加密
      const publicKey =
          'MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAzd2eYqgC6A3CvQN982C7s3xo2zjL97b0lHkfcAFEy50YHg+g5QD6RbvZt0NTQVOoC5Vv867lp0UBrwAslNCjt92QyxKkGLU+10UClkCJsiHpxy/J7AOOpS0wMGao80uWN94EZEsP/FKFfGNz3lJcZRtg4TGCMgXQGlKxBcYJDor/zK/06asTBGp4IxvTDAKTuMP+u88y5nQTUpKPnPstwmiLfqZLOSE3y4kIA7VC98GxPY8SqLZ7b9HzLSNoZNXKvA9US7A2F8/A97b8tExXsGPOodMQzrLFVr840ZV2WgpxJHqobqYReGhGMN4JdjfGlUvHyhYaCyOcSWalWuLh18cDQkum8yUrw5Tki8r8VPDTEZhsOXcq46cMr4437HGYeDA2ib7TOArFq1d0DD9Z0DAsjNfhgqqIP9A9kXrs6JIRrkz82skco2WQ5NUdpLT3yaAiXTxmFaajQGVIhFG24VL8CTloRo3FZmy9vMlUseCKmhfCBFbhUG9r7HOuhkO+jY4yfItE8BIrClbkQBAzMBMTuRM84VQQ4MnYlbdT3uSt5Qw5WmGIxsAKk93o8Hyhrg/OX8FCwntw2h5AjGDGn/H5H0TDAp8vX0NJgh4xhOpNT8pshuX7W1vcqr42sqOjM/mbRPV3s+tT4ynY0xQLuqp0P7GW3fu0fT0/OeHPwcsCAwEAAQ=='
      const encrypt = new JSEncrypt()
      encrypt.setPublicKey(publicKey)
      const json = {
        timestamp: Math.floor(new Date().getTime() / 1000),
        client: 'pcweb',
        login_type: loginData.login_type || 1,
        config_code: loginData.config_code || '',
        identity_type: loginData.identity_type,
        identifier: loginData.identifier,
        credential: loginData.credential
      }

      if (loginData.code) {
        json.code = loginData.code
        json.captchaID = loginData.captchaID
      }

      if (loginData.loginfrom) {
        json.loginfrom = loginData.loginfrom
      }

      console.log('json2', json)
      const tjson = encrypt.encrypt(JSON.stringify(json)) as string
      let param = {}
      if (loginData.current_step === 2) {
        param = {
          auth_token: loginData.auth_token,
          current_step: loginData.current_step,
          login_step: loginData.login_step,
          userInfo: tjson
        }
      } else {
        param = {
          select_type: loginData.select_type,
          userInfo: tjson
        }
      }
      getLoginCodeV2(param)
        .then((res) => {
          resolve(res)
        })
        .catch((error) => {
          reject(error)
        })
    })
  }

  /* 临时登录3 - 双因子登录 */
  const getFastLogin = (loginData) => {
    return new Promise((resolve, reject) => {
      // 使用公钥加密
      const publicKey =
          'MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAzd2eYqgC6A3CvQN982C7s3xo2zjL97b0lHkfcAFEy50YHg+g5QD6RbvZt0NTQVOoC5Vv867lp0UBrwAslNCjt92QyxKkGLU+10UClkCJsiHpxy/J7AOOpS0wMGao80uWN94EZEsP/FKFfGNz3lJcZRtg4TGCMgXQGlKxBcYJDor/zK/06asTBGp4IxvTDAKTuMP+u88y5nQTUpKPnPstwmiLfqZLOSE3y4kIA7VC98GxPY8SqLZ7b9HzLSNoZNXKvA9US7A2F8/A97b8tExXsGPOodMQzrLFVr840ZV2WgpxJHqobqYReGhGMN4JdjfGlUvHyhYaCyOcSWalWuLh18cDQkum8yUrw5Tki8r8VPDTEZhsOXcq46cMr4437HGYeDA2ib7TOArFq1d0DD9Z0DAsjNfhgqqIP9A9kXrs6JIRrkz82skco2WQ5NUdpLT3yaAiXTxmFaajQGVIhFG24VL8CTloRo3FZmy9vMlUseCKmhfCBFbhUG9r7HOuhkO+jY4yfItE8BIrClbkQBAzMBMTuRM84VQQ4MnYlbdT3uSt5Qw5WmGIxsAKk93o8Hyhrg/OX8FCwntw2h5AjGDGn/H5H0TDAp8vX0NJgh4xhOpNT8pshuX7W1vcqr42sqOjM/mbRPV3s+tT4ynY0xQLuqp0P7GW3fu0fT0/OeHPwcsCAwEAAQ=='
      const encrypt = new JSEncrypt()
      encrypt.setPublicKey(publicKey)
      const json = {
        timestamp: Math.floor(new Date().getTime() / 1000),
        client: 'pcweb',
        login_type: 1,
        config_code: loginData.config_code || '',
        identity_type: loginData.identity_type,
        identifier: loginData.identifier,
        credential: loginData.credential
      }

      if (loginData.code) {
        json.code = loginData.code
        json.captchaID = loginData.captchaID
      }

      if (loginData.to_tenant_id) {
        json.to_tenant_id = loginData.to_tenant_id
      }

      if (loginData.to_user_id) {
        json.to_user_id = loginData.to_user_id
      }

      if (loginData.is_password_different_account) {
        json.is_password_different_account = loginData.is_password_different_account
      }

      console.log('json2', json)
      const tjson = encrypt.encrypt(JSON.stringify(json)) as string
      let param = {}
      if (loginData.current_step === 2) {
        param = {
          auth_token: loginData.auth_token,
          current_step: loginData.current_step,
          login_step: loginData.login_step,
          userInfo: tjson
        }
      } else {
        param = {
          select_type: loginData.select_type,
          userInfo: tjson
        }
      }
      fastLogin(param)
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
          if (res.code === 200) {
            setToken(res.data.accessToken)
            setTenantId(res.data.tenantId)
            token.value = res.data.accessToken
            userInfo.value = res.data
            // 保存刷新token
            if (res.data.refreshToken) setRefreshToken(res.data.refreshToken)
          }
          resolve(res)
        })
        .catch((error) => {
          reject(error)
        })
    })
  }

  // 切换账号
  const switchAccounts = (loginData) => {
    setToken(loginData.accessToken)
    setTenantId(loginData.tenantId)
    token.value = loginData.accessToken
    userInfo.value = loginData
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
    // 保存最近登录的租户
    window.localStorage.setItem('recentlyLoginTenantId', data)
    setHeaders()
  }

  const setHeaders = () => {
    config.headers.tenantid = tenantId.value
  }

  const versionToken = (params: IVerifyTokenData = { with_im: 0, accessToken: '' }) => {
    if (!token.value) {
      token.value = getToken()
    }
    params.accessToken = params.accessToken || token.value
    // 后台可以判断 ，这里不需要传了
    // params.with_im = params.with_im || 0
    return new Promise((resolve, reject) => {
      verifyToken(params)
        .then((res) => {
          setToken(res.data.accessToken)
          token.value = res.data.accessToken
          userInfo.value = { ...userInfo.value, ...res.data }
          setTenantId(res.data.tenantId)
          if (params.with_im && res.data.im) {
            imDataHandle(res.data)
          }
          resolve(res)
        })
        .catch((error) => {
          reject(error)
        })
    })
  }

  const setStoreToken = (data: string) => {
    setToken(data)
    // 更新userInfo 里面的accessToken
    userInfo.value = { ...userInfo.value, accessToken: data }
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
    window.sessionStorage.setItem('loginData', '')
    window.localStorage.setItem('http_key', '')
    window.localStorage.setItem('message_key', '')
    window.localStorage.setItem('user_id', '')
  }

  const loginOut = (params: IVerifyTokenData) => {
    return new Promise((resolve, reject) => {
      logout(params)
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

  // 保存验证token的返回数据
  const verifyTokenUserInfo = ref(null)
  const setVerifyTokenUserInfo = (data) => {
    data.oort_uuid = data.user_id || data.userId
    data.oort_isadmin = data.isAdmin || data.is_admin
    verifyTokenUserInfo.value = data
    setTenantId(data.tenantId)
  }

  // 处理im的数据写入sesstion
  const imDataHandle = (data) => {
    const imData = data.im
    const sessitonData = {
      access_Token: data.accessToken,
      access_token: data.accessToken,
      ...imData
    }
    window.sessionStorage.setItem('loginData', JSON.stringify(sessitonData))
    window.localStorage.setItem('http_key', imData?.httpKey || '')
    window.localStorage.setItem('message_key', imData?.http_key || '')
    window.localStorage.setItem('user_id', imData?.userId || '')
  }

  return {
    verifyTokenUserInfo,
    setVerifyTokenUserInfo,
    setUserInfo,
    // getLogin,
    getLoginV2,
    getFastLogin,
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
    getLocalRouter,
    switchAccounts
  }
})

/** 在 setup 外使用 */
export function useUserStoreHook() {
  return useUserStore(store)
}
