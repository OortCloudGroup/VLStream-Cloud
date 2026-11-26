<template>
    <svg viewBox="0 0 100 100" aria-hidden="true" preserveAspectRatio="xMidYMid meet">
        <component v-for="(shape,i) in shapes" :is="tags[shape.type]" :key="i" :d="shape.d" :x="shape.x" :y="shape.y"
            :cx="shape.cx" :cy="shape.cy" :r="shape.r" :width="shape.width" :height="shape.height" :x1="shape.x1" :y1="shape.y1"
            :x2="shape.x2" :y2="shape.y2" :fill="shape.fill || 'none'" :stroke="shape.stroke || 'currentColor'" :stroke-width="shape.strokeWidth ?? 1" />
    </svg>
</template>
<script setup>
defineProps({ shapes: { type: Array, required: true } })
const tags = { circle: 'circle', rect: 'rect', line: 'line', path: 'path' }
</script>
