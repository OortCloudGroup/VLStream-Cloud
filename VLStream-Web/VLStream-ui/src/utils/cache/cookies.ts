/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 11:04:34
 * @Last Modified by: 兰舰
 * @Last Modified time: 2024-12-11 21:39:466
* @Copyright aPaaS-front-team. All rights reserved.
*/
/** 统一处理 Cookie */


export const getToken = () => {
  // 加绒pcweb2.0的保存在sessionStorage的token
  let tmeoBToken = ''
  if (sessionStorage.getItem('tempObj')) {
    try {
      const tempObj = JSON.parse(sessionStorage.getItem('tempObj') || '')
      tmeoBToken = tempObj.token
    } catch (error) {
      tmeoBToken = ''
    }
  }
  return sessionStorage.getItem('token') || tmeoBToken || localStorage.getItem('apaas_token') || ''
}

export const setToken = (token: string) => {
  sessionStorage.setItem('token', token)
  // 保存到本地
  localStorage.setItem('apaas_token', token)
}

export const removeToken = () => {
  sessionStorage.setItem('token', '')
  sessionStorage.setItem('tempObj', '')
  // 保存到本地
  localStorage.setItem('apaas_token', '')
  localStorage.setItem('refresh_token', '')
  // 把浏览器地址栏的 参数清空
  window.location.search = ''
}

// 获取刷新token
export const getRefreshToken = () => {
  return localStorage.getItem('refresh_token') || ''
}

// 设置刷新token 
export const setRefreshToken = (refreshToken)=>{
   return localStorage.setItem('refresh_token', refreshToken)
}
