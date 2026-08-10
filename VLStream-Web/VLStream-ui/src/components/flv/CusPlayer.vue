<template>
  <video
    ref="videoElement"
    class="wvp-flv-player"
    controls
    autoplay
    muted
    playsinline
    @click="emit('clickPlayer')"
  />
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import mpegts from 'mpegts.js'

const props = defineProps({
  videoUrl: { type: String, default: '' }
})
const emit = defineEmits(['clickPlayer'])

const videoElement = ref()
let player

const destroy = () => {
  if (!player) return
  player.pause()
  player.unload()
  player.detachMediaElement()
  player.destroy()
  player = undefined
}

const createPlayer = (url) => {
  destroy()
  if (!url || !videoElement.value || !mpegts.isSupported()) return
  player = mpegts.createPlayer({ type: 'flv', isLive: true, url })
  player.attachMediaElement(videoElement.value)
  player.load()
  Promise.resolve(player.play()).catch(() => {})
}

const changeVideo = (url) => createPlayer(url)
const closePlayer = () => destroy()

onMounted(() => createPlayer(props.videoUrl))
watch(() => props.videoUrl, createPlayer)
onBeforeUnmount(destroy)

defineExpose({ createPlayer, changeVideo, closePlayer, destroy })
</script>

<style scoped>
.wvp-flv-player {
  width: 100%;
  max-height: 450px;
  background: #000;
}
</style>
