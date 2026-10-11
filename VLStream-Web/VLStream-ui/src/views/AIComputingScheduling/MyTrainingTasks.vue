<template>
  <section class="my-training-tasks" v-loading="loading">
    <div class="heading"><div><h2>{{ $tp('训练任务') }}</h2><p>{{ $tp('我的训练任务，统计当前登录用户在全部算力上的任务；不包含归属未确认的历史任务。') }}</p></div><el-button @click="load">{{ $tp('刷新') }}</el-button></div>
    <div class="statistics"><el-card><span>{{ $tp('筛选结果') }}</span><strong>{{ total }}</strong></el-card><el-card v-for="state in states" :key="state.value"><span>{{ $tp(state.label) }}</span><strong>{{ statistics[state.value] || 0 }}</strong></el-card></div>
    <div class="filters"><el-input v-model="name" clearable :placeholder="$tp('任务名称')" @keyup.enter="search" /><el-select v-model="stage" clearable :placeholder="$tp('全部阶段')"><el-option v-for="state in states" :key="state.value" :label="$tp(state.label)" :value="state.value" /></el-select><el-date-picker v-model="dates" type="datetimerange" value-format="YYYY-MM-DD HH:mm:ss" /><el-button type="primary" @click="search">{{ $tp('搜索') }}</el-button><el-button @click="reset">{{ $tp('重置') }}</el-button><el-button @click="router.push('/algorithm-training')">{{ $tp('创建训练任务') }}</el-button></div>
    <el-table :data="records" :empty-text="$tp('当前用户没有符合条件的训练任务')">
      <el-table-column prop="taskName" :label="$tp('任务名称')" min-width="180" />
      <el-table-column prop="computeName" :label="$tp('执行算力')" min-width="180" />
      <el-table-column :label="$tp('状态')" min-width="130"><template #default="{ row }"><el-tag>{{ $tp(states.find(state => state.value === row.executionState)?.label || row.executionState) }}</el-tag></template></el-table-column>
      <el-table-column :label="$tp('进度')" min-width="170"><template #default="{ row }"><el-progress :percentage="Math.max(0, Math.min(100, Number(row.progress) || 0))" /></template></el-table-column>
      <el-table-column prop="createTime" :label="$tp('创建时间')" min-width="190" />
      <el-table-column :label="$tp('操作')" width="140"><template #default><el-button link type="primary" @click="router.push('/algorithm-training')">{{ $tp('算法训练') }}</el-button></template></el-table-column>
    </el-table>
    <el-pagination v-model:current-page="current" v-model:page-size="size" :total="total" :page-sizes="[10,20,50,100]" layout="total, sizes, prev, pager, next" @current-change="load" @size-change="search" />
  </section>
</template>
<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { listMyTrainingTasks } from '@/api/compute'
const router = useRouter(), route = useRoute()
const records = ref([]), statistics = ref({}), total = ref(0), current = ref(1), size = ref(20), loading = ref(false)
const name = ref(''), stage = ref(''), dates = ref(null)
let timer, revision = 0
const states = [ { value: 'PENDING', label: '待启动' }, { value: 'QUEUED', label: '排队中' }, { value: 'PREPARING', label: '准备中' }, { value: 'RUNNING', label: '训练中' }, { value: 'ARCHIVING', label: '回存中' }, { value: 'COMPLETED', label: '已完成' }, { value: 'FAILED', label: '失败' }, { value: 'CANCELLED', label: '已取消' }, { value: 'STOPPED', label: '已停止' } ]
const load = async () => {
  const version = ++revision
  loading.value = true
  try {
    const response = await listMyTrainingTasks({ current: current.value, size: size.value, name: name.value || undefined, state: stage.value || undefined, start: dates.value?.[0], end: dates.value?.[1] })
    if (version !== revision) return
    records.value = response.data?.records || []; total.value = response.data?.total || 0; statistics.value = response.data?.statistics || {}
  } finally { if (version === revision) loading.value = false }
}
const search = () => { current.value = 1; return load() }
const reset = () => { name.value = ''; stage.value = ''; dates.value = null; return search() }
onMounted(() => {
  if (route.query.tab === 'cloud') { router.replace('/compute-management'); return }
  load().catch(() => {})
  timer = window.setInterval(() => { if (!loading.value) load().catch(() => {}) }, 15000)
})
onUnmounted(() => { ++revision; window.clearInterval(timer) })
</script>
<style scoped>
.my-training-tasks { padding: 24px; box-sizing: border-box; width: 100%; overflow: auto; }
.heading { display: flex; justify-content: space-between; align-items: center; gap: 16px; }
.heading p { color: #667085; }
.statistics { display: grid; grid-template-columns: repeat(auto-fit, minmax(110px, 1fr)); gap: 10px; margin: 20px 0; }
.statistics span { font-size: 13px; color: #667085; }.statistics strong { display: block; margin-top: 10px; font-size: 24px; }
.filters { display: flex; flex-wrap: wrap; gap: 12px; margin-bottom: 20px; }.filters .el-input,.filters .el-select { width: 190px; }
.el-pagination { margin-top: 20px; }
</style>
