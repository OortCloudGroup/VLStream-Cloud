<template>
  <div class="home">
    <div class="table_c flexColumn">
      <div class="table_cc">
        <div class="depNameBox_out flexRowAC">
          <div />
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
          <el-table-column label="工单编号" align="center" prop="workorderNumber" :show-overflow-tooltip="true" />
          <el-table-column label="工单标题" align="center" prop="title" :show-overflow-tooltip="true" />
          <el-table-column label="所属分类" align="center" prop="categoryName" />
          <el-table-column label="工单来源" align="center" prop="source">
            <template #default="scope">
              <div v-if="scope.row.source">
                {{ scope.row.source }}
              </div>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="流程版本" align="center">
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
          <el-table-column label="提交时间" align="center" prop="createTime" :width="clacPXToVW(180)" />
          <el-table-column align="right" fixed="right" label="操作" :width="clacPXToVW(120)">
            <template #default="scope">
              <div class="operateBox flexRowAC">
                <div class="new_table_svg_group" @click="handleDetail(scope.row)">
                  <oort-svg-icon width="20" height="20" name="table_handle" class="new_table_svg_group_svg" />
                  <span>办理</span>
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
import { todoWorkOrderList } from '@/api/processui/index'
// import { useRoute, useRouter } from 'vue-router'
import { clacPXToVW } from '@/utils/index.ts'
import TaskHandleDetail from '@/pages/processui/views/page/flowManage/taskHandleDetail.vue'
import dayjs from 'dayjs'
import { resolveWorkOrderAppContext } from '@/utils/workOrderAppContext'

// const router = useRouter()
// const route = useRoute()
let appObj = ref<any>('')// 分类
const dVisi = ref<boolean>(false)
const ditem = ref<any>('')
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
  processName: undefined,
  category: undefined,
  processKey: undefined
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
  // 没分类加上true
  todoWorkOrderList(data).then((response: any) => {
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
</style>
