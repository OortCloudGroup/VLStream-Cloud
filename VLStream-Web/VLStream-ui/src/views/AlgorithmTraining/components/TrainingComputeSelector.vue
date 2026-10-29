<template>
  <el-select
    :model-value="selected ? modelValue : ''"
    :placeholder="$tp('请选择 GPU 设备')"
    :disabled="disabled"
    :loading="loading"
    :no-data-text="$tp('暂无可用 GPU 设备')"
    fit-input-width
    class="training-compute-select"
    popper-class="training-compute-popper"
    @update:model-value="$emit('update:modelValue', $event)"
    @change="$emit('change', $event)"
    @visible-change="handleVisible"
  >
    <el-option-group v-for="group in groups" :key="group.key" :label="$tp(group.title)">
      <el-option v-if="!group.nodes.length" :value="`__empty_${group.key}`" disabled class="compute-empty-option">
        {{ $tp(group.key === 'market' ? '暂无已接入收费算力' : '暂无免费算力') }}
      </el-option>
      <el-option
        v-for="node in group.nodes"
        :key="node.id"
        :value="String(node.id)"
        :label="nodeLabel(node)"
        :disabled="['OTHER', 'ISOLATED'].includes(node.claimState) || (!node.schedulable && !node.stopped && !node.canClaim)"
        class="training-compute-option"
      >
        <div class="compute-option-heading">
          <span class="compute-option-name">{{ node.name || node.gpuModel || node.id }}</span>
          <span class="compute-option-state" :class="{ ready: node.schedulable }">{{ $tp(computeAvailabilityLabel(node)) }}</span>
        </div>
        <div class="compute-option-model">{{ node.gpuModel || $tp('GPU 型号未提供') }} · GPU {{ node.gpuIndex }}</div>
        <div v-if="node.billingType !== 'FREE'" class="compute-option-task">{{ $tp(computeFixedPriceLabel(node)) }}</div>
        <template v-if="isFreeSharedCompute(node)">
          <div class="compute-option-task">{{ $tp(presentation(node).phase) }}</div>
          <div class="compute-option-queue">
            <span>{{ $tp(presentation(node).queue) }}</span>
            <span>{{ $tp(presentation(node).eta) }}</span>
          </div>
          <div v-if="presentation(node).estimateNote" class="compute-option-model">{{ $tp(presentation(node).estimateNote) }}</div>
        </template>
      </el-option>
    </el-option-group>
  </el-select>
  <div v-if="selected && isFreeSharedCompute(selected)" class="selected-compute-queue" role="status" aria-live="polite">
    <span>{{ $tp(presentation(selected).phase) }}</span>
    <span>{{ $tp(presentation(selected).queue) }} · {{ $tp(presentation(selected).eta) }}</span>
    <span v-if="presentation(selected).estimateNote">{{ $tp(presentation(selected).estimateNote) }}</span>
  </div>
  <div class="compute-queue-caption">
    {{ $tp('仅统计已登记的共享任务；外部进程占用可能延长等待。') }}
    <el-button v-if="selected && isFreeSharedCompute(selected)" link type="primary" :disabled="!!pending[String(selected.id)]" @click="refresh">{{ $tp('刷新队列') }}</el-button>
  </div>
</template>

<script setup>
import { computed, onUnmounted, ref, watch } from 'vue'
import { getComputeQueueSnapshot } from '@/api/compute'
import { captureComputeIdentity } from '@/utils/computeIdentityRequest'
import { computeAvailabilityLabel, computeFixedPriceLabel, isFreeSharedCompute, queuePresentation, trainingComputeGroups } from '@/utils/trainingComputeQueue.mjs'

const props = defineProps({
  modelValue: { type: String, default: '' }, nodes: { type: Array, default: () => [] },
  loading: Boolean, disabled: Boolean, nodeLabel: { type: Function, required: true }
})
const emit = defineEmits(['update:modelValue', 'change', 'reload'])
const groups = computed(() => trainingComputeGroups(props.nodes))
const selected = computed(() => props.nodes.find(node => String(node.id) === props.modelValue))
const snapshots = ref({})
const pending = ref({})
const now = ref(Date.now())
const requests = new Map()
let generation = 0
let open = false
let refreshTimer
const clockTimer = setInterval(() => { now.value = Date.now() }, 1000)
const presentation = node => queuePresentation(snapshots.value[String(node.id)], { now: now.value, loading: !!pending.value[String(node.id)] })

const refresh = async () => {
  const current = generation
  const identity = captureComputeIdentity()
  const nodes = props.nodes.filter(isFreeSharedCompute)
  await Promise.allSettled(nodes.map(async node => {
    const id = String(node.id)
    if (pending.value[id]) return
    if (node.stopped || node.powerState === 'STOPPED' || node.operationPending) {
      snapshots.value[id] = null
      return
    }
    pending.value[id] = true
    try {
      if (!requests.has(id)) requests.set(id, getComputeQueueSnapshot(id).finally(() => requests.delete(id)))
      const response = await requests.get(id)
      identity.assertCurrent()
      if (current === generation) snapshots.value[id] = response.data
    } catch {
      if (current === generation) snapshots.value[id] = null
    } finally {
      if (current === generation) pending.value[id] = false
    }
  }))
}
const handleVisible = visible => {
  open = visible
  clearInterval(refreshTimer)
  if (visible) {
    emit('reload')
    refresh()
    refreshTimer = setInterval(refresh, 15000)
  }
}
watch(() => props.nodes, () => {
  generation++
  snapshots.value = {}
  pending.value = {}
  if (open || selected.value) refresh()
}, { immediate: true })
watch(() => props.modelValue, () => { if (selected.value && isFreeSharedCompute(selected.value)) refresh() })
onUnmounted(() => {
  generation++
  clearInterval(refreshTimer)
  clearInterval(clockTimer)
})
</script>

<style scoped>
.training-compute-select { width: 100%; min-width: 0; }
.selected-compute-queue { display: flex; flex-direction: column; gap: 5px; margin-top: 10px; font-size: 12px; line-height: 1.6; color: #606975; overflow-wrap: anywhere; }
.compute-queue-caption { margin-top: 8px; font-size: 11px; line-height: 1.6; color: #9098a3; }
</style>

<style>
.training-compute-popper { max-width: calc(100vw - 32px); }
.training-compute-popper .el-select-dropdown__wrap { max-height: min(430px, 65vh); }
.training-compute-popper .el-select-group__title { height: auto; padding-top: 10px; padding-bottom: 6px; line-height: 24px; color: #657385; font-size: 12px; font-weight: 600; }
.training-compute-popper .training-compute-option { height: auto; min-height: 64px; padding: 12px 20px; line-height: 1.5; white-space: normal; }
.training-compute-popper .compute-option-heading { display: flex; align-items: baseline; justify-content: space-between; gap: 12px; }
.training-compute-popper .compute-option-name { min-width: 0; font-size: 14px; font-weight: 600; overflow-wrap: anywhere; }
.training-compute-popper .compute-option-state { flex-shrink: 0; font-size: 11px; font-weight: 400; color: #909399; }
.training-compute-popper .compute-option-state.ready { color: #38926a; }
.training-compute-popper .compute-option-model { margin-top: 4px; font-size: 12px; font-weight: 400; color: #8a929e; overflow-wrap: anywhere; }
.training-compute-popper .compute-option-task { margin-top: 9px; font-size: 12px; font-weight: 400; color: #596579; overflow-wrap: anywhere; }
.training-compute-popper .compute-option-queue { display: flex; flex-wrap: wrap; gap: 4px 16px; margin-top: 4px; font-size: 12px; font-weight: 400; color: #7f8b9c; }
.training-compute-popper .compute-empty-option { font-size: 12px; height: auto; min-height: 38px; white-space: normal; }
</style>
