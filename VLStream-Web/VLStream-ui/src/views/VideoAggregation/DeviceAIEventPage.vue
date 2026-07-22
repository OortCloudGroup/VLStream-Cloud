<template>
  <div class="device-sub-page tenant_Page draHeaPB">
    <div class="page-header">
      <div class="breadcrumb">
        <span class="breadcrumb-item" @click="goBack">设备列表</span>
        <span class="breadcrumb-separator">></span>
        <span class="breadcrumb-item active">{{ showModelMarket ? '模型超市' : 'AI事件配置' }}</span>
      </div>
    </div>
    <div v-loading="loading" class="page-body">
      <DeviceModelMarket
        v-if="showModelMarket && deviceInfo"
        :device-info="deviceInfo"
        @save="handleModelMarketSave"
        @cancel="showModelMarket = false"
      />
      <DeviceAIEvent
        v-else-if="deviceInfo"
        :device-info="deviceInfo"
        @save="handleSave"
        @cancel="goBack"
        @open-model-market="showModelMarket = true"
      />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import DeviceAIEvent from './DeviceAIEvent.vue'
import DeviceModelMarket from './DeviceModelMarket.vue'
import { getDeviceById } from '@/api/device'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const deviceInfo = ref(null)
const showModelMarket = ref(false)

const goBack = () => {
  router.push('/device-management')
}

const loadDevice = async () => {
  const id = route.query.id
  if (!id) {
    ElMessage.error('缺少设备ID')
    goBack()
    return
  }
  loading.value = true
  try {
    const response = await getDeviceById(id)
    if (response.code === 200) {
      deviceInfo.value = { ...response.data, id: response.data.id || id }
    } else {
      deviceInfo.value = { id }
    }
  } catch (error) {
    console.error('获取设备详情失败:', error)
    deviceInfo.value = { id }
  } finally {
    loading.value = false
  }
}

const handleSave = async (aiEventData) => {
  console.log('保存AI事件配置:', aiEventData)
  ElMessage.success('AI事件配置保存成功')
}

const handleModelMarketSave = async (modelData) => {
  console.log('安装模型:', modelData)
  ElMessage.success('模型安装成功')
  showModelMarket.value = false
}

onMounted(loadDevice)
</script>

<style scoped lang="scss">
.device-sub-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: var(--common-border-radius) var(--common-border-radius) 0 0;
  overflow: hidden;
}

.page-header {
  padding: 16px 20px 0;
  flex-shrink: 0;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.breadcrumb-item {
  color: #606266;
  cursor: pointer;

  &.active {
    color: #303133;
    cursor: default;
  }

  &:not(.active):hover {
    color: var(--el-color-primary);
  }
}

.breadcrumb-separator {
  color: #c0c4cc;
}

.page-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  width: 100%;
}
</style>
