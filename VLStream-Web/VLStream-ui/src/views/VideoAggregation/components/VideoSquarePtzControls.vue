<template>
  <section class="square-ptz">
    <el-alert v-if="notice" :title="$tp(notice)" :type="capability.status === 'checking' ? 'info' : 'warning'" :closable="false" />
    <el-button v-if="source?.protocol === 'ONVIF' && visible && !viewOnly" text size="small" @click="retry">{{ $tp('重新检查云台能力') }}</el-button>
    <PTZControl show-camera-management hold-controls variant="square" :movement-enabled="canMove" :zoom-enabled="false"
      :supported-actions="source && !viewOnly ? ['camera-management'] : []"
      @movement-start="move" @movement-stop="stop" @control-action="openManagement" />
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import PTZControl from '@/components/PTZControl.vue'
import { getPtzCapabilities, onvifPZTStart, onvifPZTEnd } from '@/api/onvif/device'
import { createPtzCapabilitiesController, canUsePtzMovement } from '@/utils/onvifPtzCapabilities'
import { createPtzHoldController } from '@/utils/onvifPtzHold'
import { checkPermi } from '@/utils/wvpPermission'
import { VIDEO_PROTOCOLS } from '@/utils/videoSquareCatalog'
const props = defineProps({ source: { type: Object, default: null }, visible: Boolean, viewOnly: Boolean })
const router = useRouter()
const capability = ref({ status: 'unknown', reason: '', continuousPanTilt: false })
const controller = createPtzCapabilitiesController({ request: getPtzCapabilities, onChange: state => { capability.value = state } })
const hold = createPtzHoldController({ startRequest: onvifPZTStart, stopRequest: onvifPZTEnd,
  onError: error => controller.markUnknown(error?.message || '云台控制失败') })
const canMove = computed(() => props.visible && !props.viewOnly && props.source?.protocol === 'ONVIF' &&
  checkPermi(['onvif:service:continuousMove']) && checkPermi(['onvif:service:continuousMoveStop']) && canUsePtzMovement(capability.value))
const notice = computed(() => props.viewOnly ? '仅有查看权限，不能控制设备' :
  !props.source ? '请选择视频窗口' : props.source.protocol !== 'ONVIF' ? '当前来源未提供可用的云台控制能力' : capability.value.reason)
const stop = () => hold.stop()
watch(canMove, enabled => { if (!enabled) stop() })
const move = direction => {
  if (!canMove.value) return
  hold.start({ id: props.source.rowId, direction: ({ up: 'upper', down: 'below', left: 'left', right: 'right' })[direction], speed: .1 })
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
  window.addEventListener('blur', stop)
  document.addEventListener('visibilitychange', stopHidden)
})
onBeforeUnmount(() => {
  stop()
  controller.close()
  window.removeEventListener('pointerup', stop)
  window.removeEventListener('blur', stop)
  document.removeEventListener('visibilitychange', stopHidden)
})
</script>

<style scoped>
.square-ptz { width: 100%; }
.square-ptz :deep(.el-alert) { margin-bottom: 8px; }
</style>
