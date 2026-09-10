<template>
  <div v-if="origins.length" class="origins"><p v-for="origin in origins" :key="origin.id">来源视频：{{ origin.videoName }}<br />视频时间：{{ (Number(origin.timestampMs) / 1000).toFixed(3) }} 秒</p></div>
  <p v-else-if="error" class="origins">来源信息暂时无法加载</p>
</template>
<script setup>
import { ref, watch } from 'vue'
import { getVideoFrameOrigins } from '@/api/datasetImport'
const props = defineProps({ datasetId: { type: [String, Number], required: true }, sampleId: { type: [String, Number], required: true } })
const origins = ref([]), error = ref(false)
watch(() => [props.datasetId, props.sampleId], async (ids, _, cleanup) => {
  let active = true; cleanup(() => { active = false }); origins.value = []; error.value = false
  try { const result = await getVideoFrameOrigins(ids[0], ids[1]); if (active) origins.value = result } catch { if (active) error.value = true }
}, { immediate: true })
</script>
<style scoped>.origins { color: #7c8594; font-size: 12px; line-height: 1.8; }</style>
