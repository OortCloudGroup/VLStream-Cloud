<template>
  <el-card class="box-card rePage" shadow="never">
    <el-col>
      <div class="iconsBox flexRowAC">
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
          label="审批人"
          align="center"
        >
          <template #default="scope">
            {{ scope.row.assigneeName }}
          </template>
        </el-table-column>
        <el-table-column
          prop="result"
          label="审批结果"
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
          label="审批意见"
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
          label="审批时间"
          align="center"
        />
      </el-table>
      <div v-if="currentView === 'flow'" class="block">
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
      </div>
      <div v-if="currentView === 'list'" class="block">
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
</template>
<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import ID2HeadPic from '@/components/ID2HeadPic.vue'

const props = defineProps(['historyProcNodeList',
  'nodeConfigProp',
  'unFinishedTaskNode',
  'finishedTaskNode'])
const nodeConfig = ref(null) // 流程图
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

onMounted(async() => {
})

watch(() => props.historyProcNodeList, () => {
}, { immediate: true })
watch(() => props.nodeConfigProp, (val) => {
  nodeConfig.value = val
}, { immediate: true })
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

.btn3, .btn3:hover {
  opacity: .5;
  background-color: gray;
  border-color: gray;
}

// 工单信息
.infoBox {
  .infoItem {
    padding-bottom: 20px;

    &.img:first-child {
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

  &Out {
    padding: 20px 20px 0;
  }
}

// 处理人
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

// 人员选择
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

.iconsBox {
  z-index: 9;
  position: relative;
  width: 100%;
  height: 40px;
  align-items: flex-start;
  justify-content: flex-end;

  svg{
    cursor: pointer;
  }
}

.rePage > {
  :deep(.el-card__body) {
    padding-top: 0;
    .el-col{
      padding-left: 20px;
    }
  }
}

.approval-table{

}
</style>
