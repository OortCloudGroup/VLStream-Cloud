<template>
  <div class="square-device-player">
    <el-select v-model="selectedKey" size="small" class="source-selector">
      <el-option v-for="source in device.sources" :key="sourceKey(source)" :value="sourceKey(source)"
        :label="protocolLabel(source.protocol) + ' · ' + (source.deviceId || source.rowId)" />
    </el-select>
    <VlsDevicePlayer v-if="selected?.protocol === 'VLSTREAM'" :key="selectedKey"
      :device="{ id: selected.rowId, online: ['ONLINE', 'ON', '1', 'TRUE'].includes(String(selected.state).toUpperCase()) }" />
    <div v-else class="protocol-preview-entry">
      <span data-i18n-ignore>{{ device.deviceName }}</span>
      <p>{{ $tp('通过协议设备页选择通道并播放') }}</p>
      <el-button type="primary" @click="openProtocol">{{ $tp('前往设备页') }}</el-button>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import VlsDevicePlayer from '@/components/VlsDevicePlayer.vue'
import { VIDEO_PROTOCOLS } from '@/utils/videoSquareCatalog'
const props = defineProps({ device: { type: Object, required: true } })
const router = useRouter()
const selectedKey = ref('')
const sourceKey = source => source.protocol + ':' + source.rowId
const selected = computed(() => props.device.sources?.find(source => sourceKey(source) === selectedKey.value))
const protocolLabel = value => VIDEO_PROTOCOLS.find(protocol => protocol.value === value)?.label || value
watch(() => props.device.id, () => {
  const source = props.device.sources?.find(item => item.protocol === 'VLSTREAM') || props.device.sources?.[0]
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
.square-device-player :deep(.vls-device-player) { flex: 1; min-height: 0; }
.protocol-preview-entry { margin: auto; padding: 20px; text-align: center; }
</style>
