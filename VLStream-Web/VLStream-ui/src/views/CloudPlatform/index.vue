<template>
  <div class="cloud-platform">
    <div class="page-header">
      <h2 class="page-title">云平台</h2>
      <p class="page-subtitle">查看和管理你的云平台信息</p>
    </div>

    <div class="content-card">
      <el-tabs v-model="activeTab" class="cloud-tabs" lazy>
        <el-tab-pane label="平台信息" name="platform">
          <div class="placeholder-panel">平台信息功能开发中</div>
        </el-tab-pane>
        <el-tab-pane label="用户信息" name="user">
          <!-- 仅当前 tab 挂载，避免切到模型页仍走用户信息逻辑 -->
          <CloudUserInfo v-if="hasToken && activeTab === 'user'" />
          <div v-else-if="activeTab === 'user'" class="placeholder-panel">正在准备登录...</div>
        </el-tab-pane>
        <el-tab-pane label="我上传的模型" name="models">
          <MyUploadedModels v-if="hasToken && activeTab === 'models'" />
          <div v-else-if="activeTab === 'models'" class="placeholder-panel">请先登录后查看已上传模型</div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import CloudUserInfo from './CloudUserInfo.vue'
import MyUploadedModels from './MyUploadedModels.vue'
import {
  captureModelHubTokenFromUrl,
  clearPendingModelHubPublish,
  getModelHubAccessToken,
  startModelHubLogin
} from '@/utils/modelHubAuth'
import { ElMessage } from 'element-plus'

const route = useRoute()

// setup 阶段同步解析回调 token（早于子组件 onMounted）
const tokenFromUrl = captureModelHubTokenFromUrl()
if (tokenFromUrl) {
  clearPendingModelHubPublish()
}

const hasToken = ref(!!getModelHubAccessToken())

function resolveActiveTab(rawTab) {
  const tab = String(rawTab || '').split('?')[0]
  if (tab === 'user' || tab === 'models' || tab === 'platform') {
    return tab
  }
  return 'user'
}

const activeTab = ref(resolveActiveTab(route.query.tab))

/** 只改 URL query，不走 vue-router 守卫（避免误调 /blade-system/user/info） */
function syncTabToUrl(tab) {
  const url = new URL(window.location.href)
  url.searchParams.set('tab', tab)
  window.history.replaceState({}, '', url.toString())
}

onMounted(() => {
  if (tokenFromUrl) {
    ElMessage.success('登录成功')
    activeTab.value = 'user'
    hasToken.value = true
    syncTabToUrl('user')
    return
  }
  if (!hasToken.value && (activeTab.value === 'user' || activeTab.value === 'models')) {
    ElMessage.info('请先登录后继续')
    startModelHubLogin({ from: 'cloud-platform' })
  }
})

watch(activeTab, (tab) => {
  syncTabToUrl(tab)
  if (!hasToken.value && (tab === 'user' || tab === 'models')) {
    ElMessage.info('请先登录后继续')
    startModelHubLogin({ from: 'cloud-platform' })
  }
})
</script>

<style scoped>
.cloud-platform {
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  padding: 24px 32px;
  background: #f5f7fa;
  min-height: calc(100vh - 64px);
}

.page-header {
  flex-shrink: 0;
  padding-bottom: 0 !important;
  border: none !important;
  margin-bottom: 16px;
}

.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  line-height: 36px;
  color: #3D3D3D;
}

.page-subtitle {
  margin: 4px 0 0;
  font-size: 14px;
  line-height: 22px;
  color: #999999;
}

.content-card {
  flex: 1 0 auto;
  background: #fff;
  border-radius: 6px;
  padding: 20px 20px 32px;
  overflow: visible;
}

.cloud-tabs {
  overflow: visible;
}

.cloud-tabs :deep(.el-tabs__content),
.cloud-tabs :deep(.el-tab-pane) {
  overflow: visible;
}

.cloud-tabs :deep(.el-tabs__item) {
  font-size: 15px;
  padding: 0 20px;
  height: 48px;
  line-height: 48px;
}

.cloud-tabs :deep(.el-tabs__item.is-active) {
  color: #1a53ff;
  font-weight: 600;
}

.cloud-tabs :deep(.el-tabs__active-bar) {
  background-color: #1a53ff;
}

.placeholder-panel {
  padding: 48px 0;
  text-align: center;
  color: #909399;
  font-size: 14px;
}
</style>
