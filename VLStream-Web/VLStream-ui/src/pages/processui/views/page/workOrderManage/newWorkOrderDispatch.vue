<!--
 *@Created by: 兰舰
 * Email: gglanjian@qq.com
 * Phone: 16620805419
 * @Date: 2024-11-15 11:45:51
 * @Last Modified by:  兰舰
 * @Copyright aPaaS-front-team. All rights reserved.
!-->
<template>
  <div class="page dispatchPage">
    <!--    <div class="nav_status flexRowAC">-->
    <!--      <div class="nav_title lth">-->
    <!--        <span v-if="infoTemp?.type==='1'">接单</span>-->
    <!--        <span v-if="infoTemp?.type==='2'">指派</span>-->
    <!--        <span v-if="infoTemp?.type==='3'">处理</span>-->
    <!--        <span v-if="infoTemp?.type==='4'">审核</span>-->
    <!--        <span v-if="infoTemp?.type==='5'">待访问</span>-->
    <!--        <span v-if="infoTemp?.type==='6'">待评价</span>-->
    <!--        ({{ infoTemp?.['title'] }})-->
    <!--      </div>-->
    <!--    </div>-->
    <div class="infoBoxOut">
      <div class="infoBox_t">
        工单信息
      </div>
      <div class="infoBox">
        <div class="flexRowAC infoItem" :class="{img:infoTemp?.type}">
          <div class="infoItem_t flexRowAC">
            <span class="infoItem_label">所属项目</span>
            {{ infoTemp?.['dept_name'] || '-' }}
            <img v-if="infoTemp?.type==='1'" class="pro_djd" src="@/assets/img/processui/pro_djd.png" alt="" />
            <img v-if="infoTemp?.type==='5'" class="pro_djd" src="@/assets/img/processui/pro_dhf.png" alt="" />
            <img v-if="infoTemp?.type==='6'" class="pro_djd" src="@/assets/img/processui/pro_dpj.png" alt="" />
            <img v-if="infoTemp?.type==='2'" class="pro_djd" src="@/assets/img/processui/pro_dpd.png" alt="" />
            <img v-if="infoTemp?.type==='3'" class="pro_djd" src="@/assets/img/processui/pro_dcl.png" alt="" />
            <img v-if="infoTemp?.type==='4'" class="pro_djd" src="@/assets/img/processui/pro_dsh.png" alt="" />
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
        <el-tab-pane label="处置记录" name="record" />
        <!--<el-tab-pane label="审批记录" name="record" />-->
        <el-tab-pane label="基本信息" name="form" />
        <el-tab-pane label="权限" name="form1" />
      </el-tabs>
      <div v-if="activeName === 'record'" class="flow_designer_page">
        <el-card class="box-card" shadow="never">
          <el-col :span="20" :offset="2">
            <div class="block">
              <el-timeline>
                <el-timeline-item
                  v-for="(item,index) in historyProcNodeList"
                  :key="index"
                  :icon="setIcon(item.endTime)"
                  :color="setColor(item.endTime)"
                >
                  <!--用户-时间-->
                  <div class="avatarBox flexRowAC">
                    <div class="flexRowAC">
                      <i-d2-head-pic v-if="item['assigneeId']" :id="item['assigneeId']" class="avatarImg" />
                      <div v-else class="avatarImg" />
                      <div>
                        <p style="font-weight: 700">
                          {{ item['assigneeName'] }}
                        </p>
                        <div class="flexRowAC">
                          {{ item['activityName'] }}
                          <span v-if="item['activityType'] === 'userTask'">
                            （候选办理:{{ item.candidate || '-' }}）
                          </span>
                        </div>
                      </div>
                    </div>
                    <div class="avatarTime flexRowAC">
                      <div>{{ item['createTime'] }}</div>
                      耗时{{ item.duration || '-' }}
                    </div>
                  </div>
                  <!--节点-->
                  <div v-if="item['activityType'] === 'userTask'">
                    <div v-if="item['commentList'] && item['commentList'].length > 0">
                      <div v-for="(comment, index) in item['commentList'].slice().reverse()" :key="index">
                        <div class="commentBox flexRowAC">
                          <el-tag :type="approveTypeTag(comment.type)" size="small">
                            {{ commentType(comment.type) }}
                          </el-tag>
                          {{ comment.time }}
                        </div>
                        <el-card class="box-card" shadow="hover">
                          <el-descriptions :column="5" :label-style="{'font-weight': 'bold'}" />
                          <div>{{ comment['fullMessage'] }}</div>
                        </el-card>
                      </div>
                    </div>
                  </div>
                  <el-card v-if="item['activityType'] === 'endEvent'" class="box-card" shadow="hover">
                    {{ item['createTime'] }} 结束流程
                  </el-card>
                </el-timeline-item>
              </el-timeline>
            </div>
          </el-col>
        </el-card>
      </div>
      <div v-if="activeName==='flow'" class="flow_designer_page">
        <FlowChart v-if="nodeConfig" ref="flowChartRef" v-model:node-config="nodeConfig" :is-edit="false" :finished="finishedTaskNode" :un-finished="unFinishedTaskNode" />
        <div v-else class="empty_flow">
          <span>流程图加载失败</span>
        </div>
      </div>
      <div v-show="activeName==='form'" class="preview_form">
        <template v-if="formJson&&formJson.length">
          <div v-for="(item,i) in formJson" :key="i" class="preFormBox">
            <VFormRender
              v-if="item"
              ref="preForm"
              disabled
              :form-json="item"
              :form-data="{}"
              :preview-state="true"
              :option-data="{}"
              :global-dsv="{}"
            />
          </div>
        </template>
      </div>
      <!--任务办理-->
      <div v-if="activeName==='task'" class="taskBox">
        <!--子表单-->
        <div v-if="formJsonSub" style="width: 50%;">
          <VFormRender
            v-if="formJsonSub"
            ref="preFormSub"
            :form-json="formJsonSub"
            :form-data="{}"
            :preview-state="true"
            :option-data="{}"
            :global-dsv="{}"
          />
        </div>
        <div class="infoBox">
          <div class="flexRowAC infoItem">
            <div class="infoItem_t flexRowAC" style="width: 100%;">
              <span v-if="infoTemp?.type==='1'" class="infoItem_label">派单人</span>
              <span v-else class="infoItem_label">处置人</span>
              <div v-if="infoTemp?.type==='2'" class="infoItem_add flexRowAC" @click="userVisi=true">
                <el-icon>
                  <Plus />
                </el-icon>
                添加人员
              </div>
              <div v-for="(item,i) in userRangeList" :key="i" class="elIconPerBox flexRowAC">
                <el-icon v-if="infoTemp?.type==='2'" @click="removeClick(i)">
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
          <div v-if="infoTemp?.type==='5'" class="flexRowAC infoItem">
            <div class="infoItem_t flexRowAC">
              <span class="infoItem_label"><span class="red">*</span>回访方式</span>
              <el-checkbox-group v-model="callbackType">
                <el-checkbox label="电话" value="1" />
                <el-checkbox label="上门" value="2" />
                <el-checkbox label="微信  " value="3" />
              </el-checkbox-group>
            </div>
          </div>
          <div class="flexRowAC infoItem">
            <div class="infoItem_t flS flexRowAC">
              <span v-if="infoTemp?.type==='5'" class="infoItem_label"><span class="red">*</span>回访记录</span>
              <span v-else class="infoItem_label">处置意见</span>
              <el-input v-model="commentInput" type="textarea" rows="3" show-word-limit maxlength="300" />
            </div>
          </div>
          <div v-if="returnTaskList&&returnTaskList.length>2" class="flexRowAC infoItem">
            <div class="infoItem_t flexRowAC" style="width: 100%;">
              <span class="infoItem_label">退回节点</span>
              <el-radio-group v-model="targetKey">
                <el-radio-button
                  v-for="item in returnTaskList"
                  :key="item.id"
                  :label="item.id"
                  :value="item.name"
                />
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
              <span class="infoItem_label">指定处置人</span>
              <ChoosePerson v-model="nextUserIds" is-multi />
            </div>
          </div>
          <div class="flexRowAC infoItem" style="padding-bottom: 0;">
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
                <el-button v-preReClick :type="approveTypeBtn(item['dictLabel'])" :disabled="btnDisabledFn(item)" :class="{btn3:btnDisabledFn(item)}" @click="submitTask(item.dictValue)">
                  {{ item['dictLabel'] }}
                </el-button>
              </el-col>
            </template>
          </el-row>
        </div>
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
import { ref } from 'vue'
import ID2HeadPic from '@/components/ID2HeadPic.vue'
import {
  claimTask,
  completeTask,
  delegateTask,
  detailProcess,
  getApprovalButton_wk, listSynthesisAll, listwfAll,
  rejectTask, returnList,
  returnTask,
  transferTask, unClaimTask
} from '@/api/processui'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserList } from '@/api/system/directory'
import { useUserStore } from '@/store/modules/useraPaas'

const store: any = useUserStore()
const props = defineProps(['item'])
const emits: any = defineEmits(['handle', 'close'])
const activeName = ref<any>('record')
const commentInput = ref<any>('')
const callbackType = ref<any>([]) // 待访问
const copyUserIds = ref('') // 抄送人
const userId = ref('') // 转办人
const nextUserIds = ref('') // 指定审批人
const categoryOptions = ref<any>([]) // 工单类型
const targetKey = ref('') // 可退回的节点
const returnTaskList = ref('') // 可退回的节点list
const historyProcNodeList = ref([]) // 流转记录
const preForm = ref<any>(null) // 表单信息
const preFormSub = ref<any>(null) // 子表单信息
const preFormSubTemp = ref<any>(null) // 子表单信息
const formJson = ref<any>(null) // 表单信息
const formJsonSub = ref<any>(null) // 子表单信息
const showTaskTab = ref(false) // 任务办理
const nodeConfig = ref(null) // 流程图
const finishedTaskNode = ref([]) // 流程图
const unFinishedTaskNode = ref([]) // 流程图
const buttonsArr = ref([]) // 按钮
const oprArr = ref([]) // 按钮-all
const infoTemp = ref<any>(null)
let userVisi = ref(false) // 人员选择
let userRangeList = ref([]) // 人员选择[]
let deptRangeList = ref([]) // 部门选择[]

// 保存用户选中
const saveConfirm = async(address: any) => {
  // 选择的用户
  if (address.user) {
    userRangeList.value = address.user || []
    deptRangeList.value = address.dept || []
  }
  userVisi.value = false
  console.log(address)
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
  if ((!!preFormSub.value)) {
    await preFormSub.value.getFormData().then(async(formData) => {
      preFormSubTemp.value = JSON.parse(JSON.stringify(formData, null, ''))
      preFormSubFlage = true
    }).catch(error => {
      preFormSubFlage = false
      ElMessage.error(error)
    })
  }
}

// 获取按钮
const getOprBtns = async() => {
  let res: any = await getApprovalButton_wk('') // 工单按钮列表
  buttonsArr.value = (res.data || [])
}

// 按钮禁用
const btnDisabledFn = (item:any) => {
  if (item?.dictValue === '1' && !userId.value) return true
  if (item?.dictValue === '2' && !userId.value) return true
  if (item?.dictValue === '3' && !targetKey.value) return true
  if (item?.dictValue === '8' && (!callbackType.value.length || !commentInput.value)) return true
  return false
}

const commentType = val => {
  switch (val) {
    case '1': return '通过'
    case '2': return '退回'
    case '3': return '驳回'
    case '4': return '委派'
    case '5': return '转办'
    case '6': return '终止'
    case '7': return '撤回'
  }
}
function setIcon(val) {
  if (val) {
    return 'el-icon-check'
  } else {
    return 'el-icon-time'
  }
}
function setColor(val) {
  if (val) {
    return '#2bc418'
  } else {
    return '#b3bdbb'
  }
}

const approveTypeTag = val => {
  switch (val) {
    case '1': return 'success'
    case '2': return 'warning'
    case '3': return 'danger'
    case '4': return 'primary'
    case '5': return 'success'
    case '6': return 'danger'
    case '7': return 'info'
  }
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

// 流程节点详情
const getProcessDetail = async(procInsId, taskId) => {
  const params = {
    procInsId: procInsId,
    taskId: taskId
  }
  detailProcess(params).then((res: any) => {
    if (res.code === 200) {
      oprArr.value = res.data?.buttonsMap?.buttonOprArr || []
      if (res.data.processFormList && res.data.processFormList.length > 0) {
        formJson.value = res.data.processFormList
        formJsonSub.value = res.data?.taskFormData
        // 禁用表单
        setTimeout(async() => {
          preForm.value?.map(item => item.disableForm())
        }, 500)
      }
      if (res.data?.historyProcNodeList) {
        historyProcNodeList.value = res.data?.historyProcNodeList.reverse()
      }
      try {
        if (res.data.flowViewer) {
          finishedTaskNode.value = res.data.flowViewer.finishedTaskSet
          unFinishedTaskNode.value = res.data.flowViewer.unfinishedTaskSet
        }
        if (res.data.bpmnJson) {
          // 去除第一个开始节点
          nodeConfig.value = JSON.parse(res.data.bpmnJson).process.childNode
        }
      } catch (error) {
        console.error(error)
      }
    }
  })
}

// 移除
const removeClick = (index) => {
  userRangeList.value.splice(index, 1)
}

// 可退回的节点
const getReturnList = async() => {
  const params = {
    procInsId: infoTemp.value.procInsId,
    taskId: infoTemp.value.taskId
  }
  returnList(params).then((res: any) => {
    if (res.code === 200) {
      returnTaskList.value = res.data || []
    }
  })
}

// 获取按钮
const submitTask = async(flag) => {
  if (infoTemp.value?.type === '5') {
    if (!callbackType.value.length) return ElMessage.warning('请选择回访方式')
    if (!commentInput.value) return ElMessage.warning('请输入对客户的回访记录，不超过300字')
  }
  await startFlow()
  // flag 0 同意 ，1委派， 2 转办 ，3退回 ，4拒绝 5指派 6接单 7退单 8提交回访记录  9验收通过 10验收不通过
  if (flag === '5' && !userRangeList.value.length && !deptRangeList.value.length && !infoTemp.value?.assignId) return ElMessage.warning('请选择处理人或者部门')
  let params:any = {
    taskId: infoTemp.value.taskId,
    procInsId: infoTemp.value.procInsId,
    comment: commentInput.value,
    nextUserIds: nextUserIds.value,
    copyUserIds: copyUserIds.value
  }
  if (!params.comment) params.comment = buttonsArr.value.find((item: any) => item.dictValue === flag)?.['dictLabel']
  // 子表单
  if (!!preFormSub.value) {
    if (preFormSubFlage) {
      params['variables'] = preFormSubTemp.value
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
    params.userId = userId.value
    res = await delegateTask(params)
  }
  if (flag === '2') {
    if (!userId.value) return ElMessage.warning('请选择委派人或者转办人')
    params.userId = userId.value
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
  if (flag === '8' && infoTemp.value?.type === '5') {
    let str = ['是否确认提交工单客户回访记录？', '客户回访']
    params['callbackType'] = callbackType.value.join(',')
    visitFn(params, str)
  }
  if (flag === '9') {
    let str = ['是否确认该工单处理结果验收通过？', '工单验收通过']
    params['acceptance'] = true
    visitFn(params, str)
  }
  if (flag === '10') {
    let str = ['是否确定该工单验收不通过，验收不通过的订单将回退到处理节点重新进行处理？', '工单验收不通过']
    visitFn(params, str)
  }
  if (flag && parseInt(flag) < 8 && res.code === 200) {
    ElMessage.success('操作成功')
    emits('close')
    emits('handle')
  }
}

const visitFn = (params, str) => {
  ElMessageBox.confirm(str[0], str[1], {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async() => {
    let res:any = await completeTask(params)
    if (res.code === 200) {
      ElMessage.success('操作成功')
      emits('close')
      emits('handle')
    }
  })
}

// info
const formTemp = async(params) => {
  // const data = params.id
  // let res: any = await infoWorkorder(data)
  // if (res.code === 200) {
  let val = { ...params }
  infoTemp.value = val
  infoTemp.value['procInsId'] = val.procInsId
  infoTemp.value['taskId'] = val.taskId || undefined
  if (val.procInsId) getProcessDetail(val.procInsId, val.taskId) // 流程节点详情
  if (val.taskId) {
    showTaskTab.value = true // 任务办理
    activeName.value = 'task'
    await getReturnList() // 可退回的节点
  }
  getOprBtns() // 按钮
  if (val.assignId) assignIdFn(val.assignId) // 用户列表-处理人
  if (!val.assignId && val.userId && infoTemp.value?.type === '1') assignIdFn(val.userId) // 用户列表-派单人
  if (!params.workorderName) workorderTypeFn() // 工单类型
  // }
}

// 用户列表
const assignIdFn = (user_id) => {
  let data = {
    accessToken: store.userInfo.accessToken,
    user_id: [user_id],
    tenant_id: store.tenantId
  }
  getUserList(data).then((res: any) => {
    if (res.code === 200) {
      if (res.data.list.length > 0) {
        userRangeList.value = res.data.list || []
      }
    }
  })
}

if (props.item) {
  let merObj = props.item
  if (merObj.id) formTemp(merObj)
}

</script>
<style lang="scss" scoped>
.dispatchPage{
  width: 100%;
  height: 100%;
  overflow: auto;
}

// nav
.nav_status {
  height: 60px;
  padding: 10px 0 0 20px;
  border-bottom: 1px solid hsla(0, 0%, 20%, 0.06);
  position: relative;

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

  // 确定取消
  .subBtnsBox {
    position: absolute;
    right: 20px;
    top: 5px;
  }
}

.infoBox {
  .infoItem {
    padding-bottom: 20px;
    &.img:first-child{
      padding-bottom: 0;
    }
  }

  .infoItem_t {
    width: 40%;
    font-size: 14px;
    line-height: 20px;
    color: #333333;
    white-space: nowrap;

    &.flS{
      align-items: flex-start;
    }

    .infoItem_label {
      width: 80px;
      text-align: right;
      padding-right: 20px;
      flex-shrink: 0;
      font-size: 14px;
      color: #939393;
      position: relative;

      >.red{
        position: absolute;
        left: -4px;
        top: 0px;
        color: red;
      }
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
}

.dia_btn{
  justify-content: flex-start;
  padding: 40px 0;
  //border-bottom: 1px solid hsla(0, 0%, 20%, 0.06);
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

.content_dis{
  min-height: 600px;
}

// 流转记录
.flow_designer_page {
  width: 100%;
  height: calc(100% - 60px - 80px);
  overflow: auto;
  display: flex;
  background-color: #fff;
  flex-direction: column;
  align-items: center;
  position: relative;

  .box-card {
    width: 100%;
    margin-bottom: 20px;
    border: none;
    overflow: auto;
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
}

.preview_form {
  width: 80%;
  padding: 20px
}

.flow_designer_page {
  width: 100%;
  min-height: 600px;
  overflow: auto;
  display: flex;
  background-color: #fff;
  flex-direction: column;
  align-items: center;
  position: relative;
}

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

.btn3,.btn3:hover{
  opacity: .5;
  background-color: gray;
  border-color: gray;
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
