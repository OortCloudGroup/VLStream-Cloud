<template>
  <el-dialog :model-value="true" :title="node.name" width="min(580px, calc(100vw - 32px))" @close="$emit('closed')">
    <el-alert type="info" :closable="false" :title="$tp('公司实例已登记，SSH训练身份待配置；收费使用尚未开通。')" />
    <el-descriptions :column="1" border class="company-configuration">
      <el-descriptions-item :label="$tp('实例 ID')">{{ node.providerInstanceId }}</el-descriptions-item>
      <el-descriptions-item label="GPU">{{ node.gpuModel }}</el-descriptions-item>
      <el-descriptions-item :label="$tp('SSH 地址')">{{ node.host }}:{{ node.port }}</el-descriptions-item>
      <el-descriptions-item :label="$tp('SSH 认证')">{{ $tp('待配置') }}</el-descriptions-item>
      <el-descriptions-item :label="$tp('训练环境')">{{ $tp('尚未通过平台检查') }}</el-descriptions-item>
      <el-descriptions-item :label="$tp('市场展示')">{{ $tp(node.resourceScope === 'PLATFORM' ? '已发布' : '未发布') }}</el-descriptions-item>
    </el-descriptions>
    <p>{{ $tp('配置获批的专用训练身份并完成环境与回存验收后，才可进入后续使用验证。当前无法检查、启停或训练。') }}</p>
    <el-form v-if="node.companyPublicationAllowed" label-width="120px">
      <el-form-item :label="$tp('发布到市场')"><el-switch v-model="published" :disabled="saving" /></el-form-item>
      <p>{{ $tp('此操作只改变市场可见性，不启用计费或计算能力。') }}</p>
    </el-form>
    <template #footer>
      <el-button @click="$emit('closed')">{{ $tp('关闭') }}</el-button>
      <el-button v-if="node.companyPublicationAllowed" type="primary" :loading="saving" @click="save">{{ $tp('保存展示设置') }}</el-button>
    </template>
  </el-dialog>
</template>
<script setup>
import { ref, watch, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { translatePhrase as uiText } from '@/i18n'
import { currentIdentityRequest, computePathPart } from '@/utils/computeIdentityRequest'
const props = defineProps({ node: { type: Object, required: true } })
const emit = defineEmits(['closed', 'updated'])
const saving = ref(false), published = ref(false)
let alive = true, revision = 0
watch(() => props.node, node => { revision++; saving.value = false; published.value = node.resourceScope === 'PLATFORM' }, { immediate: true })
const closeForIdentity = () => { revision++; emit('closed') }
window.addEventListener('modelHubAuthChanged', closeForIdentity)
onBeforeUnmount(() => { alive = false; revision++; window.removeEventListener('modelHubAuthChanged', closeForIdentity) })
const save = async () => {
  if (saving.value || !props.node.companyPublicationAllowed) return
  const current = revision
  saving.value = true
  try {
    await currentIdentityRequest({ url: `/vlsCompute/company-offers/${computePathPart(props.node.companyOfferCode)}/publication`, method: 'put', params: { published: published.value } })
    if (!alive || current !== revision) return
    ElMessage.success(uiText('市场展示已更新；收费使用仍未开通'))
    emit('updated'); emit('closed')
  } catch (error) {
    if (alive && current === revision) ElMessage.error(error?.data?.msg || error?.message || uiText('展示设置保存失败'))
  } finally { if (alive && current === revision) saving.value = false }
}
</script>
<style scoped>
.company-configuration { margin-top: 18px; }
p { color: var(--el-text-color-secondary); font-size: 13px; line-height: 1.7; }
</style>
