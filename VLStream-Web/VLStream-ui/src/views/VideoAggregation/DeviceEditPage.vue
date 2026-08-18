<template>
  <div class="device-sub-page tenant_Page draHeaPB">
    <div class="page-header">
      <div class="breadcrumb">
        <span class="breadcrumb-item" @click="goBack">设备列表</span>
        <span class="breadcrumb-separator">></span>
        <span class="breadcrumb-item active">编辑设备</span>
      </div>
    </div>
    <div v-loading="loading" class="page-body">
      <DeviceEditForm
        v-if="editForm"
        v-model="editForm"
        mode="edit"
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
import DeviceEditForm from './DeviceEditForm.vue'
import { getDeviceById, updateDevice } from '@/api/device'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const editForm = ref(null)

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
      editForm.value = { ...response.data }
    } else {
      ElMessage.error(response.message || '获取设备详情失败')
      goBack()
    }
  } catch (error) {
    console.error('获取设备详情失败:', error)
    ElMessage.error('获取设备详情失败')
    goBack()
  } finally {
    loading.value = false
  }
}

const handleSave = async (formData) => {
  try {
    await updateDevice(route.query.id, formData)
    ElMessage.success('更新成功')
    goBack()
  } catch (error) {
    console.error('保存设备失败:', error)
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
