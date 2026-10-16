<template>
  <div class="square-device-player">
    <el-select v-if="device.sources?.length > 1" v-model="selectedKey" size="small" class="source-selector"
      :aria-label="$tp('播放通道')" :teleported="false">
      <el-option v-for="source in device.sources" :key="sourceKey(source)" :value="sourceKey(source)"
        :label="sourceLabel(source)" />
    </el-select>
    <VlsDevicePlayer v-if="selected?.protocol === 'VLSTREAM' && canPreviewVideoSource(selected)" :key="selectedKey"
      :device="{ id: selected.rowId, online: videoSourceOnline(selected) }" />
    <RtspDevicePlayer v-else-if="['RTSP', 'ONVIF'].includes(selected?.protocol) && (!selected.playType || selected.playType === '1')"
      :key="selectedKey" :device-id="selected.rowId" :protocol="selected.protocol" />
    <SquareProtocolDevicePlayer v-else-if="selected?.protocol === 'CUSTOM' ||
      (selected?.protocol === 'ONVIF' && ['2', '3'].includes(selected.playType))"
      :key="selectedKey" :source="selected" :device-name="device.deviceName" />
    <div v-else class="protocol-preview-entry">
      <span data-i18n-ignore>{{ device.deviceName }}</span>
      <p v-if="!selected" role="status">{{ $tp('暂无可播放来源') }}</p>
      <p v-else-if="selected.protocol === 'VLSTREAM'" role="status">{{ $tp('VLStream 来源离线或状态未知，无法直接播放') }}</p>
      <p v-else>{{ $tp('通过协议设备页选择通道并播放') }}</p>
      <el-button v-if="selected" type="primary" @click="openProtocol">{{ $tp('前往设备页') }}</el-button>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import VlsDevicePlayer from '@/components/VlsDevicePlayer.vue'
import RtspDevicePlayer from '@/components/RtspDevicePlayer.vue'
import SquareProtocolDevicePlayer from './SquareProtocolDevicePlayer.vue'
import { translatePhrase } from '@/i18n'
import { customStatusDetail } from '@/utils/customDeviceState'
import { VIDEO_PROTOCOLS, videoSourceKey, defaultVideoSource, canPreviewVideoSource, videoSourceOnline, videoSourceState } from '@/utils/videoSquareCatalog'
const props = defineProps({ device: { type: Object, required: true } })
const router = useRouter()
const selectedKey = ref('')
const sourceKey = videoSourceKey
const selected = computed(() => props.device.sources?.find(source => sourceKey(source) === selectedKey.value))
const protocolLabel = value => VIDEO_PROTOCOLS.find(protocol => protocol.value === value)?.label || value
const sourceLabel = source => [protocolLabel(source.protocol), source.deviceName || source.deviceId,
  source.channel ? `${translatePhrase('通道')} ${source.channel}` : null, source.rowId,
  translatePhrase({ ONLINE: '在线', OFFLINE: '离线', UNKNOWN: '未知状态', AUTH_ERROR: '认证异常', STREAM_ERROR: '视频流异常' }
    [source.protocol === 'CUSTOM' ? customStatusDetail(source) : videoSourceState(source)])].filter(Boolean).join(' · ')
watch(() => props.device, () => {
  const sources = props.device.preferredProtocols?.length ? props.device.sources?.filter(source =>
    props.device.preferredProtocols.includes(source.protocol)) : props.device.sources
  const source = sources?.find(item => sourceKey(item) === selectedKey.value) || defaultVideoSource(sources)
  selectedKey.value = source ? sourceKey(source) : ''
}, { immediate: true })
function openProtocol() {
  const protocol = VIDEO_PROTOCOLS.find(item => item.value === selected.value?.protocol)
  if (protocol) router.push(protocol.path)
}
</script>

<style scoped>
.square-device-player { height: 100%; width: 100%; display: flex; flex-direction: column; background: #151515; color: #eee; }
.source-selector { width: 100%; z-index: 2; }
.square-device-player :deep(.vls-device-player), .square-device-player :deep(.rtsp-device-player),
.square-device-player :deep(.square-protocol-player) { flex: 1; min-height: 0; }
.protocol-preview-entry { margin: auto; padding: 20px; text-align: center; }
</style>
