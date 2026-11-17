<template>
  <span class="featured-publish-actions" @click.stop>
    <el-tooltip :content="$tp(state?.featured === true ? '取消本地精选，不影响已发布资源' : '设为本地精选，不会发布到开发广场')">
      <el-button link :type="state?.featured === true ? 'warning' : 'primary'" :icon="state?.featured === true ? StarFilled : Star"
        :disabled="loading || busy || !state" :aria-pressed="state?.featured === true" @click="setFeatured(state?.featured !== true)">
        {{ $tp(state?.featured === true ? '取消精选' : '设为精选') }}
      </el-button>
    </el-tooltip>
    <el-button link type="primary" @click="open">{{ $tp(listLabel) }}</el-button>
    <el-dialog v-model="visible" :title="$tp('发布到开发广场')" width="min(540px, 94vw)" append-to-body
      :close-on-click-modal="false" :close-on-press-escape="true" :show-close="true">
      <div v-loading="loading">
        <p class="resource-name">{{ name }}</p>
        <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
        <div v-if="cloudAuthRequired" class="cloud-auth-recovery">
          <p class="publish-note">{{ $tp('请重新登录与开发广场同一环境的 OortCloud 账号。登录返回后需再次确认公开发布，不会自动重发；VLS 本地账号保持不变。') }}</p>
          <el-button type="primary" plain :disabled="busy" @click="reloginCloud">{{ $tp('重新登录 OortCloud') }}</el-button>
        </div>
        <template v-if="state">
          <el-form label-position="top">
            <el-form-item :label="$tp('发布状态')"><span>{{ statusLabel }}</span></el-form-item>
          </el-form>
          <p class="publish-note">{{ kind === 'model' ? $tp('将当前固定模型及配对类别文件发布到 OortCloud 开发广场，供其他用户公开发现和授权下载。仅兼容的模型可下发到摄像机。') : $tp('将当前固定数据集包发布到 OortCloud 开发广场，供其他用户公开发现和授权下载。请确认包中不含未经授权公开的数据。') }}</p>
          <el-alert v-if="!canPublish" :title="publishBlockReason" type="warning" :closable="false" show-icon />
          <p v-if="state.remoteUid" class="published-id">{{ $tp('远端资源编号：') }}{{ state.remoteUid }}</p>
          <p v-if="state.receiptMessage || state.error" class="publish-note">{{ state.receiptMessage || state.error }}</p>
          <p v-if="pending || submitting" class="publish-note">{{ $tp('正在后台发布，可关闭此窗口。列表会继续更新状态；关闭不取消任务，也不会重复上传。') }}</p>
        </template>
      </div>
      <template #footer>
        <el-button @click="visible = false">{{ $tp('关闭') }}</el-button>
        <el-button :disabled="loading || busy" @click="load">{{ $tp(state ? '刷新状态' : '重试') }}</el-button>
        <el-button v-if="canResume" type="primary" :disabled="loading || busy" @click="resume">{{ $tp('确认继续提交') }}</el-button>
        <el-button v-if="canReconcile" type="primary" :loading="checking" :disabled="loading || submitting || checking" @click="reconcile(true)">{{ $tp('核实发布结果') }}</el-button>
        <el-button v-else-if="state && !confirmed" type="primary" :loading="submitting" :disabled="loading || busy || !canPublish" @click="publish">{{ $tp('确认公开发布') }}</el-button>
      </template>
    </el-dialog>
  </span>
</template>
<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { Star, StarFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { translatePhrase as uiText } from '@/i18n'
import { getPublication, setPublicationFeatured, publishToModelHub, reconcilePublication, resumePublication } from '@/api/modelHubPublication'
import { getOortCloudSession } from '@/utils/oortCloudSession'
import { isMultiTenantMode, redirectToPlatformLogin } from '@/utils/platformSession'
import { startModelHubLogin, clearModelHubAuth, clearPendingModelHubPublish } from '@/utils/modelHubAuth'
import { captureComputeIdentity } from '@/utils/computeIdentityRequest'
import { pendingPublication, confirmedPublication, uploadAllowed, publicationPollDelay, publicationLabel, resumeAllowed } from '@/utils/publicationProgress.mjs'
const props = defineProps({ kind: { type: String, required: true }, id: { type: [String, Number], required: true }, name: { type: String, default: '' } })
const visible = ref(false), loading = ref(false), state = ref(null), error = ref(''), cloudAuthRequired = ref(false)
const submitting = ref(false), checking = ref(false), starring = ref(false), confirming = ref(false)
const busy = computed(() => submitting.value || checking.value || starring.value || confirming.value)
const pending = computed(() => pendingPublication(state.value))
const canReconcile = computed(() => pending.value || state.value?.publishState === 'REMOTE_FAILED' || state.value?.receiptState === 'FAILED')
const confirmed = computed(() => confirmedPublication(state.value))
const canResume = computed(() => resumeAllowed(state.value))
const canPublish = computed(() => uploadAllowed(state.value))
const listLabel = computed(() => publicationLabel(state.value, cloudAuthRequired.value, submitting.value))
const publishBlockReason = computed(() => state.value?.reason || uiText(pending.value ? '请核实已有发布任务，不要重复上传' : '当前资源尚未就绪，暂不能公开发布'))
const statusLabel = computed(() => state.value?.receiptState === 'FAILED' ? uiText('发布失败') : state.value?.publishState === 'PUBLISHED' && !confirmed.value ? uiText('状态待核实') : uiText(({ DRAFT: '尚未发布', CHANGED: '内容已更改，需重新发布', UNCERTAIN: '发布结果未确认，请核实原版本', UNKNOWN: '发布结果未确认', PUBLISHED: '已发布', FAILED: '发布失败', REMOTE_FAILED: '发布失败', PUBLISHING: '准备并提交中', PROCESSING: '开发广场后台处理中', UPLOADING: '发布中' })[state.value?.publishState] || '状态待核实'))
let sequence = 0, disposed = false, timer, pollAttempt = 0
function stopPolling() { clearTimeout(timer); timer = undefined }
function current(revision, identity) {
  if (disposed || revision !== sequence) return false
  try { identity.assertCurrent(); return true } catch {
    stopPolling(); sequence++; state.value = null; loading.value = false; submitting.value = false; checking.value = false; starring.value = false; confirming.value = false
    error.value = uiText('登录身份已变化，请刷新状态')
    return false
  }
}
function schedulePoll() {
  stopPolling()
  if (disposed || !pending.value || cloudAuthRequired.value || busy.value) return
  timer = setTimeout(() => reconcile(false), publicationPollDelay(pollAttempt++, document.hidden))
}
function authFailure(e, session) {
  error.value = e.message || uiText('发布结果未确认，请核实原任务，不要重复上传')
  if (Number(e.code) === 4401) {
    cloudAuthRequired.value = true; stopPolling()
    if (!session.usesPlatformSession) clearModelHubAuth()
  }
}
function cloudSession() {
  const session = getOortCloudSession(isMultiTenantMode() ? 'multi' : 'single')
  if (!session.accessToken) {
    cloudAuthRequired.value = true; stopPolling(); error.value = uiText('请先登录 OortCloud，再核实或确认公开发布')
    return null
  }
  return session
}
async function open() { visible.value = true; if (!busy.value) await load() }
async function load() {
  if (disposed || busy.value) return
  stopPolling()
  const revision = ++sequence, identity = captureComputeIdentity()
  loading.value = true; error.value = ''
  try {
    const result = await getPublication(props.kind, props.id)
    if (current(revision, identity)) state.value = result
  } catch (e) { if (current(revision, identity)) error.value = e.message || uiText('读取发布状态失败') }
  finally { if (revision === sequence && !disposed) { loading.value = false; schedulePoll() } }
}
async function setFeatured(value) {
  if (busy.value || loading.value || !state.value) return
  starring.value = true; error.value = ''
  const revision = sequence, identity = captureComputeIdentity()
  try {
    const result = await setPublicationFeatured(props.kind, props.id, value)
    if (current(revision, identity)) state.value = result
  } catch (e) { if (current(revision, identity)) { error.value = e.message || uiText('更新精选失败'); ElMessage.error(error.value) } }
  finally { if (revision === sequence && !disposed) { starring.value = false; schedulePoll() } }
}
function reloginCloud() {
  stopPolling(); clearPendingModelHubPublish()
  if (isMultiTenantMode()) redirectToPlatformLogin()
  else startModelHubLogin(null, { returnToCurrent: true })
}
async function publish() {
  if (disposed || busy.value || loading.value || !canPublish.value) return
  const session = cloudSession()
  if (!session) return
  const revision = sequence, identity = captureComputeIdentity()
  stopPolling(); submitting.value = true; error.value = ''; cloudAuthRequired.value = false
  try {
    identity.assertCurrent()
    const result = await publishToModelHub(props.kind, props.id, {}, session)
    if (!current(revision, identity)) return
    state.value = result; pollAttempt = 0
    if (confirmedPublication(result)) ElMessage.success(uiText('已发布到开发广场'))
    else if (!pendingPublication(result)) error.value = result.receiptMessage || result.error || uiText('尚未确认发布成功，请核实状态')
  } catch (e) {
    if (!current(revision, identity)) return
    if (Number(e.code) !== 4401) state.value = { ...state.value, publishState: 'UNCERTAIN' }
    authFailure(e, session)
  } finally { if (revision === sequence && !disposed) { submitting.value = false; schedulePoll() } }
}
async function reconcile(manual = false) {
  if (disposed || busy.value || loading.value || !canReconcile.value) return
  const session = cloudSession()
  if (!session) return
  const revision = sequence, identity = captureComputeIdentity()
  stopPolling(); checking.value = true
  if (manual) error.value = ''
  try {
    const result = await reconcilePublication(props.kind, props.id, session)
    if (!current(revision, identity)) return
    state.value = result; error.value = ''; cloudAuthRequired.value = false
    if (result.receiptState === 'PUBLISHED' && result.publishState === 'PUBLISHED') {
      error.value = ''; ElMessage.success(uiText('已核实发布成功'))
    } else if (['FAILED', 'NOT_FOUND'].includes(result.receiptState)) {
      error.value = result.receiptMessage || uiText('未确认发布成功，不会自动重传')
    }
  } catch (e) { if (current(revision, identity)) authFailure(e, session) }
  finally { if (revision === sequence && !disposed) { checking.value = false; schedulePoll() } }
}
async function resume() {
  if (disposed || busy.value || loading.value || !canResume.value) return
  const revision = sequence, identity = captureComputeIdentity()
  stopPolling(); confirming.value = true
  try {
    await ElMessageBox.confirm(uiText('将使用原请求继续提交，可能重新传输文件。确认将该资源公开发布到开发广场吗？'), uiText('确认继续提交'), { confirmButtonText: uiText('确认继续提交'), cancelButtonText: uiText('取消'), type: 'warning' })
  } catch { confirming.value = false; schedulePoll(); return }
  confirming.value = false
  if (!current(revision, identity) || busy.value || !canResume.value) return
  const session = cloudSession()
  if (!session) return
  stopPolling(); submitting.value = true; error.value = ''; cloudAuthRequired.value = false
  try {
    const result = await resumePublication(props.kind, props.id, session)
    if (!current(revision, identity)) return
    state.value = result; pollAttempt = 0
    if (confirmedPublication(result)) ElMessage.success(uiText('已发布到开发广场'))
    else if (!pendingPublication(result)) error.value = result.receiptMessage || result.error || uiText('尚未确认发布成功，请核实状态')
  } catch (e) {
    if (!current(revision, identity)) return
    if (Number(e.code) !== 4401) state.value = { ...state.value, publishState: 'UNCERTAIN', receiptState: undefined }
    authFailure(e, session)
  } finally { if (revision === sequence && !disposed) { submitting.value = false; schedulePoll() } }
}
function visibilityChanged() { if (pending.value && !busy.value) schedulePoll() }
document.addEventListener('visibilitychange', visibilityChanged)
watch([() => props.kind, () => props.id], () => {
  stopPolling(); sequence++; visible.value = false; state.value = null; cloudAuthRequired.value = false
  submitting.value = false; checking.value = false; starring.value = false; confirming.value = false; pollAttempt = 0; load()
}, { immediate: true })
onBeforeUnmount(() => { disposed = true; sequence++; stopPolling(); document.removeEventListener('visibilitychange', visibilityChanged) })
</script>
<style scoped>
.resource-name { font-size: 16px; font-weight: 600; overflow-wrap: anywhere; }
.featured-publish-actions { display: inline-flex; align-items: center; gap: 12px; white-space: nowrap; }
.featured-publish-actions > .el-button { margin-left: 0; }
.publish-note { color: #556479; font-size: 13px; line-height: 1.8; }
.published-id { color: #7b8799; font-size: 12px; overflow-wrap: anywhere; }
</style>
