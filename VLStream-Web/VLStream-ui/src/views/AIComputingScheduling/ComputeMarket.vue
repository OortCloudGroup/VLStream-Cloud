<template>
  <section class="compute-market" v-loading="loading">
    <div class="heading"><div><h2>{{ $tp('算力市场') }}</h2><p>{{ $tp('平台提供的已购算力；服务器与 GPU 配额分别管理，库存及环境状态以最近检查为准。') }}</p></div><el-button @click="load">{{ $tp('刷新') }}</el-button></div>
    <el-alert type="info" :closable="false" :title="$tp('关机不保证停止计费。厂商 GPU 库存未知时，启动可能失败；开机后需重新检查训练环境。')" />
    <el-alert type="warning" :closable="false" :title="$tp('收费实例由开机用户承担整台实例直到关机确认的费用，闲置也计费；当前收费接入未完成，暂不可启动收费实例。')" />
    <el-empty v-if="!resources.length" :description="$tp('暂无平台发布的算力，不代表已接入自有 P100 服务器')" />
    <div class="cards">
      <el-card v-for="item in resources" :key="item.id" :class="{ selected: item.id === route.query.nodeId }">
        <template #header><strong>{{ item.name }}</strong> <el-tag>{{ item.billingType === 'FREE' ? $tp('免费') : $tp('收费') }}</el-tag></template>
        <p><el-tag :type="item.claimState === 'MINE' ? 'success' : item.claimState === 'FREE' ? 'info' : 'warning'">{{ $tp(claimLabels[item.claimState] || '隔离') }}</el-tag> · {{ $tp('整实例独享，多张GPU共一个占用者') }}</p>
        <p>{{ providers[item.provider] || item.provider }} · {{ item.gpuModel || $tp('GPU 规格未登记') }}</p>
        <p>{{ $tp('服务器') }}: {{ item.serverGroup || $tp('未登记') }} · GPU {{ item.gpuIndex }}</p>
        <p>{{ $tp('运行状态') }}: {{ $tp(states[item.powerState] || '未知') }}</p>
        <p>{{ $tp('厂商容量') }}: {{ $tp(states[item.capacityState] || '未知') }}</p>
        <p v-if="item.capacitySource">{{ $tp('状态来源') }}: {{ item.capacitySource === 'OPERATOR_CONSOLE' ? $tp('管理员控制台观察') : $tp('厂商接口') }} · {{ item.capacityCheckedAt }}</p>
        <p>{{ $tp('环境检查') }}: {{ $tp(states[item.healthState] || '待检查') }}</p>
        <p v-if="item.healthState !== 'READY'">{{ $tp('环境检查有效期五分钟；运行实例可刷新检查，关机实例请先开机。') }}</p>
        <p v-if="item.providerGpuAmount != null">{{ $tp('实例规格 GPU 配额') }}: {{ item.providerGpuAmount }} {{ $tp('（不代表空闲 GPU 或库存）') }}</p>
        <p v-if="item.actualStoppedMode">{{ $tp('厂商实际停机模式') }}: {{ item.actualStoppedMode }}</p>
        <p v-if="item.message">{{ item.message }}</p>
        <p v-if="item.taskCount != null">{{ $tp('当前任务数') }}: {{ item.taskCount }} · {{ $tp('预计排队') }}: {{ $tp(item.waitingEstimate) }}</p>
        <el-button v-if="item.canClaim" :loading="operating === item.id" @click="claim(item)">{{ $tp('占用整实例') }}</el-button>
        <el-button v-if="item.claimState === 'MINE'" :loading="operating === item.id" @click="release(item)">{{ $tp('撤销占用 / 申请安全复用') }}</el-button>
        <p v-if="item.claimState === 'ISOLATED'">{{ $tp('实例身份或工作区保障未就绪；产物可靠回存并验证安全清理前不再分配。') }}</p>
        <p v-if="item.billingType === 'POINTS'">{{ item.pointsPerHour ?? '—' }} {{ $tp('额度参考值/小时') }} · {{ $tp('实际成本 ×2 的售价与额度换算待确认，暂不可训练') }}</p>
        <el-button v-if="item.canOperate" :loading="operating === item.id" :disabled="item.operationPending || item.capacityState === 'UNAVAILABLE' || item.provider === 'self' || item.powerState === 'RUNNING'" @click="power(item, 'start')">{{ $tp(item.apiControl ? '启动实例' : '开机指引') }}</el-button>
        <el-button v-if="item.canOperate" :disabled="item.operationPending || item.taskCount > 0 || item.provider === 'self' || item.powerState === 'STOPPED'" :loading="operating === item.id" @click="power(item, 'stop')">{{ $tp(item.apiControl ? '停止实例' : '关机指引') }}</el-button>
        <el-button v-if="item.canQueryProvider && item.apiControl" :loading="operating === item.id" @click="refresh(item)">{{ $tp('查询厂商状态') }}</el-button>
        <el-button v-if="item.canQueryProvider && item.operationPending && item.apiControl" :loading="operating === item.id" @click="refresh(item, true)">{{ $tp('恢复挂起记录') }}</el-button>
        <el-button v-if="item.canManage" @click="router.push({ path: '/compute-management', query: route.query })">{{ $tp('检查连接与环境') }}</el-button>
        <el-button v-if="item.canOperate" :disabled="item.operationPending || item.powerState !== 'RUNNING'" :loading="operating === item.id" @click="health(item)">{{ $tp('刷新环境检查') }}</el-button>
      </el-card>
    </div>
    <el-button v-if="route.query.returnTo === 'training'" type="primary" @click="router.push('/algorithm-training')">{{ $tp('返回训练配置') }}</el-button>
  </section>
</template>
<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { listComputeMarket, requestPublicComputePower, claimComputeInstance, releaseComputeClaim, refreshPublicComputeProviderState, refreshComputeHealth } from '@/api/compute'
import { translatePhrase as uiText } from '@/i18n'
import { requireComputePlatformAuthorization, isComputeAuthorizationCurrent, bindComputeDraftToActor } from '@/utils/computePlatformAuth'
const route = useRoute(), router = useRouter()
const resources = ref([]), loading = ref(false), operating = ref('')
const providers = { autodl: 'AutoDL', aliyun: '阿里云 EGS', self: '自有服务器' }
const claimLabels = { FREE: '空闲', MINE: '我的占用', OTHER: '他人占用', ISOLATED: '隔离' }
const states = { UNKNOWN: '未知', RUNNING: '运行中', STOPPED: '已关机', STARTING: '启动中', STOPPING: '关机中', UNAVAILABLE: '无 GPU 库存', AVAILABLE_HINT: '可尝试启动', READY: '可训练', ERROR: '检查未通过', STALE: '检查已过期', CHECKING: '检查中' }
const load = async () => { loading.value = true; try { resources.value = (await listComputeMarket()).data || [] } finally { loading.value = false } }
const power = async (item, action) => {
  const needsAuthorization = true
  const proof = needsAuthorization ? await requireComputePlatformAuthorization(async context => { bindComputeDraftToActor(context); await router.replace({ query: { ...route.query, nodeId: item.id } }) }) : null
  if (needsAuthorization && !proof) return
  if (item.apiControl && !await ElMessageBox.confirm(uiText('将向厂商申请实例启停，可能产生费用或启动失败；停止不保证停止计费。请确认已核对实例与停机模式。'), item.name, { type: 'warning' }).then(() => true).catch(() => false)) return
  if (proof && !isComputeAuthorizationCurrent(proof)) { await ElMessageBox.alert(uiText('登录账户已变化，请重新选择操作'), uiText('平台授权')); return }
  operating.value = item.id
  try { const response = await requestPublicComputePower(item.id, action, proof?.platformToken); await ElMessageBox.alert(response.data, uiText('实例操作说明')); await load() }
  catch (error) { await ElMessageBox.alert(uiText(error.data?.msg || error.data?.message || '实例操作未完成，请刷新状态后重试'), uiText('实例操作说明')) }
  finally { operating.value = '' }
}
const claim = async item => {
  const proof = await requireComputePlatformAuthorization(context => { bindComputeDraftToActor(context); return router.replace({ query: { ...route.query, nodeId: item.id } }) })
  if (!proof || !isComputeAuthorizationCurrent(proof)) return
  operating.value = item.id
  try { const response = await claimComputeInstance(item.id, proof.platformToken); await ElMessageBox.alert(response.data, uiText('整实例占用')); await load() }
  catch (error) { await ElMessageBox.alert(uiText(error.data?.msg || error.data?.message || '占用未完成，实例状态可能已变化，请刷新后重试'), uiText('整实例占用')); await load() }
  finally { operating.value = '' }
}
const release = async item => {
  const proof = await requireComputePlatformAuthorization()
  if (!proof || !isComputeAuthorizationCurrent(proof)) return
  if (!await ElMessageBox.confirm(uiText('未使用的工作空间可撤销占用；已训练而安全清理未验证的实例将隔离，不会删除现存文件或交给下一用户。'), uiText('安全复用'), { type: 'warning' }).then(() => true).catch(() => false)) return
  if (!isComputeAuthorizationCurrent(proof)) return
  operating.value = item.id
  try { const response = await releaseComputeClaim(item.id, proof.platformToken); await ElMessageBox.alert(response.data, uiText('安全复用')); await load() }
  catch (error) { await ElMessageBox.alert(uiText(error.data?.msg || error.data?.message || '撤销未完成，请确认任务与回存状态后重试'), uiText('安全复用')); await load() }
  finally { operating.value = '' }
}
const refresh = async (item, recover = false) => {
  const proof = await requireComputePlatformAuthorization()
  if (!proof || !isComputeAuthorizationCurrent(proof)) return
  if (recover && !await ElMessageBox.confirm(uiText('仅在十分钟后，厂商查询明确返回运行或停止终态时，恢复本地挂起记录；不会重发启停，请先核对厂商控制台。'), item.name, { type: 'warning' }).then(() => true).catch(() => false)) return
  operating.value = item.id
  if (!isComputeAuthorizationCurrent(proof)) { operating.value = ''; return }
  try { const response = await refreshPublicComputeProviderState(item.id, recover, proof.platformToken); await ElMessageBox.alert(response.data, uiText('厂商状态查询')); await load() }
  catch (error) { await ElMessageBox.alert(uiText(error.data?.msg || error.data?.message || '厂商状态查询未完成，请刷新后重试'), uiText('厂商状态查询')); await load() }
  finally { operating.value = '' }
}
const health = async item => {
  const proof = await requireComputePlatformAuthorization()
  if (!proof || !isComputeAuthorizationCurrent(proof)) return
  operating.value = item.id
  try { const response = await refreshComputeHealth(item.id, proof.platformToken); await ElMessageBox.alert(response.data, uiText('环境检查')); await load() }
  catch (error) { await ElMessageBox.alert(uiText(error.data?.msg || error.data?.message || '检查未完成，请刷新实例状态后重试'), uiText('环境检查')); await load() }
  finally { operating.value = '' }
}
let timer
onMounted(() => { load(); timer = setInterval(() => { if (!loading.value && !operating.value) load() }, 15000) })
onUnmounted(() => clearInterval(timer))
</script>
<style scoped>
.compute-market { padding: 24px; width: 100%; overflow: auto; box-sizing: border-box; }
.heading { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.heading p { color: #667085; }
.cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 20px; margin: 24px 0; }
.cards p { font-size: 14px; line-height: 1.6; color: #667085; }
.selected { border: 2px solid #409eff; }
</style>
