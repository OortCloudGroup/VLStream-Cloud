<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
  Created by: ChaoQun Lei
  Updated by: ChaoQun Lei
-->

<template>
  <div class="prop_body">
    <div class="prop_body_tab">
      <el-tabs v-model="activeName" class="demo-tabs">
        <el-tab-pane label="通知节点" name="first">
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
            <span>通知优先级</span>
          </div>
          <div class="prop_item">
            <div class="priority-options">
              <div
                v-for="(option, index) in priorityOptions"
                :key="index"
                class="priority-option"
                :class="{ 'selected': activeChooseData.priority === option.priorityId }"
                @click="selectPriority(option.priorityId,index)"
              >
                <div class="priority-radio">
                  <div>
                    {{ option.label }}
                    <div v-if="activeChooseData.priority === option.priorityId" class="priority-radio_span flexRowAC">
                      <el-icon>
                        <WarningFilled />
                      </el-icon>
                      &nbsp;已选择此通知方式。若此方式发送失败，系统将自动按优先级[逐步降级]尝试，直到通知成功为止
                    </div>
                  </div>
                  <el-icon v-if="activeChooseData.priority === option.priorityId" class="arrow-icon">
                    <Check />
                  </el-icon>
                </div>
                <div
                  class="channel-types"
                  :class="{ 'show': activeChooseData.priority === option.priorityId }"
                  @click.stop
                >
                  <el-checkbox-group v-model="activeChooseData.channelTypes[index]">
                    <el-checkbox
                      v-for="(channel, channelIndex) in option.channels"
                      :key="channelIndex"
                      :label="channelIndex"
                      :value="channelIndex"
                    >
                      {{ channel }}
                    </el-checkbox>
                  </el-checkbox-group>
                </div>
              </div>
            </div>
          </div>
          <div class="prop_title">
            <span>通知说明文字</span>
          </div>
          <div class="prop_item">
            <el-input
              v-model="activeChooseData.data"
              placeholder="请输入"
            />
          </div>
          <!-- Set notificationobject -->
          <choose-person-panel-notify-node ref="choosePersonPanelRef" v-model:active-choose-data="activeChooseData" />
          <div class="prop_title">
            <span>通知期限（为0则不生效）</span>
          </div>
          <div class="prop_item prop_item_group">
            <el-input
              v-model.number="activeChooseData.timeoutMinutes"
              type="number"
              placeholder="0"
              style="max-width: 200px"
            >
              <template #append>
                分钟
              </template>
            </el-input>
            <!-- user notification after, user Process is is -->
            &nbsp;   &nbsp;   &nbsp; &nbsp;
            <el-radio-group v-model="activeChooseData.timeoutAction">
              <el-radio :value="1">
                重复通知
              </el-radio>
              <el-radio :value="2">
                转下个节点
              </el-radio>
              <el-radio :value="3">
                自动驳回
              </el-radio>
            </el-radio-group>
          </div>
          <template v-if="activeChooseData.timeoutAction===1">
            <div class="prop_title">
              <span>重复通知</span>
            </div>
            <div class="prop_item prop_item_group">
              <el-input
                v-model.number="activeChooseData.repeatCount"
                type="number"
                placeholder="0"
                style="max-width: 200px"
              >
                <template #append>
                  次数
                </template>
              </el-input>
            </div>
          </template>
        </el-tab-pane>
      </el-tabs>
    </div>
    <div class="prop_body_bottom button_group">
      <!-- button , -->
      <el-button @click="cancel" class="common_btn">
        取消
      </el-button>
      <el-button type="primary" @click="confirm" class="common_btn">
        确定
      </el-button>
    </div>
    <el-dialog v-model="chooseUserVis" title="选择人员" width="50%">
      <address-seting-dialog
        :user-list="activeChooseData.timeoutHandlers[addTimeoutRuleIndex].notificationUserIds"
        :mode="3"
        :is-single="false"
        @saveChoose="confirmUser"
        @close="chooseUserVis=false"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, inject } from 'vue'
import ChoosePersonPanelNotifyNode from './components/choosePersonPanelNotifyNode.vue'
import {
  ElButton, ElTabs, ElTabPane, ElRadioGroup, ElRadio, ElInput, ElDialog, ElIcon
} from 'element-plus'
import { getForm } from '@/api/processui'
import { ElMessage } from 'element-plus'
import { extractFormFields } from './utils/formFieldExtractor'
import { getOrCreateNodeFormState, updateNodeFormFields } from './utils/nodeFormStateManager'

const props = defineProps({
  nodeConfig: {
    type: Object,
    default: () => ({})
  }
})

const chooseUserVis = ref(false) //
const addTimeoutRuleIndex = ref(0) // index
// model- data
const flowDesignerPage = inject('flowDesignerPage')
// 0 workflow 1 work order
let formDesignType = ref(undefined)
//
let classifyType = ref(true)
formDesignType.value = flowDesignerPage.formDesignType
if (flowDesignerPage?.synthesisId) classifyType.value = false

const activeName = ref('first')
// Initialize from node Get formfield, is from full Store
const nodeFormState = getOrCreateNodeFormState(props.nodeConfig.id)
const activeChooseData = ref({
  noNotifyAllSteps: false, // Push ( : noNotifyAllSteps=true)
  priority: undefined, // notification
  channelTypes: {
    0: [], // priority=0 channelTypes
    1: [], // priority=1 channelTypes
    2: [], // priority=2 channelTypes
    3: [], // priority=3 channelTypes
    4: [], // priority=4 channelTypes
    5: [] // priority=5 channelTypes
  },
  data: '', // notification
  timeoutMinutes: 0, //
  timeoutAction: '', // : 1- notification, 2- node, 3-
  repeatCount: 1, // notification
  approvalType: 1, // notification
  users: [], // user
  dept: [], // department
  multiPercent: 100, // -100%
  roles: [], // role
  leader: 1, //
  multi: 'sequential', // notification
  emptyApproType: 1, // notification is empty
  emptyApproUser: [], // notification is empty
  shouldSign: false, // notification whether need to
  approDueTime: 0, // notification
  approDueTimeUnit: 1, // notification
  disAgreenEnd: false, // if notification ,whether finish
  formProperties: nodeFormState.formFiledList.value || [], // from node Get , is full Store
  operations: ['0', '3', '4'], // operation - ( , , )
  taskListeners: [],
  formKey: '' // nodeform
})

// notification item data
const priorityOptions = [
  {
    label: 'PSTN电话通知',
    priorityId: 0,
    channels: ['PSTN电话通知']
  },
  {
    label: '视频通话',
    priorityId: 1,
    channels: ['workup视频通话', '钉钉', '微信']
  },
  {
    label: '息屏弹窗提醒',
    priorityId: 2,
    channels: ['workup息屏弹窗提醒', '钉钉', '微信']
  },
  {
    label: '顶部消息栏提醒',
    priorityId: 3,
    channels: ['workup顶部消息栏提醒', '钉钉', '微信']
  },
  {
    label: 'APP红点提示或微信服务号消息',
    priorityId: 4,
    channels: ['APP红点提示或微信服务号消息', '钉钉', '微信']
  },
  {
    label: '短信或邮件提醒',
    priorityId: 5,
    channels: ['短信或邮件提醒']
  }
]

//
function selectPriority(priority, index) {
  if (activeChooseData.value.priority === priority) {
    // activeChooseData.value.priority = undefined
  } else {
    activeChooseData.value.priority = priority
    if (priorityOptions[index]?.channels?.length > 0) {
      if (!Array.isArray(activeChooseData.value.channelTypes[index]) ||
        activeChooseData.value.channelTypes[index].length === 0) {
        activeChooseData.value.channelTypes[index] = [0]
      }
    }
    // null / empty channelTypes
    // for (let i = 0; i < priorityOptions.length; i++) {
    //   if (i !== index) {
    //     activeChooseData.value.channelTypes[i] = []
    //   }
    // }
  }
}

//
function confirmUser(data) {
  activeChooseData.value.timeoutHandlers[addTimeoutRuleIndex.value].notificationUserIds = data.user.map(item => item.user_id)
  chooseUserVis.value = false
}

//
if (!activeChooseData.value.timeoutHandlers) {
  activeChooseData.value.timeoutHandlers = [{
    triggerId: null,
    triggerTime: 0, // value
    triggerTimeUnit: 2, // 1 /2 /3
    triggerType: 1, // (1= notification/2= )
    notificationUserIds: [] // notificationobjectID
  }]
  addTimeoutRuleIndex.value = 0
}
import { setApprovalErrorMsg } from '@/utils/setNodeErrorMsg'
const setErrorMsg = () => {
  setApprovalErrorMsg(activeChooseData.value, props.nodeConfig.id)
}

const emits = defineEmits(['update:nodeConfig', 'close'])
function cancel() {
  emits('close')
}

const nodeName = ref(props.nodeConfig.nodeName)
let choosePersonPanelRef = ref(null) // Set notificationobject
function confirm() {
  // Set notificationobject- and -
  if (activeChooseData.value?.approvalType === 6) { //
    if (choosePersonPanelRef.value?.jobPost === '0') {
      activeChooseData.value.jobLeaders = [choosePersonPanelRef.value?.jobMod]
      activeChooseData.value.postLeaders = undefined
    } else {
      activeChooseData.value.jobLeaders = undefined
      activeChooseData.value.postLeaders = [choosePersonPanelRef.value?.postMod]
    }
  } else {
    activeChooseData.value.postLeaders = undefined
    activeChooseData.value.jobLeaders = undefined
  }

  // , postLeaders can value
  if (activeChooseData.value?.approvalType === 1) {
    activeChooseData.value.postLeaders = []
    activeChooseData.value.leader = null
  } else {
    // leader main 1 , 2 , 3 , 4
    let num = choosePersonPanelRef.value?.postMod || choosePersonPanelRef.value?.jobMod
    // postMod/jobMod is ('0','1','2','3'), leader is (1,2,3,4)
    activeChooseData.value.leader = num ? parseInt(num) + 1 : 1
  }

  /* item -start */
  // : 1- notification, 2- node, 3- timeoutAction etc. 1
  if (activeChooseData.value.timeoutAction !== 1) {
    activeChooseData.value.repeatCount = undefined
  }
  // Set notification
  if (!activeChooseData.value.users || !activeChooseData.value.users?.length) {
    activeChooseData.value.repeatCount = undefined
    return ElMessage.warning('请设置通知对象')
  }
  // ,
  if (activeChooseData.value.priority === 0 || activeChooseData.value.priority === 1) {
    if (!activeChooseData.value.timeoutMinutes) {
      return ElMessage.warning('通知期限不能为0')
    }
  }
  // /* item -end */
  // props.nodeConfig.property = activeChooseData.value
  const nodeConfig = { ...props.nodeConfig, ...activeChooseData.value }
  nodeConfig.nodeName = nodeName.value
  /* notification */
  if (nodeConfig.priority !== undefined) {
    nodeConfig.channelTypes = [...(nodeConfig.channelTypes[nodeConfig.priority] || [])]
  } else {
    nodeConfig.channelTypes = []
  }
  // new item prompt / tip
  setErrorMsg()
  emits('update:nodeConfig', nodeConfig)
  emits('close')

  console.log('nodeConfig', activeChooseData.value)
}

import { listForm } from '@/api/processui'
import AddressSetingDialog from '@/components/personHome/addressSetingDialog.vue'
import { Check } from '@element-plus/icons-vue'
const currentForm = ref(null)
function getFormList() {
  const params = {
    type: formDesignType.value,
    pageNum: 1,
    pageSize: 999,
    isFormComponents: 0
  }
  listForm(params).then(res => {
    (res.rows || []).map(item => {
      if (item.formId === activeChooseData.value.formKey) {
        currentForm.value = item
      }
    })
  })
}

const currentNodeFormProp = ref([])
const setNodeFormFieldProp = async(formKey) => {
  const params = {
    formId: formKey
  }
  let res = await getForm(params)
  if (res.code === 200) {
    try {
      // field
      const jsonList = JSON.parse(res.data.content)
      const formFields = extractFormFields(jsonList)
      currentNodeFormProp.value = JSON.parse(JSON.stringify(formFields))
      // form Update to current
      activeChooseData.value.formProperties = currentNodeFormProp.value || []
      // new node formfield , data
      updateNodeFormFields(props.nodeConfig.id, activeChooseData.value.formProperties)
      // store current forminfo
    } catch (error) {
      console.log(error)
    }
  }
}
onMounted(async() => {
  activeChooseData.value = { ...activeChooseData.value, ...props.nodeConfig }
  /* notification */
  if (props.nodeConfig.priority !== undefined && props.nodeConfig.channelTypes !== undefined) {
    if (Array.isArray(props.nodeConfig.channelTypes)) {
      activeChooseData.value.channelTypes[props.nodeConfig.priority] = [...props.nodeConfig.channelTypes]
    } else if (typeof props.nodeConfig.channelTypes === 'number') {
      activeChooseData.value.channelTypes[props.nodeConfig.priority] = [props.nodeConfig.channelTypes]
    }
  }
  // if nodeConfig in formProperties, need to node
  if (props.nodeConfig.formProperties && props.nodeConfig.formProperties.length > 0) {
    updateNodeFormFields(props.nodeConfig.id, props.nodeConfig.formProperties)
    activeChooseData.value.formProperties = props.nodeConfig.formProperties
  } else if (props.nodeConfig.formKey && activeChooseData.value.formProperties?.length === 0) {
    await setNodeFormFieldProp(props.nodeConfig.formKey)
  }
  if (!props.nodeConfig.formKey) {
    activeChooseData.value.formProperties = []
  }
  // -100%
  if (!props.nodeConfig.activeChooseData) {
    activeChooseData.value.multiPercent = 100
  }
  setErrorMsg()
  getFormList()
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
    padding: 0 20px;
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

  &_add {
    display: flex;
    align-items: center;
    justify-content: center;

    img {
      width: 48px;
      height: 48px;
      cursor: pointer;
    }
  }

  &_user {
    background: #FFFFFF;
    width: 220px;
    min-width: 220px;
    height: 60px;
    position: relative;
    cursor: pointer;
    display: flex;
    flex-direction: row;
    align-items: center;
    margin: 4px;

    &_remove {
      display: none;
      width: 16px;
      height: 16px;
      position: absolute;
      right: 4px;
      top: 4px;
    }

    &_headpic {
      width: 48px;
      height: 48px;
      margin: 8px;
      border-radius: 100%;
    }

    &_name {
      flex: 1;
      display: flex;
      flex-direction: column;

      span:nth-child(1) {
        font-size: 14px;
        color: #333333;
        letter-spacing: 0;
        font-weight: 700;
      }

      span:nth-child(2) {
        font-size: 12px;
        color: #8D93A2;
        letter-spacing: 0;
        font-weight: 400;
      }
    }
  }

  &_user:hover {
    box-shadow: 0px 0px 6px 0px rgba(0, 0, 0, 0.15);
    border-radius: 2px;

    .prop_item_user_remove {
      display: flex !important;
    }
  }
}

.prop_item_group {
  flex-wrap: wrap;
}

.empty_div {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  min-height: 450px;

  span {
    font-size: 16px;
    color: #f8a305;
  }
}

.task_item {
  display: flex;
  position: relative;

  &_delete {
    position: absolute;
    right: 0px;
    top: 0px;
    width: 16px;
    height: 16px;
  }
}

.form_class {
  font-size: 10px;
  color: var(--el-color-primary);
  background-color: var(--el-menu-hover-bg-color);
  border: 2px;
  padding: 4px 6px;
}

.current_form {
  display: flex;
  width: 100%;
  align-items: center;
  height: 32px;

  span {
    font-size: 14px;
    color: var(--el-color-primary);
  }
}

.button_group button {
  width: 170px;
  height: 48px;
  border-radius: 2px;
}

.priority-options {
  width: 100%;
}

.priority-option {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  margin-bottom: 12px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  background-color: #fff;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);

  &:hover {
    border-color: var(--el-color-primary);
    box-shadow: 0 2px 8px var(--el-menu-hover-bg-color);
  }

  &.selected {
    border-color: var(--el-color-primary);
    box-shadow: 0 2px 8px var(--el-menu-hover-bg-color);
    background-color: var(--el-menu-hover-bg-color);
    .priority-radio{
      color: var(--el-color-primary);
    }
  }

  .priority-radio {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-weight: 500;
    color: #606266;

    .arrow-icon {
      transition: transform 0.3s ease;
      font-size: 20px;

      &.expanded {
        transform: rotate(90deg);
      }
    }

    .priority-radio-el {
      flex: 1;

      :deep(.el-radio__input.is-checked+.el-radio__label) {
        color: var(--el-color-primary);
      }
    }
  }

  .channel-types {
    padding-left: 20px;
    max-height: 0;
    overflow: hidden;
    transition: max-height 0.3s cubic-bezier(0.4, 0, 0.2, 1);

    &.show {
      padding-top: 8px;
      max-height: 300px;
    }

    :deep(.el-checkbox-group) {
      display: flex;
      flex-direction: column;

      .el-checkbox {
        margin-bottom: 8px;
        padding: 6px 0;

        &:last-child {
          margin-bottom: 0;
        }

        :deep(.el-checkbox__input.is-checked+.el-checkbox__label) {
          color: var(--el-color-primary);
        }
      }
    }
  }
}

.priority-radio_span{
  padding-top: 8px;
  line-height: 12px;
  font-size: 11px;
  color: #999;
}

</style>
