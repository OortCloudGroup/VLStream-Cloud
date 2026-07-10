<template>
  <div class="prop_body">
    <div class="prop_body_tab">
      <el-tabs v-model="activeName" class="demo-tabs">
        <el-tab-pane :label="flowDesignerPage?.notifyNode? '设置处置人' : '设置审批人'" name="first">
          <div class="prop_title">
            <span>节点名称</span>
          </div>
          <div class="prop_item">
            <el-input
              v-model="nodeName"
              placeholder="请输入节点名称"
            />
          </div>
          <!-- 设置审批对象-->
          <choose-person-panel ref="choosePersonPanelRef" v-model:active-choose-data="activeChooseData" />
          <div class="prop_title">
            <span>多人{{ flowDesignerPage?.notifyNode ? '处置' : '审批' }}方式</span>
          </div>
          <div class="prop_item prop_item_group">
            <el-radio-group v-model="activeChooseData.multi">
              <el-col>
                <el-radio v-if="flowDesignerPage?.notifyNode" value="sequential">
                  依次处置（按顺序处置）
                </el-radio>
                <el-radio v-else value="sequential">
                  依次审批（按顺序审批）
                </el-radio>
              </el-col>
              <el-col>
                <el-radio value="joint">
                  <div class="flexRowAC">
                    会签（需要所有{{ flowDesignerPage?.notifyNode ? '处置' : '审批' }}人都通过）
                    <div>
                      需要
                      <el-input-number v-model="activeChooseData.multiPercent" :min="1" :max="100" />
                      %人员通过
                    </div>
                  </div>
                </el-radio>
              </el-col>
              <el-col>
                <el-radio value="single">
                  或签（其中一名{{ flowDesignerPage?.notifyNode ? '处置' : '审批' }}人通过即可）
                </el-radio>
              </el-col>
            </el-radio-group>
          </div>
          <div class="prop_title">
            <span>{{ flowDesignerPage?.notifyNode ? '处置' : '审批' }}人为空时</span>
          </div>
          <choose-empty-appro-panel v-model:active-choose-data="activeChooseData" />
          <div class="prop_title">
            <span>{{ flowDesignerPage?.notifyNode ? '处置' : '审批' }}人同意时是否需要签字</span>
          </div>
          <div class="prop_item prop_item_group">
            <el-switch v-model="activeChooseData.shouldSign" />
          </div>
          <div class="prop_title">
            <span>{{ flowDesignerPage?.notifyNode ? '处置' : '审批' }}期限（为0则不生效）</span>
          </div>
          <div class="prop_item prop_item_group">
            <el-input
              v-model="activeChooseData.approDueTime"
              placeholder="0"
              style="max-width: 200px"
            >
              <template #append>
                <el-select v-model="activeChooseData.approDueTimeUnit" style="width: 80px">
                  <el-option label="天" :value="1" />
                  <el-option label="小时" :value="2" />
                  <el-option label="分钟" :value="3" />
                </el-select>
              </template>
            </el-input>
            <!--当用户添加了审批期限后，给用户选择超时处理是退回给发起人还是驳回 -->
            &nbsp;   &nbsp;   &nbsp;   &nbsp;
            <el-radio-group v-model="activeChooseData.approTimeoutProcessing">
              <el-radio value="1">
                驳回
              </el-radio>
              <el-radio value="2">
                退回
              </el-radio>
            </el-radio-group>
          </div>
          <div class="prop_title">
            <span>如果{{ flowDesignerPage?.notifyNode ? '处置' : '审批' }}被驳回</span>
          </div>
          <div class="prop_item prop_item_group">
            <el-switch v-model="activeChooseData.disAgreenEnd" />直接结束流程
          </div>
        </el-tab-pane>
        <el-tab-pane label="表单权限" name="second">
          <FormPermissionTable v-if="activeChooseData.formProperties" v-model:filed-list="activeChooseData.formProperties" />
          <div v-else class="empty_div">
            <span>请在发起人节点设置表单</span>
          </div>
        </el-tab-pane>
        <el-tab-pane label="操作权限" name="third">
          <operations-panel v-model:active-choose-data="activeChooseData" />
        </el-tab-pane>
        <el-tab-pane label="监听器" name="fourth">
          <div class="add_listener">
            <el-button type="primary" @click="addTaskListener">
              添加监听器
            </el-button>
          </div>
          <div v-for="(item, index) in activeChooseData.taskListeners" :key="index" class="task_item">
            <listener-panel v-model:item="activeChooseData.taskListeners[index]" :data="item" :node-id="props.nodeConfig.id" />
            <el-icon size="16" class="task_item_delete" color="red" style="cursor: pointer;">
              <DeleteFilled @click="removeListener( index)" />
            </el-icon>
          </div>
        </el-tab-pane>
        <el-tab-pane label="节点表单" name="fivth">
          <div class="prop_title">
            <span>设置节点表单</span>
          </div>
          <div class="prop_item" style="flex-direction: column;">
            <div v-if="currentForm" class="current_form">
              <span>
                当前表单： {{ currentForm.formName }}
              </span>
            </div>
            <el-cascader
              :props="cascaderProps"
              placeholder="请选择表单"
              style="width: 100%"
              @change="setFormKey"
            />
            <!-- <el-select
              v-model="activeChooseData.formKey"
              placeholder="请选择表单"
              style="width: 100%"
            >
              <el-option
                v-for="item in formList"
                :key="item.formId"
                :label="item.formName"
                :value="item.formId"
              />
            </el-select> -->
          </div>
        </el-tab-pane>
        <el-tab-pane label="通知方式" name="sixth">
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
          <div class="prop_item interBox">
            <div class="">
              <span>站内消息推送</span>
              <div v-if="!activeChooseData.noNotifyAllSteps" class="priority-radio_span flexRowAC">
                <el-icon>
                  <WarningFilled />
                </el-icon>
                &nbsp;[此通知独立于上方优先级选择]。勾选后，无论主通知方式是否成功，都会额外发送一条站内消息用于留存记录和后续查阅
              </div>
            </div>
            <div class="prop_item prop_item_group">
              <el-switch
                v-model="activeChooseData.noNotifyAllSteps"
                :active-value="false"
                :inactive-value="true"
              />
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
          <div class="prop_title">
            <span>处置节点</span>
          </div>
          <div v-for="(itt, index) in activeChooseData.timeoutHandlers" :key="index">
            <div class="prop_item">
              <div class="prop_title">
                <span>超时</span>
              </div>
              <div class="prop_item prop_item_group">
                <el-input
                  v-model="itt.triggerTime"
                  placeholder="0"
                  style="max-width: 200px"
                >
                  <template #append>
                    <el-select v-model="itt.triggerTimeUnit" style="width: 80px">
                      <el-option label="天" :value="1" />
                      <el-option label="小时" :value="2" />
                      <el-option label="分钟" :value="3" />
                    </el-select>
                  </template>
                </el-input>
                &nbsp; &nbsp; &nbsp; &nbsp;<div class="prop_title">
                  <span>触发事件</span>
                </div>
                <div class="prop_item">
                  <el-select v-model="itt.triggerType" style="width: 120px">
                    <el-option label="通知" :value="1" />
                    <el-option label="转下一节点" :value="2" />
                  </el-select>
                </div>
              </div>
              <el-button @click="addTimeoutRule">
                +
              </el-button>
              <el-button
                v-if="activeChooseData.timeoutHandlers&&activeChooseData.timeoutHandlers.length>1"
                @click="activeChooseData.timeoutHandlers.splice(index,1)"
              >
                -
              </el-button>
            </div>
            <!--触发事件-通知 -->
            <div v-if="itt.triggerType===1" class="prop_item" style="flex-wrap: wrap">
              <div class="prop_item prop_item_add">
                <img
                  src="@/assets/img/processui/flownode/add.png"
                  @click="chooseUserVis = true,addTimeoutRuleIndex=index"
                />
              </div>
              <div v-for="(child,ind) in itt.notificationUserIds" :key="ind" class="prop_item_user">
                <id2HeadPic :id="child" class="prop_item_user_headpic" />
                <div class="prop_item_user_name">
                  <id2Name :id="child" />
                </div>
                <el-icon
                  class="prop_item_user_remove"
                  color="red"
                  size="20"
                  @click="itt.notificationUserIds.splice(ind,1)"
                >
                  <CircleCloseFilled />
                </el-icon>
              </div>
            </div>
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
import id2HeadPic from '@/components/ID2HeadPic.vue'
import id2Name from '@/components/ID2Name.vue'
import { ref, onMounted, inject } from 'vue'
import ChoosePersonPanel from './components/choosePersonPanel.vue'
import ChooseEmptyApproPanel from './components/chooseEmptyApproPanel.vue'
import FormPermissionTable from './components/formPermissionTable.vue'
import OperationsPanel from './components/operationsPanel.vue'
import ListenerPanel from './components/listenerPanel.vue'
import {
  ElButton, ElTabs, ElTabPane, ElCol, ElRadioGroup, ElRadio, ElSelect, ElOption, ElCascader,
  ElSwitch, ElInput, ElIcon, ElDialog
} from 'element-plus'
import { CircleCloseFilled, DeleteFilled, Check } from '@element-plus/icons-vue'
import { formAppList, formSynthesisList, getForm } from '@/api/processui'
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
  approvalType: 1, // 审批类型
  users: [], // 用户列表
  dept: [], // 部门列表
  multiPercent: 100, // 或签-100%人员通过
  roles: [], // 角色列表
  leader: 1, // 直接上级
  multi: 'sequential', // 多人审批方式
  emptyApproType: 1, // 审批人为空类型
  emptyApproUser: [], // 审批人为空人员列表
  shouldSign: false, // 审批人同意时是否需要签字
  approDueTime: 0, // 审批期限
  approDueTimeUnit: 1, // 审批期限单位
  disAgreenEnd: false, // 如果审批被驳回,是否直接结束
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
    channels: ['workup视频通话']
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

const addTaskListener = () => {
  if (!activeChooseData.value.taskListeners) {
    activeChooseData.value.taskListeners = []
  }
  activeChooseData.value.taskListeners.push({
    triggerType: 1, // 1:网络请求，2:消息
    event: 'complete', // 触发时机
    url: '', // 请求地址
    method: 'GET', // 请求方式
    headers: [], // 请求头
    params: [], // 请求参数
    paramsType: 1 // 请求参数类型，1:json，2:form
  })
}

// 保存人员
function confirmUser(data) {
  activeChooseData.value.timeoutHandlers[addTimeoutRuleIndex.value].notificationUserIds = data.user.map(item => item.user_id)
  chooseUserVis.value = false
}

// 添加超时规则
const addTimeoutRule = () => {
  activeChooseData.value.timeoutHandlers.push({
    triggerId: null,
    triggerTime: 0, // 触发时间数值
    triggerTimeUnit: 2, // 时间单位 1天/2小时/3分钟
    triggerType: 1, // 触发类型（1=消息通知/2=自动通过）
    notificationUserIds: [] // 通知对象ID列表
  })
}

const removeListener = (index) => {
  activeChooseData.value.taskListeners.splice(index, 1)
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
let choosePersonPanelRef = ref(null) // 设置审批对象
function confirm() {
  // 设置审批对象-岗位和职位-单选
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
import { useUserStore } from '@/store/modules/useraPaas'
import AddressSetingDialog from '@/components/personHome/addressSetingDialog.vue'
const store = useUserStore()
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

const setFormKey = (data) => {
  activeChooseData.value.formKey = data[data.length - 1]
  getFormList()
  setNodeFormFieldProp(activeChooseData.value.formKey)
}

const cascaderProps = {
  lazy: true,
  lazyLoad(node, resolve) {
    const { level } = node
    // if (level === 0) {
    //   resolve([{
    //     value: '',
    //     label: '全部',
    //     leaf: false
    //   }])
    //   return
    // }
    const params = {
      accessToken: store.token,
      type: formDesignType.value,
      pageNo: 1,
      pageSize: 999
    }
    if (node.data.categoryId) {
      params.parentId = node.data.categoryId
    }
    if (flowDesignerPage?.applicationId) {
      params.applicationId = flowDesignerPage.applicationId
    }
    let resFn = classifyType.value ? formAppList(params) : formSynthesisList(params)
    resFn.then((res) => {
      if (res.code === 200) {
        let resData = classifyType.value ? res.rows : res.data
        let tempCateGory = resData?.map((item) => ({
          value: item.categoryId,
          label: (item.applicationName || item.categoryName) + '(分类)',
          leaf: false,
          categoryId: item.categoryId
        }))
        if (level !== 0) {
          const pp = {
            pageNo: 1,
            pageSize: 999,
            type: formDesignType.value,
            isFormComponents: '0'
          }
          if (node.data.categoryId) {
            pp.categoryId = node.data.categoryId
          }
          listForm(pp).then((resp) => {
            if (resp.code === 200) {
              resp.rows.map((item) => {
                tempCateGory.push({
                  value: item.formId,
                  label: item.formName,
                  leaf: true
                })
              })
            }
            resolve(tempCateGory)
          })
        } else {
          resolve(tempCateGory)
        }
      } else {
        resolve([])
      }
    })
  }
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
  // 超时规则,默认1个
  if (!activeChooseData.value.timeoutHandlers?.length) {
    activeChooseData.value.timeoutHandlers = [{
      triggerId: null,
      triggerTime: 0, // 触发时间数值
      triggerTimeUnit: 2, // 时间单位 1天/2小时/3分钟
      triggerType: 1, // 触发类型（1=消息通知/2=自动通过）
      notificationUserIds: [] // 通知对象ID列表
    }]
    addTimeoutRuleIndex.value = 0
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
        box-shadow: 0px 0px 6px 0px rgba(0,0,0,0.15);
        border-radius: 2px;
        .prop_item_user_remove {
          display: flex!important;
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
    height:32px;
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

    .interBox{
      border: 1px solid #ebeef5;
      border-radius: 6px;
      padding: 0 16px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      font-weight: 500;
      color: #606266;

      .prop_title{
        height: 0;
      }
    }

    .priority-radio_span{
      padding-top: 8px;
      line-height: 12px;
      font-size: 11px;
      color: #999;
    }
</style>
