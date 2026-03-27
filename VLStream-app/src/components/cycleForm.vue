<template>
  <div class="cycle-form-wrapper">
    <!-- 工单循环 -->
    <van-field readonly label="巡查时间" label-align="top">
      <template #left-icon>
        <img class="label-icon" src="@/assets/img/maintenanceManagement/itmeIcon.png" alt="时间图标" />
        <span class="custom-required">*</span>
      </template>
      <template #input>
        <div class="maintainTime">
          <div class="timeButBox">
            <div class="timeBut" :class="{ butActive: localEvery === 'every' }" @click="updateEvery('every')">
              每天
            </div>
            <div class="timeBut" :class="{ butActive: localEvery === 'next' }" @click="updateEvery('next')">
              隔天
            </div>
            <div class="timeBut" :class="{ butActive: localEvery === 'week' }" @click="updateEvery('week')">
              每周
            </div>
            <div class="timeBut" :class="{ butActive: localEvery === 'month' }" @click="updateEvery('month')">
              每月
            </div>
          </div>
        </div>
      </template>
    </van-field>
    <van-cell-group v-if="modelValue.cycle" class="selectionCriteria">
      <div class="time2SelBox">
        <!-- 开始时间 -->
        <van-field
          v-model="localStartTime"
          readonly
          label="开始时间"
          placeholder="选择时间"
          @click="showStartTimePicker = true"
        />

        <!-- 触发时间 -->
        <van-field
          v-model="localTime"
          readonly
          label="触发时间"
          placeholder="00:00:00"
          @click="showTimePicker = true"
        />
        <van-field v-if="localEvery==='next'" v-model="localStartTime" label-width="80px" type="number" label="每隔" class="custom-input">
          <template #right-icon>
            <span class="timeUnit">天</span>
          </template>
        </van-field>

        <!-- 每周 -->
        <van-cell v-if="localEvery==='week'" title="触发日期">
          <van-checkbox-group v-model="localWeek" @change="update('week', $event)">
            <van-checkbox name="1">
              星期一
            </van-checkbox>
            <van-checkbox name="2">
              星期二
            </van-checkbox>
            <van-checkbox name="3">
              星期三
            </van-checkbox>
            <van-checkbox name="4">
              星期四
            </van-checkbox>
            <van-checkbox name="5">
              星期五
            </van-checkbox>
            <van-checkbox name="6">
              星期六
            </van-checkbox>
            <van-checkbox name="7">
              星期日
            </van-checkbox>
          </van-checkbox-group>
        </van-cell>
        <van-cell v-if="localEvery==='week'" title="每隔">
          <van-stepper
            v-model="localNext"
            :min="0"
            :max="99"
            @change="update('next', $event)"
          />
          <span style="margin-left: 8px;">周</span>
        </van-cell>

        <!-- 每月 -->
        <van-cell v-if="localEvery==='month'" title="触发日期">
          <van-checkbox-group v-model="localMonth" @change="update('month', $event)">
            <van-checkbox v-for="(item,i) in 31" :key="i" :name="''+item">
              {{ item > 9 ? '' + item : '0' + item }}
            </van-checkbox>
            <van-checkbox name="L">
              最后一天
            </van-checkbox>
          </van-checkbox-group>
        </van-cell>

        <!-- 结束时间 -->
        <van-field
          v-model="localEndTime"
          readonly
          label="结束时间"
          placeholder="选择时间"
          @click="showEndTimePicker = true"
        />

        <!-- 提示 -->
        <div class="tipBox">
          <van-icon name="warning-o" />
          <span v-if="localEvery==='every'">每天按设置时间点触发，直到结束时间</span>
          <span v-if="localEvery==='next'">隔天按设置间隔触发，直到结束时间</span>
          <span v-if="localEvery==='week'">每周按设置日期和时间触发，直到结束时间</span>
          <span v-if="localEvery==='month'">每月按设置日期和时间触发，直到结束时间</span>
        </div>
      </div>
    </van-cell-group>

    <!-- 开始时间选择器 -->
    <van-popup v-model:show="showStartTimePicker" position="bottom">
      <van-datetime-picker
        :model-value="startTimeDate"
        type="datetime"
        title="选择开始时间"
        @confirm="onStartTimeConfirm"
        @cancel="showStartTimePicker = false"
      />
    </van-popup>

    <!-- 结束时间选择器 -->
    <van-popup v-model:show="showEndTimePicker" position="bottom">
      <van-datetime-picker
        :model-value="endTimeDate"
        type="datetime"
        title="选择结束时间"
        @confirm="onEndTimeConfirm"
        @cancel="showEndTimePicker = false"
      />
    </van-popup>

    <!-- 时间选择器 -->
    <van-popup v-model:show="showTimePicker" position="bottom">
      <van-time-picker
        :model-value="timeDate"
        title="选择时间"
        @confirm="onTimeConfirm"
        @cancel="showTimePicker = false"
      />
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import dayjs from 'dayjs'
// 移除不再使用的 butActive 变量
interface CycleFormData {
  priority: string
  cycle: boolean
  every: string
  time: string
  week: string[]
  month: string[]
  interval: number
  next: number
  startTime?: string
  endTime?: string
}

const props = defineProps<{ modelValue: CycleFormData }>()
const emit = defineEmits<{ 'update:modelValue': [value: CycleFormData] }>()

// 本地数据
const localEvery = ref(props.modelValue.every)
const localTime = ref(props.modelValue.time)
const localWeek = ref([...props.modelValue.week])
const localMonth = ref([...props.modelValue.month])
const localNext = ref(props.modelValue.next)
const localStartTime = ref(props.modelValue.startTime)
const localEndTime = ref(props.modelValue.endTime)

// 弹窗控制
const showStartTimePicker = ref(false)
const showEndTimePicker = ref(false)
const showTimePicker = ref(false)

// 日期时间数据
const startTimeDate = ref<any>(props.modelValue.startTime ? new Date(props.modelValue.startTime) : new Date())
const endTimeDate = ref<any>(props.modelValue.endTime ? new Date(props.modelValue.endTime) : new Date())
const timeDate = ref<any>(props.modelValue.time ? new Date(`2000-01-01 ${props.modelValue.time}`) : new Date())

// 监听 props 变化
watch(() => props.modelValue, (newVal) => {
  localEvery.value = newVal.every
  localTime.value = newVal.time
  localWeek.value = [...newVal.week]
  localMonth.value = [...newVal.month]
  localNext.value = newVal.next
  localStartTime.value = newVal.startTime
  localEndTime.value = newVal.endTime

  if (newVal.startTime) {
    startTimeDate.value = new Date(newVal.startTime)
  }
  if (newVal.endTime) {
    endTimeDate.value = new Date(newVal.endTime)
  }
  if (newVal.time) {
    timeDate.value = new Date(`2000-01-01 ${newVal.time}`)
  }
}, { deep: true })

// 时间确认处理
const onStartTimeConfirm = (value: Date) => {
  const formattedTime = dayjs(value).format('YYYY-MM-DD HH:mm:ss')
  localStartTime.value = formattedTime
  update('startTime', formattedTime)
  showStartTimePicker.value = false
}

const onEndTimeConfirm = (value: Date) => {
  const formattedTime = dayjs(value).format('YYYY-MM-DD HH:mm:ss')
  localEndTime.value = formattedTime
  update('endTime', formattedTime)
  showEndTimePicker.value = false
}

const onTimeConfirm = (value: Date) => {
  const formattedTime = dayjs(value).format('HH:mm:ss')
  localTime.value = formattedTime
  update('time', formattedTime)
  showTimePicker.value = false
}

const update = (key: keyof CycleFormData, val: any) => {
  emit('update:modelValue', { ...props.modelValue, [key]: val })
}

const updateEvery = (value: string) => {
  localEvery.value = value
  update('every', value)
}
</script>

<style lang="scss" scoped>
// 模拟原来的 formBox 样式
.cycle-form-wrapper {
  // 移除Vant的默认样式，使用更接近Element Plus的样式
  :deep(.van-cell-group) {
    margin-bottom: 20px;
    border: none;
    background: transparent;

    .van-cell-group__title {
      font-size: 12x;
      font-weight: 500;
      color: #606266;
      margin-bottom: 12px;
      padding: 0;
    }
  }

  :deep(.van-cell) {
    padding: 8px 0;
    border: none;
    background: transparent;
    align-items: center;

    .van-cell__title {
      font-size: 12px;
      color: #606266;
      font-weight: 500;
      flex: none !important;
    }

    .van-cell__value {
      flex: 1;
      text-align: left !important;
    }
  }

  // 单选按钮组样式 - 更接近Element Plus的效果
  :deep(.van-radio-group) {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;

    .van-cell {
      padding: 0;
      margin-bottom: 8px;

      .van-radio {
        margin: 0;

        .van-radio__label {
          margin-left: 8px;
          font-size: 12px;
          color: #606266;
        }

        .van-radio__icon {
          border: 1px solid #dcdfe6;
          border-radius: 2px;

          &.van-radio__icon--checked {
            background-color: #409eff;
            border-color: #409eff;
          }
        }
      }
    }
  }

  // 复选框组样式
  :deep(.van-checkbox-group) {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;

    .van-checkbox {
      margin: 0;

      .van-checkbox__label {
        font-size: 12px;
        color: #606266;
      }

      .van-checkbox__icon {
        border: 1px solid #dcdfe6;
        border-radius: 2px;

        &.van-checkbox__icon--checked {
          background-color: #409eff;
          border-color: #409eff;
        }
      }
    }
  }

  // 步进器样式
  :deep(.van-stepper) {
    display: inline-block;

    .van-stepper__minus,
    .van-stepper__plus {
      border: 1px solid #dcdfe6;
      background: #fff;
      color: #606266;

      &:active {
        background: #f5f7fa;
      }
    }

    .van-stepper__input {
      border: 1px solid #dcdfe6;
      background: #fff;
      color: #606266;
    }
  }

  // 字段样式
  :deep(.van-field) {
    .van-field__control {
      border: 1px solid #dcdfe6;
      border-radius: 4px;
      padding: 8px 12px;
      background: #fff;
      color: #606266;
      font-size: 12px;

      &:focus {
        border-color: #409eff;
      }
    }
  }
}

// 延时选择框样式 - 更接近原来的效果
.time2SelBox {
  width: 75%;
  border: 1px solid #e4e4e4;
  padding: 20px;
  flex-direction: column;
  align-items: flex-start;
  gap: 20px;
  border-radius: 6px;
  background: #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

// 提示框样式
.tipBox {
  align-items: flex-start;
  // width: 100%;
  padding: 16px 16px;
  background: #f0f9ff;
  border: 1px solid #b3d8ff;
  border-radius: 6px;
  font-size: 12px;
  color: #1F84F0;
  line-height: 20px;
  font-weight: 400;
  gap: 8px;

  .van-icon {
    font-size: 16px;
    color: #409eff;
    flex-shrink: 0;
    margin-top: 2px;
  }
}

// 响应式设计
@media (max-width: 768px) {
  .time2SelBox {
    padding: 16px;
    gap: 16px;
  }

  .tipBox {
    padding: 12px 12px;
    font-size: 12px;
  }
}
.label-icon{
  width: 20px;
  height: 20px;
}
.custom-required {
  color: #ee0a24;
  font-size: 14px;
  margin-left: 5px;
}
.maintainTime {
  width: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  // align-items: center;
}

.timeButBox {
  display: flex;
  align-items: center;
}

.timeBut {
  width: 65px;
  line-height: 28px;
  border-radius: 5px;
  background-color: #F6F6F6;
  text-align: center;
  margin: 10px 5px;
  font-family: AppleSystemUIFont;
  font-size: 12px;
  color: #999999;
  font-weight: 400;

}
.butActive {
  background-color: #2F69F8;
  color: #fff;
}
.selectionCriteria{
  text-align: center;
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;

}
.custom-input {
  display: flex;
  align-items: center;
  width: 100%;
  min-height: 30px;
  margin-left: 0px !important;
}
</style>
