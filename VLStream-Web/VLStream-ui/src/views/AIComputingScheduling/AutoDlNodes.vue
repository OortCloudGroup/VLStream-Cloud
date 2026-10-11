<template>
  <section class="cloud-compute" v-loading="loading">
    <div class="cloud-heading">
      <div><h3>{{ $tp('算力管理') }}</h3><p>{{ $tp('接入自己管理的第三方或自有 GPU；平台供给由有权限的管理员发布。') }}</p></div>
      <div><el-button @click="showSetup = true">{{ $tp('新 GPU 接入指南') }}</el-button><el-button @click="load">{{ $tp('刷新') }}</el-button><el-button type="primary" @click="edit()">{{ $tp('接入实例') }}</el-button></div>
    </div>
    <el-alert type="info" :closable="false" show-icon :title="$tp('实例按当前租户管理。同一实例按顺序训练，完成后将 PT 和类别文件回存平台。实例开关机与费用在 AutoDL 管理。')" />
    <el-table :data="nodes" class="cloud-table" :empty-text="$tp('尚未接入线上实例，点击“接入实例”填写 SSH 信息')">
      <el-table-column prop="name" :label="$tp('实例名称')" min-width="140" />
      <el-table-column prop="provider" :label="$tp('厂商')" width="100" />
      <el-table-column :label="$tp('资源归属')" width="110"><template #default="{ row }">{{ row.resourceScope === 'PLATFORM' ? $tp('平台供给') : $tp('自接算力') }}</template></el-table-column>
      <el-table-column :label="$tp('SSH 地址')" min-width="200"><template #default="{ row }">{{ row.host }}:{{ row.port }}</template></el-table-column>
      <el-table-column :label="$tp('环境检查')" min-width="120"><template #default="{ row }"><el-tag :type="row.probeState === 'READY' ? 'success' : row.probeState === 'ERROR' ? 'danger' : 'info'">{{ $tp(labels[row.probeState]) || row.probeState }}</el-tag></template></el-table-column>
      <el-table-column :label="$tp('最近检查的资源')" min-width="190"><template #default="{ row }"><span v-if="details(row).gpu">{{ details(row).gpu }}<br>{{ $tp('可用磁盘') }} {{ details(row).diskFreeGb }} GB</span><span v-else>—</span></template></el-table-column>
      <el-table-column :label="$tp('使用状态')" width="100"><template #default="{ row }">{{ row.enabled ? $tp('已启用') : $tp('已停用') }}</template></el-table-column>
      <el-table-column :label="$tp('操作')" min-width="300"><template #default="{ row }"><el-button link type="primary" :loading="probing === row.id" @click="probe(row)">{{ $tp('检查连接与环境') }}</el-button><el-button link @click="edit(row)">{{ $tp('编辑') }}</el-button><el-button v-hasPermi="['vls:compute:publish']" link @click="offer(row)">{{ $tp('发布供给') }}</el-button><el-button link type="danger" @click="remove(row)">{{ $tp('移除') }}</el-button></template></el-table-column>
    </el-table>
    <div class="cloud-table"><el-select v-model="providerNode" :placeholder="$tp('选择实例配置厂商接口')"><el-option v-for="node in nodes.filter(n => n.provider !== 'self')" :key="node.id" :label="node.name" :value="node.id" /></el-select><el-button v-hasPermi="['vls:compute:configure']" :disabled="!providerNode" @click="configureProvider">{{ $tp('厂商接口配置') }}</el-button><el-button v-hasPermi="['vls:compute:power']" :disabled="!providerNode" :loading="saving" @click="refreshProvider">{{ $tp('查询厂商状态') }}</el-button><el-button v-hasPermi="['vls:compute:configure']" :disabled="!providerNode" :loading="saving" @click="queryCost">{{ $tp('查询价格参考') }}</el-button></div>
    <el-button v-hasPermi="['vls:compute:configure']" :disabled="!providerNode" @click="openInventory">{{ $tp('Pro库存同步') }}</el-button>
    <el-dialog v-model="inventoryVisible" :title="$tp('AutoDL Pro库存草稿')" width="90%">
      <el-alert :closable="false" type="warning" :title="$tp('需已有后端Pro凭据、稳定账号身份与控制开关。同步仅发现已购实例，不购买、启停或自动上架；未知价格单位和完整停机终态仍禁止收费。')" />
      <p v-if="inventoryError" role="alert">{{ inventoryError }}</p>
      <el-button :loading="inventoryBusy" @click="syncInventory">{{ $tp('分页同步草稿') }}</el-button><el-button :disabled="inventoryBusy" @click="loadInventory">{{ $tp('刷新草稿') }}</el-button>
      <el-table :data="inventoryDrafts" v-loading="inventoryBusy" :empty-text="$tp('尚无库存草稿；请先配置已接入Pro实例并登记公共账号范围，再同步')">
        <el-table-column prop="accountScope" :label="$tp('账号管理引用')" min-width="140" />
        <el-table-column prop="providerInstanceId" :label="$tp('实例ID')" min-width="150" />
        <el-table-column prop="gpuSpec" :label="$tp('规格引用')" min-width="120" />
        <el-table-column prop="gpuAmount" :label="$tp('GPU配额（非库存）')" width="150" />
        <el-table-column :label="$tp('观察状态')" width="100"><template #default="{ row }">{{ $tp(row.powerState === 'RUNNING' ? '运行中' : '未知') }}</template></el-table-column>
        <el-table-column :label="$tp('观察时间')" min-width="150"><template #default="{ row }">{{ row.stateCheckedAt || row.syncedAt || '—' }} {{ row.observationStale ? $tp('已过期') : '' }}</template></el-table-column>
        <el-table-column :label="$tp('接入阶段')" min-width="140"><template #default="{ row }">{{ $tp(row.nodeId ? '已关联，仍待核对' : '待手工接入') }}</template></el-table-column>
        <el-table-column :label="$tp('价格原值 / 单位')" min-width="140"><template #default="{ row }">{{ row.paygPrice ?? '—' }} / {{ $tp('待核实') }}</template></el-table-column>
        <el-table-column :label="$tp('操作')" min-width="200" fixed="right"><template #default="{ row }"><el-button link :disabled="inventoryBusy" @click="refreshDraft(row)">{{ $tp('检查厂商状态') }}</el-button><el-button link @click="draftGuide(row)">{{ $tp('接入指引') }}</el-button></template></el-table-column>
      </el-table>
      <p>{{ $tp('缺失或分页失败不移除旧库存。草稿不会覆盖SSH、环境、公共身份或占用者；每张实际GPU仍按原接入流程管理。') }}</p>
    </el-dialog>
    <el-dialog v-model="costVisible" :title="$tp('厂商价格观察（未启用收费）')" width="560px"><el-alert :closable="false" type="warning" :title="costObservation?.message" /><el-descriptions v-if="costObservation" :column="1" border><el-descriptions-item :label="$tp('来源')">{{ costObservation.source }} / {{ costObservation.priceKind }}</el-descriptions-item><el-descriptions-item :label="$tp('返回金额原值')">{{ costObservation.amount ?? '—' }}</el-descriptions-item><el-descriptions-item :label="$tp('币种 / 单位 / 口径')">{{ costObservation.currency }} / {{ costObservation.amountUnit }} / {{ costObservation.resourceScope }}</el-descriptions-item><el-descriptions-item :label="$tp('组件参考值')">{{ costObservation.components }}</el-descriptions-item></el-descriptions><p>{{ $tp('收费售价为核实后的实例小时算力成本 ×2，按实际开机秒数折算；磁盘、流量等独立费用须单独确认。额度单位和真实扣款未接入前不启动收费训练。') }}</p></el-dialog>
    <el-dialog v-model="providerVisible" :title="$tp('厂商接口配置')" width="620px" destroy-on-close @closed="clearProviderSecrets">
      <el-alert :closable="false" type="warning" :title="$tp('保存不会调用厂商。默认仅手工管理，API 需管理员启用服务端控制开关；凭据只提交后端加密保存，留空保留，更换实例或产品须重填。')" />
      <el-form label-width="150px"><el-form-item :label="$tp('控制方式')"><el-select v-model="providerForm.controlMode"><el-option :label="$tp('厂商控制台手工管理')" value="MANUAL" /><el-option :label="$tp('已验证产品 API')" value="API" /></el-select></el-form-item>
        <el-form-item :label="$tp('产品')"><el-select v-model="providerForm.providerProduct"><el-option v-if="selectedProvider?.provider === 'autodl'" label="AutoDL 容器 Pro（普通容器不支持）" value="AUTODL_PRO" /><el-option v-else label="阿里云 ECS / EGS" value="ECS" /></el-select></el-form-item>
        <el-form-item :label="$tp('厂商实例 ID')"><el-input v-model="providerForm.providerInstanceId" maxlength="100" /></el-form-item>
        <template v-if="providerForm.providerProduct === 'ECS'"><el-form-item label="RegionId"><el-input v-model="providerForm.providerRegion" maxlength="100" /></el-form-item><el-form-item :label="$tp('停止时请求模式')"><el-select v-model="providerForm.requestedStoppedMode"><el-option :label="$tp('KeepCharging：保留计算资源并继续计费')" value="KeepCharging" /><el-option :label="$tp('StopCharging：回收计算资源，重启可能无库存')" value="StopCharging" /></el-select></el-form-item><el-alert :closable="false" type="warning" :title="$tp('StopCharging 不停止磁盘等费用；厂商可能忽略不支持的模式，以查询到的实际 StoppedMode 为准。公网 IP 可能改变，需要更新 SSH 接入。')" /><el-form-item label="AccessKey ID"><el-input v-model="providerForm.accessKeyId" type="password" autocomplete="new-password" /></el-form-item><el-form-item label="AccessKey Secret"><el-input v-model="providerForm.accessKeySecret" type="password" autocomplete="new-password" /></el-form-item></template>
        <el-form-item v-else label="AutoDL Token"><el-input v-model="providerForm.token" type="password" autocomplete="new-password" /></el-form-item><el-form-item :label="$tp('清除已存凭据')"><el-checkbox v-model="providerForm.clearCredentials">{{ $tp('仅手工模式可清除') }}</el-checkbox></el-form-item>
      </el-form><template #footer><el-button @click="providerVisible = false">{{ $tp('取消') }}</el-button><el-button type="primary" :loading="saving" @click="saveProvider">{{ $tp('保存配置') }}</el-button></template>
    </el-dialog>
    <el-select v-model="observedNode" :placeholder="$tp('选择实例登记厂商状态')"><el-option v-for="node in nodes" :key="node.id" :label="node.name" :value="node.id" /></el-select>
    <el-button v-hasPermi="['vls:compute:observe']" :disabled="!observedNode" @click="observationVisible = true">{{ $tp('登记控制台观察') }}</el-button>
    <el-dialog v-model="observationVisible" :title="$tp('登记厂商控制台观察')" width="520px">
      <el-alert type="warning" :closable="false" :title="$tp('仅登记管理员在厂商控制台确认的当前事实，不会执行开关机；未知或查询失败请选择未知。')" />
      <el-form label-width="130px"><el-form-item :label="$tp('运行状态')"><el-select v-model="observation.powerState"><el-option :label="$tp('未知')" value="UNKNOWN" /><el-option :label="$tp('运行中')" value="RUNNING" /><el-option :label="$tp('已关机')" value="STOPPED" /></el-select></el-form-item><el-form-item :label="$tp('厂商容量')"><el-select v-model="observation.capacityState"><el-option :label="$tp('未知')" value="UNKNOWN" /><el-option :label="$tp('可尝试启动（非保证）')" value="AVAILABLE_HINT" /><el-option :label="$tp('厂商明确无库存')" value="UNAVAILABLE" /></el-select></el-form-item><el-form-item :label="$tp('观察依据')"><el-input v-model="observation.note" maxlength="400" :placeholder="$tp('填写观察时间及厂商返回说明，不含密码或 Token')" /></el-form-item></el-form>
      <template #footer><el-button :loading="saving" :disabled="!observation.note.trim()" @click="saveObservation">{{ $tp('保存并重新检查环境') }}</el-button></template>
    </el-dialog>
    <p class="cloud-help">{{ $tp('新实例请先按接入指南安装固定版本依赖并预下载四类基础模型，再检查连接与环境。无需访问项目开发测试 GPU。') }}<a href="https://www.autodl.com/docs/ssh/" target="_blank" rel="noopener noreferrer">{{ $tp('查看 AutoDL SSH 文档') }}</a></p>
    <el-dialog v-model="showSetup" :title="$tp('新 GPU 接入指南')" width="720px">
      <el-alert type="info" :closable="false" :title="$tp('GPU 型号按需选择，下表示例不是型号限制。兼容 standard 的实例可选 PyTorch 2.5.1 / Python 3.12 / CUDA 12.4 基础镜像；接入后仍需环境检查和实际训练验证。')" />
      <p>{{ $tp('使用与平台版本一致的 VLStream 源码，在你购买的新实例终端执行初始化。平台不会从开发测试服务器复制预装环境。') }}</p>
      <p>{{ $tp('下表是初始化后的运行环境。脚本会另建 Python 3.10 环境，不使用基础镜像的 Python 3.12。') }}</p>
      <el-table :data="setupProfiles" border>
        <el-table-column prop="gpu" :label="$tp('GPU 类型')" />
        <el-table-column prop="torch" label="PyTorch / Torchvision" />
        <el-table-column prop="cuda" label="CUDA" width="85" />
        <el-table-column prop="profile" :label="$tp('初始化配置')" width="110" />
      </el-table>
      <p>{{ $tp('基础镜像没有上述组合时先核对可选版本，不要随意混配。两套初始化配置都安装 Ultralytics 8.3.240；首次验收先使用 standard。') }}</p>
      <pre class="setup-command">bash tools/compute/bootstrap-autodl.sh --profile standard</pre>
      <p>{{ $tp('Blackwell 显卡将 standard 改为 blackwell。填写脚本最后输出的 Python 路径和工作目录，检查通过后分别用小样本验证所需训练类型。版本组合不是对所有显卡的实测承诺。') }}</p>
      <p>{{ $tp('实例需有卡开机、可用公网 SSH、足够磁盘，并能访问软件包源和官方基础权重下载地址。初始化不包含 OM/RKNN 转换工具或线上智能标注。') }}</p>
      <p>{{ $tp('操作顺序：购买开机 → 初始化 → 保存实例并检查 → 导入、标注、划分数据集 → 建立训练任务并选择 AutoDL → 训练及回存 → 保存模型并下载校验。首轮建议1轮、batch=2、尺寸320。') }}</p>
    </el-dialog>
    <el-dialog v-model="visible" :title="editingId ? $tp('编辑 AutoDL 实例') : $tp('接入 AutoDL 实例')" width="640px" destroy-on-close @closed="clearPassword">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item :label="$tp('实例名称')" prop="name"><el-input v-model="form.name" maxlength="100" :placeholder="$tp('例如：AutoDL 训练实例')" /></el-form-item>
        <el-form-item :label="$tp('厂商')"><el-select v-model="form.provider"><el-option label="AutoDL" value="autodl" /><el-option label="阿里云 EGS" value="aliyun" /><el-option :label="$tp('自有服务器')" value="self" /></el-select></el-form-item>
        <el-form-item :label="$tp('服务器标识')"><el-input v-model="form.serverGroup" maxlength="100" /></el-form-item>
        <el-form-item :label="$tp('GPU 型号')"><el-input v-model="form.gpuModel" maxlength="100" /></el-form-item>
        <el-form-item :label="$tp('粘贴 SSH 命令')"><el-input v-model="sshCommand" placeholder="ssh -p 12345 root@connect.example.autodl.com" @change="parseSsh" /><div class="field-help">{{ $tp('可从 AutoDL 控制台复制，也可直接填写下方字段。') }}</div></el-form-item>
        <el-form-item :label="$tp('SSH 主机')" prop="host"><el-input v-model="form.host" :placeholder="$tp('填写主机名，不包含端口')" /></el-form-item>
        <el-form-item :label="$tp('SSH 端口')" prop="port"><el-input-number v-model="form.port" :min="1" :max="65535" /></el-form-item>
        <el-form-item :label="$tp('用户名')" prop="username"><el-input v-model="form.username" /></el-form-item>
        <el-form-item :label="$tp('SSH 密码')"><el-input v-model="form.password" type="password" show-password autocomplete="new-password" :placeholder="editingId ? $tp('已配置，留空保留原密码') : $tp('输入实例 SSH 密码')" /></el-form-item>
        <el-form-item :label="$tp('Python 路径')" prop="pythonPath"><el-input v-model="form.pythonPath" /><div class="field-help">{{ $tp('填写实例中已准备好训练环境的 Python 完整路径。') }}</div></el-form-item>
        <el-form-item :label="$tp('工作目录')" prop="workDir"><el-input v-model="form.workDir" /></el-form-item>
        <el-form-item :label="$tp('GPU 编号')"><el-input-number v-model="form.gpuIndex" :min="0" :max="15" /></el-form-item>
        <el-form-item :label="$tp('启用')"><el-switch v-model="form.enabled" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="visible = false">{{ $tp('取消') }}</el-button><el-button type="primary" :loading="saving" @click="save">{{ $tp('保存并检查环境') }}</el-button></template>
    </el-dialog>
    <el-dialog v-model="offerVisible" :title="$tp('发布平台算力')" width="480px">
      <el-form label-width="150px"><el-form-item :label="$tp('发布到市场')"><el-switch v-model="offerForm.published" /></el-form-item><el-form-item :label="$tp('计费方式')"><el-select v-model="offerForm.billingType"><el-option :label="$tp('免费')" value="FREE" /><el-option :label="$tp('New API 额度（待接入）')" value="POINTS" /></el-select></el-form-item><el-form-item v-if="offerForm.billingType === 'POINTS'" :label="$tp('额度参考值/小时')"><el-input-number v-model="offerForm.pointsPerHour" :min="0.01" :precision="2" /></el-form-item></el-form>
      <el-alert :closable="false" type="warning" :title="$tp('此字段仅作展示参考；实际成本×2及额度换算确认并接入前，不接受收费开机或训练。')" />
      <template #footer><el-button :loading="saving" @click="saveOffer">{{ $tp('保存') }}</el-button></template>
    </el-dialog>
    <el-button v-if="route.query.returnTo === 'training'" @click="router.push('/algorithm-training')">{{ $tp('返回训练配置') }}</el-button>
    <div class="cloud-table"><el-select v-model="publicNode" :placeholder="$tp('选择公司实例登记公共身份')"><el-option v-for="node in nodes" :key="node.id" :label="node.name" :value="node.id" /></el-select><el-button v-hasPermi="['vls:compute:publish']" :disabled="!publicNode" @click="publicIdentityVisible = true">{{ $tp('公共实例身份') }}</el-button></div>
    <el-dialog v-model="publicIdentityVisible" :title="$tp('登记公共整实例身份')" width="560px">
      <el-alert :closable="false" type="warning" :title="$tp('填写已核实的厂商账户范围与实例ID，同账号同实例的多GPU共用占用锁。不填写Token，不自动购买；历史训练目录无安全清理证据会隔离。')" />
      <el-form label-width="140px"><el-form-item :label="$tp('厂商账户范围')"><el-input v-model="publicIdentityForm.accountScope" maxlength="128" :placeholder="$tp('账号管理标识，不是密钥')" /></el-form-item><el-form-item :label="$tp('实例 ID')"><el-input v-model="publicIdentityForm.instanceId" maxlength="100" :placeholder="$tp('云实例ID或自有服务器库存ID')" /></el-form-item></el-form>
      <template #footer><el-button @click="publicIdentityVisible = false">{{ $tp('取消') }}</el-button><el-button type="primary" :loading="saving" @click="savePublicIdentity">{{ $tp('登记身份') }}</el-button></template>
    </el-dialog>
    <el-dialog v-model="showProbe" :title="$tp('连接与训练环境检查')" width="540px">
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
  </section>
</template>

<script setup>
import { translatePhrase as uiText } from '@/i18n'

import { onMounted, onBeforeUnmount, reactive, ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listComputeNodes, saveComputeNode, probeComputeNode, deleteComputeNode, publishComputeOffer, recordComputeObservation, saveComputeProviderSettings, refreshComputeProviderState, queryComputeCostObservation, savePublicComputeIdentity, listProviderInventory, syncProviderInventory, refreshProviderInventory } from '@/api/compute'
import { useRoute, useRouter } from 'vue-router'
const route = useRoute(), router = useRouter()
const observedNode = ref(''), observationVisible = ref(false)
const inventorySource = ref(''), inventoryVisible = ref(false), inventoryBusy = ref(false), inventoryError = ref(''), inventoryDrafts = ref([])
let inventoryRequestEpoch = 0
const invalidateInventory = () => {
  inventoryRequestEpoch++
  inventoryBusy.value = false; inventoryError.value = ''; inventoryDrafts.value = []
}
watch([inventoryVisible, inventorySource], invalidateInventory, { flush: 'sync' })
onBeforeUnmount(() => { inventoryVisible.value = false })
const isCurrentInventoryRequest = request => inventoryVisible.value && inventorySource.value === request.source && inventoryRequestEpoch === request.epoch
const runInventoryRequest = async action => {
  if (!inventoryVisible.value || !inventorySource.value) return
  const request = { source: inventorySource.value, epoch: ++inventoryRequestEpoch }
  inventoryBusy.value = true; inventoryError.value = ''
  try { await action(request) } catch (error) {
    if (isCurrentInventoryRequest(request)) inventoryError.value = uiText(error?.data?.msg || error?.data?.message || 'Pro库存请求未完成，旧记录保留；请核对配置并重试')
  } finally {
    if (isCurrentInventoryRequest(request)) inventoryBusy.value = false
  }
}
const readInventory = async request => {
  const response = await listProviderInventory(request.source)
  if (isCurrentInventoryRequest(request)) inventoryDrafts.value = response.data || []
}
const loadInventory = () => runInventoryRequest(readInventory)
const openInventory = async () => { inventorySource.value = providerNode.value; inventoryDrafts.value = []; inventoryVisible.value = true; await loadInventory() }
const syncInventory = () => runInventoryRequest(async request => {
  const response = await syncProviderInventory(request.source)
  if (!isCurrentInventoryRequest(request)) return
  ElMessage.info(response.data); await readInventory(request)
})
const refreshDraft = row => {
  if (!inventoryDrafts.value.includes(row)) return
  const instanceKey = row.instanceKey
  return runInventoryRequest(async request => {
    const response = await refreshProviderInventory(request.source, instanceKey)
    if (!isCurrentInventoryRequest(request)) return
    ElMessage.info(response.data); await readInventory(request)
  })
}
const draftGuide = row => ElMessageBox.alert(`${uiText('先通过原“接入实例”填写并核对SSH和实际GPU编号，再配置Pro并登记以下公共身份。不要修改已绑定身份；环境检查通过后再按原权限发布免费供给，当前不能发布收费Pro。')}\n${uiText('账号管理引用')}: ${row.accountScope}\n${uiText('实例ID')}: ${row.providerInstanceId}`, uiText('待手工接入'))
const observation = reactive({ powerState: 'UNKNOWN', capacityState: 'UNKNOWN', note: '' })
const saveObservation = async () => { saving.value = true; try { await recordComputeObservation(observedNode.value, { ...observation }); observationVisible.value = false; await load() } finally { saving.value = false } }
const offerVisible = ref(false), offerId = ref('')
const offerForm = reactive({ published: true, billingType: 'FREE', pointsPerHour: null })
const offer = row => { offerId.value = row.id; Object.assign(offerForm, { published: row.resourceScope === 'PLATFORM', billingType: row.billingType === 'POINTS' ? 'POINTS' : 'FREE', pointsPerHour: row.pointsPerHour }); offerVisible.value = true }
const saveOffer = async () => { saving.value = true; try { await publishComputeOffer(offerId.value, { ...offerForm }); offerVisible.value = false; await load() } finally { saving.value = false } }

const nodes = ref([])
const publicNode = ref(''), publicIdentityVisible = ref(false)
const publicIdentityForm = reactive({ accountScope: '', instanceId: '' })
const savePublicIdentity = async () => { saving.value = true; try { await savePublicComputeIdentity(publicNode.value, { ...publicIdentityForm }); publicIdentityVisible.value = false; await load() } finally { saving.value = false } }
const providerNode = ref(''), providerVisible = ref(false)
const selectedProvider = computed(() => nodes.value.find(n => n.id === providerNode.value))
const costVisible = ref(false), costObservation = ref(null)
const queryCost = async () => { saving.value = true; try { costObservation.value = (await queryComputeCostObservation(providerNode.value)).data; costVisible.value = true } finally { saving.value = false } }
const providerForm = reactive({ controlMode: 'MANUAL', providerProduct: null, providerInstanceId: '', providerRegion: '', requestedStoppedMode: null, token: '', accessKeyId: '', accessKeySecret: '', clearCredentials: false })
const clearProviderSecrets = () => { providerForm.token = ''; providerForm.accessKeyId = ''; providerForm.accessKeySecret = '' }
const configureProvider = () => {
  const row = selectedProvider.value
  clearProviderSecrets()
  Object.assign(providerForm, { controlMode: row.controlMode || 'MANUAL', providerProduct: row.providerProduct || (row.provider === 'autodl' ? 'AUTODL_PRO' : 'ECS'), providerInstanceId: row.providerInstanceId || '', providerRegion: row.providerRegion || '', requestedStoppedMode: row.requestedStoppedMode || null, clearCredentials: false })
  providerVisible.value = true
}
const saveProvider = async () => {
  saving.value = true
  try { await saveComputeProviderSettings(providerNode.value, { ...providerForm }); clearProviderSecrets(); providerVisible.value = false; await load(); ElMessage.success(uiText('配置已保存，未执行厂商操作')) } catch { /* sensitiveData interceptor shows a safe error */ } finally { saving.value = false }
}
const refreshProvider = async () => { saving.value = true; try { const response = await refreshComputeProviderState(providerNode.value); ElMessage.info(response.data); await load() } finally { saving.value = false } }
const loading = ref(false)
const visible = ref(false)
const saving = ref(false)
const probing = ref('')
const editingId = ref('')
const sshCommand = ref('')
const formRef = ref()
const showProbe = ref(false)
const showSetup = ref(false)
const setupProfiles = [
  { gpu: 'RTX 3090 / 4090 / 4090D', torch: '2.5.1 / 0.20.1', cuda: '12.4', profile: 'standard' },
  { gpu: 'RTX 5090 / PRO 6000 Blackwell', torch: '2.7.1 / 0.22.1', cuda: '12.8', profile: 'blackwell' }
]
const probeResult = ref(null)
const labels = { READY: '可训练', ERROR: '检查未通过', UNTESTED: '待检查' }
const defaults = () => ({ name: '', provider: 'autodl', serverGroup: '', gpuModel: '', host: '', port: 22, username: 'root', password: '', pythonPath: '/root/autodl-tmp/vlstream/envs/vls-standard/bin/python', workDir: '/root/autodl-tmp/vlstream', gpuIndex: 0, enabled: true })
const form = reactive(defaults())
const rules = Object.fromEntries(['name', 'host', 'username', 'pythonPath', 'workDir'].map(key => [key, [{ required: true, message: '请填写此项', trigger: 'blur' }]]))
const details = row => { try { return JSON.parse(row.probeJson || '{}') } catch { return {} } }
const clearPassword = () => { form.password = '' }
const load = async () => {
  loading.value = true
  try { nodes.value = (await listComputeNodes()).data || [] } finally { loading.value = false }
}
const edit = row => {
  editingId.value = row?.id || ''
  Object.assign(form, defaults(), row ? { name: row.name, provider: row.provider, serverGroup: row.serverGroup, gpuModel: row.gpuModel, host: row.host, port: row.port, username: row.username, pythonPath: row.pythonPath, workDir: row.workDir, gpuIndex: row.gpuIndex, enabled: row.enabled } : {})
  sshCommand.value = ''; visible.value = true
}
const parseSsh = value => {
  const match = value.trim().match(/^ssh\s+-p\s+(\d+)\s+([A-Za-z0-9_-]+)@([A-Za-z0-9.-]+)$/)
  if (!match) { ElMessage.warning(uiText('命令格式应为 ssh -p 端口 用户名@主机')); return }
  form.port = Number(match[1]); form.username = match[2]; form.host = match[3]
}
const probe = async row => {
  probing.value = row.id
  try { probeResult.value = (await probeComputeNode(row.id)).data; showProbe.value = true; await load() } finally { probing.value = '' }
}
const save = async () => {
  if (!(await formRef.value.validate().catch(() => false))) return
  if (!editingId.value && !form.password) { ElMessage.warning(uiText('请输入 SSH 密码')); return }
  saving.value = true
  try {
    const row = (await saveComputeNode(editingId.value, { ...form })).data
    clearPassword(); visible.value = false; await load(); await probe(row)
  } catch {
    // The shared interceptor displays the error; never rethrow a request containing the password.
  } finally { saving.value = false }
}
const remove = async row => {
  try { await ElMessageBox.confirm(uiText('移除平台中的接入记录后，AutoDL 实例和文件仍保留。'), uiText('移除实例'), { type: 'warning' }) } catch { return }
  await deleteComputeNode(row.id); await load()
}
onMounted(load)
</script>

<style scoped>
.cloud-compute { padding: 20px; width: 100%; box-sizing: border-box; overflow: auto; }
.cloud-heading { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-bottom: 20px; }
.cloud-heading h3 { margin: 0 0 8px; font-size: 20px; }
.cloud-heading p, .cloud-help, .field-help { color: #667085; line-height: 1.7; font-size: 13px; margin: 0; }
.cloud-table { margin: 20px 0; }
.field-help { margin-top: 5px; }
.cloud-help a { color: #3979ef; }
.setup-command { padding: 12px; background: #f3f5f8; white-space: pre-wrap; overflow-wrap: anywhere; }
@media (max-width: 800px) { .cloud-heading { align-items: flex-start; flex-direction: column; } }
</style>
