<!--
 *@Created by: 兰舰
 * Email: gglanjian@qq.com
 * Phone: 16620805419
 * @Date: 2024-11-15 11:45:51
 * @Last Modified by:  兰舰
 * @Copyright aPaaS-front-team. All rights reserved.
!-->
<template>
  <div class="tenantN_Page">
    <!--    <div class="nav_status flexRowAC">-->
    <!--      <router-link to="" class="nav_title" @click="goBack">-->
    <!--        返回-->
    <!--      </router-link>-->
    <!--      <div class="nav_title lth">-->
    <!--        /&nbsp;&nbsp;-->
    <!--        <span>流程详情</span>-->
    <!--      </div>-->
    <!--    </div>-->
    <div v-if="taskInfo?.workorderId==='true'" class="infoBoxOut">
      <div class="infoBox_t">
        工单信息
      </div>
      <div class="infoBox">
        <div class="flexRowAC infoItem" :class="{img:taskInfo?.type}">
          <div class="infoItem_t flexRowAC">
            <span class="infoItem_label">所属项目</span>
            {{ infoTemp?.['dept_name'] || '-' }}
            <img v-if="taskInfo?.type==='1'" class="pro_djd" src="@/assets/img/processui/pro_djd.png" alt="" />
            <img v-if="taskInfo?.type==='6'" class="pro_djd" src="@/assets/img/processui/pro_dpj.png" alt="" />
            <img v-if="taskInfo?.type==='2'" class="pro_djd" src="@/assets/img/processui/pro_dpd.png" alt="" />
            <img v-if="taskInfo?.type==='3'" class="pro_djd" src="@/assets/img/processui/pro_dcl.png" alt="" />
            <img v-if="taskInfo?.type==='4'||taskInfo?.type==='5'" class="pro_djd" src="@/assets/img/processui/pro_dsh.png" alt="" />
          </div>
          <div class="infoItem_t flexRowAC">
            <span class="infoItem_label">工单编号</span>
            {{ infoTemp?.['workorderNumber'] || '-' }}
          </div>
        </div>
        <div class="flexRowAC infoItem">
          <div class="infoItem_t flexRowAC">
            <span class="infoItem_label">建单人</span>
            {{ infoTemp?.['createBy'] || '-' }}
          </div>
          <div class="infoItem_t flexRowAC">
            <span class="infoItem_label">联系电话</span>
            {{ infoTemp?.['createTime1'] || '-' }}
          </div>
        </div>
        <div class="flexRowAC infoItem">
          <div class="infoItem_t flexRowAC">
            <span class="infoItem_label">工单类型</span>
            {{ infoTemp?.['workorderName']|| workorderIdFormat(infoTemp?.['workorderId']) || infoTemp?.['workorderId'] || '-' }}
          </div>
          <div class="infoItem_t flexRowAC">
            <span class="infoItem_label">报单时间</span>
            {{ infoTemp?.['createTime'] || '-' }}
          </div>
        </div>
        <div class="flexRowAC infoItem">
          <div class="infoItem_t flexRowAC">
            <span class="infoItem_label">详细地址</span>
            {{ infoTemp?.['addr'] || '-' }}
          </div>
          <div class="infoItem_t flexRowAC">
            <span class="infoItem_label">紧急程度</span>
            <template v-if="infoTemp?.priority">
              <span v-if="infoTemp.priority==='normal'">一般</span>
              <span v-if="infoTemp.priority==='urgent'">紧急</span>
              <span v-if="infoTemp.priority==='critical'">严重</span>
            </template>
            <span v-else>-</span>
          </div>
        </div>
      </div>
    </div>
    <div class="content_dis">
      <el-tabs v-model="activeName" class="tenanat-tabs">
        <el-tab-pane v-if="showTaskTab" label="任务办理" name="task" />
        <el-tab-pane v-if="taskInfo?.workorderId" label="处置记录" name="record" />
        <el-tab-pane v-else label="审批记录" name="record" />
        <el-tab-pane label="流程图" name="flow" />
        <el-tab-pane label="基本信息" name="form" />
        <el-tab-pane label="权限" name="auth" />
      </el-tabs>
      <!--审批记录 -->
      <div v-if="activeName === 'record'" class="flow_designer_page">
        <task-handle-detail-record
          :node-config-prop="nodeConfig"
          :finished-task-node="finishedTaskNode"
          :un-finished-task-node="unFinishedTaskNode"
          :history-proc-node-list="historyProcNodeList"
        />
      </div>
      <!--  流程图-->
      <div v-if="activeName==='flow'" class="flow_designer_page">
        <FlowChart
          v-if="nodeConfig"
          ref="flowChartRef"
          v-model:node-config="nodeConfig"
          :is-edit="false"
          :finished="finishedTaskNode"
          :un-finished="unFinishedTaskNode"
        />
        <div v-else class="empty_flow">
          <span>流程图加载失败</span>
        </div>
      </div>
      <div v-if="activeName==='form'" class="preview_form">
        <template v-if="formJson&&formJson.length">
          <div v-for="(item,i) in formJson" :key="i" class="preFormBox">
            <VFormContainer
              v-if="item"
              :node-form-properties="nodeFormProperties"
              :form-json="item"
              :form-data="{}"
              :preview-state="false"
              :option-data="{}"
              :global-dsv="{}"
            />
          </div>
        </template>
      </div>
      <div v-if="activeName==='task'" class="taskBox">
        <!--子表单-->
        <template v-if="formJsonSub&&formJsonSub.length">
          <div v-for="(item,i) in formJsonSub" :key="i" style="width: 50%;">
            <VFormRender
              v-if="item"
              ref="preFormSub"
              :form-json="item"
              :form-data="{}"
              :preview-state="true"
              :option-data="{}"
              :global-dsv="{}"
            />
          </div>
        </template>
        <div class="infoBox">
          <div class="flexRowAC infoItem">
            <div class="infoItem_t flexRowAC" style="width: 100%;">
              <span class="infoItem_label">{{ taskInfo?.workorderId ? '处置人' : '审批人' }}</span>
              <div class="infoItem_add flexRowAC" @click="userVisi=true">
                <el-icon>
                  <Plus />
                </el-icon>
                添加人员
              </div>
              <div v-for="(item,i) in userRangeList" :key="i" class="elIconPerBox flexRowAC">
                <el-icon v-if="taskInfo?.type==='2'" @click="removeClick(i)">
                  <CircleCloseFilled />
                </el-icon>
                <img v-if="item.photo" :src="item?.photo" class="avatarPho" />
                <img v-else class="avatarPho" src="@/assets/img/tx.png" alt="" />
                <div>
                  <div>{{ item?.user_name }} {{ item?.user_detail?.phone }}</div>
                  {{ item['dept_name'] }}
                </div>
              </div>
              <div v-for="(item,i) in deptRangeList" :key="i" class="elIconPerBox flexRowAC">
                <el-icon @click="removeClick(i)">
                  <CircleCloseFilled />
                </el-icon>
                <img v-if="item.photo" :src="item?.photo" class="avatarPho" />
                <oort-svg-icon v-else class="avatarPho" name="dept_default" />
                <div>
                  <div>{{ item?.dept_name }}</div>
                </div>
              </div>
            </div>
          </div>
          <div class="flexRowAC infoItem">
            <div class="infoItem_t flexRowAC">
              <span class="infoItem_label">{{ taskInfo?.workorderId ? '处置意见' : '审批意见' }}</span>
              <el-input v-model="form.comment" type="textarea" rows="3" />
            </div>
          </div>
          <div class="flexRowAC infoItem">
            <div class="infoItem_t flexRowAC" style="width: 100%;">
              <span class="infoItem_label">退回节点</span>
              <el-radio-group v-model="targetKey">
                <el-radio-button
                  v-for="item in returnTaskList"
                  :key="item.id"
                  :label="item.id"
                >
                  {{ item.name }}
                </el-radio-button>
              </el-radio-group>
              <span class="infoItem_t_span">退回操作时必选</span>
            </div>
          </div>
          <div class="flexRowAC infoItem" style="padding-bottom: 0;">
            <div class="infoItem_t flexRowAC" style="width: 100%;">
              <span class="infoItem_label">抄送人</span>
              <ChoosePerson v-model="copyUserIds" is-multi />
            </div>
          </div>
          <div class="flexRowAC infoItem" style="padding-bottom: 0;">
            <div class="infoItem_t flexRowAC" style="width: 100%;">
              <span class="infoItem_label">{{ taskInfo?.workorderId ? '指定处置人' : '指定审批人' }}</span>
              <ChoosePerson v-model="nextUserIds" is-multi />
            </div>
          </div>
          <div class="flexRowAC infoItem">
            <div class="infoItem_t flexRowAC" style="width: 100%;">
              <span class="infoItem_label">委派人或<br />转办人</span>
              <ChoosePerson v-model="userId" is-multi />
              <span class="infoItem_t_span">委派操作或者转办操作时必填</span>
            </div>
          </div>
        </div>
        <!--3退回 1委派 2转办-->
        <div class="dia_btn flexRowAC">
          <el-row :gutter="10" type="flex">
            <template v-for="(item, index) in buttonsArr">
              <el-col v-if="oprArr.includes(item.dictValue)" :key="index" :span="1.5">
                <el-button v-preReClick :type="approveTypeBtn(item['dictLabel'])" :disabled="item?.dictValue==='3'&&!targetKey || (item?.dictValue==='1'||item?.dictValue==='2')&&!userId" :class="{btn3:item?.dictValue==='3'&&!targetKey || (item?.dictValue==='1'||item?.dictValue==='2')&&!userId}" @click="submitTask(item.dictValue)">
                  {{ item['dictLabel'] }}
                </el-button>
              </el-col>
            </template>
          </el-row>
        </div>
      </div>
      <div v-if="activeName==='auth'">
        <authority :submit="props.submit" />
      </div>
    </div>
    <el-dialog
      v-model="userVisi"
      title="人员选择"
      width="53%"
      destroy-on-close
      append-to-body
    >
      <address-seting-dialog
        :mode="1"
        :user-list="userRangeList?.map(item=>item.user_id)"
        :dept-list="deptRangeList?.map(item=>item.dept_id)"
        @cancelChoose="userVisi=false"
        @saveChoose="saveConfirm"
        @close="userVisi=false"
      />
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import {
  detailProcess,
  completeTask,
  rejectTask,
  getApprovalButton,
  returnList,
  returnTask,
  transferTask,
  delegateTask,
  listwfAll,
  listSynthesisAll,
  unClaimTask,
  claimTask,
  getApprovalButton_wk
} from '@/api/processui/index'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/modules/useraPaas'
import VFormContainer from './components/vformContainer.vue'
import Authority from '@/pages/processui/views/page/flowManage/authority.vue'
import TaskHandleDetailRecord from '@/pages/processui/views/page/flowManage/taskHandleDetailRecord.vue'

// const router = useRouter()
const route = useRoute()
const store: any = useUserStore()
const props = defineProps(['item', 'submit', 'tabsView'])
const emits: any = defineEmits(['handle', 'close'])
const taskInfo = ref<any>(props.item || route.query)
const activeName = ref<any>(props.tabsView || 'form')
const nextUserIds = ref('')
const copyUserIds = ref('')
const userId = ref('')
const buttonsArr = ref([])
const targetKey = ref('')
const returnTaskList = ref([])
const showTaskTab = ref(false)
const nodeConfig = ref(null) // 流程图
const showSP = ref(true)
const finishedTaskNode = ref([]) // 流程图
const unFinishedTaskNode = ref([]) // 流程图
const rejectTaskNode = ref([])
// const oprArr = ref(['0', '1', '2', '3', '4'])
const oprArr = ref([])
const formJson = ref(null) // 表单信息
const preFormSub = ref<any>(null) // 子表单信息
const preFormSubTemp = ref<any>(null) // 子表单信息
const formJsonSub = ref<any>(null)
const historyProcNodeList = ref<any>([]) // 流转记录
const infoTemp = ref<any>(null) // 工单详情
const categoryOptions = ref<any>([]) // 工单类型
let userVisi = ref(false) // 人员选择
let userRangeList = ref([]) // 人员选择[]
let deptRangeList = ref([]) // 部门选择[]
const form = ref({
  comment: ''
})

// 保存用户选中
const saveConfirm = async(address: any) => {
  // 选择的用户
  if (address.user) {
    userRangeList.value = address.user || []
    deptRangeList.value = address.dept || []
  }
  userVisi.value = false
}

// 工单类型
const workorderTypeFn = async() => {
  categoryOptions.value = []
  let data = {
    accessToken: store.userInfo.accessToken
  }
  listwfAll(data).then((res: any) => {
    listSynthesisAll(data).then((res_: any) => {
      categoryOptions.value = [...res.data, ...res_.data]
    })
  })
}

// 过滤器-工单类型
const workorderIdFormat = (cellValue) => {
  if (categoryOptions.value && categoryOptions.value.length) {
    const category:any = categoryOptions.value.find((item:any) => item.synthesisId === cellValue)
    return category ? category.categoryName : ''
  }
}

// 子表单-提交
let preFormSubFlage = false
const startFlow = async() => {
  if (preFormSub.value && Array.isArray(preFormSub.value)) {
    try {
      const formDataPromises = preFormSub.value.map(form => form.getFormData())
      const formDataResults = await Promise.all(formDataPromises)

      preFormSubTemp.value = formDataResults.map(formData =>
        JSON.parse(JSON.stringify(formData, null, ''))
      )
      preFormSubFlage = true
      return true
    } catch (error:any) {
      preFormSubFlage = false
      ElMessage.error(error)
      return false
    }
  }
  return false
}

// 获取按钮
const getOprBtns = async() => {
  let res: any = props.item?.workorderId ? await getApprovalButton_wk('') : await getApprovalButton('')
  buttonsArr.value = (res.data || [])
}
// 移除
const removeClick = (index) => {
  userRangeList.value.splice(index, 1)
}

function approveTypeBtn(val) {
  switch (val) {
    case '转办':
      return 'warning'
    case '拒绝':
      return 'danger'
    default:
      return 'primary'
  }
}

// 获取按钮-通过
const submitTask = async(flag) => {
  await startFlow()
  // flag 0 同意 ，1委派， 2 转办 ，3退回 ，4拒绝 7退单
  if (flag === '5' && !userRangeList.value.length && !deptRangeList.value.length && !infoTemp.value?.assignId) return ElMessage.warning('请选择处理人或者部门')
  const params:any = {
    taskId: taskInfo.value.taskId,
    procInsId: taskInfo.value.procInsId,
    businessKey: taskInfo.value?.businessKey || undefined, // 干预记录需要businessKey
    comment: form.value.comment,
    nextUserIds: nextUserIds.value,
    copyUserIds: copyUserIds.value
  }
  if (!params.comment) params.comment = buttonsArr.value.find((item: any) => item.dictValue === flag)?.['dictLabel']
  // 子表单
  if (!!preFormSub.value) {
    if (preFormSubFlage) {
      params['variables'] = preFormSubTemp.value.reduce((acc, item) => {
        return item
      }, {})
    } else {
      return ElMessage.warning('表单信息')
    }
  }
  let res: any = null
  if (flag === '0') {
    res = await completeTask(params)
  }
  if (flag === '1') {
    if (!userId.value) return ElMessage.warning('请选择委派人或者转办人')
    params['userId'] = userId.value
    res = await delegateTask(params)
  }
  if (flag === '2') {
    if (!userId.value) return ElMessage.warning('请选择委派人或者转办人')
    params['userId'] = userId.value

    res = await transferTask(params)
  }
  if (flag === '3') {
    if (!targetKey.value) return ElMessage.warning('请选择可退回的节点')
    params['targetKey'] = targetKey.value
    res = await returnTask(params)
  }
  if (flag === '4') {
    res = await rejectTask(params)
  }
  // 指派
  if (flag === '5') {
    // 人或者部门
    if (userRangeList.value.length) params['candidateUsers'] = userRangeList.value.map((item:any) => item.user_id)
    if (deptRangeList.value.length) params['candidateGroups'] = deptRangeList.value.map((item:any) => item.dept_id)
    res = await completeTask(params)
  }
  // 接单-签收
  if (flag === '6') {
    res = await claimTask(params)
  }
  // 7退单
  if (flag === '7') {
    res = await unClaimTask(params)
  }
  if (res.code === 200) {
    ElMessage.success('操作成功')
    // router.back()
    emits('handle')
    emits('close')
  }
}

// 可退回的节点
const getReturnList = async() => {
  const params = {
    procInsId: taskInfo.value.procInsId,
    taskId: taskInfo.value.taskId
  }
  returnList(params).then((res: any) => {
    if (res.code === 200) {
      returnTaskList.value = res.data || []
    }
  })
}

// finishedTaskNode 排除在 UnFinishedTaskNode
const getFinishedTaskNode = () => {
  const arr = []
  finishedTaskNode.value.forEach(item => {
    if (!unFinishedTaskNode.value.includes(item) && !rejectTaskNode.value.includes(item)) {
      arr.push(item)
    }
  })
  return arr
}

const nodeFormProperties = ref('')
// 流程节点详情
const getProcessDetail = async() => {
  const params = {
    procInsId: taskInfo.value.procInsId
  }
  if (taskInfo.value.taskId) {
    params['taskId'] = taskInfo.value.taskId
  }
  detailProcess(params).then((res: any) => {
    if (res.code === 200) {
      oprArr.value = res.data?.buttonsMap?.buttonOprArr || []
      if (res.data.processFormList && res.data.processFormList.length > 0) {
        formJson.value = res.data?.processFormList
        formJsonSub.value = res.data?.taskFormData
      }
      if (res.data?.historyProcNodeList) {
        historyProcNodeList.value = res.data?.historyProcNodeList.reverse()
      }
      try {
        if (res.data.flowViewer) {
          unFinishedTaskNode.value = res.data.flowViewer.unfinishedTaskSet || []
          finishedTaskNode.value = [...(res.data.flowViewer.finishedTaskSet || []), ...(res.data.flowViewer.finishedSequenceFlowSet || [])]
          rejectTaskNode.value = res.data.flowViewer.rejectedTaskSet || []
          // 存在退回节点的逻辑 使用这里要排除 (包括退回的逻辑)
          finishedTaskNode.value = getFinishedTaskNode()
        }
        if (res.data.bpmnJson) {
          // 去除第一个开始节点
          nodeConfig.value = JSON.parse(res.data.bpmnJson).process.childNode
          nodeFormProperties.value = getNodeById(nodeConfig.value, res.data.wfBasicInfoVo.taskDefId)?.formProperties || []
          console.log('-------0000----', res.data.wfBasicInfoVo.taskDefId, nodeConfig.value, getNodeById(nodeConfig.value, res.data.wfBasicInfoVo.taskDefId))
          // 获取最后一个节点 ，判断当前节点是不是最后一个节点
          let lastNode = jugeLastTwoNode(nodeConfig.value)
          if (historyProcNodeList.value.length > 0 && lastNode) {
            showSP.value = historyProcNodeList.value[historyProcNodeList.value.length - 1]?.activityId !== lastNode.id
          }
        }
      } catch (error) {
        console.error(error)
      }
    }
  })
}

// 递归判断是否倒数第二节点
const jugeLastTwoNode = (node) => {
  if (node.childNode.id === 'end') {
    return node
  }
  if (node.childNode) {
    return jugeLastTwoNode(node.childNode)
  }
}

// 递归获取指定id的node
const getNodeById = (node, id = 'end') => {
  if (node.id === id) {
    return node
  }
  if (node.childNode) {
    return getNodeById(node.childNode, id)
  }
}

onMounted(async() => {
  if (taskInfo.value?.workorderId !== 'true') {
    workorderTypeFn() // 工单类型
  }
  if (taskInfo.value.taskId) {
    showTaskTab.value = true
    activeName.value = 'task'
    await getReturnList() // 可退回的节点
  }
  if (taskInfo.value.tabName) {
    activeName.value = taskInfo.value.tabName
  }
  await getOprBtns() // 按钮
  getProcessDetail() // 流程节点详情
})

</script>
<style lang="scss" scoped>
.tenantN_Page {
  height: 100%;
  overflow: auto;

  .nav_status {
    position: relative;
    border-bottom: 1px solid #D8D8D8;
  }
}

.chetitle {
  padding: 20px;
}

// 确定取消
.subBtnsBox {
  position: absolute;
  right: 20px;
  top: 15px;

  .sub_s {
    color: red;
  }
}

// nav
.nav_status {
  height: 60px;
  padding: 10px 0 0 20px;

  &:before {
    content: '';
    width: 3px;
    height: 18px;
    background-color: #1A1A1A;
  }

  .nav_title {
    height: 40px;
    line-height: 40px;
    color: #333;
    margin-left: 12px;

    &.lth {
      color: #999;
    }
  }
}

// tabs
:deep(.tenanat-tabs) {
  padding: 0 20px;

  .el-tabs__item {
    color: #999999;
  }

  .el-tabs__item.is-active {
    color: var(--el-color-primary);
  }
}

// tabs
:deep(.el-tabs__header) {
  padding-top: 20px;

  .el-tabs__nav-wrap::after {
    display: none;
  }

  .el-tabs__item.is-top {
    font-size: 16px;
    font-weight: 700;
  }
}

.form-render-wrapper.h5-layout {
  margin: 0 auto;
  width: 360px;
  border-radius: 10px;
  box-shadow: 0 0 1px 1px var(--el-color-primary);
  height: calc(100vh - 175px);
  overflow-y: auto;
  overflow-x: hidden;
}

.form-render-wrapper.pad-layout {
  margin: 0 auto;
  width: 960px;
  border-radius: 10px;
  box-shadow: 0 0 1px 1px var(--el-color-primary);
  height: calc(100vh - 175px);
  overflow-y: auto;
  overflow-x: hidden;
}

.preview_form {
  width: 80%;
  padding: 20px
}

.task_detail_page {
  padding: 20px;
  width: calc(100% - 20px);
  height: calc(100% - 60px - 80px - 20px);
  display: flex;
  background-color: #fff;
  display: flex;
  flex-direction: column;
}

.task_form {
  padding: 20px;
}

.button_group {
  padding: 20px;
}

.flow_designer_page {
  width: 100%;
  height: 500px;
  height: calc(100% - 60px - 80px);
  overflow: auto;
  display: flex;
  background-color: #fff;
  flex-direction: column;
  align-items: center;
  position: relative;
  min-height: 600px;
}

.empty_flow {
  display: flex;
  height: 100%;
  width: 100%;
  align-items: center;
  justify-content: center;

  span {
    color: #999;
    font-size: 16px;
  }
}

.box-card {
  width: 100%;
  margin-bottom: 20px;
  border: none;
  overflow: auto;
}

// 流程
:deep(.el-card.is-hover-shadow.box-card) {
  .el-card__body {
    background: #F7F7F7;
    border-radius: 10px;
  }
}

:deep(.el-timeline) {
  .el-timeline-item__node {
    position: relative;
    z-index: 3;
    top: 18px;
    left: 9px;
  }

  .el-timeline-item__wrapper {
    position: relative;
    z-index: 2;
    top: -18px;
    left: 0;
  }
}

// avatar
.avatarBox {
  width: calc(100% + 44px);
  justify-content: space-between;
  margin-left: -44px;
  padding-bottom: 12px;

  .avatarImg {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    background-color: #f7f7f7;
    margin-right: 8px;
  }

  .avatarTime {
    flex-direction: column;
    align-items: flex-end;
  }
}

// 通过
.commentBox {
  justify-content: space-between;
}

:deep(.task_form) {
  // 审批-抄送人
  .chooose_item.chooose_item_group {
    margin: 0;
    padding: 0 12px;
  }

  .chooose_item_add_litte {
    margin: 0;
  }
}

.btn3,.btn3:hover{
  opacity: .5;
  background-color: gray;
  border-color: gray;
}

// 工单信息
.infoBox {
  .infoItem {
    padding-bottom: 20px;
    &.img:first-child{
      padding-bottom: 0;
    }
  }

  .infoItem_t {
    width: 30%;
    font-size: 14px;
    line-height: 20px;
    color: #333333;
    white-space: nowrap;

    .infoItem_label {
      width: 80px;
      text-align: right;
      padding-right: 20px;
      flex-shrink: 0;
      font-size: 14px;
      color: #939393;
    }

    .infoItem_t_span{
      color: #999;
      padding-left: 8px;
    }
  }

  .pro_djd{
    width: 54px;
    height: 54px;
    margin-left: 10px;
  }

  &_t {
    padding-bottom: 14px;
    font-size: 16px;
    font-weight: 700;
    color: var(--el-color-primary);
  }

  &Out {
    padding: 20px 20px 0;
  }
}

// 处理人
.infoItem_add{
  flex-direction: column;
  justify-content: center;
  gap: 4px;
  cursor: pointer;
  width: 200px;
  height: 72px;
  background: #FFFFFF;
  border-radius: 4px;
  border: 1px dashed var(--el-color-primary);
  color: var(--el-color-primary);
  margin-right: 8px;

  .el-icon{
    font-size: 20px;
  }
}

// 人员选择
.elIconPerBox{
  width: 200px;
  height: 72px;
  padding: 8px;
  border-radius: 4px;
  background: hsl(0, 0%, 100%);
  border: 1px solid hsl(0, 0%, 96.1%);
  gap: 12px;
  margin-right: 8px;
  position: relative;

  .el-icon {
    display: none;
    cursor: pointer;
    position: absolute;
    top: 4px;
    right: 4px;
    font-size: 16px;
    color: red;
  }
  &:hover{
    box-shadow: 0 2px 12px 0 var(--el-color-primary-hb);

    >.el-icon {
      display: flex;
    }
  }

  .avatarPho {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    background-color: var(--el-menu-hover-bg-color);
  }

  &Out {
    gap: 20px;
  }
}
// 基本信息
.preFormBox{
  border-radius: 4px;
  padding: 20px;
  margin-bottom: 20px;
  border: 1px solid rgba(0, 0, 0, 0.1);
}

// 任务办理
.taskBox{
  padding: 20px;
}
</style>
