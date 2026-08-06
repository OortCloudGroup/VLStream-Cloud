<template>
  <div class="model-detail" v-loading="loading">
    <div class="detail-breadcrumb">
      <span class="crumb-link" @click="$emit('back')">服务目录</span>
      <span class="crumb-sep">></span>
      <span class="crumb-current">模型详情</span>
    </div>

    <template v-if="detail">
      <h1 class="detail-title">{{ detail.name || detail.alias || '未命名模型' }}</h1>
      <div class="detail-alias">模型别名：{{ detail.alias || '-' }}</div>
      <div class="detail-desc">{{ detail.description || '暂无描述' }}</div>

      <el-descriptions :column="2" class="detail-descriptions">
        <el-descriptions-item>
          <template #label>
            <span class="info-label">
              <el-icon><Share /></el-icon>
              模型类型
            </span>
          </template>
          {{ detail.model_type || '-' }}
        </el-descriptions-item>
        <el-descriptions-item>
          <template #label>
            <span class="info-label">
              <el-icon><Paperclip /></el-icon>
              模型文件
            </span>
          </template>
          {{ fileName }}
        </el-descriptions-item>
        <el-descriptions-item>
          <template #label>
            <span class="info-label">
              <el-icon><Share /></el-icon>
              状态
            </span>
          </template>
          {{ statusLabel }}
        </el-descriptions-item>
        <el-descriptions-item>
          <template #label>
            <span class="info-label">
              <el-icon><Clock /></el-icon>
              创建时间
            </span>
          </template>
          {{ detail.created_at || '-' }}
        </el-descriptions-item>
        <el-descriptions-item>
          <template #label>
            <span class="info-label">
              <el-icon><Grid /></el-icon>
              审核时间
            </span>
          </template>
          {{ approveTimeText }}
        </el-descriptions-item>
        <el-descriptions-item>
          <template #label>
            <span class="info-label">
              <el-icon><Clock /></el-icon>
              更新时间
            </span>
          </template>
          {{ detail.updated_at || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :span="2">
          <template #label>
            <span class="info-label">
              <el-icon><ChatDotRound /></el-icon>
              审核意见
            </span>
          </template>
          {{ detail.approver_opinion || '-' }}
        </el-descriptions-item>
      </el-descriptions>
    </template>

    <el-empty v-else-if="!loading" description="未找到模型详情" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Clock, Grid, Paperclip, Share } from '@element-plus/icons-vue'
import { getAiModelDetail } from '@/api/aiModel'

const props = defineProps({
  uid: {
    type: String,
    required: true
  }
})

defineEmits(['back'])

const loading = ref(false)
const detail = ref(null)

const statusLabel = computed(() => {
  const map = {
    1: '审核通过',
    2: '审核中',
    3: '审核拒绝'
  }
  return map[detail.value?.status] || '-'
})

const fileName = computed(() => {
  const path = detail.value?.file_path || ''
  if (!path) return '-'
  const parts = String(path).split('/')
  return parts[parts.length - 1] || path
})

const approveTimeText = computed(() => {
  const t = detail.value?.approver_time
  if (t === null || t === undefined || t === '') return '-'
  // 接口示例可能是秒级时间戳
  if (typeof t === 'number' || /^\d+$/.test(String(t))) {
    const num = Number(t)
    const ms = num < 1e12 ? num * 1000 : num
    const d = new Date(ms)
    if (!Number.isNaN(d.getTime())) {
      const pad = (n) => String(n).padStart(2, '0')
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
    }
  }
  return String(t)
})

const fetchDetail = async () => {
  if (!props.uid) return
  loading.value = true
  try {
    const res = await getAiModelDetail({ uid: props.uid })
    if (res?.code === 200 && res.data) {
      detail.value = res.data
    } else {
      detail.value = null
      ElMessage.error(res?.msg || '获取模型详情失败')
    }
  } catch (error) {
    console.error('getAiModelDetail failed:', error)
    detail.value = null
    ElMessage.error(error?.response?.data?.msg || error?.message || '获取模型详情失败')
  } finally {
    loading.value = false
  }
}

watch(
  () => props.uid,
  () => {
    fetchDetail()
  }
)

onMounted(() => {
  fetchDetail()
})
</script>

<style scoped>
.model-detail {
  min-height: 360px;
  padding: 8px 4px 24px;
}

.detail-breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
  font-size: 18px;
  color: #909399;
}

.crumb-link {
  cursor: pointer;
  color: #606266;
}

.crumb-link:hover {
  color: #1a53ff;
}

.crumb-sep {
  color: #c0c4cc;
}

.crumb-current {
  color: #303133;
}

.detail-title {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  line-height: 40px;
  color: #1f2329;
}

.detail-alias {
  margin-top: 8px;
  font-size: 14px;
  color: #606266;
}

.detail-desc {
  margin: 12px 0 28px;
  font-size: 14px;
  line-height: 22px;
  color: #606266;
}

.detail-descriptions {
  max-width: 920px;
}

.info-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #606266;
}

.model-detail :deep(.el-descriptions__label) {
  width: 120px;
}

.model-detail :deep(.el-descriptions__content) {
  color: #303133;
}

.model-detail :deep(.el-descriptions__cell) {
  padding-bottom: 18px;
}
</style>
