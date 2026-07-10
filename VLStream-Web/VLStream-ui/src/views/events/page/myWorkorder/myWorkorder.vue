<template>
  <div class="home">
    <div class="table_c flexColumn">
      <div class="table_cc">
        <div class="depNameBox_out flexRowAC">
          <div class="depNameBox flexRowAC" />
          <!--搜索-->
          <div class="searchHeight_out flexRowAC">
            <search-height-box
              keyword="title"
              placeholder="工单标题"
              :data="searchData"
              @handle="searchResetFn"
            />
            <export-excel-pdf />
          </div>
        </div>
        <TableSelf
          current-row-key="taskId"
          class="new_table"
          header-cell-class-name="header_cell"
          stripe
          :data="tableData"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" :width="clacPXToVW(55)" />
          <el-table-column label="序号" type="index" :width="clacPXToVW(60)" align="center">
            <template #default="scope">
              {{ scope.$index + 1 + (page_num - 1) * page_size }}
            </template>
          </el-table-column>
          <el-table-column label="工单编号" align="center" prop="workorderNumber" :width="clacPXToVW(140)" />
          <el-table-column
            label="工单标题"
            align="center"
            prop="title"
            :show-overflow-tooltip="true"
            :width="clacPXToVW(180)"
          />
          <el-table-column label="工单来源" align="center" prop="source">
            <template #default="scope">
              <div v-if="scope.row.source">
                {{ scope.row.source }}
              </div>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="流程版本" align="center" :width="clacPXToVW(100)">
            <template #default="scope">
              <el-tag>
                v{{ scope.row.procDefVersion }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="发起人" align="center" prop="createBy" />
          <el-table-column label="工单状态" align="center">
            <template #default="scope">
              <div v-if="scope.row.workorderStatus" class="workorderStatus">
                <span v-if="scope.row.workorderStatus==='pendingDispatch'" class="LKL">待派单</span>
                <span v-if="scope.row.workorderStatus==='pendingOrders'" class="pink">待接单</span>
                <span v-if="scope.row.workorderStatus==='processing'" class="red">处理中</span>
                <span v-if="scope.row.workorderStatus==='referred'" class="LKL">已转办</span>
                <span v-if="scope.row.workorderStatus==='Returned'" class="LKL">已退回</span>
                <span v-if="scope.row.workorderStatus==='return'" class="ZFB">待回访</span>
                <span v-if="scope.row.workorderStatus==='completed'" class="WX">已完成</span>
                <span v-if="scope.row.workorderStatus==='closed'" class="gray">已关闭</span>
                <span v-if="scope.row.workorderStatus==='toBeEvaluated'" class="blue">待评价</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="提交时间" align="center" prop="createTime" :width="clacPXToVW(180)" />
          <el-table-column align="right" fixed="right" label="操作" :width="clacPXToVW(120)">
            <template #default="scope">
              <div class="operateBox flexRowAC">
                <template v-if="store?.userInfo?.userId===(scope.row?.userId || scope.row?.assignId)">
                  <div
                    v-if="scope.row.workorderStatus==='pendingDispatch'"
                    class="new_table_svg_group"
                    @click="newEditClick(scope.row,2)"
                  >
                    <oort-svg-icon width="20" height="20" name="table_paidan" class="new_table_svg_group_svg" />
                    <span>派单</span>
                  </div>
                  <div
                    v-if="scope.row.workorderStatus==='processing'"
                    class="new_table_svg_group"
                    @click="newEditClick(scope.row,3)"
                  >
                    <oort-svg-icon width="20" height="20" name="table_chuli" class="new_table_svg_group_svg" />
                    <span>处理</span>
                  </div>
                </template>
                <el-tooltip
                  content="详情"
                  effect="light"
                  placement="top"
                >
                  <div class="new_table_svg_group" @click="handleDetail(scope.row)">
                    <oort-svg-icon width="20" height="20" name="table_detail" class="new_table_svg_group_svg" />
                  </div>
                </el-tooltip>
                <el-tooltip
                  v-if="scope.row.finishTime||scope.row.workorderStatus==='closed'"
                  content="删除"
                  effect="light"
                  placement="top"
                >
                  <div class="new_table_svg_group" @click="handleDelete(scope.row)">
                    <oort-svg-icon color="red" width="20" height="20" name="table_del" />
                  </div>
                </el-tooltip>
                <div
                  v-if="scope.row.processStatus !== 'canceled' && !scope.row.finishTime"
                  class="new_table_svg_group"
                  @click="handleCancel(scope.row)"
                >
                  <oort-svg-icon width="20" height="20" name="table_positiveclose" class="new_table_svg_group_svg" />
                  <span>强制关闭</span>
                </div>
              </div>
            </template>
          </el-table-column>
        </TableSelf>
        <div class="paginationBox flexRowAC">
          <el-pagination
            background
            :page-sizes="[10, 20, 30, 50]"
            layout="total, prev, pager, next, sizes"
            :total="count"
            class="justifyAlign"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>
    </div>
    <el-dialog
      v-model="woVisi"
      title="工单详情"
      width="60%"
      top="5vh"
      :direction="direction"
      :destroy-on-close="true"
    >
      <my-workorder-details v-if="woVisi" :workorder="detailsObj" @close="woVisi=false" />
    </el-dialog>
    <el-drawer
      v-model="wodisVisi"
      :title="wodisVisiTitle"
      size="60%"
      class="eventmyWorkBox"
      :direction="direction"
      :destroy-on-close="true"
    >
      <new-work-order-dispatch v-if="wodisVisi" :item="detailsObj" @close="wodisVisi=false" @handle="myWorkorderFn" />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { clacPXToVW } from '@/utils/index'
import { useUserStore } from '@/store/modules/useraPaas'
import { deleteProcess, ownWorkOrderList, stopProcess } from '@/api/processui'
import NewWorkOrderDispatch from '@/pages/processui/views/page/workOrderManage/newWorkOrderDispatch.vue'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import MyWorkorderDetails from '@/pages/events/views/page/myWorkorder/myWorkorderDetails.vue'
import { resolveWorkOrderAppContext } from '@/utils/workOrderAppContext'

const store: any = useUserStore()
const datePickerTime = ref([])
const tableData = ref<any>([])
const count = ref<any>(0)
const page_num = ref<Number>(1)
const page_size = ref<Number>(10)
let appObj = ref<any>('')// 分类
const woVisi = ref<boolean>(false)// 详情
const wosVisi = ref<boolean>(false)// 发起dra
const wodisVisi = ref<boolean>(false)// 处理dra
const wodisVisiTitle = ref<any>('')// 处理dra
const direction = ref<any>('rtl') // 详情左
let detailsObj = ref<any>('')// 详情 obj
const multipleSelection = ref<any>([]) // 批量
const searchData = ref<any>([]) // 高级搜索框内容
searchData.value = [
  { label: '工单编号', value: 'workorderNumber', type: 'text', default: '' },
  { label: '工单标题', value: 'title', type: 'text', default: '' },
  { label: '工单来源', value: 'source', type: 'text', default: '' },
  {
    label: '工单状态',
    value: 'processStatus',
    type: 'select',
    option: [{ label: '进行中', value: 'running' }, { label: '已完成', value: 'completed' }, {
      label: '已终止',
      value: 'terminated'
    }, { label: '已取消', value: 'canceled' }],
    default: undefined
  },
  { label: '时间', value: 'datePickerTime', type: 'daterange', default: [] }
]
const form = reactive({
  orderByColumn: '', // 工单编号
  title: '', // 工单标题
  source: '', // 工单来源
  workorderId: '', // 工单类型
  workorderNumber: '', // 工单编号
  workorderStatus: '', // 工单状态
  processStatus: '' // 审批状态
})

// 搜索重置
function searchResetFn(val, reset) {
  if (reset) {
    page_num.value = 1
  }
  form.title = val.title
  form.workorderNumber = val.workorderNumber
  form.workorderStatus = val.workorderStatus
  form.processStatus = val.processStatus
  form.source = val.source
  form.orderByColumn = val.orderByColumn
  datePickerTime.value = val.datePickerTime
  myWorkorderFn()
}

// 派单/处理
const newEditClick = (val: any, type: number) => {
  if (type === 0) {
    wosVisi.value = true
    detailsObj.value = val
  }
  if (type < 9 && type > 0) {
    let t = ''
    if (type === 1) t = '接单'
    if (type === 2) t = '指派'
    if (type === 3) t = '处理'
    if (type === 4) t = '审核'
    if (type === 5) t = '待访问'
    if (type === 6) t = '待评价'
    wodisVisiTitle.value = `${t}工单（${val.title || ''}）`
    wodisVisi.value = true
    detailsObj.value = val
    detailsObj.value['type'] = type + ''
  }
}

// 批量
const handleSelectionChange = (val: any) => {
  multipleSelection.value = val
}

// 删除工单
function handleDelete(row) {
  ElMessageBox.confirm(
    '删除该流程，是否继续',
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    deleteProcess({ procInsId: row.procInsId }).then((res: any) => {
      if (res.code === 200) {
        // 删除工单
        ElMessage.success('流程删除成功')
        myWorkorderFn()
      }
    })
  }).catch(() => {
  })
}

// 强制关闭
function handleCancel(row) {
  ElMessageBox.confirm(
    '取消该流程，是否继续',
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    stopProcess({ procInsId: row.procInsId }).then((res: any) => {
      if (res.code === 200) {
        ElMessage.success('流程取消成功')
        myWorkorderFn()
      }
    })
  }).catch(() => {
  })
}

// 我的工单
const myWorkorderFn = async() => {
  try {
    const data = {
      accessToken: store.userInfo?.accessToken,
      pageNum: 1,
      pageSize: 99,
      categoryType: 'WorkOrderAppAll', // 工单-应用分类
      category: appObj.value?.appId
    }
    if (datePickerTime.value && datePickerTime.value?.length !== 0) {
      data['start_date'] = dayjs(datePickerTime.value[0]).format('YYYY-MM-DD HH:mm:ss')
      data['end_date'] = dayjs(datePickerTime.value[1]).format('YYYY-MM-DD 23:59:59')
    }
    const res: any = await ownWorkOrderList(data)
    if (res?.code === 200 && res?.rows) {
      tableData.value = res.rows || []
      count.value = res.total || 0
    }
  } catch (error) {
    tableData.value = []
    count.value = []
  }
}

function handleSizeChange(pageSize) {
  page_size.value = pageSize
  myWorkorderFn()
}

function handleCurrentChange(pageNum) {
  page_num.value = pageNum
  myWorkorderFn()
}

function handleDetail(row) {
  detailsObj.value = row
  woVisi.value = true
}

onMounted(async() => {
  appObj.value = await resolveWorkOrderAppContext()
  myWorkorderFn()
})

</script>
<style lang="scss" scoped>
.searchImg {
  width: 14px;
  height: 14px;
  background-size: cover;
  margin-right: 8px;
}

.table_cc :deep(.header_cell) {
  color: #333;
  background-color: #FAFAFA;
}

.home {
  /* height: 100%; */
  background: #F7F7F7;
  border-radius: var(--common-border-radius) var(--common-border-radius) 0px 0px;
  /* overflow: auto; */

  .content_top {
    margin-top: 20px;
    height: 420px;
    padding-bottom: 20px;
    gap: 20px;
  }

  .content_bottom {
    height: 556px;
    gap: 20px;
    align-items: flex-start;
  }

  .bgRadius {
    background: #FFFFFF;
    border-radius: 10px;
    padding: 20px;
  }
}

.table_c {
  width: 100%;
  border-radius: 10px;
}

.table_c_header {
  height: 60px;
  margin: 0 24px;
}

.table_cc {
  background: #fff;
  padding: 20px;
}

.paginationBox {
  justify-content: center;
  height: 100px;
}

.operateDe {
  cursor: pointer;
  color: var(--el-color-primary);;
}

.reSucc {
  width: 54px;
  height: 22px;
  background: #E9FFD9;
  border: 1px solid rgba(167, 237, 119, 1);
  font-size: 12px;
  padding: 4px;
  border-radius: 4px;
  color: #5CD905;
}

.operateBox {
  justify-content: flex-end;

  .operateRe {
    cursor: pointer;
    font-size: 14px;
    color: #FB5504;
  }
}

// 新增
.exportBtnBox {
  gap: 12px;
  padding: 0;

  // 新增
  .exportBtn {
    cursor: pointer;
    justify-content: center;
    padding: 0 12px;
    height: 32px;
    background: var(--el-color-primary);
    border-radius: 5px;
    border: 1px solid var(--el-color-primary);
    font-size: 14px;
    color: #FFFFFF;
    box-shadow: 0px 2px 0px 0px rgba(0, 0, 0, 0.04);

    &.Btn2 {
      background: #FFFFFF;
      color: #333333;
      border: 1px solid rgba(217, 217, 217, 1);

      &:hover {
        color: var(--el-color-primary);
        border: 1px solid var(--el-color-primary-light-3) !important;
        background-color: var(--el-color-primary-light-8) !important;
      }
    }

    &.sta {
      background: #10CE00;
      border: 1px solid #10CE00;

      &:hover {
        opacity: 0.8;
        background: #10CE00 !important;
      }
    }

    &.end {
      background: #F06800;
      border: 1px solid #F06800;
    }

    .BtnImg {
      width: 14px;
      height: 14px;
      margin-right: 8px;
      background-size: cover;
    }
  }
}

// 工单状态
.workorderStatus {
  font-size: 12px;
  line-height: 22px;
  font-style: normal;

  > span {
    padding: 2px 6px;
    border-radius: 4px;
  }

  > .LKL {
    color: #FA8D00;
    background: #FFF7E6;
    border: 1px solid #FFD591;
  }

  > .ZFB {
    color: #338CF6;
    background: #E9F3FF;
    border: 1px solid #90C5FF;
  }

  > .WX {
    color: #5CD905;
    background: #E9FFD9;
    border: 1px solid #A7ED77;
  }

  > .pink {
    color: #FA0070;
    background: #FFE7F2;
    border: 1px solid #FFA3CC;
  }

  > .gray {
    color: #999999;
    background: #F3F3F3;
    border: 1px solid #BFBFBF;
  }

  > .red {
    color: #FF0000;
    background: #FFE4E4;
    border: 1px solid #FFA3A3;
  }

  > .blue {
    color: #0225FF;
    background: #E9ECFF;
    border: 1px solid #A1AEFF;
  }
}

// 紧急程度
.priority {
  width: 90px
}

// 打印
.printBox {
  width: 100%;
  height: 100vh;
  padding: 40px;
  position: fixed;
  top: 0;
  left: 0;
  background-color: #fff;
  z-index: 99;
  overflow: auto;
}

// 紧急程度
:deep(.priority) {
  .el-select__selected-item > span {
    color: var(--el-color-primary);
    position: relative;
    padding-left: 12px;

    &:before {
      content: "";
      display: flex;
      position: absolute;
      top: 4px;
      left: 0px;
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: var(--el-color-primary);
    }
  }

  &.a {
    .el-select__selected-item > span {
      color: #23B6FF;

      &:before {
        background: #23B6FF;
      }
    }
  }

  &.b {
    .el-select__selected-item > span {
      color: #FF0000;

      &:before {
        background: #FF0000;
      }
    }
  }
}

:deep(.eventmyWorkBox) {
  .content_dis {
    margin-left: -20px;

  }

  //.el-tabs__item.is-top {
  //  font-size: 14px;
  //  font-weight: 400;
  //}
}
</style>
