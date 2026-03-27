<template>
  <div class="ai-defense-time">
    <DefenseTimeMobile v-model:protection-time="protectionTime" />
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { getVlsAudioDefenseTimeSetting, saveVlsAudioDefenseTimeSetting } from '@/api/VLStreamCloud/device'
import DefenseTimeMobile from '@/pages/VLStreamCloud/page/components/DefenseTimeMobile.vue'
import { useUserStore } from '@/store/modules/useraPaas'

type TimePeriodItem =
    | { start: string; end: string }
    | { interval: number }
    | { weekday: number }
    | { day: number }

type TimePeriods =
    | TimePeriodItem[]
    | TimePeriodItem[][]

interface ProtectionTime {
  frequency: string
  time_periods: TimePeriods
}

const props = defineProps<{
  deviceId?: string | number
}>()

const store = useUserStore()

const originData = ref<any | null>(null)

const protectionTime = ref<ProtectionTime>({
  frequency: '每周',
  time_periods: []
})

const id = ref()

const fetchSetting = async() => {
  if (!props.deviceId) return
  const params: any = {
    accessToken: store.userInfo?.accessToken,
    id: id.value,
    deviceId: Number(props.deviceId)
  }
  const res: any = await getVlsAudioDefenseTimeSetting(params)
  if (res && res.code === 200) {
    const data = res.data || {}
    originData.value = data
    id.value = data.id

    const raw = data.protectionTime
    if (typeof raw === 'string') {
      protectionTime.value = JSON.parse(raw)
    } else if (raw && typeof raw === 'object') {
      protectionTime.value = raw
    }
  }
}

const save = async() => {
  if (!props.deviceId) return
  const base = originData.value || {}
  const payload: any = {
    id: id.value ?? null,
    deviceId: Number(props.deviceId) || Number(base.deviceId),
    tenantId: base.tenantId ?? null,
    protectionTime: protectionTime.value,
    remark: base.remark ?? null
  }
  const params: any = {
    ...payload,
    accessToken: store.userInfo?.accessToken
  }
  const res: any = await saveVlsAudioDefenseTimeSetting(params)
  if (res && res.code === 200) {
    await fetchSetting()
  }
}

watch(() => props.deviceId, () => {
  fetchSetting()
}, { immediate: true })

defineExpose({
  save
})
</script>

<style scoped lang="scss">
.ai-defense-time {
  width: 100%;
}
</style>

