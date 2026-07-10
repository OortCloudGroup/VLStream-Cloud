<template>
  <div class="prop_body">
    <div class="prop_body_tab">
      <el-tabs v-model="activeName" class="demo-tabs">
        <el-tab-pane label="节点设置" name="zero">
          <div class="prop_title">
            <span>节点名称</span>
          </div>
          <div class="prop_item">
            <el-input
              v-model="nodeName"
              placeholder="请输入节点名称"
            />
          </div>
          <div class="prop_title">
            <span>延时方式</span>
          </div>
          <div class="prop_item">
            <el-radio-group v-model="activeChooseData.waitType" size="large">
              <el-radio-button label="固定时长" value="duration" />
              <el-radio-button label="自动计算(日期)" value="date" />
            </el-radio-group>
          </div>
          <div v-if="activeChooseData.waitType === 'duration'" class="prop_item">
            <el-input v-model.number="activeChooseData.delayDuration" style="width: 160px" placeholder="请输入时长">
              <template #append>
                小时
              </template>
            </el-input>
            <span> 后， 进入下一步</span>
          </div>
          <div v-if="activeChooseData.waitType === 'date'" class="prop_item">
            <el-date-picker v-model="activeChooseData.timeDate" value-format="YYYY-MM-DD" style="width: 160px" placeholder="请输入延时时间" />
            <span> 后，进入下一步</span>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
    <div class="prop_body_bottom button_group">
      <!-- 两个按钮 一个取消 ，一个确定 -->
      <el-button @click="cancel">
        取消
      </el-button>
      <el-button type="primary" @click="confirm">
        确定
      </el-button>
    </div>
  </div>
</template>

<script setup>

import { ref, onMounted } from 'vue'
import { ElButton, ElRadioGroup, ElRadioButton, ElInput, ElDatePicker, ElTabs, ElTabPane } from 'element-plus'

const props = defineProps({
  nodeConfig: {
    type: Object,
    default: () => ({})
  }
})

const activeName = ref('zero')

const activeChooseData = ref({
  waitType: 'duration', // duration 固定时间，date 自动计算(日期)
  unit: '时', // 单位
  delayDuration: 0, // 延时时间
  timeDate: '' // 自动计算时间
})

import { setYsclErrorMsg } from '@/utils/setNodeErrorMsg'
const setErrorMsg = () => {
  setYsclErrorMsg(activeChooseData.value, props.nodeConfig.id)
}

const emits = defineEmits(['update:nodeConfig', 'close'])
function cancel() {
  emits('close')
}

const nodeName = ref(props.nodeConfig.nodeName)
function confirm() {
  // 属性单词
  // props.nodeConfig.property = activeChooseData.value
  const nodeConfig = { ...props.nodeConfig, ...activeChooseData.value }
  nodeConfig.nodeName = nodeName.value
  setErrorMsg()
  emits('update:nodeConfig', nodeConfig)
  emits('close')
}

onMounted(() => {
  activeChooseData.value = { ...activeChooseData.value, ...props.nodeConfig }
  setErrorMsg()
})

</script>

<style scoped lang="scss">
    .prop_body {
      display: flex;
      height: 100%;
      width: 100%;
      flex-direction: column;

      &_tab {
        display: flex;
        flex-direction: column;
        flex: 1;
        padding: 16px;
        overflow: auto;
      }

      &_bottom {
        display: flex;
        align-items: center;
        height: 48px;
        padding: 0 32px;
        justify-content: flex-end;
        gap: 10px;
      }
    }

    .prop_title {
      display: flex;
      align-items: center;
      height: 48px;
      span {
        font-size: 14px;
        color: #333333;
        letter-spacing: 0;
        font-weight: 400;
      }
    }

    .prop_item {
      display: flex;
      align-items: center;
      min-height: 48px;
      margin: 8px;

      &_text {
        font-size: 14px;
        margin: 0 6px;
        color: #666;
      }
    }

    .prop_item_group {
      flex-wrap: wrap;
    }

    .button_group button {
      width: 170px;
      height: 48px;
      border-radius: 2px;
    }

</style>
