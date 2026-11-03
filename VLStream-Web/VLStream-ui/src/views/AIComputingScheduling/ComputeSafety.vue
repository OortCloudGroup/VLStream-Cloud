<template>
  <section class="compute-safety">
    <h2>我的算力安全停机</h2>
    <p>这里只显示当前账户可停止的活跃算力实例。停止和结算状态以实际结果为准，列表为空不代表费用已结清。</p>
    <el-button :loading="loading" @click="refresh">刷新状态</el-button>
    <el-alert v-if="error" :title="error" type="warning" :closable="false" show-icon />
    <el-table v-loading="loading" :data="runs" empty-text="暂无可显示的活跃实例">
      <el-table-column prop="runId" label="运行编号" />
      <el-table-column prop="nodeId" label="节点编号" />
      <el-table-column prop="state" label="当前状态" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button type="danger" link :disabled="loading || stopping" @click="stopRun(row)">请求停止</el-button>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listComputeSafetyRuns, stopComputeSafetyRun } from '@/api/computeBilling'
import { captureComputeIdentity } from '@/utils/computeIdentityRequest'
import { loadSafetyRuns, stopSafetyRun } from '@/utils/computeSafety.mjs'

const runs = ref([])
const loading = ref(false)
const stopping = ref(false)
const error = ref('')
let loadedIdentity = null
let sequence = 0

async function refresh() {
  const current = ++sequence
  const identity = captureComputeIdentity()
  loading.value = true
  error.value = ''
  runs.value = []
  loadedIdentity = null
  try {
    const rows = await loadSafetyRuns(listComputeSafetyRuns, identity)
    if (current !== sequence) return
    runs.value = rows
    loadedIdentity = identity
  } catch {
    if (current === sequence) error.value = '当前状态未确认，请检查登录和授权后刷新；不要据此判断已停止或费用已结清。'
  } finally {
    if (current === sequence) loading.value = false
  }
}

async function stopRun(row) {
  if (stopping.value || !loadedIdentity) return
  stopping.value = true
  try {
    await stopSafetyRun(row, loadedIdentity, item => ElMessageBox.confirm(
      `确认请求停止运行 ${item.runId}？实际停止和结算可能需要时间。`, '安全停机',
      { type: 'warning', confirmButtonText: '请求停止', cancelButtonText: '取消' }), stopComputeSafetyRun)
    ElMessage.success('已请求停止，实际停止及结算状态以刷新结果为准')
    await refresh()
  } catch (failure) {
    if (failure !== 'cancel' && failure !== 'close') {
      runs.value = []
      loadedIdentity = null
      error.value = '停止请求状态未确认，请核对当前账户并刷新；不要据此判断已停止或费用已结清。'
    }
  } finally { stopping.value = false }
}
function invalidateChangedIdentity() {
  if (!loadedIdentity) return
  try { loadedIdentity.assertCurrent() } catch {
    sequence++
    loading.value = false
    runs.value = []
    loadedIdentity = null
    error.value = '登录账户或租户已变化，请刷新确认当前状态。'
  }
}
onMounted(() => {
  window.addEventListener('modelHubAuthChanged', invalidateChangedIdentity)
  window.addEventListener('storage', invalidateChangedIdentity)
  window.addEventListener('focus', invalidateChangedIdentity)
  refresh()
})
onBeforeUnmount(() => {
  sequence++
  window.removeEventListener('modelHubAuthChanged', invalidateChangedIdentity)
  window.removeEventListener('storage', invalidateChangedIdentity)
  window.removeEventListener('focus', invalidateChangedIdentity)
})
</script>

<style scoped>
.compute-safety { padding: 24px; }
.compute-safety p { color: #606266; margin-bottom: 20px; }
.compute-safety .el-alert, .compute-safety .el-table { margin-top: 16px; }
</style>
