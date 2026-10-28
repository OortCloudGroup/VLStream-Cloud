<template>
  <section class="square-ptz">
    <el-alert v-if="notice" :title="$tp(notice)" :type="capability.status === 'checking' ? 'info' : 'warning'" :closable="false" />
    <el-button v-if="source?.protocol === 'ONVIF' && visible && !viewOnly" text size="small" @click="retry">{{ $tp('重新检查云台能力') }}</el-button>
    <PTZControl show-camera-management hold-controls variant="square" :movement-enabled="canMove" :zoom-enabled="false"
      :supported-actions="source && !viewOnly ? ['camera-management'] : []"
      @movement-start="move" @movement-stop="stop" @control-action="openManagement">
      <template #movement-settings>
        <div v-if="source?.protocol === 'ONVIF'" class="square-ptz-speed">
          <span class="speed-label">{{ $tp('云台速度') }}</span>
          <el-slider v-model="speed" :aria-label="$tp('云台速度')" :min="0.1" :max="1" :step="0.1" :disabled="!canMove" @change="stop" />
        </div>
      </template>
    </PTZControl>
    <VideoSquareOnvifPresets :source="source" :visible="visible" :view-only="viewOnly" :capable="capability.status === 'supported' && capability.presets === true" :before-action="preparePresetAction" />
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import PTZControl from '@/components/PTZControl.vue'
import VideoSquareOnvifPresets from './VideoSquareOnvifPresets.vue'
import { getPtzCapabilities, onvifPZTStart, onvifPZTEnd } from '@/api/onvif/device'
import { createPtzCapabilitiesController, canUsePtzMovement } from '@/utils/onvifPtzCapabilities'
import { createPtzHoldController } from '@/utils/onvifPtzHold'
import { checkPermi } from '@/utils/wvpPermission'
import { VIDEO_PROTOCOLS } from '@/utils/videoSquareCatalog'
const props = defineProps({ source: { type: Object, default: null }, visible: Boolean, viewOnly: Boolean })
const router = useRouter()
const capability = ref({ status: 'unknown', reason: '', continuousPanTilt: false })
const speed = ref(0.1)
const controller = createPtzCapabilitiesController({ request: getPtzCapabilities, onChange: state => { capability.value = state } })
const hold = createPtzHoldController({ startRequest: onvifPZTStart, stopRequest: onvifPZTEnd,
  onError: error => controller.markUnknown(error?.message || '云台控制失败') })
const canMove = computed(() => props.visible && !props.viewOnly && props.source?.protocol === 'ONVIF' &&
  checkPermi(['onvif:service:continuousMove']) && checkPermi(['onvif:service:continuousMoveStop']) && canUsePtzMovement(capability.value))
const notice = computed(() => props.viewOnly ? '仅有查看权限，不能控制设备' :
  !props.source ? '请选择视频窗口' : props.source.protocol !== 'ONVIF' ? '当前来源未提供可用的云台控制能力' : capability.value.reason)
const stop = () => hold.stop()
const preparePresetAction = async () => {
  await stop()
  if(capability.value.status !== 'supported' || !capability.value.presets) throw new Error('Preset capability unavailable')
}
watch(canMove, enabled => { if (!enabled) stop() })
const move = direction => {
  if (!canMove.value) return
  const mapped = ({ up: 'upper', down: 'below', left: 'left', right: 'right' })[direction]
  if (!mapped) return
  const selectedSpeed = Number(speed.value)
  hold.start({ id: props.source.rowId, direction: mapped, speed: Number.isFinite(selectedSpeed) ? Math.max(.1, Math.min(1, selectedSpeed)) : .1 })
}
async function retry() {
  stop()
  if (props.visible && !props.viewOnly && props.source?.protocol === 'ONVIF') await controller.check()
}
watch(() => `${props.visible}:${props.viewOnly}:${props.source?.protocol}:${props.source?.rowId}:${props.source?.channel || ''}`, () => {
  stop()
  controller.close()
  if (props.visible && !props.viewOnly && props.source?.protocol === 'ONVIF') {
    controller.open({ id: props.source.rowId, channel: props.source.channel })
    controller.check()
  }
}, { immediate: true })
function openManagement(action) {
  if (action !== 'camera-management' || props.viewOnly) return
  const protocol = VIDEO_PROTOCOLS.find(item => item.value === props.source?.protocol)
  if (protocol) router.push(protocol.path)
}
const stopHidden = () => { if (document.hidden) stop() }
onMounted(() => {
  window.addEventListener('pointerup', stop)
  window.addEventListener('pointercancel', stop)
  window.addEventListener('blur', stop)
  window.addEventListener('pagehide', stop)
  document.addEventListener('visibilitychange', stopHidden)
})
onBeforeUnmount(() => {
  stop()
  controller.close()
  window.removeEventListener('pointerup', stop)
  window.removeEventListener('pointercancel', stop)
  window.removeEventListener('blur', stop)
  window.removeEventListener('pagehide', stop)
  document.removeEventListener('visibilitychange', stopHidden)
})
</script>

<style scoped>
.square-ptz { width: 100%; }
.square-ptz :deep(.el-alert) { margin-bottom: 8px; }
.square-ptz-speed { width:100%; min-width:0; padding-inline:8px; box-sizing:border-box; }
.square-ptz-speed .speed-label { display:block; font-size:12px; }
</style>
