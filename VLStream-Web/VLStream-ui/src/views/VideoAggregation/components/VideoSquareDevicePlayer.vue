<template>
  <div class="square-device-player" :class="{ 'is-recording': isRecording }">
    <span v-if="isRecording" class="recording-indicator" role="status"><span class="recording-dot" aria-hidden="true"></span>{{ $tp('录像中') }}</span>
    <PlatformRecordingActions v-if="recordingControls && selected" class="square-recording" :protocol="selected.protocol" :row-id="selected.rowId" :online="videoSourceOnline(selected)" />
    <el-select v-if="device.sources?.length > 1" v-model="selectedKey" size="small" class="source-selector"
      :aria-label="$tp('播放通道')" :teleported="false">
      <el-option v-for="source in device.sources" :key="sourceKey(source)" :value="sourceKey(source)"
        :label="sourceLabel(source)" />
    </el-select>
    <VlsDevicePlayer v-if="selected?.protocol === 'VLSTREAM'" :key="`vls:${selectedKey}`"
      :device="{ id: selected.rowId, online: videoSourceOnline(selected) }" square compact-status />
    <RtspDevicePlayer v-else-if="['RTSP', 'ONVIF'].includes(selected?.protocol) && (!selected.playType || selected.playType === '1')"
      :key="`rtsp:${selectedKey}`" :device-id="selected.rowId" :protocol="selected.protocol" compact-status />
    <SquareProtocolDevicePlayer v-else-if="selected?.protocol === 'CUSTOM' ||
      (selected?.protocol === 'ONVIF' && ['2', '3'].includes(selected.playType))"
      :key="`protocol:${selectedKey}`" :source="selected" :device-name="device.deviceName" compact-status />
    <div v-else class="protocol-preview-entry">
      <p role="status">{{ $tp('无信号') }}</p>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useDeviceRecording } from '@/composables/useDeviceRecording'
import VlsDevicePlayer from '@/components/VlsDevicePlayer.vue'
import PlatformRecordingActions from '@/components/PlatformRecordingActions.vue'
import RtspDevicePlayer from '@/components/RtspDevicePlayer.vue'
import SquareProtocolDevicePlayer from './SquareProtocolDevicePlayer.vue'
import { translatePhrase } from '@/i18n'
import { customStatusDetail } from '@/utils/customDeviceState'
import { VIDEO_PROTOCOLS, videoSourceKey, defaultVideoSource, canPreviewVideoSource, videoSourceOnline, videoSourceState } from '@/utils/videoSquareCatalog'
const props = defineProps({ device: { type: Object, required: true }, recordingControls: { type: Boolean, default: false } })
const isRecording = useDeviceRecording(() => props.device)
const emit = defineEmits(['source-change'])
const selectedKey = ref('')
const sourceKey = videoSourceKey
const selected = computed(() => props.device.sources?.find(source => sourceKey(source) === selectedKey.value))
watch(selected, source => emit('source-change', source || null), { immediate: true })
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
</script>

<style scoped>
.square-device-player { position: relative; height: 100%; width: 100%; display: flex; flex-direction: column; background: #151515; color: #eee; }
.square-device-player.is-recording::after { content: ''; position: absolute; inset: 0; border: 3px solid #24c76b; z-index: 8; pointer-events: none; }
.recording-indicator { position: absolute; top: 6px; right: 6px; z-index: 9; display: inline-flex; align-items: center; gap: 6px; padding: 4px 9px; border-radius: 4px; background: #169b4f; color: #fff; font-size: 12px; line-height: 18px; pointer-events: none; }
.recording-dot { width: 7px; height: 7px; flex: none; border-radius: 50%; background: #ff4d4f; box-shadow: 0 0 0 1px #ffffffb3; animation: recording-pulse 1.6s ease-in-out infinite; }
@keyframes recording-pulse { 50% { opacity: .45; } }
@media (prefers-reduced-motion: reduce) { .recording-dot { animation: none; } }
.square-recording { position: absolute; inset-inline-end: 10px; top: 36px; z-index: 4; opacity: 0; transition: opacity .15s; }
.square-device-player:hover .square-recording, .square-device-player:focus-within .square-recording { opacity: 1; }
.source-selector { width: 100%; z-index: 2; }
.square-device-player :deep(.vls-device-player), .square-device-player :deep(.rtsp-device-player),
.square-device-player :deep(.square-protocol-player) { flex: 1; min-height: 0; }
.protocol-preview-entry { margin: auto; padding: 20px; text-align: center; }
</style>
