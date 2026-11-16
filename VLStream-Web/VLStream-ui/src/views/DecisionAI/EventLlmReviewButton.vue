<!-- SPDX-License-Identifier: MIT -->
<template>
  <button v-if="canReview" class="event-review-button" type="button" :disabled="submitting"
          :aria-busy="submitting" :aria-label="$tp('大模型复核')" @click.stop="review">
    <el-icon><Loading v-if="submitting" class="is-loading" /><ChatDotRound v-else /></el-icon>
    <span>{{ $tp('大模型复核') }}</span>
  </button>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Loading } from '@element-plus/icons-vue'
import { reviewExistingEvent } from '@/api/llmReview'
import { authManager } from '@/utils/auth'
import { canAccessPage } from '@/utils/menuAccess.mjs'
import { menuPermissions } from '@/utils/menuAccessState'
import { translatePhrase as uiText } from '@/i18n'

const props = defineProps({ eventId: { type: [String, Number], required: true } })
const router = useRouter()
const submitting = ref(false)
const canReview = computed(() => Array.isArray(menuPermissions.value) && canAccessPage('/llm-review-tasks', menuPermissions.value))

async function review() {
  if (submitting.value) return
  const identity = authManager.getIdentitySnapshot()
  submitting.value = true
  try {
    const response = await reviewExistingEvent(String(props.eventId))
    if (!authManager.isIdentityCurrent(identity)) return
    if (response.code !== 200 || !response.data?.id) throw new Error(response.message || '复核任务提交失败')
    await router.push({ path: '/llm-review-tasks', query: { taskId: String(response.data.id) } })
  } catch (error) {
    if (authManager.isIdentityCurrent(identity)) ElMessage.error(error?.message || uiText('复核任务提交失败'))
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.event-review-button {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-height: 28px;
  padding: 4px;
  border: 0;
  border-radius: 4px;
  color: var(--el-color-primary);
  background: transparent;
  font: inherit;
  white-space: nowrap;
  cursor: pointer;
}
.event-review-button:hover {
  background: var(--el-color-primary-light-9);
}
.event-review-button:disabled { opacity: 0.6; cursor: wait; }
.event-review-button:focus-visible {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 2px;
}
</style>
