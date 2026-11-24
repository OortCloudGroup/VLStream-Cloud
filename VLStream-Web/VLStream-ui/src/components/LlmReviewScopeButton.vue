<template>
  <el-button v-if="canReview" link type="primary" :loading="busy" :aria-busy="busy"
    :aria-label="label + ' · ' + $tp('大模型复核')" @click.stop="configure">{{ $tp('大模型复核') }}</el-button>
</template>

<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getReviewScope, setReviewScope } from '@/api/llmReview'
import { authManager } from '@/utils/auth'
import { canAccessPage } from '@/utils/menuAccess.mjs'
import { menuPermissions } from '@/utils/menuAccessState'
import { translatePhrase as tp } from '@/i18n'

const props = defineProps({
  type: { type: String, required: true },
  targetId: { type: [Number, String], required: true },
  label: { type: String, required: true }
})
const canReview = computed(() => Array.isArray(menuPermissions.value) && canAccessPage('/llm-review-tasks', menuPermissions.value))
const busy = ref(false)
let mounted = true
onBeforeUnmount(() => { mounted = false })

async function configure() {
  if (busy.value) return
  const { type, targetId, label } = props
  const identity = authManager.getIdentitySnapshot()
  const current = () => mounted && type === props.type && targetId === props.targetId && authManager.isIdentityCurrent(identity)
  busy.value = true
  try {
    const response = await getReviewScope(type, targetId, identity)
    if (!current()) return
    if (response.code !== 200 || typeof response.data !== 'boolean') throw new Error(response.msg || tp('读取复核设置失败'))
    const enabled = response.data
    const description = type === 'SCENE' ? '仅影响该场景云端检测任务产生的新告警。'
      : type === 'DEVICE' ? '影响这台设备新上报的结构化告警。算法和分类的复核开关独立生效。'
        : '影响该分类及子节点下 VLS 设备的新告警，设备移入或移出后自动生效。其他复核开关独立生效。'
    await ElMessageBox.confirm(tp(enabled ? '此处已开启自动复核。' : '此处已关闭自动复核。') + tp(description),
      label + ' · ' + tp('大模型复核'), {
        confirmButtonText: tp(enabled ? '关闭复核' : '开启复核'), cancelButtonText: tp('取消')
      })
    if (!current()) return
    const saved = await setReviewScope(type, targetId, !enabled, identity)
    if (!current()) return
    if (saved.code !== 200 || saved.data !== !enabled) throw new Error(saved.msg || tp('保存复核设置失败'))
    ElMessage.success(tp(enabled ? '已关闭此处的自动复核' : '已开启此处的自动复核'))
  } catch (error) {
    if (current() && !['cancel', 'close'].includes(error)) ElMessage.error(error?.message || tp('更新复核设置失败'))
  } finally {
    busy.value = false
  }
}
</script>
