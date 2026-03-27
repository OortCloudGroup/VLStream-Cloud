/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 10:42:40
* @Last Modified by:   兰舰
* @Last Modified time: 2024-11-15 10:42:40
* @Copyright aPaaS-front-team. All rights reserved.
*/
export interface ILoginRequestData {
  userInfo: string
}

export interface IGetUserInfoData {
  accessToken: string
  uuid: string
}

export interface IVerifyTokenData {
  accessToken: string
}

export interface ILoginFormData {
  /** admin 或 editor */
  username: string
  /** 密码 */
  password: string
  /** 验证码 */
  code: string,
  tenant_id: string,
  login_id: string
}

export type UserInfo = {
  accessToken: string
  oort_code: string
  oort_isadmin: 0 | 1
  oort_name: string
  oort_phone: string
  oort_uuid:string
}

type LoginRes = {
  userInfo: UserInfo,
  accessToken: string
}

export type LoginResponseData = IApiResponseData<LoginRes>

