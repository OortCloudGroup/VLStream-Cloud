<template>
  <div class="model-hub-cover">
    <img :src="failed ? fallback : source" :alt="alt" loading="lazy" decoding="async" @error="failed = true" />
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { getDefaultModelHubCover, getModelHubCover } from '@/utils/modelHubCover'

const props = defineProps({
  resource: { type: Object, default: () => ({}) },
  alt: { type: String, default: '' }
})
const failed = ref(false)
const source = computed(() => getModelHubCover(props.resource))
const fallback = computed(() => getDefaultModelHubCover(props.resource))
watch(source, () => { failed.value = false })
</script>

<style scoped>
.model-hub-cover { overflow: hidden; background: #edf2f9; }
.model-hub-cover img { display: block; width: 100%; height: 100%; object-fit: cover; }
</style>
