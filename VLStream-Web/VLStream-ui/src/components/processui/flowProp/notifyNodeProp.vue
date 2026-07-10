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
          <!-- 设置通知对象-->
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
            <!--当用户添加了通知期限后，给用户选择超时处理是退回给发起人还是驳回 -->
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
      <!-- 两个按钮 一个取消 ，一个确定 -->
      <el-button @click="cancel">
        取消
      </el-button>
      <el-button type="primary" @click="confirm">
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

const chooseUserVis = ref(false) // 选人
const addTimeoutRuleIndex = ref(0) // 选人index
// 模型-列表数据
const flowDesignerPage = inject('flowDesignerPage')
// 0 流程 1 工单
let formDesignType = ref(undefined)
// 默认应用
let classifyType = ref(true)
formDesignType.value = flowDesignerPage.formDesignType
if (flowDesignerPage?.synthesisId) classifyType.value = false

const activeName = ref('first')
// 初始化时从节点级别的状态管理器获取表单字段，而不是从全局Store
const nodeFormState = getOrCreateNodeFormState(props.nodeConfig.id)
const activeChooseData = ref({
  noNotifyAllSteps: false, // 站内消息推送(关掉加上：noNotifyAllSteps=true)
  priority: undefined, // 通知优先级
  channelTypes: {
    0: [], // 对应 priority=0 的 channelTypes
    1: [], // 对应 priority=1 的 channelTypes
    2: [], // 对应 priority=2 的 channelTypes
    3: [], // 对应 priority=3 的 channelTypes
    4: [], // 对应 priority=4 的 channelTypes
    5: [] // 对应 priority=5 的 channelTypes
  },
  data: '', // 通知说明文字
  timeoutMinutes: 0, // 超时时间
  timeoutAction: '', // 超时动作: 1-重复通知, 2-自动转下个节点, 3-自动驳回
  repeatCount: 1, // 重复通知次数
  approvalType: 1, // 通知类型
  users: [], // 用户列表
  dept: [], // 部门列表
  multiPercent: 100, // 或签-100%人员通过
  roles: [], // 角色列表
  leader: 1, // 直接上级
  multi: 'sequential', // 多人通知方式
  emptyApproType: 1, // 通知人为空类型
  emptyApproUser: [], // 通知人为空人员列表
  shouldSign: false, // 通知人同意时是否需要签字
  approDueTime: 0, // 通知期限
  approDueTimeUnit: 1, // 通知期限单位
  disAgreenEnd: false, // 如果通知被驳回,是否直接结束
  formProperties: nodeFormState.formFiledList.value || [], // 从节点级别状态获取，而不是全局Store
  operations: ['0', '3', '4'], // 操作权限列表-默认有这三个权限（通过，退回，拒绝）
  taskListeners: [],
  formKey: '' // 节点表单
})

// 通知优先级选项数据
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

// 选择优先级
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
    // 清空其他优先级的 channelTypes
    // for (let i = 0; i < priorityOptions.length; i++) {
    //   if (i !== index) {
    //     activeChooseData.value.channelTypes[i] = []
    //   }
    // }
  }
}

// 保存人员
function confirmUser(data) {
  activeChooseData.value.timeoutHandlers[addTimeoutRuleIndex.value].notificationUserIds = data.user.map(item => item.user_id)
  chooseUserVis.value = false
}

// 添加超时规则
if (!activeChooseData.value.timeoutHandlers) {
  activeChooseData.value.timeoutHandlers = [{
    triggerId: null,
    triggerTime: 0, // 触发时间数值
    triggerTimeUnit: 2, // 时间单位 1天/2小时/3分钟
    triggerType: 1, // 触发类型（1=消息通知/2=自动通过）
    notificationUserIds: [] // 通知对象ID列表
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
let choosePersonPanelRef = ref(null) // 设置通知对象
function confirm() {
  // 设置通知对象-岗位和职位-单选
  if (activeChooseData.value?.approvalType === 6) { // 选择了上级才传参
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

  // 指定了人员，postLeaders不能有值
  if (activeChooseData.value?.approvalType === 1) {
    activeChooseData.value.postLeaders = []
    activeChooseData.value.leader = null
  } else {
    // leader 主管 1 直属上级 ，2 二级上级， 3 三级上级， 4 四级上级
    let num = choosePersonPanelRef.value?.postMod || choosePersonPanelRef.value?.jobMod
    // postMod/jobMod 是字符串('0','1','2','3')，leader 是数字(1,2,3,4)
    activeChooseData.value.leader = num ? parseInt(num) + 1 : 1
  }

  /* 必填项-start */
  // 超时动作: 1-重复通知, 2-自动转下个节点, 3-自动驳回 timeoutAction不等于1不传
  if (activeChooseData.value.timeoutAction !== 1) {
    activeChooseData.value.repeatCount = undefined
  }
  // 必须设置通知谁
  if (!activeChooseData.value.users || !activeChooseData.value.users?.length) {
    activeChooseData.value.repeatCount = undefined
    return ElMessage.warning('请设置通知对象')
  }
  // 电话或者视频通话，期限必填
  if (activeChooseData.value.priority === 0 || activeChooseData.value.priority === 1) {
    if (!activeChooseData.value.timeoutMinutes) {
      return ElMessage.warning('通知期限不能为0')
    }
  }
  // /* 必填项-end */
  // props.nodeConfig.property = activeChooseData.value
  const nodeConfig = { ...props.nodeConfig, ...activeChooseData.value }
  nodeConfig.nodeName = nodeName.value
  /* 通知优先级 */
  if (nodeConfig.priority !== undefined) {
    nodeConfig.channelTypes = [...(nodeConfig.channelTypes[nodeConfig.priority] || [])]
  } else {
    nodeConfig.channelTypes = []
  }
  // 更新必填项的错误提示
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
      // 使用统一的字段提取函数
      const jsonList = JSON.parse(res.data.content)
      const formFields = extractFormFields(jsonList)
      currentNodeFormProp.value = JSON.parse(JSON.stringify(formFields))
      // 切换表单强制将权限修改为当前切换的
      activeChooseData.value.formProperties = currentNodeFormProp.value || []
      // 更新节点级别的表单字段状态，确保数据隔离性
      updateNodeFormFields(props.nodeConfig.id, activeChooseData.value.formProperties)
      // store保存当前表单信息
    } catch (error) {
      console.log(error)
    }
  }
}
onMounted(async() => {
  activeChooseData.value = { ...activeChooseData.value, ...props.nodeConfig }
  /* 通知优先级 */
  if (props.nodeConfig.priority !== undefined && props.nodeConfig.channelTypes !== undefined) {
    if (Array.isArray(props.nodeConfig.channelTypes)) {
      activeChooseData.value.channelTypes[props.nodeConfig.priority] = [...props.nodeConfig.channelTypes]
    } else if (typeof props.nodeConfig.channelTypes === 'number') {
      activeChooseData.value.channelTypes[props.nodeConfig.priority] = [props.nodeConfig.channelTypes]
    }
  }
  // 如果nodeConfig中有formProperties，需要同步到节点级别的状态管理器
  if (props.nodeConfig.formProperties && props.nodeConfig.formProperties.length > 0) {
    updateNodeFormFields(props.nodeConfig.id, props.nodeConfig.formProperties)
    activeChooseData.value.formProperties = props.nodeConfig.formProperties
  } else if (props.nodeConfig.formKey && activeChooseData.value.formProperties?.length === 0) {
    await setNodeFormFieldProp(props.nodeConfig.formKey)
  }
  if (!props.nodeConfig.formKey) {
    activeChooseData.value.formProperties = []
  }
  // 或签-100%人员通过
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
