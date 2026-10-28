<template>
  <el-tooltip :content="view.tooltip" placement="top" :show-after="350" popper-class="event-location-tooltip">
    <div class="event-location">
      <span class="event-location__text">{{ view.text }}</span>
    </div>
  </el-tooltip>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { eventLocationView, eventLocationCoordinate } from './eventLocation.mjs'
import { lookupEventLocation } from './eventLocationLookup'
const props = defineProps({ event: { type: Object, default: () => ({}) }, lookupEnabled: { type: Boolean, default: true } })
const resolved = ref(null)
const candidate = computed(() => eventLocationCoordinate(props.event))
watch([candidate, () => props.event, () => props.lookupEnabled], async ([coordinate, , enabled], _, onCleanup) => {
  let current = true
  onCleanup(() => { current = false })
  resolved.value = null
  if (!coordinate || !enabled) return
  resolved.value = { address: '', status: 'LOOKUP' }
  const result = await lookupEventLocation(coordinate, () => current)
  if (current) resolved.value = result
}, { immediate: true })
const view = computed(() => {
  if (!resolved.value) return eventLocationView(props.event)
  const coordinate = candidate.value
  return eventLocationView({ ...props.event, display_location: {
    source: 'EVENT', longitude: coordinate?.lng, latitude: coordinate?.lat,
    coordinate_system: { WGS84: 1, GCJ02: 2, BD09: 3 }[coordinate?.coordinateSystem],
    ...props.event.display_location, ...resolved.value
  } })
})
</script>

<style scoped>
.event-location { min-width: 0; max-width: 100%; line-height: 1.5; }
.event-location__text { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
:global(.event-location-tooltip) { max-width: min(560px, calc(100vw - 32px)); white-space: normal; overflow-wrap: anywhere; line-height: 1.6; }
</style>
