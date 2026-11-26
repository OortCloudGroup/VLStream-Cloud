<template>
    <div class="scene-block" :class="'block-' + block.type" :style="style">
        <template v-if="block.type === 'row' || block.type === 'column'">
            <VideoSquareScenePage v-for="(child, index) in block.children" :key="index" :block="child">
                <template #canvas><slot name="canvas" /></template>
                <template #preview><slot name="preview" /></template>
            </VideoSquareScenePage>
        </template>
        <slot v-else-if="block.type === 'canvas'" name="canvas" />
        <slot v-else-if="block.type === 'preview'" name="preview" />
        <span v-else-if="block.type === 'text'" data-i18n-ignore>{{ block.text }}</span>
        <VideoSquareVector v-else-if="block.type === 'drawing'" :shapes="block.shapes" />
    </div>
</template>
<script setup>
import { computed } from 'vue'
import VideoSquareVector from './VideoSquareVector.vue'
const props = defineProps({ block: { type: Object, required: true } })
const px = n => n == null ? undefined : n + 'px'
const style = computed(() => ({
    backgroundColor: props.block.bg, color: props.block.color, '--scene-accent': props.block.accent,
    padding: px(props.block.pad), gap: px(props.block.gap), flexGrow: props.block.grow, fontSize: px(props.block.size),
}))
</script>
<style scoped>
.scene-block { box-sizing: border-box; min-width: 0; overflow-wrap: anywhere; }
.block-row, .block-column { display: flex; flex: 1 1 0; gap: 12px; align-items: stretch; }
.block-row { flex-direction: row; } .block-column { flex-direction: column; }
.block-canvas { display: flex; flex: 4 1 0; min-height: 215px; overflow: auto; }
.block-preview { flex: 1 1 140px; min-width: min(165px, 100%); max-width: 100%; }
.block-row > .block-preview { align-self: flex-start; }
.block-text { flex: 0 1 auto; align-content: center; white-space: pre-wrap; }
.block-drawing { display: flex; flex: 0 1 96px; min-height: 45px; max-height: 180px; }
.block-drawing svg { width: 100%; height: 100%; max-height: 180px; }
@container scene-workspace (max-width: 680px) {
    .block-row { flex-direction: column; }
    .block-preview { width: 100%; flex-basis: auto; }
}
</style>
