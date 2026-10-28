<template>
    <section v-show="visible" ref="panel" class="square-floating-ptz" :class="{ 'is-dragging': dragging }"
        :style="{ left: position.x + 'px', top: position.y + 'px' }" :aria-label="$tp('PTZ控制器')">
        <header class="floating-ptz-header">
            <button type="button" class="floating-ptz-handle" :title="$tp('拖动PTZ控制器，方向键移动')"
                :aria-label="$tp('拖动PTZ控制器')" @pointerdown.stop="startDrag" @mousedown.stop
                @keydown="moveByKey" @lostpointercapture="stopDrag">
                <svg class="floating-ptz-grip" width="14" height="14" viewBox="0 0 16 16" aria-hidden="true"><path d="M5 3h1v1H5zm5 0h1v1h-1zM5 7h1v1H5zm5 0h1v1h-1zM5 11h1v1H5zm5 0h1v1h-1z" fill="currentColor" /></svg>
                <span>PTZ</span>
            </button>
            <button type="button" class="floating-ptz-close" :title="$tp('关闭PTZ控制器')"
                :aria-label="$tp('关闭PTZ控制器')" @pointerdown.stop @mousedown.stop @click.stop="close">
                <img :src="ptzCloseIcon" alt="" class="floating-ptz-close-icon" />
            </button>
        </header>
        <div class="floating-ptz-body">
            <VideoSquarePtzControls :source="source" :visible="visible" :view-only="viewOnly" />
        </div>
    </section>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import VideoSquarePtzControls from './VideoSquarePtzControls.vue'
import ptzCloseIcon from '@/assets/mastergo/video-square/ptz-close.svg'
import { clampFloatingPanelPosition, createFloatingPanelDrag } from '@/utils/videoSquareFloatingPanel.mjs'

const props = defineProps({ source: { type: Object, default: null }, visible: Boolean, viewOnly: Boolean })
const emit = defineEmits(['close'])
const panel = ref(null)
const position = ref({ x: 0, y: 0 })
const dragging = ref(false)
let drag
let observer
let placed = false
const readBounds = () => ({
    width: panel.value?.parentElement?.clientWidth || 0,
    height: panel.value?.parentElement?.clientHeight || 0,
    panelWidth: panel.value?.offsetWidth || 0,
    panelHeight: panel.value?.offsetHeight || 0,
})
const fit = () => {
    if (!props.visible || !drag) return
    const bounds = readBounds()
    if (!placed) {
        if (!bounds.width || !bounds.height || !bounds.panelWidth || !bounds.panelHeight) return
        position.value = clampFloatingPanelPosition({ x: bounds.width - bounds.panelWidth - 8, y: 8 }, bounds)
        placed = true
    } else drag.fit()
}
const startDrag = event => { if (props.visible) { fit(); drag?.start(event) } }
const stopDrag = event => drag?.stop(event)
const moveByKey = event => { if (props.visible) drag?.moveByKey(event) }
const close = () => { stopDrag(); emit('close') }

onMounted(() => {
    drag = createFloatingPanelDrag({ readBounds, readPosition: () => position.value,
        writePosition: value => { position.value = value }, eventTarget: window,
        onDragChange: value => { dragging.value = value } })
    if (typeof ResizeObserver !== 'undefined') {
        observer = new ResizeObserver(fit)
        observer.observe(panel.value)
        if (panel.value?.parentElement) observer.observe(panel.value.parentElement)
    }
    window.addEventListener('resize', fit)
    fit()
})
watch(() => props.visible, visible => {
    if (!visible) stopDrag()
    else nextTick(fit)
})
onBeforeUnmount(() => {
    drag?.destroy()
    observer?.disconnect()
    window.removeEventListener('resize', fit)
})
</script>

<style scoped>
.square-floating-ptz {
    position: absolute;
    pointer-events: auto;
    z-index: 20;
    width: 320px;
    max-width: max(0px, calc(100% - 16px));
    max-height: max(0px, calc(100% - 16px));
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
    gap: 24px;
    overflow: hidden;
    border: 0;
    padding: 12px;
    border-radius: 12px;
    background: #f0f4f9;
    box-shadow: 0 0 10px rgba(0, 0, 0, 0.08), 0 0 50px rgba(0, 0, 0, 0.2);
}
.floating-ptz-header { display: flex; flex: 0 0 24px; min-height: 0; align-items: center; justify-content: space-between; border-bottom: 0; background: transparent; }
.floating-ptz-handle { display: flex; flex: 1; align-items: center; gap: 0; min-width: 0; border: 0; padding: 0; background: transparent; color: #000; font: inherit; font-size: 18px; line-height: 24px; cursor: grab; user-select: none; touch-action: none; }
.floating-ptz-grip { display: none; }
.is-dragging .floating-ptz-handle { cursor: grabbing; }
.floating-ptz-close { flex: 0 0 20px; width: 20px; height: 20px; padding: 0; display: flex; align-items: center; justify-content: center; border: 0; background: transparent; cursor: pointer; }
.floating-ptz-close-icon { display: block; width: 12px; height: 12px; }
.floating-ptz-close:hover { background: #e8edf2; }
.floating-ptz-handle:focus-visible,.floating-ptz-close:focus-visible { outline: 2px solid #409eff; outline-offset: -2px; }
/* MasterGo 309:26907 — 面板 320 = 12 + 296 + 12，滚动条不得占用设计稿的 296 内容宽 */
.floating-ptz-body { flex: 1 1 auto; min-height: 0; overflow: auto; overscroll-behavior: contain; scrollbar-width: none; }
.floating-ptz-body::-webkit-scrollbar { width: 0; height: 0; }
.floating-ptz-body :deep(.ptz-control-panel) { width: 100%; height: auto; min-height: 0; box-sizing: border-box; overflow: visible; }
.floating-ptz-body :deep(.ptz-control-panel) { background: transparent; border: 0; box-shadow: none; }
.floating-ptz-body :deep(.ptz-control-panel--square) { padding: 0; gap: 20px; border-radius: 0; }
.floating-ptz-body :deep(.ptz-title) { display: none; }
.floating-ptz-body :deep(.function-section),.floating-ptz-body :deep(.zoom-wrapper),.floating-ptz-body :deep(.zoom-control-area) { width: 100%; min-width: 0; }
.floating-ptz-body :deep(.zoom-background-container),.floating-ptz-body :deep(.control-layout),.floating-ptz-body :deep(.function-grid),.floating-ptz-body :deep(.function-buttons-container) { width: 100%; max-width: 100%; min-width: 0; box-sizing: border-box; }
.floating-ptz-body :deep(.ptz-section),.floating-ptz-body :deep(.ptz-wrapper),.floating-ptz-body :deep(.ptz-control-area),.floating-ptz-body :deep(.ptz-circle-container) { width: 100%; min-width: 0; }
.floating-ptz-body :deep(.ptz-outer-ring) { width: min(240px, 100%); height: auto; aspect-ratio: 1; }
</style>
