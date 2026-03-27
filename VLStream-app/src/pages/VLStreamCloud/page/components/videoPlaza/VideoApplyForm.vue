<template>
  <oort-popup
    v-model="innerShow"
    position="center"
    round
    style="width: 90%; max-width: 440px;"
  >
    <div class="apply-dialog">
      <div class="dialog-title">
        申请使用
      </div>

      <div class="form-content">
        <div class="form-item">
          <label class="form-label">申请人</label>
          <van-field
            v-model="form.applyUserName"
            placeholder="请输入"
            class="form-input"
          />
        </div>

        <div class="form-item">
          <label class="form-label">申请原因</label>
          <van-field
            v-model="form.applyReason"
            placeholder="请输入"
            class="form-input"
          />
        </div>

        <div class="form-item">
          <label class="form-label">申请备注</label>
          <van-field
            v-model="form.applyRemark"
            type="textarea"
            rows="3"
            autosize
            placeholder="请输入"
            class="form-textarea"
          />
        </div>
      </div>

      <div class="dialog-footer">
        <van-button
          class="btn cancel"
          @click="onCancel"
        >
          取消
        </van-button>
        <div class="divider" />
        <van-button
          class="btn confirm"
          @click="onSubmit"
        >
          申请
        </van-button>
      </div>
    </div>
  </oort-popup>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { vlsCameraApply } from '@/api/VLStreamCloud/device'
import OortPopup from '@/components/popup/oort_popup.vue'
import { showToast } from 'vant'

const props = defineProps<{
  show: boolean
  deviceInfoId: string | number
}>()

const emit = defineEmits(['update:show', 'submit', 'cancel'])

const innerShow = ref(props.show)

watch(
  () => props.show,
  val => {
    innerShow.value = val
  }
)

watch(innerShow, val => {
  emit('update:show', val)
})

const form = ref({
  applyUserName: '',
  applyReason: '',
  applyRemark: ''
})

const onCancel = () => {
  emit('cancel', form.value)
  innerShow.value = false
}

const onSubmit = async() => {
  if (!props.deviceInfoId) {
    showToast({ type: 'fail', message: '缺少设备ID' })
    return
  }
  if (!form.value.applyUserName) {
    showToast({ type: 'fail', message: '请填写申请人' })
    return
  }

  const payload = {
    deviceInfoId: props.deviceInfoId,
    applyReason: form.value.applyReason || '',
    applyRemark: form.value.applyRemark || '',
    applyUserName: form.value.applyUserName || ''
  }

  const res: any = await vlsCameraApply(props.deviceInfoId, payload)
  if (res && res.code === 200) {
    showToast({ type: 'success', message: res?.msg || '申请成功' })
    emit('submit', payload)
    innerShow.value = false
  }
}
</script>

<style scoped lang="scss">
.apply-dialog {
  background: #fff;
  border-radius: 12px;
  padding: 0;
  overflow: hidden;
}

.dialog-title {
  text-align: center;
  line-height: 21px;
  font-weight: 500;
  color: #333333;
  font-size: 16px;
  padding: 16px 0;
}

.form-content {
  padding: 0 20px;
}

.form-item {
  margin-bottom: 16px;

  &:last-of-type {
    margin-bottom: 0;
  }
}

.form-label {
  display: block;
  font-size: 14px;
  color: #111827;
  margin-bottom: 8px;
  font-weight: 400;
}

:deep(.van-cell){
  border: 1px solid #c3c3c3;
  border-radius:8px;
  background:rgba(247, 247, 247, 0);
}

.form-textarea {
  :deep(.van-field__control) {
    font-size: 14px;
    min-height: 80px;
    resize: vertical;
  }

  :deep(.van-field__body) {
    padding: 0;
  }
}

.dialog-footer {
  padding: 16px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.btn {
  flex: 1;
  height: 44px;
  border: none;
  background: transparent;
  font-size: 16px;
  border-radius: 0;
}

.btn.cancel {
  color: #111827;
  font-weight: 400;
}

.btn.confirm {
  color: #2f69f8;
  font-weight: 500;
}

.divider {
  width: 1px;
  height: 24px;
  background: #e5e7eb;
  margin: 0 8px;
}
</style>

