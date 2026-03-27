<template>
  <div class="config-record">
    <nav-header-banner title="配置录像" :is-call-back="true" @call-back="onBack">
      <span class="header-save" @click="onSave">保存</span>
    </nav-header-banner>

    <div class="config-content">
      <!-- 顶部 tab -->
      <div class="tab-header">
        <div
          v-for="tab in tabs"
          :key="tab.value"
          class="tab-item"
          :class="{ active: activeTab === tab.value }"
          @click="activeTab = tab.value"
        >
          {{ tab.label }}
          <div v-if="activeTab === tab.value" class="tab-underline" />
        </div>
      </div>

      <!-- tab 内容 -->
      <div class="tab-body">
        <!-- 时间策略 -->
        <div v-if="activeTab === 'time'">
          <DefenseTimeMobile v-model:protection-time="protectionTime" />
        </div>

        <!-- 事件策略 -->
        <div v-else>
          <EventStrategy ref="eventRef" :device-id="props.deviceId" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { showToast } from 'vant'
import { getVlsTimeStrategy, updateVlsTimeStrategy } from '@/api/VLStreamCloud/device'
import DefenseTimeMobile from '@/pages/VLStreamCloud/page/components/DefenseTimeMobile.vue'
import EventStrategy from '@/pages/VLStreamCloud/page/components/deviceManagement/EventStrategy.vue'
import { useUserStore } from '@/store/modules/useraPaas'

const emit = defineEmits(['close'])

const props = defineProps({
  deviceId: {
    type: [String, Number],
    default: ''
  }
})

const store = useUserStore()

const tabs = [
  { label: '时间策略', value: 'time' },
  { label: '事件策略', value: 'event' }
]

const activeTab = ref('time')

const protectionTime = ref({
  frequency: '每周',
  time_periods: []
})

const id = ref('')
const eventRef = ref(null)
// 初始化获取时间策略
const fetchTimeStrategy = async() => {
  if (!props.deviceId) return
  const params = {
    accessToken: store.userInfo?.accessToken,
    id: id.value
  }
  if (!id.value) {
    params.deviceId = props.deviceId
  }
  const res = await getVlsTimeStrategy(params)
  if (res?.code === 200 && res.data) {
    const data = res.data
    id.value = data.id
    let raw = data.protectionTime
    if (!raw) return

    if (typeof raw === 'string') {
      const parsed = JSON.parse(raw)
      if (parsed && typeof parsed === 'object') {
        protectionTime.value = parsed
      }
    } else if (typeof raw === 'object') {
      protectionTime.value = raw
    }
  }
}

const onSave = async() => {
  if (!props.deviceId) {
    showToast({ type: 'fail', message: '缺少设备ID' })
    return
  }

  // 时间策略保存
  if (activeTab.value === 'time') {
    const body = {
      accessToken: store.userInfo?.accessToken,
      id: id.value,
      protectionTime: JSON.parse(JSON.stringify(protectionTime.value))
    }
    if (!id.value) {
      body.deviceId = props.deviceId
    }
    const res = await updateVlsTimeStrategy(body)
    if (res && (res.code === 0 || res.code === 200)) {
      showToast({ type: 'success', message: res?.msg })
      await fetchTimeStrategy()
    }
    return
  }

  // 事件策略保存交给子组件
  if (activeTab.value === 'event') {
    const comp = eventRef.value
    if (comp && comp.save) {
      await comp.save()
    }
  }
}

onMounted(() => {
  fetchTimeStrategy()
})

const onBack = () => {
  emit('close')
}
</script>

<style scoped lang="scss">
.config-record {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #F9FAFF;
}

.header-save {
  font-size: 16px;
  color: #fff;
  line-height: 1;
}

.config-content {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  padding-bottom: 16px;
}

.tab-header {
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 64px;
  margin-bottom: 16px;
  background: #ffffff;
}

.tab-item {
  position: relative;
  line-height: 18px;
  color:#aeaeba;
  font-size: 14px;
  padding-bottom: 6px;

  &.active {
    font-weight:500;
    color:#2f69f8;
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
  background:#2f69f8;
}

.tab-body {
  background: #F9FAFF;
  padding: 0 12px 20px;
}

.event-placeholder {
  padding: 12px 4px;
}

.placeholder-card {
  border-radius: 12px;
  background: #f5f7fb;
  padding: 16px 14px;
}

.placeholder-title {
  font-size: 15px;
  color: #111827;
  font-weight: 600;
  margin-bottom: 6px;
}

.placeholder-desc {
  font-size: 13px;
  color: #6b7280;
  line-height: 1.5;
}
</style>

