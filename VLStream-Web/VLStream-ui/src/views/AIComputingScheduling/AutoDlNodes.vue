<template>
  <section class="cloud-compute" v-loading="loading">
    <div class="cloud-heading">
      <div><h3>{{ $tp('线上算力 · AutoDL') }}</h3><p>{{ $tp('接入已开机的普通容器实例，在算法训练中选择它执行任务。') }}</p></div>
      <div><el-button @click="showSetup = true">{{ $tp('新 GPU 接入指南') }}</el-button><el-button @click="load">{{ $tp('刷新') }}</el-button><el-button type="primary" @click="edit()">{{ $tp('接入实例') }}</el-button></div>
    </div>
    <el-alert type="info" :closable="false" show-icon :title="$tp('实例按当前租户管理。同一实例按顺序训练，完成后将 PT 和类别文件回存平台。实例开关机与费用在 AutoDL 管理。')" />
    <el-table :data="nodes" class="cloud-table" :empty-text="$tp('尚未接入线上实例，点击“接入实例”填写 SSH 信息')">
      <el-table-column prop="name" :label="$tp('实例名称')" min-width="140" />
      <el-table-column :label="$tp('SSH 地址')" min-width="200"><template #default="{ row }">{{ row.host }}:{{ row.port }}</template></el-table-column>
      <el-table-column :label="$tp('环境检查')" min-width="120"><template #default="{ row }"><el-tag :type="row.probeState === 'READY' ? 'success' : row.probeState === 'ERROR' ? 'danger' : 'info'">{{ $tp(labels[row.probeState]) || row.probeState }}</el-tag></template></el-table-column>
      <el-table-column :label="$tp('最近检查的资源')" min-width="190"><template #default="{ row }"><span v-if="details(row).gpu">{{ details(row).gpu }}<br>{{ $tp('可用磁盘') }} {{ details(row).diskFreeGb }} GB</span><span v-else>—</span></template></el-table-column>
      <el-table-column :label="$tp('使用状态')" width="100"><template #default="{ row }">{{ row.enabled ? $tp('已启用') : $tp('已停用') }}</template></el-table-column>
      <el-table-column :label="$tp('操作')" min-width="235"><template #default="{ row }"><el-button link type="primary" :loading="probing === row.id" @click="probe(row)">{{ $tp('检查连接与环境') }}</el-button><el-button link @click="edit(row)">{{ $tp('编辑') }}</el-button><el-button link type="danger" @click="remove(row)">{{ $tp('移除') }}</el-button></template></el-table-column>
    </el-table>
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

import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listComputeNodes, saveComputeNode, probeComputeNode, deleteComputeNode } from '@/api/compute'

const nodes = ref([])
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
const defaults = () => ({ name: '', host: '', port: 22, username: 'root', password: '', pythonPath: '/root/autodl-tmp/vlstream/envs/vls-standard/bin/python', workDir: '/root/autodl-tmp/vlstream', gpuIndex: 0, enabled: true })
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
  Object.assign(form, defaults(), row ? { name: row.name, host: row.host, port: row.port, username: row.username, pythonPath: row.pythonPath, workDir: row.workDir, gpuIndex: row.gpuIndex, enabled: row.enabled } : {})
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
