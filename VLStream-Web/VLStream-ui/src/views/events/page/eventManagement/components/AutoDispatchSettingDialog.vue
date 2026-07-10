<template>
  <el-dialog
    :model-value="modelValue"
    title="设置"
    width="35%"
    class="auto-dispatch-dialog"
    append-to-body
    @close="handleCancel"
  >
    <div class="info-banner">
      <img src="@/assets/img/pri_icon.png" alt="" />
      <div class="info-text">
        {{ infoText }}
      </div>
    </div>

    <div class="form-row">
      <div class="label">
        选择工单
      </div>
      <el-select v-model="localValue" class="workorder-select" placeholder="请选择工单">
        <el-option
          v-for="item in options"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>
    </div>
    <div class="workorder-content">
      更多自动派单
      <router-link to="/sysWork" class="workorder-content-item">
        <span>工单设置</span>
        <oort-svg-icon class="app-card-logo" name="forward" />
      </router-link>
      ,
      或联系管理员在
      <div class="workorder-content-item" @click="processuiRouterLinkFn">
        <span>统一工单中台配置</span>
        <oort-svg-icon class="app-card-logo" name="forward" />
      </div>
    </div>
    <div class="dialog-footer">
      <el-button @click="handleCancel">
        取消
      </el-button>
      <el-button type="primary" @click="handleConfirm">
        确定
      </el-button>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { listModel } from '@/api/processui'
import { useUserStore } from '@/store/modules/useraPaas'
import { openApaasWebPage } from '@/utils/apaasApiBase'
import { resolveWorkOrderAppContext } from '@/utils/workOrderAppContext'
import Config from '@/config'

interface OptionItem {
  label: string
  value: string
}

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  infoText: {
    type: String,
    default: 'AI主动安全告警事件触发后，系统自动派单。'
  },
  model: {
    type: String,
    default: ''
  },
  processId: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue', 'confirm'])

const store: any = useUserStore()
const localValue = ref(props.model)
const options = ref<OptionItem[]>([])
const workOrderList = ref<any[]>([]) // 保存完整的工单列表数据

// 获取工单列表
const fetchWorkOrderList = async() => {
  try {
    const appObj = await resolveWorkOrderAppContext()
    const data = {
      accessToken: store.userInfo?.accessToken,
      pageNum: 1,
      pageSize: 99,
      workOrderCategory: appObj?.appId
    }
    const res: any = await listModel(data)
    if (res?.code === 200 && res?.rows) {
      workOrderList.value = res.rows // 保存完整数据
      options.value = res.rows.map((item: any) => ({
        label: item.modelName,
        value: item.modelId
      }))

      // 根据 processId 匹配 definitionId，设置选中的工单
      if (props.processId && workOrderList.value.length > 0) {
        const matchedWorkOrder = workOrderList.value.find((item: any) => item.definitionId === props.processId)
        if (matchedWorkOrder) {
          localValue.value = matchedWorkOrder.modelId
        }
      }
    } else {
      options.value = []
      workOrderList.value = []
    }
  } catch (error) {
    // 获取工单列表失败，使用空数组
    options.value = []
    workOrderList.value = []
  }
}

// 统一工单中台配置
const processuiRouterLinkFn = () => {
  let appendStr = '?accessToken=' + store.token + '&fromWhere=console_manage'
  'http://183.62.103.20:21410/bus/apaas-web/processui/index.html#/ybpHome'
  window.open(Config.URL.slice(0, -1) + Config.frontURLStr + '/processui/index.html' + appendStr, '_blank')
}

watch(
  () => props.model,
  (val) => {
    localValue.value = val
  },
  { immediate: true }
)

watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      // 对话框打开时获取工单列表
      fetchWorkOrderList()
      localValue.value = props.model
    } else {
      localValue.value = ''
    }
  }
)

// 监听 processId 变化，更新选中的工单
watch(
  () => props.processId,
  (val) => {
    if (val && workOrderList.value.length > 0) {
      const matchedWorkOrder = workOrderList.value.find((item: any) => item.definitionId === val)
      if (matchedWorkOrder) {
        localValue.value = matchedWorkOrder.modelId
      }
    }
  }
)

const infoText = computed(() => props.infoText)

const handleCancel = () => {
  emit('update:modelValue', false)
}

const handleConfirm = () => {
  // 根据选择的 modelId 找到对应的 definitionId 和 modelName
  const selectedWorkOrder = workOrderList.value.find((item: any) => item.modelId === localValue.value)
  const definitionId = selectedWorkOrder?.definitionId || ''
  const modelName = selectedWorkOrder?.modelName || ''
  emit('confirm', {
    definitionId,
    modelName
  })
  emit('update:modelValue', false)
}
</script>

<style lang="scss" scoped>
.auto-dispatch-dialog {
  :deep(.el-dialog__header) {
    border-bottom: none;
    padding-bottom: 0;
  }

  :deep(.el-dialog__body) {
    padding: 10px 20px 0 20px;
  }
}

.info-banner {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  border-radius: 4px;
  margin-bottom: 20px;
  background: rgba(72, 93, 255, 0.12);
  img{
    width: 14px;
    height: 14px;
  }
  .info-text{
    font-size: 12px;
    line-height: 24px;
    color: #485DFF;
  }
}

.form-row {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 0 12px;
}

.label {
  width: 80px;
  text-align: right;
  font-size: 16px;
  color: #666;
}

.workorder-select {
  flex: 1;
}

.workorder-content{
  display: flex;
  align-items: center;
  margin-top: 20px;
  gap: 3px;
  .workorder-content-item{
    display: flex;
    align-items: center;
    gap: 3px;
    color: var(--el-color-primary);
    cursor: pointer;
  }
  .app-card-logo{
    width: 16px;
  }
}

.dialog-footer {
  text-align: right;
  margin-top: 20px;
}
</style>

