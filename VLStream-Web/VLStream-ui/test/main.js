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

// 导入StorageEvent token同步机制（备用方案）
import { storageEventSync } from './utils/storageEventSync.js'

// 创建Vue应用实例
const app = createApp(App)

// 初始化StorageEvent同步机制
storageEventSync.init()

// 监听token更新事件
window.addEventListener('storageEventTokenUpdated', (event) => {
  console.log('Token updated via StorageEvent, reloading page...', event.detail);
  // 可以在这里添加更精细的逻辑，例如只更新用户信息而不刷新页面
  window.location.reload();
});

// 暴露token调试方法
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

// 暴露token清理方法
window.clearAllTokens = () => {
  console.log('🧹 清理所有token');
  sessionStorage.removeItem('accessToken');
  sessionStorage.removeItem('token');
  localStorage.removeItem('accessToken');
  localStorage.removeItem('token');
  console.log('✅ 所有token已清理');
};

// 暴露强制token同步方法
window.forceTokenSync = () => {
  console.log('🔄 强制token同步');
  if (storageEventSync && storageEventSync.manualSync) {
    storageEventSync.manualSync();
  } else {
    console.log('⚠️ storageEventSync.manualSync 方法不可用');
  }
};

// 暴露完整的token重置方法
window.resetTokenSync = () => {
  console.log('🔄 完整重置token同步');
  
  // 1. 清理所有token
  window.clearAllTokens();
  
  // 2. 等待一秒后重新请求
  setTimeout(() => {
    if (storageEventSync && storageEventSync.requestTokenFromOtherWindows) {
      storageEventSync.requestTokenFromOtherWindows();
      console.log('📡 已发送新的token请求');
    }
  }, 1000);
  
  // 3. 显示当前状态
  setTimeout(() => {
    window.debugTokens();
  }, 2000);
};

// 暴露手动同步方法用于调试
window.manualTokenSync = () => {
  storageEventSync.manualSync();
};

// 暴露StorageEvent同步实例到全局，供统一用户平台调用
window.storageEventSync = storageEventSync;

// 将HLS.js添加到全局window对象，供VideoPlayer组件使用
window.Hls = Hls

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 配置应用插件
app.use(createPinia())
app.use(router)
app.use(ElementPlus, {
  locale: zhCn,
})

// 挂载应用到DOM
app.mount('#app')

// 添加更多全局调试函数
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

// 添加更多全局调试函数
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