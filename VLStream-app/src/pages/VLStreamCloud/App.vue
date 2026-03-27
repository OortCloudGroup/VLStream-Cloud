<!-- eslint-disable vue/no-multiple-template-root -->
<!--
 * @Author: lanjian
 * @Date: 2021-11-13 09:13:06
 * @LastEditors: lanjian
 * @LastEditTime: 2021-11-27 12:19:03
 * @FilePath: \cordava_utils\demo\src\App.vue
 * @Description: Copyright 奥尔特云（深圳）智慧科技有限公司. All rights reserved.
-->

<template>
  <div id="page_container">
    <router-view v-if="isInitFinsh" />
    <!--    pc 登陆的开发界面-->
    <oort-popup v-model="showLoginDev" position="right" style="width: 100%;height: 100%;">
      <login-dev @loginSuccess="loginSuccess" />
    </oort-popup>
  </div>
</template>

<script setup>

import { onMounted, ref } from 'vue'

import Config from '@/config'
import appConfig from '@/config/AppConfig'
import { showLoadingToast, showFailToast } from 'vant'
import { getAccessToken, getGateWay, getAppInfo, getAppInfoFromHttp } from '@/utils/cordovatUtils.js'
import { useUserStore } from '@/store/modules/useraPaas'
import OortPopup from '@/components/popup/oort_popup'
import LoginDev from '@/components/commonpage/LoginDev'

const store = useUserStore()

const showLoginDev = ref(false)
const isInitFinsh = ref(false)
const init = () => {
// 设置 应用id等配置
  const configObj = appConfig['VLStreamCloud']
  Config.appId = configObj.appId
  Config.secretKey = configObj.secretKey
  Config.requestType = configObj.requestType
  Config.headers.appId = Config.appId
  Config.headers.secretKey = Config.secretKey
  Config.headers.requestType = Config.requestType

  Config.appName = configObj.appName
  if (!!window.cordova && !!window.cordova.exec) {
    let toastInst = showLoadingToast({
      message: '加载中...',
      forbidClick: true,
      duration: 0
    })
    Promise.all([
      getAccessToken(),
      getGateWay()
    ]).then(async res => {
      window.accessToken = res[0]
      store.setStoreToken(res[0])
      Config.URL = res[1] + '/'
      toastInst.close()
      let resp = await store.versionToken()
      if (resp.code === 200) {
        let respp = await getAppInfo()
        if (respp) {
          isInitFinsh.value = true
        } else {
          let resa = await getAppInfoFromHttp()
          if (resa) {
            isInitFinsh.value = true
          } else {
            showFailToast('获取应用信息失败')
          }
        }
      }
    })
  } else {
    if (window.location.hostname !== 'localhost') {
      Config.URL = window.location.origin + '/'
    }
    showLoginDev.value = true
  }
}

const loginSuccess = async() => {
  let res = await getAppInfoFromHttp()
  if (res.code === 200) {
    showLoginDev.value = false
    isInitFinsh.value = true
  }
}

onMounted(() => {
  if (window.global.is_cordova) {
    // this.isInitFinsh = true
    document.addEventListener('deviceready', init, false)
  } else {
    init()
  }
})

</script>

<style>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #2c3e50;
  width:100vw;
  height: 100vh;
}

#page_container {
  width:100%;
  height: 100%;
  overflow:auto;
}
</style>
