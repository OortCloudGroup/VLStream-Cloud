<!--
 *@Created by: 兰舰
 * Email: gglanjian@qq.com
 * Phone: 16620805419
 * @Date: 2024-11-15 11:45:51
 * @Last Modified by:  兰舰
 * @Copyright aPaaS-front-team. All rights reserved.
!-->
<template>
  <div class="d_page">
    <van-form ref="formRef" class="formBox" :model="form" :rules="rules">
      <van-cell-group>
        <van-field label="紧急程度" readonly>
          <template #input>
            <van-radio-group v-model="form.priority" direction="horizontal">
              <van-radio name="normal" class="radio-item">
                一般
              </van-radio>
              <van-radio name="urgent" class="radio-item">
                紧急
              </van-radio>
              <van-radio name="critical" class="radio-item">
                严重
              </van-radio>
            </van-radio-group>
          </template>
        </van-field>

        <van-field label="工单循环" readonly>
          <template #input>
            <van-radio-group v-model="form.cycle" direction="horizontal">
              <van-radio :name="false" class="radio-item">
                即时
              </van-radio>
              <van-radio :name="true" class="radio-item">
                循环
              </van-radio>
            </van-radio-group>
          </template>
        </van-field>

        <div v-if="form.cycle" class="cy_every">
          <van-field label="循环类型" readonly>
            <template #input>
              <van-radio-group v-model="form.every" direction="horizontal">
                <van-radio name="every" class="radio-item">
                  每天
                </van-radio>
                <van-radio name="next" class="radio-item">
                  隔天
                </van-radio>
                <van-radio name="week" class="radio-item">
                  每周
                </van-radio>
                <van-radio name="month" class="radio-item">
                  每月
                </van-radio>
              </van-radio-group>
            </template>
          </van-field>
        </div>
      </van-cell-group>

      <van-cell-group v-if="form.cycle" title="循环设置">
        <div class="time2SelBox">
          <van-field
            v-model="form.start_t"
            label="开始时间"
            readonly
            placeholder="选择时间"
            @click="showStartPicker = true"
          />
          <van-popup v-model:show="showStartPicker" position="bottom">
            <van-datetime-picker
              v-model="startDate"
              type="datetime"
              title="选择开始时间"
              @confirm="onStartConfirm"
              @cancel="showStartPicker = false"
            />
          </van-popup>

          <van-field
            v-model="form.time"
            label="触发时间"
            readonly
            placeholder="00:00:00"
            @click="showTimePicker = true"
          />
          <van-popup v-model:show="showTimePicker" position="bottom">
            <van-time-picker
              v-model="timeValue"
              title="选择触发时间"
              @confirm="onTimeConfirm"
              @cancel="showTimePicker = false"
            />
          </van-popup>

          <van-field v-if="form.every==='next'" label="每隔">
            <template #input>
              <van-stepper v-model="form.next" :min="1" :max="99" />
              <span class="unit-text">天</span>
            </template>
          </van-field>

          <van-field v-if="form.every==='week'" label="触发日期" readonly>
            <template #input>
              <van-checkbox-group v-model="form.week" direction="horizontal">
                <van-checkbox name="1" class="checkbox-item">
                  星期一
                </van-checkbox>
                <van-checkbox name="2" class="checkbox-item">
                  星期二
                </van-checkbox>
                <van-checkbox name="3" class="checkbox-item">
                  星期三
                </van-checkbox>
                <van-checkbox name="4" class="checkbox-item">
                  星期四
                </van-checkbox>
                <van-checkbox name="5" class="checkbox-item">
                  星期五
                </van-checkbox>
                <van-checkbox name="6" class="checkbox-item">
                  星期六
                </van-checkbox>
                <van-checkbox name="7" class="checkbox-item">
                  星期日
                </van-checkbox>
              </van-checkbox-group>
            </template>
          </van-field>

          <van-field v-if="form.every==='week'" label="每隔">
            <template #input>
              <van-stepper v-model="form.next" :min="0" :max="99" />
              <span class="unit-text">周</span>
            </template>
          </van-field>

          <van-field v-if="form.every==='month'" label="触发日期" readonly>
            <template #input>
              <div class="month-checkboxes">
                <van-checkbox-group v-model="form.month">
                  <van-checkbox
                    v-for="(item,i) in 31"
                    :key="i"
                    :name="''+item"
                    class="month-checkbox"
                  >
                    {{ item > 9 ? '' + item : '0' + item }}
                  </van-checkbox>
                  <van-checkbox name="L" class="month-checkbox">
                    最后一天
                  </van-checkbox>
                </van-checkbox-group>
              </div>
            </template>
          </van-field>

          <van-field
            v-model="form.end_t"
            label="结束时间"
            readonly
            placeholder="选择时间"
            @click="showEndPicker = true"
          />
          <van-popup v-model:show="showEndPicker" position="bottom">
            <van-datetime-picker
              v-model="endDate"
              type="datetime"
              title="选择结束时间"
              @confirm="onEndConfirm"
              @cancel="showEndPicker = false"
            />
          </van-popup>

          <div class="tipBox">
            <van-icon name="warning-o" />
            <span v-if="form.every==='every'">在开始时间点触发后，在一天内按设置的时间点触发工单，直到截至时间（截止时间无法一直执行）</span>
            <span v-if="form.every==='next'">在开始时间点触发后，在设置时间间隔天数后的当天内按设置的时间点触发工单，直到截止时间（截止时间无法一直执行）</span>
            <span v-if="form.every==='week'">在开始时间点触发后，在一周按设置的时间点触发工单，直到截止时间（截止时间无法一直执行）</span>
            <span v-if="form.every==='month'">在开始时间点后，在一月内按设置的时间点触发任务，直到截至时间</span>
          </div>
        </div>
      </van-cell-group>
    </van-form>

    <div class="button-group">
      <van-button class="cancel-btn" @click="emits('close')">
        取消
      </van-button>
      <van-button type="primary" class="save-btn" @click="save(formRef)">
        保存
      </van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { showToast } from 'vant'

const emits: any = defineEmits(['handle', 'close'])
const props:any = defineProps(['item'])
const formRef = ref('') // form Ref

// 日期时间选择器相关
const showStartPicker = ref(false)
const showEndPicker = ref(false)
const showTimePicker = ref(false)
const startDate = ref(new Date())
const endDate = ref(new Date())
const timeValue = ref(['00', '00', '00'])

const form = reactive({
  priority: 'normal',
  cycle: false,
  every: 'every',
  time: '00:00:00', // 延时act 发送时间段
  week: ['1'],
  month: ['1'],
  interval: 1, // 间隔 隔天、每周 可传此参数
  next: 1,
  start_t: '' as string | undefined,
  end_t: '' as string | undefined
})

const rules = reactive({
  time: [
    { required: true, message: '请选择触发时间' }
  ],
  start_t: [
    { required: true, message: '请选择开始时间' }
  ],
  end_t: [
    { required: true, message: '请选择结束时间' }
  ]
})

// 开始时间确认
const onStartConfirm = (value: Date) => {
  form.start_t = formatDateTime(value)
  showStartPicker.value = false
}

// 结束时间确认
const onEndConfirm = (value: Date) => {
  form.end_t = formatDateTime(value)
  showEndPicker.value = false
}

// 触发时间确认
const onTimeConfirm = (value: string[]) => {
  form.time = value.join(':')
  showTimePicker.value = false
}

// 格式化日期时间
const formatDateTime = (date: Date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

// 保存
const save = async(formEl: any) => {
  if (!formEl) return

  // 验证必填字段
  if (!form.time) {
    showToast('请选择触发时间')
    return
  }
  if (!form.start_t) {
    showToast('请选择开始时间')
    return
  }
  if (!form.end_t) {
    showToast('请选择结束时间')
    return
  }

  console.log(props.item)
  let data:any = {
    priority: form.priority,
    cycle: form.cycle,
    end: form.end_t,
    start: form.start_t,
    types: 1,
    run: [form.time] // 循环定时触发时间: 示例：【每天: 传05:10】, 【隔天：传05:10】,【每周：传 1 (1代表星期一)】,【每月：传 10 （10 代表当月10号）】
  }
  if (form.every === 'next') {
    data.types = 2
    data.interval = form.next // 表示每次触发间隔周期， 隔天、每周 可传此参数
  }
  if (form.every === 'week') {
    data.types = 3
    data.run = form.week
    data.interval = form.next * 7 || 0 // 表示每次触发间隔周期， 隔天、每周 可传此参数
    data['trg_time'] = form.time // 每周月定时类型触发的时分秒时间
  }
  if (form.every === 'month') {
    data.types = 4
    data.run = form.month
    data['trg_time'] = form.time // 每周月定时类型触发的时分秒时间
  }
  showToast('保存成功')
  emits('close')
  emits('handle', data)
}

</script>

<style lang="scss" scoped>
.d_page {
  padding: 16px;
  background-color: #f7f8fa;
  min-height: 100vh;
}

// form
.formBox {
  background-color: #fff;
  border-radius: 8px;
  margin-bottom: 16px;

  .cy_every {
    width: 100%;
    padding-top: 12px;
  }

  .radio-item {
    margin-right: 16px;

    :deep(.van-radio__label) {
      color: #333;
    }

    :deep(.van-radio__icon--checked) {
      background-color: var(--van-primary-color);
      border-color: var(--van-primary-color);
    }
  }

  .checkbox-item {
    margin-right: 8px;
    margin-bottom: 8px;
  }

  .unit-text {
    margin-left: 8px;
    color: #666;
  }

  .month-checkboxes {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;

    .month-checkbox {
      width: 40px;
      height: 32px;
      display: flex;
      align-items: center;
      justify-content: center;
      border: 1px solid #ddd;
      border-radius: 4px;
      font-size: 12px;

      :deep(.van-checkbox__label) {
        font-size: 12px;
      }
    }
  }
}

// 延时选择
.time2SelBox {
  width: 100%;
  border: 1px solid #DADADA;
  padding: 16px;
  border-radius: 8px;
  background-color: #fafafa;
  margin-top: 12px;
}

.tipBox {
  display: flex;
  align-items: flex-start;
  width: 100%;
  padding: 12px;
  background: var(--van-gray-1);
  border: 1px solid var(--van-gray-3);
  border-radius: 4px;
  font-size: 12px;
  color: #666666;
  line-height: 18px;
  font-weight: 400;
  gap: 8px;
  margin-top: 12px;

  .van-icon {
    font-size: 16px;
    color: var(--van-primary-color);
    flex-shrink: 0;
    margin-top: 1px;
  }
}

.button-group {
  display: flex;
  gap: 12px;
  padding: 16px 0;

  .cancel-btn {
    flex: 1;
  }

  .save-btn {
    flex: 1;
  }
}

:deep(.van-cell-group__title) {
  padding: 16px 16px 8px;
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

:deep(.van-field__label) {
  width: 80px;
  color: #333;
}

:deep(.van-field__value) {
  flex: 1;
}
</style>
