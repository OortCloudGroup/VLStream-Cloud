<template>
  <div class="linkage-body">
    <!-- 常规联动 -->
    <div class="linkage-section">
      <div class="linkage-section-header">
        <van-checkbox v-model="normalParentChecked" shape="square" class="parent-checkbox">
          常规联动
        </van-checkbox>
      </div>
      <div class="card-container">
        <van-checkbox-group v-model="normalList" shape="square" class="checkbox-group">
          <van-checkbox name="email">
            邮件联动
          </van-checkbox>
          <van-checkbox name="upload">
            上传中心
          </van-checkbox>
        </van-checkbox-group>
      </div>
    </div>

    <!-- 联动报警输出 -->
    <div class="linkage-section">
      <div class="linkage-section-header">
        <van-checkbox v-model="alarmParentChecked" shape="square" class="parent-checkbox">
          联动报警输出
        </van-checkbox>
      </div>
      <div class="card-container">
        <van-checkbox-group v-model="alarmOutputList" shape="square" class="checkbox-group">
          <van-checkbox name="A-1">
            A-&gt;1
          </van-checkbox>
        </van-checkbox-group>
      </div>
    </div>

    <!-- 录像联动 -->
    <div class="linkage-section">
      <div class="linkage-section-header">
        <van-checkbox v-model="recordParentChecked" shape="square" class="parent-checkbox">
          录像联动
        </van-checkbox>
      </div>
      <div class="card-container">
        <van-checkbox-group v-model="recordLinkageList" shape="square" class="checkbox-group">
          <van-checkbox name="A1">
            A1
          </van-checkbox>
        </van-checkbox-group>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { getVlsAudioLinkageModeSetting, saveVlsAudioLinkageModeSetting } from '@/api/VLStreamCloud/device'
import { useUserStore } from '@/store/modules/useraPaas'

const props = defineProps<{
  deviceId?: string | number
}>()

const store = useUserStore()

const originData = ref<any | null>(null)

// 常规联动
const normalParentChecked = ref(false)
const normalList = ref<string[]>([])
const normalAllOptions = ['email', 'upload']

// 联动报警输出
const alarmParentChecked = ref(false)
const alarmOutputList = ref<string[]>([])
const alarmAllOptions = ['A-1']

// 录像联动
const recordParentChecked = ref(false)
const recordLinkageList = ref<string[]>([])
const recordAllOptions = ['A1']

const isInitializing = ref(false)

watch(normalParentChecked, (val) => {
  if (isInitializing.value) return
  if (!val) {
    normalList.value = []
    return
  }
  // 开启但未选择任何子项时，默认全选
  if (!normalList.value.length) {
    normalList.value = [...normalAllOptions]
  }
})

watch(normalList, (val) => {
  if (isInitializing.value) return
  normalParentChecked.value = val.length > 0
})

watch(alarmParentChecked, (val) => {
  if (isInitializing.value) return
  if (!val) {
    alarmOutputList.value = []
    return
  }
  if (!alarmOutputList.value.length) {
    alarmOutputList.value = [...alarmAllOptions]
  }
})

watch(alarmOutputList, (val) => {
  if (isInitializing.value) return
  alarmParentChecked.value = val.length > 0
})

watch(recordParentChecked, (val) => {
  if (isInitializing.value) return
  if (!val) {
    recordLinkageList.value = []
    return
  }
  if (!recordLinkageList.value.length) {
    recordLinkageList.value = [...recordAllOptions]
  }
})

watch(recordLinkageList, (val) => {
  if (isInitializing.value) return
  recordParentChecked.value = val.length > 0
})

const fetchSetting = async() => {
  if (!props.deviceId) return
  const params: any = {
    accessToken: store.userInfo?.accessToken,
    deviceId: props.deviceId
  }
  const res: any = await getVlsAudioLinkageModeSetting(params)
  if (res && (res.code === 0 || res.code === 200) && res.data) {
    const data = res.data || {}
    originData.value = data

    isInitializing.value = true

    // 常规联动
    normalParentChecked.value = Number(data.conventionalLinkageEnabled) === 1
    normalList.value = []
    if (Number(data.emailLinkageEnabled) === 1) normalList.value.push('email')
    if (Number(data.uploadCenterLinkageEnabled) === 1) normalList.value.push('upload')
    if (normalParentChecked.value && !normalList.value.length) {
      normalList.value = [...normalAllOptions]
    }

    // 报警输出
    alarmParentChecked.value = Number(data.alarmOutputLinkageEnabled) === 1
    const alarmChannelStr = (data.alarmOutputChannel || '').trim()
    alarmOutputList.value = alarmChannelStr ? alarmChannelStr.split(',').map((s: string) => s.trim()).filter(Boolean) : []
    alarmOutputList.value = alarmOutputList.value.filter(v => alarmAllOptions.includes(v))
    if (alarmParentChecked.value && !alarmOutputList.value.length) {
      alarmOutputList.value = [...alarmAllOptions]
    }

    // 录像联动
    recordParentChecked.value = Number(data.recordLinkageEnabled) === 1
    const recordChannelStr = (data.recordChannel || '').trim()
    recordLinkageList.value = recordChannelStr ? recordChannelStr.split(',').map((s: string) => s.trim()).filter(Boolean) : []
    recordLinkageList.value = recordLinkageList.value.filter(v => recordAllOptions.includes(v))
    if (recordParentChecked.value && !recordLinkageList.value.length) {
      recordLinkageList.value = [...recordAllOptions]
    }

    isInitializing.value = false
  }
}

const save = async() => {
  if (!props.deviceId) return
  const base = originData.value || {}

  const payload: any = {
    id: base.id,
    tenantId: base.tenantId || '',
    deviceId: base.deviceId || props.deviceId,
    conventionalLinkageEnabled: normalParentChecked.value ? 1 : 0,
    emailLinkageEnabled: normalParentChecked.value && normalList.value.includes('email') ? 1 : 0,
    uploadCenterLinkageEnabled: normalParentChecked.value && normalList.value.includes('upload') ? 1 : 0,
    alarmOutputLinkageEnabled: alarmParentChecked.value ? 1 : 0,
    alarmOutputChannel: alarmParentChecked.value ? alarmOutputList.value.join(',') : '',
    recordLinkageEnabled: recordParentChecked.value ? 1 : 0,
    recordChannel: recordParentChecked.value ? recordLinkageList.value.join(',') : '',
    remark: base.remark || ''
  }

  const params = {
    ...payload,
    accessToken: store.userInfo?.accessToken
  }

  const res: any = await saveVlsAudioLinkageModeSetting(params)
  if (res && (res.code === 0 || res.code === 200)) {
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
.linkage-body {
  padding: 0 16px 16px 16px;
}

.linkage-section {
  margin-top: 16px;
  background-color: #fff;
  border-radius: 7px;
  padding: 0 0 12px 0;
}

.linkage-section-header {
  display: flex;
  align-items: center;
  padding: 12px;
}

.parent-checkbox {
  color: #000000;
  font-size: 14px;
}

.card-container {
  padding: 12px;
  padding-left: 24px;
}

.checkbox-group {
  --van-checkbox-color: #2f69f8;
  --van-checkbox-icon-size: 18px;
  --van-checkbox-label-font-size: 16px;

  display: flex;
  flex-direction: column;
  gap: 20px;
}
</style>

