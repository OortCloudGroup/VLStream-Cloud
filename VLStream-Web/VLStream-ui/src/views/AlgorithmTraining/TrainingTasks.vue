<template>
  <div class="page-container">
    <!-- 查询栏 -->
    <div class="query-bar">
      <div class="query-content">
        <div class="search-item">
          <el-input 
            v-model="queryForm.taskName" 
            placeholder="请输入任务名称"
            clearable
          />
        </div>
        <div class="search-item">
          <el-select 
            v-model="queryForm.status" 
            placeholder="请选择状态"
            clearable
            style="width: 100%"
          >
            <el-option label="全部" value="" />
            <el-option label="等待中" value="pending" />
            <el-option label="训练中" value="training" />
            <el-option label="已完成" value="completed" />
            <el-option label="失败" value="failed" />
          </el-select>
        </div>
        <div class="search-buttons">
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </div>
      </div>
    </div>

    <!-- 主内容区域 -->
    <div class="main-content">
      <!-- 工具栏 -->
      <div class="toolbar">
        <div class="toolbar-left">
          <ActionButtonGroup 
            :selected-count="selectedRows.length"
            @add="handleAdd"
            @edit="handleEdit"
            @delete="handleDelete"
          />
        </div>
      </div>

      <!-- 表格内容 -->
      <div class="table-content">
        <el-table 
          :data="currentPageData" 
          stripe
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="55" />
          <el-table-column type="index" label="序号" width="80" align="center" />
          <el-table-column prop="taskName" label="任务名称" min-width="150" />
          <el-table-column prop="datasetName" label="数据集" min-width="120" />
          <el-table-column prop="baseModel" label="基础模型" min-width="120" />
          <el-table-column prop="trainStatusDesc" label="状态" min-width="100" align="center">
            <template #default="scope">
              <el-tag 
                :type="getStatusType(scope.row.trainStatus)" 
                size="small"
              >
                {{ scope.row.trainStatusDesc }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="progress" label="进度" min-width="120" align="center">
            <template #default="scope">
              <el-progress 
                :percentage="scope.row.progress || 0" 
                :status="getProgressStatus(scope.row.trainStatus)"
                :stroke-width="8"
              />
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" min-width="160" />
          <el-table-column label="操作" width="240" fixed="right" align="right">
            <template #default="scope">
              <div class="table-action-buttons">
                <el-button 
                  type="primary"
                  text
                  size="small"
                  @click="handleDetailRow(scope.row)"
                >
                  详情
                </el-button>
                <el-button 
                  type="primary"
                  text
                  size="small"
                  @click="handleEditRow(scope.row)"
                >
                  编辑
                </el-button>
                <el-button 
                  type="danger"
                  text
                  size="small"
                  @click="handleDeleteRow(scope.row)"
                >
                  删除
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="table-pagination">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="filteredData.length"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="dialogTitle" 
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form 
        ref="formRef" 
        :model="form" 
        :rules="rules" 
        label-width="120px"
      >
        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model="form.taskName" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="数据集" prop="datasetName">
          <el-input v-model="form.datasetName" placeholder="请输入数据集名称" />
        </el-form-item>
        <el-form-item label="基础模型" prop="baseModel">
          <el-input v-model="form.baseModel" placeholder="请输入基础模型" />
        </el-form-item>
        <el-form-item label="训练参数" prop="trainParams">
          <el-input 
            v-model="form.trainParams" 
            type="textarea" 
            :rows="4"
            placeholder="请输入训练参数（JSON格式）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog 
      v-model="detailVisible" 
      title="训练任务详情" 
      width="800px"
    >
      <el-descriptions :column="2" border>
        <el-descriptions-item label="任务名称">{{ detailData.taskName }}</el-descriptions-item>
        <el-descriptions-item label="数据集">{{ detailData.datasetName }}</el-descriptions-item>
        <el-descriptions-item label="基础模型">{{ detailData.baseModel }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(detailData.trainStatus)">
            {{ detailData.trainStatusDesc }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="进度">
          <el-progress 
            :percentage="detailData.progress || 0" 
            :status="getProgressStatus(detailData.trainStatus)"
          />
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detailData.createTime }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ detailData.startTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ detailData.endTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="训练时长" :span="2">
          {{ detailData.durationMinutes ? detailData.durationMinutes + '分钟' : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="训练参数" :span="2">
          <pre>{{ detailData.trainParams || '-' }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import ActionButtonGroup from '@/components/ActionButtonGroup.vue'
import { 
  listTrainingTasks, 
  getTrainingTask, 
  addTrainingTask, 
  updateTrainingTask, 
  delTrainingTask,
  startTraining,
  stopTraining,
  getTrainingProgress
} from '@/api/algorithmTraining'

// 响应式数据
const queryForm = reactive({
  taskName: '',
  status: ''
})

const tableData = ref([])
const selectedRows = ref([])
const currentPage = ref(1)
const pageSize = ref(10)

// 对话框相关
const dialogVisible = ref(false)
const detailVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref()
const form = reactive({
  id: null,
  taskName: '',
  datasetName: '',
  baseModel: '',
  trainParams: ''
})
const detailData = ref({})

// 表单验证规则
const rules = {
  taskName: [
    { required: true, message: '请输入任务名称', trigger: 'blur' }
  ],
  datasetName: [
    { required: true, message: '请输入数据集名称', trigger: 'blur' }
  ],
  baseModel: [
    { required: true, message: '请输入基础模型', trigger: 'blur' }
  ]
}

// 计算属性
const filteredData = computed(() => {
  let filtered = tableData.value
  
  if (queryForm.taskName) {
    filtered = filtered.filter(item => 
      item.taskName.toLowerCase().includes(queryForm.taskName.toLowerCase())
    )
  }
  
  if (queryForm.status) {
    filtered = filtered.filter(item => item.trainStatus === queryForm.status)
  }
  
  return filtered
})

const currentPageData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredData.value.slice(start, end)
})

// 方法
const loadData = async () => {
  try {
    const response = await listTrainingTasks()
    if (response.code === 200) {
      tableData.value = response.data || []
    }
  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.error('加载数据失败')
  }
}

const handleSearch = () => {
  currentPage.value = 1
  ElMessage.success('搜索完成')
}

const handleReset = () => {
  Object.keys(queryForm).forEach(key => {
    queryForm[key] = ''
  })
  currentPage.value = 1
  ElMessage.info('搜索条件已重置')
}

const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
}

const handleCurrentChange = (val) => {
  currentPage.value = val
}

// 工具栏操作
const handleAdd = () => {
  dialogTitle.value = '新增训练任务'
  Object.keys(form).forEach(key => {
    form[key] = key === 'id' ? null : ''
  })
  dialogVisible.value = true
}

const handleEdit = () => {
  if (selectedRows.value.length !== 1) {
    ElMessage.warning('请选择一条记录进行编辑')
    return
  }
  handleEditRow(selectedRows.value[0])
}

const handleDelete = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择要删除的记录')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedRows.value.length} 条记录吗？`,
      '确认删除',
      { type: 'warning' }
    )
    
    const ids = selectedRows.value.map(row => row.id)
    const response = await delTrainingTask(ids)
    
    if (response.code === 200) {
      ElMessage.success('删除成功')
      loadData()
      selectedRows.value = []
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 表格行操作
const handleDetailRow = async (row) => {
  try {
    const response = await getTrainingTask(row.id)
    if (response.code === 200) {
      detailData.value = response.data
      detailVisible.value = true
    }
  } catch (error) {
    console.error('获取详情失败:', error)
    ElMessage.error('获取详情失败')
  }
}

const handleEditRow = (row) => {
  dialogTitle.value = '编辑训练任务'
  Object.keys(form).forEach(key => {
    form[key] = row[key] || ''
  })
  dialogVisible.value = true
}

const handleDeleteRow = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除任务"${row.taskName}"吗？`,
      '确认删除',
      { type: 'warning' }
    )
    
    const response = await delTrainingTask([row.id])
    
    if (response.code === 200) {
      ElMessage.success('删除成功')
      loadData()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    
    const submitData = { ...form }
    let response
    
    if (form.id) {
      response = await updateTrainingTask(submitData)
    } else {
      response = await addTrainingTask(submitData)
    }
    
    if (response.code === 200) {
      ElMessage.success(form.id ? '更新成功' : '创建成功')
      dialogVisible.value = false
      loadData()
    }
  } catch (error) {
    console.error('提交失败:', error)
    ElMessage.error('提交失败')
  }
}

// 状态相关方法
const getStatusType = (status) => {
  switch (status) {
    case 'pending': return 'info'
    case 'training': return 'warning'
    case 'completed': return 'success'
    case 'failed': return 'danger'
    default: return 'info'
  }
}

const getProgressStatus = (status) => {
  switch (status) {
    case 'completed': return 'success'
    case 'failed': return 'exception'
    default: return ''
  }
}

// 生命周期
onMounted(() => {
  loadData()
})
</script>

<style scoped>
.page-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 0;
  background-color: #f5f7fa;
  padding: 20px;
  overflow: hidden;
}

.query-bar {
  background: #F0F2F5;
  border-radius: 8px 8px 0 0;
  padding: 20px;
}

.query-content {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.search-item {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.search-item:first-child {
  min-width: 240px;
}

.search-item:nth-child(2) {
  min-width: 200px;
}

.search-buttons {
  display: flex;
  gap: 12px;
  flex-shrink: 0;
}

.main-content {
  flex: 1;
  background: white;
  border-radius: 0 0 8px 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow: hidden;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toolbar-left {
  display: flex;
  align-items: center;
}

.table-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.table-pagination {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.table-action-buttons {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.table-action-buttons :deep(.el-button--primary.is-text) {
  color: #1A53FF !important;
  background: transparent !important;
  border: none !important;
  padding: 2px 8px !important;
}

.table-action-buttons :deep(.el-button--primary.is-text:hover) {
  color: #3d70ff !important;
  background: transparent !important;
  border: none !important;
}

.table-action-buttons :deep(.el-button--danger.is-text) {
  color: #f56c6c !important;
  background: transparent !important;
  border: none !important;
  padding: 2px 8px !important;
}

.table-action-buttons :deep(.el-button--danger.is-text:hover) {
  color: #f78989 !important;
  background: transparent !important;
  border: none !important;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

pre {
  background: #f5f5f5;
  padding: 8px;
  border-radius: 4px;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}
</style> 