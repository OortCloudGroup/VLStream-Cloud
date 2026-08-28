/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import config from '@/config/index'
import { jugeIsInLocalhostEnv } from '@/utils'
import { useRouter } from 'vue-router'

export default function() {
  const routerHook = useRouter()
  const goWhere = function() {
    if (!!config.notforcedLogin) return
    // whether page whether in
    if ((config.common && !config.common.commonLoginPage) || jugeIsInLocalhostEnv()) {
      if (routerHook) {
        routerHook.push('/login')
      } else {
        const newUrl = `${window.location.protocol}//${window.location.host}${window.location.pathname}#/login`
        window.location.href = newUrl
      }
    } else {
      //
      window.location.replace(config.URL.slice(0, -1))
      // window.location.replace(config.URL.slice(0, -1) + config.frontURLStr + '/console_manage/index.html')
    }
  }
  return {
    goWhere
  }
}

