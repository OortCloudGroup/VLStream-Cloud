/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import Hls from 'hls.js'
import App from './App.vue'
import router from './router'
import './style.css'
import './assets/styles/pagination.css'

// Import StorageEvent token ( )
import { storageEventSync } from './utils/storageEventSync.js'

// Vue instance
const app = createApp(App)

// Initialize StorageEvent
storageEventSync.init()

// token new event
window.addEventListener('storageEventTokenUpdated', (event) => {
  console.log('Token updated via StorageEvent, reloading page...', event.detail);
  // in , only new userinfo new page
  window.location.reload();
});

// token method
window.debugTokens = () => {
  console.log('=== Token调试信息 ===');
  console.log('sessionStorage.accessToken:', sessionStorage.getItem('accessToken'));
  console.log('sessionStorage.token:', sessionStorage.getItem('token'));
  console.log('localStorage.accessToken:', localStorage.getItem('accessToken'));
  console.log('localStorage.token:', localStorage.getItem('token'));
  console.log('当前有效token:', storageEventSync.getCurrentToken ? storageEventSync.getCurrentToken() : '方法不存在');
  console.log('用户信息 (session):', sessionStorage.getItem('userInfo'));
  console.log('用户信息 (local):', localStorage.getItem('userInfo'));
};

// token method
window.clearAllTokens = () => {
  console.log('🧹 清理所有token');
  sessionStorage.removeItem('accessToken');
  sessionStorage.removeItem('token');
  localStorage.removeItem('accessToken');
  localStorage.removeItem('token');
  console.log('✅ 所有token已清理');
};

// token method
window.forceTokenSync = () => {
  console.log('🔄 强制token同步');
  if (storageEventSync && storageEventSync.manualSync) {
    storageEventSync.manualSync();
  } else {
    console.log('⚠️ storageEventSync.manualSync 方法不可用');
  }
};

// token method
window.resetTokenSync = () => {
  console.log('🔄 完整重置token同步');

  // 1. all token
  window.clearAllTokens();

  // 2. etc. after new
  setTimeout(() => {
    if (storageEventSync && storageEventSync.requestTokenFromOtherWindows) {
      storageEventSync.requestTokenFromOtherWindows();
      console.log('📡 已发送新的token请求');
    }
  }, 1000);

  // 3. current
  setTimeout(() => {
    window.debugTokens();
  }, 2000);
};

// method
window.manualTokenSync = () => {
  storageEventSync.manualSync();
};

// StorageEvent instance full , user
window.storageEventSync = storageEventSync;

// HLS.js full windowobject, VideoPlayercomponent
window.Hls = Hls

// all
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// configuration
app.use(createPinia())
app.use(router)
app.use(ElementPlus, {
  locale: zhCn,
})

// DOM
app.mount('#app')

// full
window.checkPlatformNow = () => {
  console.log('🔍 手动检查平台token...')
  const result = window.storageEventSync.checkPlatformToken()
  console.log('检查结果:', result)
  return result
}

window.validateCurrentToken = async () => {
  const token = window.storageEventSync.getCurrentToken()
  if (token) {
    console.log('🔍 验证当前token:', token.substring(0, 8) + '...')
    const result = await window.storageEventSync.validateCurrentToken(token)
    console.log('验证结果:', result)
    return result
  } else {
    console.log('⚠️ 没有找到当前token')
    return false
  }
}

// full
window.fetchPlatformToken = () => {
  console.log('🔗 手动获取统一用户平台token...')
  window.storageEventSync.fetchTokenFromUnifiedPlatform()
}

window.forceReauth = () => {
  console.log('🔄 强制重新认证...')
  sessionStorage.clear()
  localStorage.removeItem('accessToken')
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  window.storageEventSync.redirectToLogin()
}

window.checkTokenStatus = () => {
  const token = window.storageEventSync.getCurrentToken()
  console.log('当前token状态:', {
    hasToken: !!token,
    token: token ? token.substring(0, 8) + '...' : 'null',
    sessionStorage: {
      accessToken: sessionStorage.getItem('accessToken'),
      token: sessionStorage.getItem('token')
    },
    localStorage: {
      accessToken: localStorage.getItem('accessToken'),
      token: localStorage.getItem('token')
    }
  })
}