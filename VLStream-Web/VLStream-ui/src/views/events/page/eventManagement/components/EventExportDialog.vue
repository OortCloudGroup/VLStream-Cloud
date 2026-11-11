<template>
  <el-dialog v-model="visible" class="locale-dialog" :title="$tp('导出') + ' ' + format"
    width="520px" :close-on-click-modal="false" :close-on-press-escape="!running" :show-close="!running" @closed="cancel">
    <el-form label-position="top">
      <el-form-item :label="$tp('导出范围')">
        <el-radio-group v-model="scope" :disabled="running" class="event-export-scopes">
          <el-radio value="selected" :disabled="!snapshot.selected.length">{{ $tp('已勾选') }} ({{ snapshot.selected.length }})</el-radio>
          <el-radio value="page" :disabled="!snapshot.rows.length">{{ $tp('当前页') }} ({{ snapshot.rows.length }})</el-radio>
          <el-radio value="all" :disabled="!snapshot.total">{{ $tp('当前筛选全部') }} ({{ snapshot.total }})</el-radio>
        </el-radio-group>
      </el-form-item>
      <p>{{ $tp('跟随当前可见列及顺序导出，图片和录像保存为链接。媒体链接有效期以服务端授权为准。') }}</p>
      <div v-if="running" role="status" aria-live="polite">
        <el-progress :percentage="progress" />
        <p>{{ progressText }}</p>
      </div>
    </el-form>
    <template #footer>
      <el-button @click="cancel">{{ $tp('取消') }}</el-button>
      <el-button type="primary" :loading="running" :disabled="running || !scopeCount" @click="startExport">{{ $tp('导出') }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onUnmounted, ref, shallowRef } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { saveAs } from 'file-saver'
import { eventList, eventDetail } from '@/api/smartCity/events'
import { authManager } from '@/utils/auth'
import { covertCurrentLocationURL } from '@/utils/converGatewayPath'
import { translatePhrase as uiText } from '@/i18n'
import pdfFontUrl from '@/assets/font/DingTalk-JinBuTi.ttf?url'
import { lookupEventLocation } from './eventLocationLookup'
import { buildEventExportTable, collectFilteredEvents, createEventExportFile } from './eventExport.mjs'

const props = defineProps({
  rows: { type: Array, required: true },
  selectedRows: { type: Array, required: true },
  columns: { type: Array, required: true },
  query: { type: Object, required: true },
  pageOffset: { type: Number, default: 0 },
  total: { type: Number, default: 0 }
})
const visible = ref(false)
const format = ref('Excel')
const scope = ref('page')
const running = ref(false)
const progress = ref(0)
const progressText = ref('')
const snapshot = shallowRef({ rows: [], selected: [], columns: [], query: {}, total: 0, offset: 0, identity: null })
const scopeCount = computed(() => ({ selected: snapshot.value.selected.length, page: snapshot.value.rows.length, all: snapshot.value.total })[scope.value])
let generation = 0

function open(value) {
  if (visible.value || running.value) return
  format.value = value
  snapshot.value = {
    rows: JSON.parse(JSON.stringify(props.rows)),
    selected: props.selectedRows.map(row => String(row.id)),
    columns: props.columns.map(column => ({ ...column })),
    query: JSON.parse(JSON.stringify(props.query)),
    total: props.total,
    offset: props.pageOffset,
    identity: authManager.getIdentitySnapshot()
  }
  // Fix the upper time boundary so new events do not shift later export pages.
  const now = dayjs().format('YYYY-MM-DD HH:mm:ss')
  if (!snapshot.value.query.end_at || snapshot.value.query.end_at > now) snapshot.value.query.end_at = now
  snapshot.value.query.start_at ||= '1000-01-01 00:00:00'
  scope.value = snapshot.value.selected.length ? 'selected' : 'page'
  progress.value = 0
  visible.value = true
}

function cancel() {
  generation++
  running.value = false
  visible.value = false
}

async function startExport() {
  if (running.value || !scopeCount.value) return
  const current = ++generation
  const data = snapshot.value
  const exportScope = scope.value
  const assertActive = () => {
    if (current !== generation || !authManager.isIdentityCurrent(data.identity)) throw new Error('EXPORT_CANCELLED')
  }
  running.value = true
  progress.value = 0
  try {
    assertActive()
    let rows
    if (exportScope === 'all') {
      rows = await collectFilteredEvents(eventList, data.query, { assertActive, onProgress(done, total) {
        progress.value = total ? Math.round(done / total * 80) : 80
        progressText.value = uiText('正在读取事件 {done} / {total}', { done, total })
      } })
    } else {
      const selected = new Set(data.selected)
      const source = data.rows.filter(row => exportScope === 'page' || selected.has(String(row.id)))
      rows = []
      for (const row of source) {
        assertActive()
        const response = await eventDetail({ id: row.id })
        assertActive()
        if (response?.code !== 200 || String(response.data?.id) !== String(row.id)) throw new Error(response?.msg || '事件不存在或无权访问')
        rows.push(response.data)
        progress.value = Math.round(rows.length / source.length * 80)
        progressText.value = uiText('正在读取事件 {done} / {total}', { done: rows.length, total: source.length })
      }
    }
    if (!rows.length) throw new Error('没有可导出的事件')
    progressText.value = uiText('正在生成文件')
    const positions = new Map(data.rows.map((row, index) => [String(row.id), data.offset + index + 1]))
    const table = await buildEventExportTable(rows, data.columns, {
      translate: uiText, assertActive, convertUrl: covertCurrentLocationURL, baseUrl: window.location.origin,
      resolveLocation: coordinate => lookupEventLocation(coordinate, () => current === generation && authManager.isIdentityCurrent(data.identity)),
      sequence: (index, row) => exportScope === 'all' ? index + 1 : positions.get(String(row.id))
    })
    progress.value = 90
    const file = await createEventExportFile(format.value, table, { title: uiText('主动安全事件'), fontUrl: pdfFontUrl, assertActive })
    assertActive()
    saveAs(file.blob, `events_${dayjs().format('YYYYMMDD_HHmmss')}.${file.extension}`)
    progress.value = 100
    visible.value = false
    ElMessage.success(uiText('已导出 {count} 条事件', { count: rows.length }))
  } catch (error) {
    if (current !== generation) return
    if (!authManager.isIdentityCurrent(data.identity)) {
      visible.value = false
      ElMessage.warning(uiText('登录身份已变化，请重新导出'))
    } else {
      ElMessage.error(uiText(error.message || '导出失败，请重试'))
    }
  } finally {
    if (current === generation) running.value = false
  }
}

onUnmounted(cancel)
defineExpose({ open })
</script>

<style scoped>
.event-export-scopes { display: flex; flex-direction: column; align-items: flex-start; }
p { color: var(--el-text-color-secondary); line-height: 1.6; overflow-wrap: anywhere; }
</style>
