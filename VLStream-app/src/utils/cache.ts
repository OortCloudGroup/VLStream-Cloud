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
  return sessionStorage.getItem('token') || tmeoBToken || ''
}
export const setToken = (token: string) => {
  sessionStorage.setItem('token', token)
}
export const removeToken = () => {
  sessionStorage.setItem('token', '')
  sessionStorage.setItem('tempObj', '')
  // 把浏览器地址栏的 参数清空
  window.location.search = ''
}
