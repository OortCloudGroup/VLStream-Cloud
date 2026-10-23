<template>
  <el-dialog :model-value="modelValue" :title="$tp('设备共享')" width="min(1080px, 96vw)" :close-on-click-modal="false"
    :show-close="!busy" :close-on-press-escape="!busy" @update:model-value="$emit('update:modelValue', $event)">
    <el-tabs v-model="tab" :before-leave="() => !busy">
      <el-tab-pane :label="$tp('共享设备')" name="directory">
        <div class="sharing-tools">
          <el-input v-model="keyword" :placeholder="$tp('搜索设备名称')" clearable @keyup.enter="search" />
          <el-button @click="search">{{ $tp('搜索') }}</el-button><el-button @click="loadDirectory">{{ $tp('刷新') }}</el-button>
        </div>
        <el-alert v-if="directory.error" :title="directory.error" type="error" :closable="false" />
        <el-table v-loading="directory.loading" :data="directory.rows" row-key="id" :empty-text="$tp('暂无共享设备')">
          <el-table-column prop="deviceName" :label="$tp('设备名称')" min-width="160" />
          <el-table-column :label="$tp('公开范围')" width="110"><template #default="{row}">{{ $tp(row.visibility === 'PUBLIC' ? '公开' : '私有') }}</template></el-table-column>
          <el-table-column :label="$tp('查看权限')" min-width="130"><template #default="{row}">{{ $tp(shareAccessLabels[row.viewerAccess]) }}<el-tag v-if="row.myAccessRequestStatus" size="small">{{ $tp(shareRequestLabels[row.myAccessRequestStatus] || '未知状态') }}</el-tag></template></el-table-column>
          <el-table-column :label="$tp('状态')" min-width="120"><template #default="{row}">{{ $tp(canReadSharedDevice(row) ? ({ONLINE:'在线',OFFLINE:'离线',UNKNOWN:'未知状态'}[row.status] || '未知状态') : '未获查看权限') }}</template></el-table-column>
          <el-table-column :label="$tp('操作')" width="140"><template #default="{row}">
            <el-button v-if="canReadSharedDevice(row)" link type="primary" @click="preview = row">{{ $tp('播放') }}</el-button>
            <el-button v-else-if="canRequestSharedDevice(row)" link type="primary" @click="openRequest(row)">{{ $tp('申请查看') }}</el-button>
            <span v-else>{{ $tp('待审批') }}</span>
          </template></el-table-column>
        </el-table>
        <el-pagination v-model:current-page="page" :page-size="20" :total="directory.total || 0" layout="total, prev, pager, next" @current-change="loadDirectory" />
      </el-tab-pane>
      <el-tab-pane :label="$tp('我的申请')" name="mine">
        <el-button @click="loadMine">{{ $tp('刷新') }}</el-button>
        <el-alert v-if="mine.error" :title="mine.error" type="error" :closable="false" />
        <el-table v-loading="mine.loading" :data="mine.rows" :empty-text="$tp('暂无申请')">
          <el-table-column prop="deviceName" :label="$tp('设备名称')" min-width="150" />
          <el-table-column prop="reason" :label="$tp('申请理由')" min-width="180" />
          <el-table-column :label="$tp('申请状态')" width="120"><template #default="{row}">{{ $tp(shareRequestLabels[row.status] || '未知状态') }}</template></el-table-column>
          <el-table-column prop="updatedAt" :label="$tp('更新时间')" min-width="180" />
        </el-table>
        <p class="sharing-note">{{ $tp('显示当前租户最近的申请记录') }}</p>
      </el-tab-pane>
      <el-tab-pane :label="$tp('共享管理')" name="owner">
        <div class="sharing-tools">
          <el-select v-model="ownerId" :placeholder="$tp('选择所属租户的VLStream设备')" :disabled="busy">
            <el-option v-for="device in ownedVlstream" :key="device.rowId" :value="device.rowId" :label="device.name" />
          </el-select>
          <el-button :disabled="!ownerId || busy" @click="loadOwner">{{ $tp('刷新') }}</el-button>
        </div>
        <el-alert v-if="owner.error" :title="owner.error" type="error" :closable="false" />
        <div v-loading="owner.loading" class="sharing-tools">
          <span>{{ $tp('公开范围') }}：{{ $tp(owner.visibility === 'PUBLIC' ? '公开' : owner.visibility === 'PRIVATE' ? '私有' : '未确认') }}</span>
          <el-button v-if="canManage" :disabled="busy || !['PRIVATE','PUBLIC'].includes(owner.visibility)" @click="changeVisibility">{{ $tp(owner.visibility === 'PUBLIC' ? '设为私有' : '设为公开') }}</el-button>
        </div>
        <p class="sharing-note">{{ $tp('公开设备仅允许已登录租户查看；共享不授予设备控制或管理权限') }}</p>
        <el-alert v-if="queue.error" :title="queue.error" type="error" :closable="false" />
        <el-table v-loading="queue.loading" :data="queue.rows" :empty-text="$tp('暂无待审批申请')">
          <el-table-column prop="requesterTenantId" :label="$tp('申请租户')" min-width="130" />
          <el-table-column prop="requesterUserId" :label="$tp('申请用户')" min-width="130" />
          <el-table-column prop="reason" :label="$tp('申请理由')" min-width="180" />
          <el-table-column :label="$tp('操作')" width="140"><template #default="{row}">
            <el-button v-if="canApprove" link type="primary" :disabled="busy" @click="openDecision(row, 'approve')">{{ $tp('批准') }}</el-button>
            <el-button v-if="canApprove" link type="danger" :disabled="busy" @click="openDecision(row, 'reject')">{{ $tp('拒绝') }}</el-button>
          </template></el-table-column>
        </el-table>
        <template v-if="canApprove">
          <h4>{{ $tp('授权记录') }}</h4>
          <p v-if="owner.visibility === 'PUBLIC'" class="sharing-note">{{ $tp('公开期间，撤销单项授权不会关闭其他已登录租户的公开查看权限') }}</p>
          <div class="sharing-tools">
            <el-select v-model="grantStatus" :disabled="busy" :aria-label="$tp('授权状态')" @change="searchGrants">
              <el-option v-for="(label, value) in { ALL:'全部', ...shareGrantLabels }" :key="value" :value="value" :label="$tp(label)" />
            </el-select>
            <el-button :disabled="!ownerId || busy" @click="loadGrants">{{ $tp('刷新授权记录') }}</el-button>
          </div>
          <el-alert v-if="grants.error" :title="grants.error" type="error" :closable="false" />
          <el-table v-loading="grants.loading" :data="grants.rows" row-key="grantId" :empty-text="$tp('暂无授权记录')">
            <el-table-column prop="requesterTenantId" :label="$tp('申请租户')" min-width="130" />
            <el-table-column prop="requesterUserId" :label="$tp('申请用户')" min-width="130" />
            <el-table-column :label="$tp('授权状态')" width="110"><template #default="{row}">{{ $tp(shareGrantLabels[row.status]) }}</template></el-table-column>
            <el-table-column prop="approvedAt" :label="$tp('批准时间')" min-width="170" />
            <el-table-column prop="expiresAt" :label="$tp('到期时间')" min-width="170" />
            <el-table-column prop="revokedAt" :label="$tp('撤销时间')" min-width="170" />
            <el-table-column :label="$tp('操作')" width="100"><template #default="{row}"><el-button v-if="row.canRevoke" type="danger" link :disabled="busy" @click="revoke(row)">{{ $tp('撤销') }}</el-button></template></el-table-column>
          </el-table>
          <el-pagination v-model:current-page="grantPage" :page-size="20" :total="grants.total || 0" layout="total, prev, pager, next" @current-change="loadGrants" />
        </template>
      </el-tab-pane>
    </el-tabs>
    <el-alert v-if="actionError" :title="actionError" type="error" :closable="false" />
    <el-dialog :model-value="!!requestDevice" :title="$tp('申请查看')" width="min(560px, 94vw)" append-to-body :close-on-click-modal="false" :show-close="!busy" @close="!busy && (requestDevice = null)">
      <p data-i18n-ignore>{{ requestDevice?.deviceName }}</p><p>{{ $tp('申请将提交给设备所属租户审批') }}</p>
      <el-alert v-if="actionError" :title="actionError" type="error" :closable="false" />
      <el-input v-model="reason" type="textarea" :maxlength="200" show-word-limit :rows="4" :placeholder="$tp('请填写申请理由')" :disabled="busy" />
      <div class="sharing-phrases"><el-button v-for="phrase in phrases" :key="phrase" text :disabled="busy" @click="reason = $tp(phrase)">{{ $tp(phrase) }}</el-button></div>
      <template #footer><el-button :disabled="busy" @click="requestDevice = null">{{ $tp('取消') }}</el-button><el-button type="primary" :loading="busy" @click="submitRequest">{{ $tp('提交申请') }}</el-button></template>
    </el-dialog>
    <el-dialog :model-value="!!decision" :title="$tp(decisionMode === 'approve' ? '批准查看申请' : '拒绝查看申请')" width="min(480px, 94vw)" append-to-body :close-on-click-modal="false" :show-close="!busy" @close="!busy && (decision = null)">
      <el-alert v-if="actionError" :title="actionError" type="error" :closable="false" />
      <template v-if="decisionMode === 'approve'"><p>{{ $tp('授权仅允许查看视频，不包含设备控制和管理') }}</p><el-input-number v-model="expiresInDays" :min="1" :precision="0" :placeholder="$tp('平台默认期限')" :disabled="busy" /><p>{{ $tp('留空使用平台默认期限，期限上限由平台校验') }}</p></template>
      <el-input v-else v-model="decisionReason" type="textarea" :maxlength="1000" show-word-limit :placeholder="$tp('拒绝理由（可选）')" :disabled="busy" />
      <template #footer><el-button :disabled="busy" @click="decision = null">{{ $tp('取消') }}</el-button><el-button type="primary" :loading="busy" @click="submitDecision">{{ $tp('确认') }}</el-button></template>
    </el-dialog>
    <el-dialog :model-value="!!preview" :title="preview?.deviceName || $tp('共享视频')" width="min(1000px, 96vw)" append-to-body destroy-on-close @close="preview = null">
      <VideoSquareSharedPlayer v-if="preview && modelValue" :key="preview.deviceRef" :device-ref="preview.deviceRef" :api="api" />
    </el-dialog>
  </el-dialog>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { translatePhrase as tp } from '@/i18n'
import { deviceSharingApi } from '@/api/videoDeviceSharing'
import { checkPermi } from '@/utils/wvpPermission'
import { canReadSharedDevice, canRequestSharedDevice, createShareResource, safeOwnerGrantsPage, safeSharedDevice, shareAccessLabels, shareGrantLabels, shareRequestLabels } from '@/utils/videoSquareSharing.mjs'
import VideoSquareSharedPlayer from './VideoSquareSharedPlayer.vue'
const props = defineProps({ modelValue: Boolean, initialOwnerId: { type: String, default: '' }, ownedDevices: { type: Array, default: () => [] }, api: { type: Object, default: () => deviceSharingApi } })
const emit = defineEmits(['update:modelValue', 'changed'])
const tab = ref('directory'), keyword = ref(''), page = ref(1), ownerId = ref(''), busy = ref(false), actionError = ref('')
const directory = ref({rows:[]}), mine = ref({rows:[]}), queue = ref({rows:[]}), owner = ref({}), grants = ref({rows:[]})
const grantPage = ref(1), grantStatus = ref('ALL')
const requestDevice = ref(null), reason = ref(''), decision = ref(null), decisionMode = ref('approve'), expiresInDays = ref(null), decisionReason = ref(''), preview = ref(null)
const phrases = ['工作需要查看', '协助现场排查', '申请临时查看']
const resources = { directory: createShareResource(value => { directory.value = value }), mine: createShareResource(value => { mine.value = value }), queue: createShareResource(value => { queue.value = value }), owner: createShareResource(value => { owner.value = value }), grants: createShareResource(value => { grants.value = value }) }
const ownedVlstream = computed(() => props.ownedDevices.flatMap(device => (device.sources || []).filter(source => source.protocol === 'VLSTREAM' && /^\d+$/.test(String(source.rowId))).map(source => ({rowId:String(source.rowId), name:[device.deviceName,device.deviceId || source.deviceId || source.rowId].filter(Boolean).join(' · ')}))))
const canManage = computed(() => checkPermi(['vlstream:device:share:manage']))
const canApprove = computed(() => checkPermi(['vlstream:device:share:approve']))
const errorText = cause => cause?.response?.data?.msg || cause?.message || '操作失败，请刷新后重试'
async function loadDirectory() {
  return resources.directory.load(async () => {
    const data = (await props.api.directory({pageNum:page.value,pageSize:20,keyword:keyword.value.trim()}))?.data
    if (!Array.isArray(data?.rows) || !Number.isSafeInteger(data.total) || data.total < 0) throw new Error('共享目录响应无效')
    return {rows:data.rows.map(safeSharedDevice),total:data.total}
  })
}
const search = () => { page.value = 1; loadDirectory() }
const loadMine = () => resources.mine.load(async () => { const rows=(await props.api.mine())?.data; if(!Array.isArray(rows))throw new Error('申请记录响应无效'); return {rows} })
function loadGrants() {
  const id = ownerId.value
  if (!id || !canApprove.value || !ownedVlstream.value.some(device => device.rowId === id)) { resources.grants.clear(); return }
  return resources.grants.load(async () => safeOwnerGrantsPage((await props.api.grants(id, {pageNum:grantPage.value,pageSize:20,status:grantStatus.value}))?.data))
}
const searchGrants = () => { grantPage.value=1; loadGrants() }
async function loadOwner() {
  const id = ownerId.value
  if (!id || !ownedVlstream.value.some(device=>device.rowId===id)) { resources.owner.clear(); resources.queue.clear(); resources.grants.clear(); return }
  await Promise.all([
    resources.owner.load(async () => { const device=(await props.api.owner(id))?.data?.device; if(!['PUBLIC','PRIVATE'].includes(device?.visibility))throw new Error('当前服务尚未返回共享配置'); return {visibility:device.visibility} }),
    canApprove.value ? resources.queue.load(async () => { const rows=(await props.api.requests(id))?.data; if(!Array.isArray(rows))throw new Error('审批队列响应无效'); return {rows} }) : Promise.resolve(resources.queue.clear()),
    loadGrants(),
  ])
}
async function action(work) {
  if (busy.value) return
  busy.value = true; actionError.value = ''
  try { await work() } catch(cause) { if(cause!=='cancel' && cause!=='close')actionError.value=errorText(cause) } finally { busy.value=false }
}
function openRequest(device) { requestDevice.value=device; reason.value=''; actionError.value='' }
const submitRequest = () => action(async () => {
  if(!requestDevice.value || !canRequestSharedDevice(requestDevice.value))return
  const result=(await props.api.apply(requestDevice.value.deviceRef,reason.value.trim()))?.data
  if(!['PENDING','PUBLIC','APPROVED'].includes(result?.status))throw new Error('申请响应无效，请刷新状态')
  requestDevice.value=null; await loadDirectory(); emit('changed')
})
function openDecision(row,mode) { decision.value=row; decisionMode.value=mode; expiresInDays.value=null; decisionReason.value=''; actionError.value='' }
const submitDecision = () => action(async () => {
  if(!decision.value || decision.value.status!=='PENDING' || !canApprove.value)return
  const result=(decisionMode.value==='approve' ? await props.api.approve(decision.value.requestId,expiresInDays.value) : await props.api.reject(decision.value.requestId,decisionReason.value.trim()))?.data
  if(result?.status!==(decisionMode.value==='approve'?'APPROVED':'REJECTED'))throw new Error('审批响应无效，请刷新状态')
  decision.value=null; await loadOwner(); emit('changed')
})
const changeVisibility = () => action(async () => {
  const next=owner.value.visibility==='PUBLIC'?'PRIVATE':'PUBLIC'
  await ElMessageBox.confirm(tp(next==='PUBLIC'?'公开后其他已登录租户可以查看设备视频，确认公开？':'设为私有会结束当前共享预览，确认继续？'),tp('更改公开范围'),{type:'warning'})
  const result=(await props.api.visibility(ownerId.value,next))?.data
  if(result?.visibility!==next)throw new Error('共享配置响应无效，请刷新状态')
  await loadOwner(); emit('changed'); ElMessage.success(tp('公开范围已更新'))
})
const revoke = row => action(async () => {
  if(!canApprove.value || row.status!=='ACTIVE' || row.canRevoke!==true || !grants.value.rows.some(item=>item.grantId===row.grantId && item.canRevoke))return
  await ElMessageBox.confirm(tp(owner.value.visibility==='PUBLIC'?'撤销此授权后，该设备仍可通过公开权限查看。确认撤销此授权？':'撤销后该授权将立即失效，确认继续？'),tp('撤销查看授权'),{type:'warning'})
  try {
    const result=(await props.api.revoke(row.grantId))?.data
    if(result?.status!=='REVOKED' || result.grantId!==row.grantId)throw new Error('撤销响应无效，请刷新状态')
  } catch (cause) {
    // A conflict or timeout may mean another actor already changed the grant.
    // Re-read its actual state; never retry the mutation or invent a result.
    await loadGrants()
    throw cause
  }
  await loadOwner(); emit('changed')
})
watch(ownerId,()=>{grantPage.value=1;resources.grants.clear();loadOwner()})
watch(tab, value => { actionError.value=''; if(props.modelValue){if(value==='directory')loadDirectory();else if(value==='mine')loadMine();else loadOwner()} })
const clear = () => { Object.values(resources).forEach(resource=>resource.clear()); requestDevice.value=null; decision.value=null; preview.value=null; actionError.value='' }
watch(()=>props.modelValue,open=>{
  if(!open){clear();return}
  page.value=1
  grantPage.value=1
  if(props.initialOwnerId && ownedVlstream.value.some(device=>device.rowId===props.initialOwnerId)) {
    const sameOwner=ownerId.value===props.initialOwnerId
    ownerId.value=props.initialOwnerId
    if(tab.value!=='owner')tab.value='owner'
    else if(sameOwner)loadOwner()
  }else if(tab.value==='directory')loadDirectory()
  else tab.value='directory'
})
onBeforeUnmount(clear)
</script>

<style scoped>
.sharing-tools { display:flex; gap:12px; align-items:center; margin-block:12px; flex-wrap:wrap; }
.sharing-tools .el-input,.sharing-tools .el-select { width:min(340px,100%); }
.sharing-note { color:#778da2; line-height:1.6; }
.sharing-phrases { display:flex; flex-wrap:wrap; margin-block:8px; }
</style>
