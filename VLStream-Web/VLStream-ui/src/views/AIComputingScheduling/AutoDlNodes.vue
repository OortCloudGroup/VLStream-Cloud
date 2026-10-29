<template>
  <section class="cloud-compute" v-loading="loading">
    <header class="cloud-heading">
      <h2>{{ $tp('算力管理') }}</h2>
      <div class="heading-actions">
        <el-button v-if="route.query.returnTo === 'training'" @click="router.push('/algorithm-training')">{{ $tp('返回训练配置') }}</el-button>
        <el-button @click="showSetup = true"><el-icon class="guide-help-icon" aria-hidden="true"><QuestionFilled /></el-icon><span>{{ $tp('接入指南') }}</span></el-button>
        <el-button :icon="Refresh" :loading="loading" @click="load">{{ $tp('刷新') }}</el-button>
        <el-button v-if="access" type="primary" :icon="Plus" @click="edit()">{{ $tp('接入服务器') }}</el-button>
      </div>
    </header>
    <el-alert v-if="managementError" class="management-notice" show-icon type="warning" :closable="false" :title="managementError" />
    <el-alert v-if="permissionError" class="management-notice" show-icon type="warning" :closable="false" :title="$tp('权限信息未就绪，请刷新重试')" />
    <p class="management-intro">{{ $tp('接入你自行购买或搭建的 GPU 服务器；平台统一供给请在算力市场和训练配置中选择。移除旧自有记录前，须确认其已转为平台统一供给。') }}</p>
    <el-table :data="nodes" class="cloud-table" :empty-text="$tp(loading ? '加载中' : '暂无接入实例')">
      <el-table-column :label="$tp('实例')" min-width="210"><template #default="{ row }">
        <div class="instance-name">{{ row.name }}</div>
        <div v-if="row.configurationMigrationPending" class="instance-meta">{{ $tp('旧记录身份尚未核对，暂不可移除') }}</div>
        <div v-if="row.configurationReplaced" class="instance-meta">{{ $tp('已转为平台统一供给，可移除旧接入记录') }}</div>
        <div class="instance-meta">{{ $tp(providerLabels[row.provider] || row.provider) }}<span v-if="row.resourceScope === 'PLATFORM'"> · {{ $tp('平台供给') }}</span><span v-else-if="row.companyManaged"> · {{ $tp('公司算力') }}</span></div>
      </template></el-table-column>
      <el-table-column :label="$tp('SSH 地址')" min-width="180"><template #default="{ row }"><span class="connection-address">{{ row.host }}:{{ row.port }}</span></template></el-table-column>
      <el-table-column :label="$tp('环境检查')" min-width="125"><template #default="{ row }"><el-tag :type="row.probeState === 'READY' ? 'success' : row.probeState === 'ERROR' ? 'danger' : 'info'" effect="plain" size="small">{{ $tp(row.companyManaged ? '待配置' : labels[row.probeState] || '待检查') }}</el-tag></template></el-table-column>
      <el-table-column :label="$tp('最近检查的资源')" min-width="200"><template #default="{ row }">
        <span v-if="details(row).gpu">{{ details(row).gpu }}</span><span v-else>—</span>
        <div v-if="details(row).diskFreeGb != null" class="instance-meta">{{ $tp('可用磁盘') }} {{ details(row).diskFreeGb }} GB</div>
      </template></el-table-column>
      <el-table-column :label="$tp('使用状态')" width="100"><template #default="{ row }"><span class="enabled-state" :class="{ muted: !row.enabled }">{{ $tp(row.companyManaged ? '待配置' : row.enabled ? '已启用' : '已停用') }}</span></template></el-table-column>
      <el-table-column :label="$tp('操作')" min-width="285"><template #default="{ row }"><span v-if="row.configurationMigrationPending">{{ $tp('请管理员核对节点身份') }}</span><el-button v-else-if="row.configurationReplaced && canRemoveNode(row)" link type="danger" @click="remove(row)">{{ $tp('移除旧接入记录') }}</el-button><el-button v-else-if="row.companyManaged" link type="primary" @click="companyConfigurationRow = row">{{ $tp('配置详情') }}</el-button><template v-else-if="!row.configurationReplaced && !row.configurationManaged"><div class="row-actions">
        <el-button v-if="canEditNode(row)" link type="primary" :loading="probing === row.id" @click="probe(row)">{{ $tp('检查环境') }}</el-button>
        <el-button v-if="canEditNode(row)" link @click="edit(row)">{{ $tp('编辑') }}</el-button>
        <el-button v-if="row.probeJson" link @click="viewProbe(row)">{{ $tp('检查结果') }}</el-button>
        <el-popover v-if="showAdvanced(row)" placement="bottom-end" trigger="click" :width="260">
          <template #reference><el-button link :aria-label="$tp('高级操作') + ' · ' + row.name">{{ $tp('高级') }}<el-icon><ArrowDown /></el-icon></el-button></template>
          <div class="advanced-panel">
            <strong>{{ row.name }}</strong>
            <template v-if="providerActions(row).supported && (hasPermission('vls:compute:configure') || hasPermission('vls:compute:power'))">
              <div class="advanced-label">{{ $tp('厂商 API') }}</div>
              <el-button v-if="providerActions(row).configure" link type="primary" @click="configureProvider(row)">{{ $tp('厂商接口配置') }}</el-button>
              <el-button v-if="providerActions(row).query" link type="primary" :loading="saving" @click="refreshProvider(row)">{{ $tp('查询厂商状态') }}</el-button>
              <p v-if="!providerActions(row).configure && !providerActions(row).query">{{ $tp('厂商 API 未就绪或无操作权限') }}</p>
            </template>
            <template v-if="canManageSupply(row)">
              <div class="advanced-label">{{ $tp('平台供给管理') }}</div>
              <el-button link type="primary" @click="offer(row)">{{ $tp('发布供给') }}</el-button>
              <el-button link type="primary" @click="openPublicIdentity(row)">{{ $tp('公共实例身份') }}</el-button>
            </template>
          </div>
        </el-popover>
        <el-button v-if="canEditNode(row)" link type="danger" @click="remove(row)">{{ $tp('移除') }}</el-button>
      </div></template></template></el-table-column>
    </el-table>
    <el-dialog v-model="providerVisible" :title="$tp('厂商接口配置') + ' · ' + (selectedProvider?.name || '')" width="min(620px, calc(100vw - 32px))" destroy-on-close @closed="clearProviderSecrets">
      <el-alert :closable="false" type="warning" :title="$tp('保存不会调用厂商。默认仅手工管理，API 需管理员启用服务端控制开关；凭据只提交后端加密保存，留空保留，更换实例或产品须重填。')" />
      <el-form label-width="150px"><el-form-item :label="$tp('控制方式')"><el-select v-model="providerForm.controlMode"><el-option :label="$tp('厂商控制台手工管理')" value="MANUAL" /><el-option :label="$tp('已验证产品 API')" value="API" /></el-select></el-form-item>
        <el-form-item :label="$tp('产品')"><el-select v-model="providerForm.providerProduct"><el-option v-if="selectedProvider?.provider === 'autodl'" label="AutoDL 容器 Pro（普通容器不支持）" value="AUTODL_PRO" /><el-option v-else label="阿里云 ECS / EGS" value="ECS" /></el-select></el-form-item>
        <el-form-item :label="$tp('厂商实例 ID')"><el-input v-model="providerForm.providerInstanceId" maxlength="100" /></el-form-item>
        <template v-if="providerForm.providerProduct === 'ECS'"><el-form-item label="RegionId"><el-input v-model="providerForm.providerRegion" maxlength="100" /></el-form-item><el-form-item :label="$tp('停止时请求模式')"><el-select v-model="providerForm.requestedStoppedMode"><el-option :label="$tp('KeepCharging：保留计算资源并继续计费')" value="KeepCharging" /><el-option :label="$tp('StopCharging：回收计算资源，重启可能无库存')" value="StopCharging" /></el-select></el-form-item><el-alert :closable="false" type="warning" :title="$tp('StopCharging 不停止磁盘等费用；厂商可能忽略不支持的模式，以查询到的实际 StoppedMode 为准。公网 IP 可能改变，需要更新 SSH 接入。')" /><el-form-item label="AccessKey ID"><el-input v-model="providerForm.accessKeyId" type="password" autocomplete="new-password" /></el-form-item><el-form-item label="AccessKey Secret"><el-input v-model="providerForm.accessKeySecret" type="password" autocomplete="new-password" /></el-form-item></template>
        <el-form-item v-else label="AutoDL Token"><el-input v-model="providerForm.token" type="password" autocomplete="new-password" /></el-form-item><el-form-item :label="$tp('清除已存凭据')"><el-checkbox v-model="providerForm.clearCredentials">{{ $tp('仅手工模式可清除') }}</el-checkbox></el-form-item>
      </el-form><template #footer><el-button @click="providerVisible = false">{{ $tp('取消') }}</el-button><el-button type="primary" :loading="saving" @click="saveProvider">{{ $tp('保存配置') }}</el-button></template>
    </el-dialog>
    <el-dialog v-model="showSetup" :title="$tp('服务器接入指南')" width="min(960px, calc(100vw - 32px))">
      <ComputeOnboardingGuide />
      <template #footer><el-button @click="showSetup = false">{{ $tp('关闭') }}</el-button><el-button v-if="access" type="primary" @click="showSetup = false; edit()">{{ $tp('已准备好，接入服务器') }}</el-button></template>
    </el-dialog>
    <el-dialog v-model="visible" class="connection-dialog" :title="editingId ? $tp('编辑实例') : $tp('接入服务器')" width="min(640px, calc(100vw - 32px))" destroy-on-close @closed="clearPassword">
      <el-form ref="formRef" class="connection-fields" :model="form" :rules="rules" label-width="120px">
        <el-form-item :label="$tp('实例名称')" prop="name"><el-input v-model="form.name" maxlength="100" :placeholder="$tp('例如：我的 GPU 训练服务器')" /></el-form-item>
        <el-form-item :label="$tp('厂商')"><el-select v-model="form.provider"><el-option label="AutoDL" value="autodl" /><el-option label="阿里云 EGS" value="aliyun" /></el-select></el-form-item>
        <el-form-item :label="$tp('服务器标识')"><el-input v-model="form.serverGroup" maxlength="100" /></el-form-item>
        <el-form-item :label="$tp('GPU 型号')"><el-input v-model="form.gpuModel" maxlength="100" /></el-form-item>
        <el-form-item :label="$tp('粘贴 SSH 命令')"><el-input v-model="sshCommand" placeholder="ssh -p 12345 root@connect.example.autodl.com" @change="parseSsh" /><div class="field-help">{{ $tp('可从服务器控制台复制；自建服务器填写实际 SSH 地址、端口和账号。') }}</div></el-form-item>
        <el-form-item :label="$tp('SSH 主机')" prop="host"><el-input v-model="form.host" :placeholder="$tp('填写主机名，不包含端口')" /></el-form-item>
        <el-form-item :label="$tp('SSH 端口')" prop="port"><el-input-number v-model="form.port" :min="1" :max="65535" /></el-form-item>
        <el-form-item :label="$tp('用户名')" prop="username"><el-input v-model="form.username" /></el-form-item>
        <el-form-item :label="$tp('SSH 认证')"><el-select v-model="form.authMode"><el-option :label="$tp('SSH 密码')" value="PASSWORD" /><el-option v-if="canBindDeployment && ['self', 'autodl'].includes(form.provider)" :label="$tp('管理员部署身份')" value="DEPLOYMENT_KEY" /></el-select></el-form-item>
        <el-form-item v-if="form.authMode === 'PASSWORD'" :label="$tp('SSH 密码')"><el-input v-model="form.password" type="password" show-password autocomplete="new-password" :placeholder="editingId && editingAuthMode === 'PASSWORD' ? $tp('已配置，留空保留原密码') : $tp('输入实例 SSH 密码')" /></el-form-item>
        <el-alert v-else :closable="false" type="info" :title="$tp('仅使用管理员在后端明确绑定的自有或私人AutoDL实例身份；这里不上传私钥或填写密钥路径。配置未启用或身份不匹配时，后端会拒绝连接。')" />
        <el-form-item :label="$tp('Python 路径')" prop="pythonPath"><el-input v-model="form.pythonPath" /><div class="field-help">{{ $tp('填写实例中已准备好训练环境的 Python 完整路径。') }}</div></el-form-item>
        <el-form-item :label="$tp('工作目录')" prop="workDir"><el-input v-model="form.workDir" /><div class="field-help">{{ $tp('填写初始化时选择的绝对路径，SSH账号须可读写；平台不会自动安装训练依赖。') }}</div></el-form-item>
        <el-form-item :label="$tp('GPU 编号')"><el-input-number v-model="form.gpuIndex" :min="0" :max="15" /></el-form-item>
        <el-form-item :label="$tp('启用')"><el-switch v-model="form.enabled" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="visible = false">{{ $tp('取消') }}</el-button><el-button v-if="access" type="primary" :loading="saving" @click="save">{{ $tp('保存并检查环境') }}</el-button></template>
    </el-dialog>
    <el-dialog v-model="offerVisible" :title="$tp('发布平台算力') + ' · ' + (selectedOfferNode?.name || '')" width="min(480px, calc(100vw - 32px))">
      <el-form label-width="150px">
        <el-form-item :label="$tp('发布到市场')"><el-switch v-model="offerForm.published" /></el-form-item>
        <el-form-item :label="$tp('调度方式')"><el-select v-model="offerForm.schedulingMode"><el-option :label="$tp('整实例个人独占')" value="INSTANCE_EXCLUSIVE" /><el-option v-if="selectedOfferNode?.provider === 'self'" :label="$tp('公司免费共享GPU任务队列')" value="SHARED_GPU_QUEUE" /></el-select></el-form-item>
        <el-form-item :label="$tp('计费方式')"><el-select v-model="offerForm.billingType" :disabled="offerForm.schedulingMode === 'SHARED_GPU_QUEUE'"><el-option :label="$tp('免费')" value="FREE" /><el-option :label="$tp('New API 额度（待接入）')" value="POINTS" /></el-select></el-form-item>
        <el-form-item v-if="offerForm.billingType === 'POINTS'" :label="$tp('额度参考值/小时')"><el-input-number v-model="offerForm.pointsPerHour" :min="0.01" :precision="2" /></el-form-item>
      </el-form>
      <el-alert :closable="false" type="info" :title="$tp('公司免费共享GPU按任务排队，归档确认后释放本轮资源。只有管理员已授权的公司自有资产可发布；私人和收费云实例仍按整实例个人独占规则管理。')" />
      <el-alert :closable="false" type="warning" :title="$tp('此字段仅作展示参考；实际成本×2及额度换算确认并接入前，不接受收费开机或训练。')" />
      <template #footer><el-button :loading="offerSaving" @click="saveOffer">{{ $tp('保存') }}</el-button></template>
    </el-dialog>
    <el-dialog v-model="publicIdentityVisible" :title="$tp('公共实例身份') + ' · ' + (selectedPublic?.name || '')" width="min(560px, calc(100vw - 32px))">
      <el-alert :closable="false" type="warning" :title="$tp('填写已核实的厂商账户范围与实例ID，同账号同实例的多GPU共用占用锁。不填写Token，不自动购买；历史训练目录无安全清理证据会隔离。')" />
      <el-form label-width="140px"><el-form-item :label="$tp('厂商账户范围')"><el-input v-model="publicIdentityForm.accountScope" maxlength="128" :placeholder="$tp('账号管理标识，不是密钥')" /></el-form-item><el-form-item :label="$tp('实例 ID')"><el-input v-model="publicIdentityForm.instanceId" maxlength="100" :placeholder="$tp('云实例ID或自有服务器库存ID')" /></el-form-item></el-form>
      <template #footer><el-button @click="publicIdentityVisible = false">{{ $tp('取消') }}</el-button><el-button type="primary" :loading="saving" @click="savePublicIdentity">{{ $tp('登记身份') }}</el-button></template>
    </el-dialog>
    <el-dialog v-model="showProbe" :title="$tp('连接与训练环境检查')" width="min(540px, calc(100vw - 32px))">
      <el-alert :type="probeResult?.probeState === 'READY' ? 'success' : 'warning'" :closable="false" :title="probeResult?.probeState === 'READY' ? $tp('实例可用于训练') : $tp('环境尚未就绪，请核对连接与依赖')" />
      <el-descriptions v-if="probeResult" :column="1" border class="cloud-table">
        <el-descriptions-item label="GPU">{{ details(probeResult).gpu || $tp('未检测到可用 GPU') }}</el-descriptions-item>
        <el-descriptions-item label="PyTorch">{{ details(probeResult).torch || $tp('未读取到') }}</el-descriptions-item>
        <el-descriptions-item label="Torchvision">{{ details(probeResult).torchvision || $tp('未读取到') }}</el-descriptions-item>
        <el-descriptions-item label="Ultralytics">{{ details(probeResult).ultralytics || $tp('未读取到') }}</el-descriptions-item>
        <el-descriptions-item label="Python">{{ details(probeResult).python || $tp('未读取到') }}</el-descriptions-item>
        <el-descriptions-item :label="$tp('CUDA 运行库')">{{ details(probeResult).cudaVersion || $tp('未读取到') }}</el-descriptions-item>
        <el-descriptions-item :label="$tp('说明')">{{ details(probeResult).message || $tp('请按接入指南初始化，然后重新检查。') }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="details(probeResult || {}).checks || []" border>
        <el-table-column prop="name" :label="$tp('检查项')" />
        <el-table-column :label="$tp('结果')" width="90"><template #default="{ row }">{{ row.passed ? $tp('通过') : $tp('未通过') }}</template></el-table-column>
        <el-table-column prop="message" :label="$tp('说明')" />
      </el-table>
    </el-dialog>
    <CompanyComputeConfiguration v-if="companyConfigurationRow" :node="companyConfigurationRow" @closed="companyConfigurationRow = null" @updated="load" />
  </section>
</template>

<script setup>
import CompanyComputeConfiguration from '@/components/CompanyComputeConfiguration.vue'
import { translatePhrase as uiText } from '@/i18n'

import { onMounted, onBeforeUnmount, reactive, ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ComputeOnboardingGuide from './ComputeOnboardingGuide.vue'
import { saveOwnComputeNode, probeOwnComputeNode, deleteOwnComputeNode, listComputeNodes, saveComputeNode, probeComputeNode, deleteComputeNode, publishComputeOffer, saveComputeProviderSettings, refreshComputeProviderState, savePublicComputeIdentity, listComputeSelection } from '@/api/compute'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown, Plus, QuestionFilled, Refresh } from '@element-plus/icons-vue'
import { getUserInfo } from '@/api/auth'
import { hasComputePermission, canManageComputeSupply, computeProviderActions } from '@/utils/computeManagementPresentation'
import { captureComputeIdentity } from '@/utils/computeIdentityRequest'
const route = useRoute(), router = useRouter()
const access = ref(null), permissionError = ref(false), capabilities = ref([]), capabilitiesCheckedAt = ref(0)
const providerLabels = { self: '自有服务器', autodl: 'AutoDL', aliyun: '阿里云 EGS' }
const hasPermission = permission => hasComputePermission(access.value, permission)
const ownNode = row => row?.managementOwnership === 'MINE' && row.selfServiceNode === true
const canRemoveNode = row => Boolean(access.value) && !row?.configurationManaged && !row?.configurationMigrationPending && (ownNode(row) || hasPermission('vls:compute:configure'))
const canEditNode = row => row?.provider !== 'self' && !row?.configurationReplaced && canRemoveNode(row)
const canBindDeployment = computed(() => hasPermission('vls:compute:configure') && (access.value?.isAdmin === true || hasPermission('vls:compute:manage-all')))
const canManageSupply = row => !row?.configurationMigrationPending && !row?.configurationReplaced && !row?.configurationManaged && nodes.value.includes(row) && canManageComputeSupply(access.value, row)
const providerActions = row => computeProviderActions(row, Date.now() - capabilitiesCheckedAt.value < 60000 ? capabilities.value.find(item => item.id === row?.id) : null, access.value)
const showAdvanced = row => canManageSupply(row) || providerActions(row).supported && (hasPermission('vls:compute:configure') || hasPermission('vls:compute:power'))
let pageActive = true, advancedEpoch = 0
const beginAdvanced = () => ({ epoch: ++advancedEpoch, identity: captureComputeIdentity() })
const currentAdvanced = request => {
  if (!pageActive || request.epoch !== advancedEpoch) return false
  try { request.identity.assertCurrent(); return true } catch { return false }
}
onBeforeUnmount(() => { pageActive = false; advancedEpoch++; offerVisible.value = false; nodeLoadEpoch++; window.removeEventListener('modelHubAuthChanged', managementIdentityChanged); window.removeEventListener('storage', managementStorageChanged) })
const offerVisible = ref(false), offerId = ref('')
const offerSaving = ref(false)
let offerSaveEpoch = 0
const offerForm = reactive({ published: true, billingType: 'FREE', pointsPerHour: null, schedulingMode: 'INSTANCE_EXCLUSIVE' })
const selectedOfferNode = computed(() => nodes.value.find(node => String(node.id) === String(offerId.value)))
watch(() => offerForm.schedulingMode, mode => { if (mode === 'SHARED_GPU_QUEUE') { offerForm.billingType = 'FREE'; offerForm.pointsPerHour = null } }, { flush: 'sync' })
watch(offerVisible, visible => { if (!visible) { offerSaveEpoch++; offerSaving.value = false } }, { flush: 'sync' })
const currentOfferRequest = request => {
  if (!offerVisible.value || request.epoch !== offerSaveEpoch || request.id !== offerId.value) return false
  try { request.identity.assertCurrent(); return true } catch { return false }
}
const offer = row => { if (!canManageSupply(row)) return; offerSaveEpoch++; offerSaving.value = false; offerId.value = row.id; Object.assign(offerForm, { published: row.resourceScope === 'PLATFORM', billingType: row.billingType === 'POINTS' ? 'POINTS' : 'FREE', pointsPerHour: row.pointsPerHour, schedulingMode: row.schedulingMode || 'INSTANCE_EXCLUSIVE' }); if (offerForm.schedulingMode === 'SHARED_GPU_QUEUE') { offerForm.billingType = 'FREE'; offerForm.pointsPerHour = null } offerVisible.value = true }
const saveOffer = async () => {
  if (!canManageSupply(selectedOfferNode.value) || !['INSTANCE_EXCLUSIVE', 'SHARED_GPU_QUEUE'].includes(offerForm.schedulingMode)) { ElMessage.warning(uiText('所选节点或调度方式已变化，请重新打开发布配置')); return }
  if (offerForm.schedulingMode === 'SHARED_GPU_QUEUE') {
    if (selectedOfferNode.value.provider !== 'self') { ElMessage.warning(uiText('共享任务队列仅适用于已授权的公司自有服务器')); return }
    offerForm.billingType = 'FREE'; offerForm.pointsPerHour = null
  }
  if (!['FREE', 'POINTS'].includes(offerForm.billingType)) { ElMessage.warning(uiText('计费方式无效，请重新选择')); return }
  const request = { id: offerId.value, epoch: ++offerSaveEpoch, identity: captureComputeIdentity() }
  offerSaving.value = true
  try {
    await publishComputeOffer(request.id, { ...offerForm })
    if (!currentOfferRequest(request)) return
    offerVisible.value = false; await load()
  } catch (error) { if (currentOfferRequest(request)) ElMessage.error(managementFailure(error)) }
  finally { if (currentOfferRequest(request)) offerSaving.value = false }
}

const nodes = ref([])
const companyConfigurationRow = ref(null)
const publicNode = ref(''), publicIdentityVisible = ref(false)
const publicIdentityForm = reactive({ accountScope: '', instanceId: '' })
const selectedPublic = computed(() => nodes.value.find(row => row.id === publicNode.value))
const openPublicIdentity = row => {
  if (!canManageSupply(row)) return
  advancedEpoch++; saving.value = false
  publicNode.value = row.id
  Object.assign(publicIdentityForm, { accountScope: '', instanceId: row.providerInstanceId || '' })
  publicIdentityVisible.value = true
}
const savePublicIdentity = async () => {
  if (!canManageSupply(selectedPublic.value)) return
  const request = beginAdvanced(), id = publicNode.value
  saving.value = true
  try {
    await savePublicComputeIdentity(id, { ...publicIdentityForm })
    if (!currentAdvanced(request)) return
    publicIdentityVisible.value = false; await load()
  } catch (error) { if (currentAdvanced(request)) ElMessage.error(managementFailure(error)) }
  finally { if (currentAdvanced(request)) saving.value = false }
}
const providerNode = ref(''), providerVisible = ref(false)
const selectedProvider = computed(() => nodes.value.find(row => row.id === providerNode.value))
const providerForm = reactive({ controlMode: 'MANUAL', providerProduct: null, providerInstanceId: '', providerRegion: '', requestedStoppedMode: null, token: '', accessKeyId: '', accessKeySecret: '', clearCredentials: false })
const clearProviderSecrets = () => { providerForm.token = ''; providerForm.accessKeyId = ''; providerForm.accessKeySecret = '' }
const configureProvider = row => {
  if (!nodes.value.includes(row) || !providerActions(row).configure) return
  advancedEpoch++; saving.value = false
  providerNode.value = row.id
  clearProviderSecrets()
  Object.assign(providerForm, { controlMode: row.controlMode, providerProduct: row.providerProduct, providerInstanceId: row.providerInstanceId, providerRegion: row.providerRegion || '', requestedStoppedMode: row.requestedStoppedMode || null, clearCredentials: false })
  providerVisible.value = true
}
const saveProvider = async () => {
  if (!providerActions(selectedProvider.value).configure) { ElMessage.warning(uiText('操作权限或能力已变化，请刷新后重试')); return }
  const request = beginAdvanced(), id = providerNode.value
  saving.value = true
  try {
    await saveComputeProviderSettings(id, { ...providerForm })
    if (!currentAdvanced(request)) return
    clearProviderSecrets(); providerVisible.value = false; await load(); ElMessage.success(uiText('配置已保存'))
  } catch (error) { if (currentAdvanced(request)) ElMessage.error(managementFailure(error)) }
  finally { if (currentAdvanced(request)) saving.value = false }
}
const refreshProvider = async row => {
  if (!nodes.value.includes(row) || !providerActions(row).query) return
  const request = beginAdvanced(), id = row.id
  saving.value = true
  try {
    const response = await refreshComputeProviderState(id)
    if (!currentAdvanced(request)) return
    ElMessage.info(response.data); await load()
  } catch (error) { if (currentAdvanced(request)) ElMessage.error(managementFailure(error)) }
  finally { if (currentAdvanced(request)) saving.value = false }
}
const loading = ref(false)
const managementError = ref('')
let nodeLoadEpoch = 0, nodeIdentityRetry = 0
const managementFailure = error => uiText(error?.data?.msg || error?.data?.message || (error?.code === 'COMPUTE_IDENTITY_CHANGED' ? '登录账户或租户已变化，请刷新资源' : '算力操作未完成，请核对当前授权与权限'))
let pageIdentity = captureComputeIdentity()
const managementIdentityChanged = () => {
  advancedEpoch++; saving.value = false; access.value = null; capabilities.value = []; capabilitiesCheckedAt.value = 0
  visible.value = false; providerVisible.value = false; publicIdentityVisible.value = false; offerVisible.value = false
  showProbe.value = false; probeResult.value = null; providerNode.value = ''; publicNode.value = ''
  clearPassword(); clearProviderSecrets(); pageIdentity = captureComputeIdentity(); load()
}
const managementStorageChanged = () => { try { pageIdentity.assertCurrent() } catch { managementIdentityChanged() } }
const visible = ref(false)
const saving = ref(false)
const probing = ref('')
const editingId = ref('')
const editingAuthMode = ref('PASSWORD')
const sshCommand = ref('')
const formRef = ref()
const showProbe = ref(false)
const showSetup = ref(false)
const probeResult = ref(null)
const labels = { READY: '可训练', ERROR: '检查未通过', UNTESTED: '待检查' }
const defaults = () => ({ name: '', provider: 'autodl', serverGroup: '', gpuModel: '', host: '', port: 22, username: '', authMode: 'PASSWORD', password: '', pythonPath: '', workDir: '', gpuIndex: 0, enabled: true })
const form = reactive(defaults())
watch(() => form.authMode, mode => { if (mode !== 'PASSWORD') clearPassword() }, { flush: 'sync' })
watch(() => form.provider, provider => { if (!['self', 'autodl'].includes(provider) && form.authMode === 'DEPLOYMENT_KEY') { form.authMode = 'PASSWORD'; clearPassword() } }, { flush: 'sync' })
const rules = Object.fromEntries(['name', 'host', 'username', 'pythonPath', 'workDir'].map(key => [key, [{ required: true, message: '请填写此项', trigger: 'blur' }]]))
const details = row => { try { return JSON.parse(row.probeJson || '{}') } catch { return {} } }
const clearPassword = () => { form.password = '' }
const load = async () => {
  const epoch = ++nodeLoadEpoch, identity = captureComputeIdentity()
  const current = () => { if (!pageActive || epoch !== nodeLoadEpoch) return false; try { identity.assertCurrent(); return true } catch { return false } }
  loading.value = true
  nodes.value = []; access.value = null; capabilities.value = []; capabilitiesCheckedAt.value = 0
  managementError.value = ''; permissionError.value = false
  const results = await Promise.allSettled([listComputeNodes(), getUserInfo(), listComputeSelection()])
  if (!current()) {
    if (pageActive && epoch === nodeLoadEpoch) {
      loading.value = false
      // The layout can finish restoring the current tenant during the first request.
      // Discard that response, then retry once with the newly established identity.
      if (nodeIdentityRetry++ === 0) return load()
      managementError.value = uiText('登录账户或租户已变化，请刷新资源')
    }
    return
  }
  nodeIdentityRetry = 0
  const [nodeResult, userResult, capabilityResult] = results
  if (nodeResult.status === 'fulfilled') nodes.value = Array.isArray(nodeResult.value.data) ? nodeResult.value.data : []
  else managementError.value = managementFailure(nodeResult.reason)
  if (userResult.status === 'fulfilled' && Array.isArray(userResult.value?.data?.permissions)) access.value = userResult.value.data
  else permissionError.value = true
  if (capabilityResult.status === 'fulfilled' && Array.isArray(capabilityResult.value.data)) { capabilities.value = capabilityResult.value.data; capabilitiesCheckedAt.value = Date.now() }
  if (!nodes.value.some(node => node.id === providerNode.value)) { providerNode.value = ''; providerVisible.value = false; clearProviderSecrets() }
  if (!nodes.value.some(node => node.id === publicNode.value)) { publicNode.value = ''; publicIdentityVisible.value = false }
  loading.value = false
}
const edit = async row => {
  if (!access.value || row && !canEditNode(row)) return
  editingId.value = row?.id || ''
  editingAuthMode.value = row?.authMode || 'PASSWORD'
  Object.assign(form, defaults(), row ? { name: row.name, provider: row.provider, serverGroup: row.serverGroup, gpuModel: row.gpuModel, host: row.host, port: row.port, username: row.username, authMode: row.authMode || 'PASSWORD', pythonPath: row.pythonPath, workDir: row.workDir, gpuIndex: row.gpuIndex, enabled: row.enabled } : {})
  sshCommand.value = ''; visible.value = true
}
const parseSsh = value => {
  const match = value.trim().match(/^ssh\s+-p\s+(\d+)\s+([A-Za-z0-9_-]+)@([A-Za-z0-9.-]+)$/)
  if (!match) { ElMessage.warning(uiText('命令格式应为 ssh -p 端口 用户名@主机')); return }
  form.port = Number(match[1]); form.username = match[2]; form.host = match[3]
}
const viewProbe = row => { probeResult.value = row; showProbe.value = true }
const probe = async row => {
  if (!canEditNode(row)) return
  probing.value = row.id
  try { probeResult.value = (await (ownNode(row) ? probeOwnComputeNode : probeComputeNode)(row.id)).data; showProbe.value = true; await load() } catch (error) { ElMessage.error(managementFailure(error)) } finally { probing.value = '' }
}
const save = async () => {
  if (!access.value || saving.value) return
  if (form.provider === 'self') { ElMessage.warning(uiText('自有算力仅在服务 YAML 配置')); return }
  saving.value = true
  try {
    if (!(await formRef.value.validate().catch(() => false))) return
    if (!['PASSWORD', 'DEPLOYMENT_KEY'].includes(form.authMode) || (form.authMode === 'DEPLOYMENT_KEY' && !['self', 'autodl'].includes(form.provider))) { ElMessage.warning(uiText('SSH认证方式无效，部署身份仅适用于管理员明确绑定的自有或私人AutoDL节点')); return }
    if (form.authMode === 'DEPLOYMENT_KEY') clearPassword()
    if (form.authMode === 'PASSWORD' && (!editingId.value || editingAuthMode.value === 'DEPLOYMENT_KEY') && !form.password) { ElMessage.warning(uiText('请输入 SSH 密码')); return }
    const existing = nodes.value.find(node => node.id === editingId.value)
    const ownConnection = !editingId.value || ownNode(existing)
    const submit = ownConnection && form.authMode === 'PASSWORD' ? saveOwnComputeNode : saveComputeNode
    const row = (await submit(editingId.value, { ...form })).data
    clearPassword(); visible.value = false; await load()
    const saved = nodes.value.find(node => node.id === row.id)
    if (saved) await probe(saved)
    else ElMessage.warning(uiText('接入配置已保存，请刷新列表后检查环境'))
  } catch (error) {
    ElMessage.error(managementFailure(error))
  } finally { saving.value = false }
}
const remove = async row => {
  if (!canRemoveNode(row)) return
  try { await ElMessageBox.confirm(uiText(row.configurationReplaced ? '移除旧接入记录？平台统一供给和历史任务仍保留；有排队、训练或回存任务时不能移除。' : '移除此接入记录？服务器及其文件仍保留。'), uiText('移除实例'), { type: 'warning' }) } catch { return }
  try { await (ownNode(row) ? deleteOwnComputeNode : deleteComputeNode)(row.id); await load() } catch (error) { ElMessage.error(managementFailure(error)) }
}
onMounted(() => { window.addEventListener('modelHubAuthChanged', managementIdentityChanged); window.addEventListener('storage', managementStorageChanged); load() })
</script>

<style scoped>
.cloud-compute { padding: 28px; width: 100%; min-width: 0; box-sizing: border-box; overflow: auto; color: var(--el-text-color-primary); }
.cloud-heading { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 16px; margin-bottom: 24px; }
.cloud-heading h2 { margin: 0; font-size: 22px; font-weight: 600; letter-spacing: -.5px; }
.heading-actions { display: flex; flex-wrap: wrap; gap: 8px; }
.heading-actions .el-button, .row-actions .el-button { margin: 0; }
.management-notice { margin-bottom: 16px; }
.management-intro { color: var(--el-text-color-secondary); font-size: 13px; line-height: 1.7; }
.guide-help-icon { margin-right: 6px; font-size: 14px; }
.connection-fields { max-height: 60vh; overflow: auto; padding: 2px; }
.cloud-table { margin: 0 0 20px; border: 1px solid var(--el-border-color-lighter); border-radius: 10px; }
.cloud-table :deep(th.el-table__cell) { background: var(--el-fill-color-lighter); font-weight: 500; }
.cloud-table :deep(.el-table__cell) { padding: 16px 0; }
.instance-name { font-weight: 600; color: var(--el-text-color-primary); overflow-wrap: anywhere; }
.instance-meta { margin-top: 5px; color: var(--el-text-color-secondary); font-size: 12px; line-height: 1.5; }
.connection-address { font-size: 13px; overflow-wrap: anywhere; }
.enabled-state { color: var(--el-color-success-dark-2); font-size: 13px; }
.enabled-state.muted { color: var(--el-text-color-secondary); }
.row-actions { display: flex; align-items: center; flex-wrap: wrap; gap: 8px 12px; }
.row-actions .el-icon { margin-left: 3px; }
.advanced-panel { display: flex; flex-direction: column; align-items: flex-start; gap: 12px; }
.advanced-panel > strong { overflow-wrap: anywhere; font-size: 14px; }
.advanced-panel .el-button { margin: 0; }
.advanced-label { width: 100%; padding-top: 12px; border-top: 1px solid var(--el-border-color-extra-light); color: var(--el-text-color-secondary); font-size: 12px; }
.advanced-panel p { margin: 0; color: var(--el-text-color-secondary); line-height: 1.6; font-size: 12px; }
.field-help { margin-top: 5px; color: var(--el-text-color-secondary); line-height: 1.6; font-size: 12px; }
.setup-command { padding: 12px; background: var(--el-fill-color-lighter); white-space: pre-wrap; overflow-wrap: anywhere; }
@media (max-width: 800px) { .cloud-heading { align-items: flex-start; flex-direction: column; } }
@media (max-width: 640px) { .cloud-compute { padding: 16px; } .cloud-heading h2 { font-size: 20px; } }
</style>
