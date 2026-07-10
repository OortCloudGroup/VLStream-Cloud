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
    <div class="infoBoxOut" />
    <div class="content_dis">
      <el-tabs v-model="activeName" class="tenanat-tabs" @tabClick="tabsFn">
        <el-tab-pane label="处置记录" name="record" />
        <el-tab-pane label="流程图" name="flow" />
        <el-tab-pane label="基本信息" name="form" />
        <el-tab-pane label="权限" name="auth" />
      </el-tabs>
      <el-row>
        <el-col v-if="activeName==='record'">
          <div class="depNameBox flexRowAC">
            <div class="exportBtnBox flexRowAC" style="width: 100%;display: flex;justify-content: space-between;">
              <div class="tree_s_rep flexRowAC">
                <!-- <span class="srep">审批记录</span>-->
              </div>
              <div class="icons">
                <oort-svg-icon
                  :color="currentView==='grid'?'':'#999'"
                  width="20"
                  height="20"
                  name="list"
                  @click="currentView='grid'"
                />
                <oort-svg-icon
                  :color="currentView==='flow'?'':'#999'"
                  width="20"
                  height="20"
                  name="vertical"
                  @click="currentView='flow'"
                />
                <oort-svg-icon
                  :color="currentView==='list'?'':'#999'"
                  width="20"
                  height="20"
                  name="flow_chart"
                  @click="currentView='list'"
                />
              </div>
            </div>
          </div>
        </el-col>
        <el-col v-if="infoTemp">
          <div ref="containerRef" class="containerBox" style="">
            <!-- 基本信息-start-->
            <div v-if="activeName==='form'" id="part1">
              <div class="preview_form">
                <div v-if="formJsonSub || formJson" class="taskBox">
                  <!--总表单-->
                  <template v-if="formJson&&formJson.length">
                    <div v-for="(item,i) in formJson" :key="i" class="preFormBox">
                      <VFormRender
                        v-if="item"
                        ref="preForm"
                        :form-json="item"
                        :form-data="{}"
                        :preview-state="false"
                        :option-data="{}"
                        :global-dsv="{}"
                      />
                    </div>
                  </template>
                  <!--子表单-->
                  <template v-if="formJsonSub&&formJsonSub.length">
                    <div v-for="(item,i) in formJsonSub" :key="i">
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
                </div>
                <!--3退回 1委派 2转办-->
              </div>
            </div>
            <!-- 基本信息-end-->
            <!-- 权限 -->
            <div v-else-if="activeName==='auth'">
              <authority :submit="false" />
            </div>
            <!-- 审批记录-->
            <div v-else id="part2">
              <div class="flow_designer_page">
                <div class="approval-record-container">
                  <div class="header" />
                  <!--审批记录-表格-->
                  <el-table
                    v-if="currentView === 'grid'"
                    :data="historyProcNodeList"
                    style="max-height: 100%"
                    class="approval-table"
                  >
                    <el-table-column
                      prop="activityName"
                      label="节点名称"
                      align="center"
                    />
                    <el-table-column
                      prop="approver"
                      label="处理人"
                      align="center"
                    >
                      <template #default="scope">
                        {{ scope.row.assigneeName }}
                      </template>
                    </el-table-column>
                    <el-table-column
                      prop="result"
                      label="状态"
                      align="center"
                    >
                      <template #default="scope">
                        <!-- 通过 -->
                        <div v-if="scope.row['commentList'] && scope.row['commentList']?.length > 0">
                          <div v-for="(comment, index) in scope.row['commentList'].slice().reverse()" :key="index">
                            <el-tag :type="approveTypeTag(comment.type)" size="small">
                              {{ commentType(comment.type) }}
                            </el-tag>
                          </div>
                        </div>
                        <div v-else>
                          -
                        </div>
                      </template>
                    </el-table-column>
                    <el-table-column
                      label="意见/反馈"
                      align="center"
                      width="200"
                    >
                      <template #default="scope">
                        <!-- 通过 -->
                        <div v-if="scope.row['commentList'] && scope.row['commentList']?.length > 0">
                          <div v-for="(comment, index) in scope.row['commentList'].slice().reverse()" :key="index">
                            <div>{{ comment['fullMessage'] }}</div>
                          </div>
                        </div>
                        <div v-else>
                          -
                        </div>
                      </template>
                    </el-table-column>
                    <el-table-column
                      prop="createTime"
                      label="处理时间"
                      align="center"
                    />
                  </el-table>
                  <el-card v-if="currentView === 'flow'" class="box-card flowCarBox" shadow="never">
                    <el-col :span="20" :offset="2">
                      <FlowChart
                        v-if="nodeConfig"
                        ref="flowChartRef"
                        v-model:node-config="nodeConfig"
                        class="block"
                        :is-edit="false"
                        :finished="finishedTaskNode"
                        :un-finished="unFinishedTaskNode"
                        :now-val="80"
                      />
                      <div v-else class="empty_flow">
                        <span>流程图加载失败</span>
                      </div>
                    </el-col>
                  </el-card>
                  <el-card v-if="currentView === 'list'" class="box-card" shadow="never">
                    <el-col :span="20" :offset="2">
                      <div v-if="historyProcNodeList&&historyProcNodeList.length" class="block">
                        <el-timeline>
                          <el-timeline-item
                            v-for="(item,index) in historyProcNodeList"
                            :key="index"
                            :icon="setIcon(item.endTime)"
                            :color="setColor(item.endTime)"
                          >
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
                      <div v-else class="empty_flow">
                        <span>暂无数据</span>
                      </div>
                    </el-col>
                  </el-card>
                </div>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import ID2HeadPic from '@/components/ID2HeadPic.vue'
import { detailProcess } from '@/api/processui'
import Authority from '@/pages/processui/views/page/flowManage/authority.vue'
// import { useUserStore } from '@/store/modules/useraPaas'

// const store: any = useUserStore()
const props = defineProps(['item'])
const historyProcNodeList = ref([]) // 流转记录
const preForm = ref<any>(null) // 表单信息
const preFormSub = ref<any>(null) // 子表单信息
const formJsonSub = ref<any>(null) // 子表单信息
const formJson = ref<any>(null) // 表单信息
const nodeConfig = ref(null) // 流程图
const finishedTaskNode = ref<any>([]) // 流程图
const unFinishedTaskNode = ref([]) // 流程图
const oprArr = ref([]) // 按钮-all
const infoTemp = ref<any>(null)
const containerRef = ref<HTMLElement | null>(null)
const activeName = ref<any>('record')
const currentView = ref('list')// 视图模式

const commentType = val => {
  switch (val) {
    case '1':
      return '通过'
    case '2':
      return '退回'
    case '3':
      return '驳回'
    case '4':
      return '委派'
    case '5':
      return '转办'
    case '6':
      return '终止'
    case '7':
      return '撤回'
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
    case '1':
      return 'success'
    case '2':
      return 'warning'
    case '3':
      return 'danger'
    case '4':
      return 'primary'
    case '5':
      return 'success'
    case '6':
      return 'danger'
    case '7':
      return 'info'
  }
}

// tab切换
const tabsFn = async() => {
  await nextTick()
  if (activeName.value === 'flow') currentView.value = 'flow'
  if (activeName.value === 'record') currentView.value = 'list'
  if (activeName.value === 'form') {
    // 禁用表单
    setTimeout(async() => {
      preForm.value?.map(item => item?.disableForm())
    }, 500)
  }
}

// 流程节点详情
const getProcessDetail = async(procInsId, taskId) => {
  formJsonSub.value = null
  formJson.value = null
  nodeConfig.value = null
  historyProcNodeList.value = []
  const params = {
    procInsId: procInsId,
    taskId: taskId
  }
  detailProcess(params).then((res: any) => {
    if (res.code === 200) {
      oprArr.value = res.data?.buttonsMap?.buttonOprArr || []
      if (res.data.processFormList && res.data.processFormList.length > 0) {
        formJsonSub.value = res.data?.taskFormData
        formJson.value = res.data?.processFormList
      }
      if (res.data?.historyProcNodeList) {
        historyProcNodeList.value = res.data?.historyProcNodeList.reverse()
      }
      try {
        if (res.data.flowViewer) {
          finishedTaskNode.value = [...(res.data.flowViewer.finishedTaskSet || []), ...(res.data.flowViewer.finishedSequenceFlowSet || [])]
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

// info
const formTemp = async(params) => {
  let val = { ...params }
  console.log('formTemp', val)
  infoTemp.value = val
  infoTemp.value['procInsId'] = val.procInsId
  infoTemp.value['taskId'] = val.taskId || undefined
  if (val.procInsId) getProcessDetail(val.procInsId, val.taskId) // 流程节点详情
}

watch(() => props.item, (val) => {
  val = {
    'taskId': val.work_order_data?.taskId,
    'procInsId': val.work_order_data?.procInsId
  }
  let merObj = val
  if (merObj) formTemp(merObj)
}, { immediate: true })
</script>
<style lang="scss" scoped>
.dispatchPage {
  width: 100%;
  margin-top: -40px;
}

.el-anchor {
  overflow-y: hidden;
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

    &.img:first-child {
      padding-bottom: 0;
    }
  }

  .infoItem_t {
    width: 50%;
    font-size: 14px;
    line-height: 20px;
    color: #333333;
    white-space: nowrap;

    &.flS {
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

      > .red {
        position: absolute;
        left: -4px;
        top: 0px;
        color: red;
      }
    }

    .infoItem_t_span {
      color: #999;
      padding-left: 8px;
    }
  }

  .pro_djd {
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

.dia_btn {
  justify-content: flex-start;
  padding: 40px 0;
  //border-bottom: 1px solid hsla(0, 0%, 20%, 0.06);
}

// tabs
:deep(.tenanat-tabs) {
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
    font-size: 14px;
    font-weight: 400;
  }
}

.content_dis {
  // min-height: 600px;
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

  :deep(.el-card__body) {

    // height: 200px !important;
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
  .basic {
    padding-bottom: 20px;

    > span {
      flex-shrink: 0;
      width: 120px;
      text-align: left;
      padding-right: 20px;
      font-size: 14px;
      color: #939393;
      font-weight: 400;
    }
  }
}

.flow_designer_page {
  width: 100%;
  overflow: auto;
  display: flex;
  background-color: #fff;
  flex-direction: column;
  align-items: center;
  position: relative;
}

.infoItem_add {
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

  .el-icon {
    font-size: 20px;
  }
}

.elIconPerBox {
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

  &:hover {
    box-shadow: 0 2px 12px 0 var(--el-color-primary-hb);

    > .el-icon {
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

.btn3, .btn3:hover {
  opacity: .5;
  background-color: gray;
  border-color: gray;
}

// 基本信息
.preFormBox {
  border-radius: 4px;
  padding: 20px;
  margin-bottom: 20px;
  border: 1px solid rgba(0, 0, 0, 0.1);
}

// 任务办理
.taskBox {
  padding: 20px;
}

.tree_s_rep {
  justify-content: space-between;
  font-size: 16px;
  //color: var(--el-color-primary);
  padding: 16px 0;
  font-weight: 400;

  .srep {
    display: flex;
    align-items: center;
    gap: 8px;

    &:before {
      content: '';
      width: 3px;
      height: 18px;
      background-color: var(--el-color-primary);
    }

    span {
      color: #33333360;
      font-size: 14px;
    }
  }
}

//聊天
.chat-container {
  margin: 0 auto;
  border-radius: var(--common-border-radius);
  padding: 16px 0;
  min-height: 200px;
}

.chat-message {
  display: flex;
  margin-bottom: 20px;
}

.chat-message.own-message {
  flex-direction: row-reverse;
}

.chat-avatar {
  margin-right: 8px;
}

.chat-content {
  display: flex;
  flex-direction: column;
  width: 100%;
}

.chat-meta {
  display: flex;
  align-items: center;
  margin-bottom: 4px;
}

.chat-nickname {
  margin-right: 8px;
  color: #303133;
  font-size: 12px;
}

.chat-time {
  font-size: 12px;
  color: #909399;
}

.chat-text {
  padding: 8px 12px;
  border-radius: 4px;
  color: #303133;
  max-width: 100%;
  font-size: 14px;
}

.chat-media {
  margin-top: 4px;
}

.chat-file {
  margin-top: 4px;
  width: fit-content;

}

.filesBox {
  border: 1px solid #dcdfe6;
  width: 428px;
  height: 56px;
  padding: 10px;
  border-radius: 10px;
}

.filesBox:hover {
  border-color: var(--el-color-primary);
  background-color: #F7F9FF;
}

.filesBox:hover .file-actions {
  display: flex;
}

.filesImg {
  width: 40px;
  height: 40px;
}

.filesT {
  font-size: 14px;
  color: #3D3D3D;

}

.filesC {
  display: flex;
  width: 100%;
  justify-content: space-between;
}

//.el-button {
//  --el-button-hover-bg-color: transparent
//}
//
//.el-button {
//  --el-button-hover-border-color: transparent
//}

.file-actions {
  display: none; /* 默认隐藏图标 */
  gap: 8px; /* 图标间距 */
}

.el-upload-file {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  border: 1px solid #dcdde0;
  border-radius: 4px;
  background-color: #f5f7fa;
}

.chat-input-area {
  margin-top: 16px;
}

.chat-input {
  width: 100%;
  margin-bottom: 8px;
}

.chat-actions {
  display: flex;
  gap: 16px;
}

//表格
.approval-record-container {
  width: 100%;
  margin: 0 auto;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  font-size: 20px;
  font-weight: bold;
  color: #333;
}

.icons {
  display: flex;
  gap: 10px;
}

.icons .el-button {
  color: #666;
}

.icons .el-button.active {
  color: #409eff;
}

/* 确保表格有竖线 */
.approval-table .el-table__cell {
  border-right: 1px solid #EBEEF5;
}

/* 最后一列不需要右侧边框 */
.approval-table .el-table__cell:last-child {
  border-right: none;
}

.approver-info {
  display: flex;
  align-items: center;
  gap: 5px;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
}

.name {
  font-size: 14px;
  color: #333;
}

.el-tag {
  padding: 4px 8px;
  border-radius: 4px;
}

.main-time {
  font-size: 14px;
  color: #333;
  display: block;
}

.sub-time {
  font-size: 12px;
  color: #999;
}

.action-buttons {
  display: flex;
  gap: 10px;
}

.approval-dialog {
  position: absolute;
  bottom: 38px;
  left: 21%;
  box-shadow: 0px 4px 12px 0px rgba(0, 0, 0, 0.05);
  background-color: #fff;
  border-radius: 4px;
  z-index: 1000;
}

.dialog-mask {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.01); /* 几乎透明的遮罩 */
  z-index: 999;
}

:deep(.containerBox) {
  height: 63vh;
  overflow-y: auto;
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* Internet Explorer 10+ */
  &::-webkit-scrollbar {
    display: none; /* Chrome, Safari, Opera*/
  }
}

.flowCarBox {
  min-height: 500px;
}

.flowAnchorBox {
  //padding-top: 10px;
  font-size: 14px;
  line-height: 22px;

}
</style>
