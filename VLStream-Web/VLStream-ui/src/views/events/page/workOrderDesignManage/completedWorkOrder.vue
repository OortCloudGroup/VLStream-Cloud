<template>
  <div class="home">
    <div class="table_c flexColumn">
      <div class="table_cc">
        <div class="depNameBox_out flexRowAC">
          <div class="depNameBox flexRowAC" />
          <!--搜索-->
          <div class="searchHeight_out flexRowAC">
            <search-height-box keyword="processName" placeholder="流程名称" :data="searchData" @handle="searchResetFn" />
            <export-excel-pdf />
          </div>
        </div>
        <TableSelf current-row-key="taskId" class="new_table" header-cell-class-name="header_cell" stripe :data="tableData">
          <el-table-column type="selection" :width="clacPXToVW(55)" />
          <el-table-column label="序号" type="index" :width="clacPXToVW(60)" align="center">
            <template #default="scope">
              {{ scope.$index + 1 + (queryParams.pageNum - 1) * queryParams.pageSize }}
            </template>
          </el-table-column>
          <el-table-column label="工单编号" align="center" prop="workorderNumber" :show-overflow-tooltip="true" :width="clacPXToVW(140)" />
          <el-table-column label="工单标题" align="center" prop="title" :show-overflow-tooltip="true" :width="clacPXToVW(180)" />
          <el-table-column label="所属分类" align="center" prop="categoryName" :width="clacPXToVW(140)" />
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
          <el-table-column label="发起人" align="center">
            <template #default="scope">
              <label>{{ scope.row.createBy }}</label>
            </template>
          </el-table-column>
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
          <el-table-column label="接收时间" align="center" prop="createTime" :width="clacPXToVW(180)" />
          <el-table-column v-if="false" label="审批时间" align="center" prop="endTime" :width="clacPXToVW(180)" />
          <el-table-column v-if="false" label="耗时" align="center" prop="processingTime" :width="clacPXToVW(180)" />
          <el-table-column align="right" label="操作" fixed="right" :width="clacPXToVW(240)">
            <template #default="scope">
              <div class="operateBox flexRowAC">
                <div class="new_table_svg_group" @click="handleDetail(scope.row)">
                  <oort-svg-icon width="20" height="20" name="table_record" class="new_table_svg_group_svg" />
                  <span>流转记录</span>
                </div>
                <div class="new_table_svg_group" @click="handleCallBack(scope.row)">
                  <oort-svg-icon width="20" height="20" name="table_withdraw" class="new_table_svg_group_svg" />
                  <span>撤回</span>
                </div>
              </div>
            </template>
          </el-table-column>
        </TableSelf>
      </div>
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
    <el-drawer
      v-model="dVisi"
      title="流程详情"
      size="80%"
      direction="rtl"
      :destroy-on-close="true"
    >
      <task-handle-detail v-if="dVisi" :item="ditem" @close="dVisi=false" @handle="getProcessList" />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { finishedWorkOrderList, revokeProcess } from '@/api/processui/index'
import { ElMessage } from 'element-plus'
import { clacPXToVW } from '@/utils/index.ts'
import TaskHandleDetail from '@/pages/processui/views/page/flowManage/taskHandleDetail.vue'
// import { useRoute } from 'vue-router'
import dayjs from 'dayjs'
import { resolveWorkOrderAppContext } from '@/utils/workOrderAppContext'

// const route = useRoute()
const dVisi = ref<boolean>(false)
const ditem = ref<any>('')
let appObj = ref<any>('')// 分类
const dateRange = ref('')
const tableData = ref<any>([])
const count = ref<number>(0)
const searchData = ref<any>([]) // 高级搜索框内容
searchData.value = [
  { label: '流程名称', value: 'processName', type: 'text', default: '' },
  { label: '时间', value: 'dateRange', type: 'daterange', format: 'YYYY-MM-DD HH:mm:ss', default: [] }
]
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  category: undefined,
  processName: undefined
})

function getProcessList() {
  let data = {
    ...queryParams,
    categoryType: 'WorkOrderAppAll', // 工单-应用分类
    category: appObj.value?.appId
  }
  if (dateRange.value && dateRange.value?.length !== 0) {
    data['proStartBeginTime'] = dateRange.value[0]
    data['proStartEndTime'] = dayjs(dateRange.value[1]).format('YYYY-MM-DD 23:59:59')
  }
  finishedWorkOrderList(data).then((response:any) => {
    tableData.value = response.rows || []
    count.value = response.total || 0
  })
}

// 搜索重置
function searchResetFn(val, reset) {
  if (reset) queryParams.pageNum = 1
  queryParams.processName = val.processName
  dateRange.value = val.dateRange
  getProcessList()
}

function handleSizeChange(pageSize) {
  queryParams.pageSize = pageSize
  getProcessList()
}

function handleCurrentChange(pageNum) {
  queryParams.pageNum = pageNum
  getProcessList()
}

function handleDetail(row) {
  dVisi.value = true
  ditem.value = row
}

function handleCallBack(row) {
  const params = {
    procInsId: row.procInsId,
    taskId: row.taskId
  }
  revokeProcess(params).then((res:any) => {
    if (res.code === 200) {
      ElMessage({
        type: 'success',
        message: '操作成功'
      })
    }
  })
  // todo 撤回
  console.log('撤回', row)
}

onMounted(async() => {
  appObj.value = await resolveWorkOrderAppContext()
  getProcessList()
})

</script>
<style lang='scss' scoped>
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
  /* height: 100%;
  overflow: auto; */

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
  background: #FFFFFF;
  border-radius: 10px;
}

.table_c_header {
  height: 60px;
  margin: 0 24px;
}

.table_cc {
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
  gap: 2px;

  .operateRe {
    cursor: pointer;
    font-size: 14px;
    color: #FB5504;
  }
}

// 工单状态
.workorderStatus {
  font-size: 12px;
  line-height: 22px;
  font-style: normal;

  >span {
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
</style>
