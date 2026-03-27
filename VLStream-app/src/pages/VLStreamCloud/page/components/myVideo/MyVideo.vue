<template>
  <div class="my-video-page" @click="showVideo = true">
    <SimpleMap :app-id="Config.appId" />

    <oort-popup
      v-model="showVideo"
      position="right"
      style="width: 100%; height: 100%;"
      teleport="body"
    >
      <ViewVideo v-if="showVideo" @close="showVideo = false" />
    </oort-popup>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import SimpleMap from '@/components/map/simpleMap.vue'
import Config from '@/config/index'
import OortPopup from '@/components/popup/oort_popup.vue'
import ViewVideo from '@/pages/VLStreamCloud/page/components/deviceManagement/viewVideo.vue'

const showVideo = ref(false)
</script>

<style scoped lang="scss">
.my-video-page {
    width: 100%;
    height: 100%;
}
</style>

