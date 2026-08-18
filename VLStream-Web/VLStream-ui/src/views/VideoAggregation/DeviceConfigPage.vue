<template>
  <div class="device-sub-page tenant_Page draHeaPB">
    <div class="page-header">
      <div class="breadcrumb">
        <span class="breadcrumb-item" @click="goBack">设备列表</span>
        <span class="breadcrumb-separator">></span>
        <span class="breadcrumb-item active">配置参数</span>
      </div>
    </div>
    <div v-loading="loading" class="page-body">
      <DeviceTimeStrategy
        v-if="deviceInfo"
        :device-info="deviceInfo"
        @save="handleSave"
        @cancel="goBack"
      />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import DeviceTimeStrategy from './DeviceTimeStrategy.vue'
import { getDeviceById } from '@/api/device'
import { saveTimeStrategy as saveTimeStrategyAPI } from '@/api/timeStrategy'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const deviceInfo = ref(null)

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

const handleSave = async (strategyData) => {
  try {
    await saveTimeStrategyAPI(strategyData)
    ElMessage.success('时间策略保存成功')
  } catch (error) {
    console.error('保存时间策略失败:', error)
    ElMessage.error('保存失败')
  }
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
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
  background: #fff;
}

.breadcrumb {
  display: flex;
  align-items: center;
  font-size: 14px;
  color: #606266;
}

.breadcrumb-item {
  color: var(--el-color-primary);
  cursor: pointer;
  transition: color 0.3s;

  &.active {
    color: #303133;
    cursor: default;
  }

  &:not(.active):hover {
    color: #3d70ff;
  }
}

.breadcrumb-separator {
  margin: 0 8px;
  color: #c0c4cc;
}

.page-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  width: 100%;
}
</style>
