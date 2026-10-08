<template>
  <el-dialog v-model="visible" :title="$tp(mode === 'service' ? $tp('国标设备接入帮助') : $tp('上级平台配置帮助'))" width="min(920px, 94vw)" append-to-body destroy-on-close>
    <div class="gb-help">
      <p class="help-intro">{{ mode === 'service' ? $tp('把本平台的国标服务参数填写到摄像机、NVR 等设备的 GB28181 配置中，再验证注册与视频回流。') : $tp('平台支持对接多个上级。这里填写其中一个上级的接入参数，各上级分别注册、共享目录和验证点播。') }}</p>
      <p class="help-topology">{{ $tp("摄像机 / NVR → VLStream → 多个上级平台（A / B / …）") }}</p>
      <el-tabs v-model="tab">
        <el-tab-pane :label="$tp('字段与示例')" name="fields">
          <p>{{ $tp("以下均为虚构示例，地址使用文档专用网段。请向现场运维和上级平台获取实际参数。") }}</p>
          <el-table :data="mode === 'service' ? serviceFields : platformFields" border style="width: 100%">
            <el-table-column prop="label" :label="$tp('字段')" width="145" />
            <el-table-column prop="example" :label="$tp('示例')" min-width="205" />
            <el-table-column prop="description" :label="$tp('怎么填写')" min-width="260" />
          </el-table>
          <p v-if="mode === 'service'" class="help-note">{{ $tp("服务信息列出多个 IP 时，只选设备能访问的一个地址；不要把逗号分隔的地址列表整体填入设备。容器或虚拟网卡地址通常不能直接给外部设备使用。") }}</p>
          <p v-else class="help-note">{{ $tp("“设备国标编号”是本平台对上级使用的接入身份；共享视频编号是另一项配置，不能把平台节点编号用作视频通道编号。") }}</p>
        </el-tab-pane>
        <el-tab-pane :label="$tp('联调与排查')" name="checks">
          <p>{{ $tp("每次联调保留“问题现象、排查证据、处理方法、复验结果、适用范围”，将现场经验整理为后续可复用的记录。") }}</p>
          <ol>
            <li><strong>{{ $tp("网络：") }}</strong>{{ $tp("核对 SIP 信令和实际媒体 IP、UDP/TCP 端口及回程流量。上级媒体 IP 可能与 SIP 服务 IP 不同。") }}</li>
            <li><strong>{{ $tp("注册：") }}</strong>{{ $tp("检查 REGISTER → 401 → 鉴权 REGISTER → 200。出现 403 时核对用户名、密码、国标身份和对端授权。") }}</li>
            <li><strong>{{ $tp("目录：") }}</strong>{{ $tp("给同一上级发布的共享编号必须唯一，并取得对端号段及通道授权；推送后由上级刷新目录。") }}</li>
            <li><strong>{{ $tp("点播：") }}</strong>{{ $tp("404 优先查共享映射；486 或回流超时检查源设备是否实际发流；有流无画面检查编码与解码兼容。") }}</li>
            <li><strong>{{ $tp("续期：") }}</strong>{{ $tp("核对上级返回的注册有效期，按每套上级独立配置；300 秒有效期下提前续注册的现场经验不能套用到所有平台。") }}</li>
            <li><strong>{{ $tp("验收：") }}</strong>{{ $tp("本地逐路解码，再由各个已对接上级分别点播和停止，持续观察至少一个续注册周期。") }}</li>
          </ol>
          <p class="help-note">{{ $tp("界面在线、目录推送成功或媒体发送成功，都不能替代对端实际出画面。PDF 中的现场备份、巡检和来电恢复配置需要在目标服务器另行落实。") }}</p>
        </el-tab-pane>
      </el-tabs>
    </div>
    <template #footer>
      <a :href="guideUrl" download="VLStream-国标接入与多上级对接经验.md" class="download-guide">{{ $tp("下载示例与对接经验") }}</a>
      <el-button @click="visible = false">{{ $tp("关闭帮助") }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref } from 'vue'
import guideUrl from '@/assets/docs/gb28181-guide.md?url'
import { platformFields, serviceFields } from '@/utils/gbHelp'
const props = defineProps({ mode: { type: String, default: 'platform' }, modelValue: Boolean })
const emit = defineEmits(['update:modelValue'])
const visible = computed({ get: () => props.modelValue, set: value => emit('update:modelValue', value) })
const tab = ref('fields')
</script>

<style scoped>
.gb-help { color: var(--el-text-color-regular); line-height: 1.8; max-height: 65vh; overflow-y: auto; padding-right: 8px; }
.help-intro { margin-top: 0; }
.help-topology { padding: 15px; background: var(--el-color-primary-light-9); color: var(--el-color-primary); border-radius: 8px; text-align: center; font-weight: 600; }
.help-note { padding: 12px 16px; background: var(--el-fill-color-light); border-left: 3px solid var(--el-color-primary); border-radius: 4px; }
li { margin-bottom: 14px; }
.download-guide { display: inline-block; margin: 8px 18px 8px 0; color: var(--el-color-primary); text-decoration: underline; }
</style>
