<!-- SPDX-License-Identifier: MIT -->
<template>
  <div class="review-page tenant_Page">
    <div class="page-card">
      <div class="page-header">
        <div>
          <h2>大模型复核</h2>
          <p>查看 YOLO 事件的二次判断、失败原因与人工复核记录。</p>
        </div>
        <el-button @click="load">刷新</el-button>
      </div>

      <el-form :inline="true" class="filters">
        <el-form-item label="复核状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 160px">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="算法 ID">
          <el-input v-model="query.algorithmId" clearable placeholder="请输入算法 ID" />
        </el-form-item>
        <el-form-item><el-button type="primary" @click="search">查询</el-button></el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="rows" border>
        <el-table-column prop="deviceEventId" label="设备事件 ID" min-width="180" show-overflow-tooltip />
        <el-table-column prop="deviceId" label="设备 ID" min-width="160" show-overflow-tooltip />
        <el-table-column prop="algorithmId" label="算法 ID" min-width="130" />
        <el-table-column label="状态" width="110">
          <template #default="scope"><el-tag :type="statusType(scope.row.reviewStatus)">{{ statusLabel(scope.row.reviewStatus) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="confidence" label="置信度" width="100" />
        <el-table-column prop="attemptCount" label="调用次数" width="100" />
        <el-table-column prop="reason" label="判断原因" min-width="220" show-overflow-tooltip />
        <el-table-column prop="createTime" label="接收时间" min-width="170" />
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="scope"><el-button link type="primary" @click="openDetail(scope.row)">详情</el-button></template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination background layout="total, sizes, prev, pager, next"
                       v-model:current-page="query.current" v-model:page-size="query.size"
                       :page-sizes="[10, 20, 50]" :total="total" @change="load" />
      </div>
    </div>

    <el-drawer v-model="detailVisible" title="复核详情" size="560px">
      <div v-loading="detailLoading" class="detail-content">
        <el-image v-if="mediaUrl" :src="mediaUrl" fit="contain" class="event-image" :preview-src-list="[mediaUrl]" />
        <el-descriptions v-if="detail" :column="1" border>
          <el-descriptions-item label="复核状态">{{ statusLabel(detail.reviewStatus) }}</el-descriptions-item>
          <el-descriptions-item label="模型结论">{{ detail.decision || '-' }}</el-descriptions-item>
          <el-descriptions-item label="置信度">{{ detail.confidence ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="判断原因">{{ detail.reason || '-' }}</el-descriptions-item>
          <el-descriptions-item label="错误信息">{{ detail.lastError || '-' }}</el-descriptions-item>
          <el-descriptions-item label="正式事件 ID">{{ detail.formalEventId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="原始响应"><pre>{{ detail.rawResponse || '-' }}</pre></el-descriptions-item>
        </el-descriptions>
        <div v-if="detail && canManualReview(detail.reviewStatus)" class="manual-actions">
          <el-button type="danger" @click="manual('REJECTED')">确认误报</el-button>
          <el-button type="success" @click="manual('CONFIRMED')">确认事件</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getEventMediaViewUrl,
  getLlmReviewTask,
  getLlmReviewTasks,
  submitLlmManualDecision
} from '@/api/llmReview'

const statusOptions = [
  { label: '待复核', value: 'PENDING' },
  { label: '复核中', value: 'PROCESSING' },
  { label: '等待重试', value: 'RETRY' },
  { label: '确认事件', value: 'CONFIRMED' },
  { label: '确认误报', value: 'REJECTED' },
  { label: '需要人工复核', value: 'MANUAL_REVIEW' },
  { label: '复核失败', value: 'ERROR' }
]
const query = reactive({ current: 1, size: 10, status: '', algorithmId: '' })
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)
const mediaUrl = ref('')

const statusLabel = status => statusOptions.find(item => item.value === status)?.label || status
const statusType = status => ({ CONFIRMED: 'success', REJECTED: 'info', ERROR: 'danger', MANUAL_REVIEW: 'warning' }[status] || 'primary')
const canManualReview = status => ['MANUAL_REVIEW', 'ERROR', 'RETRY', 'REJECTED'].includes(status)

async function load() {
  loading.value = true
  try {
    const params = { current: query.current, size: query.size }
    if (query.status) params.status = query.status
    if (query.algorithmId) params.algorithmId = query.algorithmId
    const response = await getLlmReviewTasks(params)
    rows.value = response.data?.records || []
    total.value = response.data?.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.current = 1
  load()
}

async function openDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  mediaUrl.value = ''
  try {
    const [taskResponse, mediaResponse] = await Promise.all([
      getLlmReviewTask(row.id),
      getEventMediaViewUrl(row.mediaId).catch(() => null)
    ])
    detail.value = taskResponse.data
    mediaUrl.value = mediaResponse?.data || ''
  } finally {
    detailLoading.value = false
  }
}

async function manual(decision) {
  const text = decision === 'CONFIRMED' ? '确认该事件并进入主动安全流程' : '确认该事件为误报'
  await ElMessageBox.confirm(`确定${text}吗？`, '人工复核', { type: 'warning' })
  const response = await submitLlmManualDecision(detail.value.id, decision)
  if (response.code !== 200) throw new Error(response.message || '人工复核失败')
  detail.value = response.data
  ElMessage.success('人工复核已提交')
  await load()
}

onMounted(load)
</script>

<style scoped>
.review-page { padding: 20px; }
.page-card { background: #fff; border-radius: 8px; padding: 20px; }
.page-header { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 18px; }
.page-header h2 { margin: 0 0 8px; font-size: 20px; }
.page-header p { margin: 0; color: #8c8c8c; }
.filters { margin-bottom: 4px; }
.pagination { display: flex; justify-content: flex-end; margin-top: 18px; }
.detail-content { min-height: 300px; }
.event-image { width: 100%; height: 280px; margin-bottom: 18px; background: #f5f7fa; }
.manual-actions { display: flex; justify-content: flex-end; margin-top: 20px; }
pre { margin: 0; white-space: pre-wrap; word-break: break-all; }
</style>
