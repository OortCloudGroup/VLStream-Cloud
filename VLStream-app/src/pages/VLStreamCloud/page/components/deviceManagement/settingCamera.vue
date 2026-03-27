<template>
  <div class="device-detail">
    <nav-header-banner title="设置摄像机" :is-call-back="true" @call-back="onBack">
      <div class="header-actions">
        <span class="action edit" @click="onEdit">恢复默认值</span>
      </div>
    </nav-header-banner>

    <div class="detail-body">
      <div class="tab-header">
        <div
          class="tab-item"
          :class="{ active: activeTab === 'ptz' }"
          @click="activeTab = 'ptz'"
        >
          显示设置
          <div v-if="activeTab === 'ptz'" class="tab-underline" />
        </div>
        <div
          class="tab-item"
          :class="{ active: activeTab === 'info' }"
          @click="activeTab = 'info'"
        >
          OSD设置
          <div v-if="activeTab === 'info'" class="tab-underline" />
        </div>
      </div>

      <div class="tab-body">
        <showSetting
          v-if="activeTab === 'ptz'"
          ref="showSettingRef"
          :device-id="device?.deviceId"
        />
        <osdSetting
          v-else
          ref="osdSettingRef"
          :device-id="device?.deviceId"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import NavHeaderBanner from '@/components/navHeaderBanner/index.vue'
import osdSetting from './osdSetting.vue'
import showSetting from './showSetting.vue'

const props = defineProps({
  device: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['edit', 'close'])

const activeTab = ref('ptz')

// 子组件实例，用于调用恢复默认接口
const showSettingRef = ref()
const osdSettingRef = ref()

const onEdit = () => {
  emit('edit', props.device)

  if (activeTab.value === 'ptz') {
    showSettingRef.value.restoreDefault()
  } else if (activeTab.value === 'info') {
    osdSettingRef.value.restoreDefault()
  }
}

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

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;

  .action {
    font-size: 16px;
    color: #fff;
    line-height: 1;

    &.delete {
      color: #fff;
    }
  }
}

.hero {
  width: 100%;
  height: 210px;
  background: linear-gradient(180deg, #dce9ff 0%, #f0f4ff 100%);
}

.tab-header {
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 64px;
  background: #ffffff;
}

.tab-item {
  position: relative;
  line-height: 18px;
  color: #aeaeba;
  font-size: 14px;
  padding-bottom: 6px;

  &.active {
    font-weight: 500;
    color: #2f69f8;
  }
}

.tab-underline {
  position: absolute;
  left: 50%;
  bottom: 0;
  transform: translateX(-50%);
  width: 24px;
  height: 3px;
  border-radius: 999px;
  background: #2f69f8;
}

.tab-body {
  flex: 1;
  background: #f6f8ff;
  padding: 16px;
}
</style>
