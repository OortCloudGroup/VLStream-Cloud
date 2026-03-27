<template>
  <div class="event-strategy">
    <van-form class="form">
      <!-- 启用选项 -->
      <div class="enable-options-section">
        <van-checkbox-group v-model="formData.enableOptions" shape="square">
          <div class="enable-option-item">
            <van-checkbox name="motionDetection">
              启用移动侦测
            </van-checkbox>
          </div>
          <div class="enable-option-item">
            <van-checkbox name="ptzAlarm">
              启用PTZ运动报警上报
            </van-checkbox>
          </div>
          <div class="enable-option-item">
            <van-checkbox name="dynamicAnalysis">
              启用动态分析
            </van-checkbox>
          </div>
        </van-checkbox-group>
      </div>

      <!-- 触发报警时 -->
      <div class="section">
        <div class="section-label">
          触发报警时
        </div>
        <div class="radio-group">
          <van-radio-group v-model="formData.triggerAction" direction="horizontal">
            <van-radio name="snapshot">
              抓图
            </van-radio>
            <van-radio name="record">
              录像
            </van-radio>
          </van-radio-group>
        </div>
      </div>

      <!-- 触发报警前录制时间 -->
      <van-field
        v-model="formData.recordBefore"
        type="number"
        label="触发报警前录制时间"
        placeholder="请输入"
        class="addMargin"
      >
        <template #right-icon>
          <span class="unit-text">秒</span>
        </template>
      </van-field>

      <!-- 触发报警后录制时间 -->
      <van-field
        v-model="formData.recordAfter"
        type="number"
        label="触发报警后录制时间"
        placeholder="请输入"
        class="addMargin"
      >
        <template #right-icon>
          <span class="unit-text">秒</span>
        </template>
      </van-field>

      <!-- 告警频率 -->
      <van-field
        v-model="formData.alarmFrequency"
        type="number"
        label="告警频率"
        placeholder="请输入"
        class="addMargin2"
      >
        <template #right-icon>
          <span class="unit-text">分钟/次</span>
        </template>
      </van-field>

      <!-- 告警级别 -->
      <div class="section">
        <div class="section-label">
          告警级别
        </div>
        <div class="radio-group">
          <van-radio-group v-model="formData.alarmLevel" direction="horizontal">
            <van-radio name="important">
              重要
            </van-radio>
            <van-radio name="general">
              一般
            </van-radio>
            <van-radio name="tip">
              提示
            </van-radio>
            <van-radio name="urgent">
              紧急
            </van-radio>
          </van-radio-group>
        </div>
      </div>

      <!-- 告警方式 -->
      <div class="section">
        <div class="section-label">
          告警方式
        </div>
        <div class="checkbox-group">
          <van-checkbox-group v-model="formData.alarmMethods" shape="square" direction="horizontal">
            <van-checkbox name="sms">
              短信
            </van-checkbox>
            <van-checkbox name="message">
              消息
            </van-checkbox>
            <van-checkbox name="site">
              站内
            </van-checkbox>
            <van-checkbox name="phone">
              电话
            </van-checkbox>
            <van-checkbox name="email">
              邮件
            </van-checkbox>
          </van-checkbox-group>
        </div>
      </div>

      <!-- 接收人 -->
      <div class="maintainPerson">
        <div class="personTitle">
          接收人
        </div>
        <div class="person_list">
          <div
            v-for="(item, index) in recipientList"
            :key="index"
            class="person_list_item"
          >
            <OortImage v-if="!!item.photo" :src="item.photo" />
            <img v-else src="@/assets/img/tx.png" alt="" />
            <span>{{ item.user_name }}</span>
            <div class="clearPerson">
              <img
                src="@/assets/img/maintenanceManagement/icon_Search_clear.png"
                class="clearPersonImg"
                @click="deleteRecipient(item)"
              />
            </div>
          </div>
          <div class="person_list_item">
            <img src="@/assets/img/maintenanceManagement/addPerson.png" alt="" @click="showContact = true" />
            <span />
          </div>
        </div>
      </div>
    </van-form>
    <!-- 时间策略 -->
    <div>
      <DefenseTimeMobile v-model:protection-time="protectionTime" />
    </div>
    <oort-popup v-model="showContact" position="right" style="width: 100%;height: 100%;">
      <Contact :person-list="recipientList" @editClose="contactClose" />
    </oort-popup>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { showToast } from 'vant'
import { getVlsRecordEventStrategy, saveVlsRecordEventStrategy } from '@/api/VLStreamCloud/device'
import { getUserInfoByUserId } from '@/api/unifiedUsert/sso'
import { useUserStore } from '@/store/modules/useraPaas'
import Contact from '@/components/contactTree/Contact.vue'
import OortPopup from '@/components/popup/oort_popup.vue'
import OortImage from '@/components/image/oort_image.vue'
import DefenseTimeMobile from '@/pages/VLStreamCloud/page/components/DefenseTimeMobile.vue'

const props = defineProps({
  deviceId: {
    type: [String, Number],
    default: ''
  }
})

const store = useUserStore()

const formData = ref({
  enableOptions: [], // ['motionDetection', 'ptzAlarm', 'dynamicAnalysis']
  triggerAction: '', // 'record' | 'snapshot'
  recordBefore: '',
  recordAfter: '',
  alarmFrequency: '',
  alarmLevel: '', // 'tip' | 'general' | 'important' | 'urgent'
  alarmMethods: [], // ['site', 'message', 'sms', 'email', 'phone']
  recipientIds: [] // 接收人ID列表
})

const recipientList = ref([])
const showContact = ref(false)

const id = ref('')

// 删除接收人
function deleteRecipient(data) {
  const index = recipientList.value.indexOf(data)
  if (index !== -1) {
    recipientList.value.splice(index, 1)
    formData.value.recipientIds = recipientList.value.map(item => item.user_id)
  }
}

// 联系人选择关闭回调
function contactClose(data) {
  showContact.value = false
  if (data && data.userList) {
    recipientList.value = data.userList
    formData.value.recipientIds = data.userList.map(item => item.user_id)
  }
}

const protectionTime = ref({
  frequency: '每周',
  time_periods: []
})

const fetchStrategy = async() => {
  if (!props.deviceId) return
  const params = {
    accessToken: store.userInfo?.accessToken,
    id: id.value,
    deviceId: Number(props.deviceId)
  }
  const res = await getVlsRecordEventStrategy(props.deviceId, params)
  if (res && res.code === 200 && res.data) {
    const data = res.data
    id.value = data.id

    const enableOptions = []
    if (data.motionDetectionEnabled) enableOptions.push('motionDetection')
    if (data.ptzAlarmReportEnabled) enableOptions.push('ptzAlarm')
    if (data.dynamicAnalysisEnabled) enableOptions.push('dynamicAnalysis')
    formData.value.enableOptions = enableOptions

    formData.value.triggerAction = data.triggerAction || 'record'
    formData.value.recordBefore = data.preRecordSeconds != null ? String(data.preRecordSeconds) : ''
    formData.value.recordAfter = data.postRecordSeconds != null ? String(data.postRecordSeconds) : ''
    formData.value.alarmFrequency = data.alarmFrequencyMinutes != null ? String(data.alarmFrequencyMinutes) : ''
    formData.value.alarmLevel = data.alarmLevel || 'tip'

    let methods = data.alarmMethod
    if (typeof methods === 'string') {
      methods = methods.split(',').map(s => s.trim()).filter(Boolean)
    }
    formData.value.alarmMethods = Array.isArray(methods) ? methods : []

    let receivers = data.receiverIds
    if (typeof receivers === 'string') {
      receivers = receivers.split(',').map(s => s.trim()).filter(Boolean)
    }
    formData.value.recipientIds = Array.isArray(receivers) ? receivers : []

    // 根据 receiverIds 拉取用户详情
    if (Array.isArray(formData.value.recipientIds) && formData.value.recipientIds.length) {
      const ids = formData.value.recipientIds
      const tasks = ids.map(userId =>
        getUserInfoByUserId({ accessToken: store.userInfo?.accessToken, user_id: userId }).catch(() => null)
      )
      const results = await Promise.all(tasks)
      const list = []
      results.forEach((res, idx) => {
        if (res && res.code === 200 && res.data) {
          list.push({
            user_id: ids[idx],
            user_name: res.data.user_name || '',
            photo: res.data.photo || ''
          })
        } else {
          list.push({
            user_id: ids[idx],
            user_name: '',
            photo: ''
          })
        }
      })
      recipientList.value = list
    } else {
      recipientList.value = []
    }

    const pt = data.protectionTime
    if (pt) {
      if (typeof pt === 'string') {
        protectionTime.value = JSON.parse(pt)
      } else if (typeof pt === 'object') {
        protectionTime.value = pt
      }
    }
  }
}

const save = async() => {
  if (!props.deviceId) {
    showToast({ type: 'fail', message: '缺少设备ID' })
    return
  }

  const enableOptions = Array.isArray(formData.value.enableOptions) ? formData.value.enableOptions : []

  const body = {
    accessToken: store.userInfo?.accessToken,
    id: id.value,
    deviceId: Number(props.deviceId),
    motionDetectionEnabled: enableOptions.includes('motionDetection'),
    ptzAlarmReportEnabled: enableOptions.includes('ptzAlarm'),
    dynamicAnalysisEnabled: enableOptions.includes('dynamicAnalysis'),
    occlusionAlarmEnabled: false,
    triggerAction: formData.value.triggerAction || null,
    preRecordSeconds: formData.value.recordBefore ? Number(formData.value.recordBefore) : 0,
    postRecordSeconds: formData.value.recordAfter ? Number(formData.value.recordAfter) : 0,
    alarmFrequencyMinutes: formData.value.alarmFrequency ? Number(formData.value.alarmFrequency) : null,
    alarmLevel: formData.value.alarmLevel || null,
    alarmMethod: Array.isArray(formData.value.alarmMethods) && formData.value.alarmMethods.length
      ? formData.value.alarmMethods.join(',')
      : null,
    receiverIds: Array.isArray(formData.value.recipientIds) && formData.value.recipientIds.length
      ? formData.value.recipientIds.join(',')
      : null,
    protectionTime: protectionTime.value
  }

  const res = await saveVlsRecordEventStrategy(body)
  if (res && res.code === 200) {
    showToast({ type: 'success', message: res.msg || '保存成功' })
    await fetchStrategy()
  }
}

watch(
  () => props.deviceId,
  () => {
    fetchStrategy()
  },
  { immediate: true }
)

// 暴露表单数据、布防时间和保存方法，供父组件使用
defineExpose({
  formData,
  protectionTime,
  save
})
</script>

<style scoped lang="scss">
.form {
  background: #fff;
  margin-bottom: 12px;
}

.checkbox-row {
  padding: 16px;
  background: #fff;
}

.enable-options-section {
  background: #fff;

  :deep(.van-checkbox-group) {
    display: flex;
    flex-direction: column;
  }

  .enable-option-item {
    padding: 16px;
    :deep(.van-checkbox__label){
      color: #000000;
      font-size: 14px;
    }
  }
}

.section {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 16px;
  background: #fff;
}

.section-label {
  color: #666666;
  font-size: 14px;
}

.radio-group {
  :deep(.van-radio-group) {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 10px;
    direction: rtl;
  }

  :deep(.van-radio-group .van-radio) {
    direction: ltr !important;
    justify-content: flex-end !important;
  }
}

.checkbox-group {
  :deep(.van-checkbox-group) {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 8px;
    direction: rtl;
  }

  :deep(.van-checkbox-group .van-checkbox) {
    direction: ltr !important;
    justify-content: flex-end !important;
  }
}

.addMargin {
  margin: 0;
  :deep(.van-field__value){
    max-width: 80px !important;
  }
}

.addMargin2 {
  margin: 0;
  :deep(.van-field__value){
    max-width: 100px !important;
  }
}

.unit-text {
  color: #000000;
  font-size: 14px;
  margin-left: 4px;
}

.maintainPerson {
  display: flex;
  align-items: flex-start;
  padding: 16px;
  gap: 16px;
  justify-content: space-between;
}

.personTitle {
  display: flex;
  align-items: center;
  color: #666666;
  font-size: 14px;
}

.person_list {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  background-color: #ffffff;

  &_item {
    position: relative;
    display: flex;
    align-items: center;
    flex-direction: column;
    justify-content: center;
    gap: 4px;

    img {
      width: 32px;
      height: 32px;
    }

    span{
      color: #333333;
      font-size: 12px;
    }
  }
}

.clearPerson {
  position: absolute;
  top: -10px;
  right: -5px;
}

.clearPersonImg {
  width: 8px !important;
  height: 8px !important;
}

:deep(.van-field__label){
  flex: 1;
  color: #666666;
  font-size: 14px;
}
</style>
