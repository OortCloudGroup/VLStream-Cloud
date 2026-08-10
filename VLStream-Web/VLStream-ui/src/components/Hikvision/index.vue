<template>
  <RtspPlayer
    v-if="rtsp"
    :rtsp-url="rtsp"
    :width="numericWidth"
    :height="numericHeight"
  />
  <el-empty v-else description="暂无可播放地址" />
</template>

<script setup>
import { computed } from 'vue'
import RtspPlayer from '@/components/RtspPlayer.vue'

const props = defineProps({
  rtsp: { type: String, default: '' },
  width: { type: [String, Number], default: 800 },
  height: { type: [String, Number], default: 457 }
})

const numericWidth = computed(() => Number(props.width) || 800)
const numericHeight = computed(() => Number(props.height) || 457)
</script>
