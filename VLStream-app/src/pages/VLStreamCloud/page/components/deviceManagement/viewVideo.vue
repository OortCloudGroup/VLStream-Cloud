<template>
  <div class="device-detail">
    <nav-header-banner title="查看视频" :is-call-back="true" @call-back="onBack" />

    <div class="detail-body">
      <img class="hero" src="@/assets/img/VLStreamCloud/fj.png" alt="" />
      <div class="tab-body">
        <PTZPanel />
      </div>
    </div>
  </div>
</template>

<script setup>
import NavHeaderBanner from '@/components/navHeaderBanner/index.vue'
import PTZPanel from './PTZPanel.vue'

const emit = defineEmits(['close'])

const onBack = () => {
  emit('close')
}
</script>

<style scoped lang="scss">
.device-detail {
  height: 100vh;
  background: #f6f8ff;
  display: flex;
  flex-direction: column;
}

.detail-body {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.hero {
  width: 100%;
  height: 188px;
}

.tab-body {
  flex: 1;
  background: #f6f8ff;
  padding: 12px 12px 24px;
}
</style>
