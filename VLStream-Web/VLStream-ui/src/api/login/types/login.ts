/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

export interface ILoginRequestData {
  userInfo: string
}

export interface IGetUserInfoData {
  accessToken: string
  uuid: string
}

export interface IVerifyTokenData {
  accessToken: string,
  with_im?: number, // whether IM 0 1 is
}

export interface ILoginFormData {
  /* * admin editor */
  username: string
  /* * */
  password: string
  /* * */
  code: string,
  tenant_id: string,
  login_id: string
}

export type UserInfo = {
  accessToken: string
  oort_code?: string
  oort_isadmin?: 0 | 1
  oort_name?: string
  oort_phone?: string
  oort_uuid?:string
}

type LoginRes = {
  userInfo: UserInfo,
  accessToken: string
}

export type LoginResponseData = IApiResponseData<LoginRes>

