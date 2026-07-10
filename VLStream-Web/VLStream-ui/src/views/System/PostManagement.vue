<template>
  <SystemPageShell>
    <!-- 顶部工具栏 -->
    <template #toolbar>
      <div class="toolbar-left">
        <el-form :inline="true" :model="queryParams" size="default">
          <el-form-item label="岗位编码">
            <el-input v-model="queryParams.postCode" placeholder="请输入岗位编码" clearable @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="岗位名称">
            <el-input v-model="queryParams.postName" placeholder="请输入岗位名称" clearable @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">
              <el-icon><Search /></el-icon> 查询
            </el-button>
            <el-button @click="handleReset">
              <el-icon><Refresh /></el-icon> 重置
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="toolbar-right">
        <!-- 按钮操作组 -->
        <ActionButtonGroup
          :selected-count="selectedRows.length"
          @add="handleCreate"
          @edit="handleEdit(selectedRows[0])"
          @delete="handleBatchRemove"
        />
      </div>
    </template>

    <!-- 列表数据表格 -->
    <el-table
      v-loading="loading"
      :data="tableData"
      stripe
      style="width: 100%; height: 100%"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" />
      <el-table-column prop="postCode" label="岗位编码" min-width="120" />
      <el-table-column prop="postName" label="岗位名称" min-width="150" />
      <el-table-column prop="category" label="岗位分类" min-width="100" align="center">
        <template #default="scope">
          <span>{{ scope.row.category === 1 ? '高管' : scope.row.category === 2 ? '经理' : '员工' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="80" align="center" />
      <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
      <el-table-column label="操作" width="180" fixed="right" align="right">
        <template #default="scope">
          <div class="operation-buttons">
            <el-button class="operation-btn edit-btn" @click="handleEdit(scope.row)">
              编辑
            </el-button>
            <el-button class="operation-btn delete-btn" @click="handleSingleRemove(scope.row)">
              删除
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- 底部分页 -->
    <template #pagination>
      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </template>
  </SystemPageShell>

  <!-- 新增/编辑岗位对话框 -->
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="500px"
    destroy-on-close
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="100px"
      style="padding: 10px 20px"
    >
      <el-form-item label="岗位类型" prop="category">
        <el-select v-model="form.category" placeholder="请选择岗位类型" style="width: 100%">
          <el-option :value="1" label="高管" />
          <el-option :value="2" label="经理" />
          <el-option :value="3" label="员工" />
        </el-select>
      </el-form-item>

      <el-form-item label="岗位编码" prop="postCode">
        <el-input v-model="form.postCode" placeholder="请输入岗位编码" />
      </el-form-item>

      <el-form-item label="岗位名称" prop="postName">
        <el-input v-model="form.postName" placeholder="请输入岗位名称" />
      </el-form-item>

      <el-form-item label="岗位排序" prop="sort">
        <el-input-number v-model="form.sort" :min="1" style="width: 100%" />
      </el-form-item>

      <el-form-item label="备注" prop="remark">
        <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" :rows="2" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSaveSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import SystemPageShell from './components/SystemPageShell.vue'
import ActionButtonGroup from '@/components/ActionButtonGroup.vue'
import { getPostList, submitPost, removePosts } from '@/api/system/post'
import { getPayload, getRecords, getTotal, joinIds, isSuccess } from './utils/response'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增岗位')
const selectedRows = ref([])
const tableData = ref([])

// 分页结构
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

// 过滤参数
const queryParams = reactive({
  postCode: '',
  postName: ''
})

// 表单与必填验证
const formRef = ref()
const form = ref({
  id: undefined,
  category: 3,
  postCode: '',
  postName: '',
  sort: 1,
  remark: ''
})

const rules = {
  postCode: [{ required: true, message: '请输入岗位编码', trigger: 'blur' }],
  postName: [{ required: true, message: '请输入岗位名称', trigger: 'blur' }]
}

/**
 * 异步获取岗位分页列表数据
 */
async function loadData() {
  loading.value = true
  try {
    const res = await getPostList({
      current: pagination.current,
      size: pagination.size,
      postCode: queryParams.postCode,
      postName: queryParams.postName
    })
    tableData.value = getRecords(res)
    pagination.total = getTotal(res)
  } catch (error) {
    console.error('加载岗位列表失败:', error)
    ElMessage.error('加载岗位列表失败')
  } finally {
    loading.value = false
  }
}

/**
 * 搜索过滤
 */
function handleSearch() {
  pagination.current = 1
  loadData()
}

/**
 * 重置查询条件
 */
function handleReset() {
  queryParams.postCode = ''
  queryParams.postName = ''
  pagination.current = 1
  loadData()
}

/**
 * 分页大小改变时重新拉取数据
 * @param {Number} size 分页每页大小
 */
function handleSizeChange(size) {
  pagination.size = size
  pagination.current = 1
  loadData()
}

/**
 * 页码改变时重新拉取数据
 * @param {Number} current 页码
 */
function handleCurrentChange(current) {
  pagination.current = current
  loadData()
}

/**
 * 勾选变化回调
 * @param {Array} rows 选中的行
 */
function handleSelectionChange(rows) {
  selectedRows.value = rows
}

/**
 * 打开新增岗位对话框
 */
function handleCreate() {
  dialogTitle.value = '新增岗位'
  form.value = {
    id: undefined,
    category: 3,
    postCode: '',
    postName: '',
    sort: 1,
    remark: ''
  }
  dialogVisible.value = true
}

/**
 * 打开修改岗位对话框，回显行数据
 * @param {Object} row 行数据
 */
function handleEdit(row) {
  if (!row) return
  form.value = { ...row }
  dialogTitle.value = '编辑岗位'
  dialogVisible.value = true
}

/**
 * 确定并保存表单内容，先做必填项检验
 */
async function handleSaveSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      saving.value = true
      try {
        const res = await submitPost(form.value)
        if (isSuccess(res)) {
          ElMessage.success('保存成功')
          dialogVisible.value = false
          loadData()
        } else {
          ElMessage.error(res?.msg || '保存失败')
        }
      } catch (error) {
        console.error('提交岗位信息失败:', error)
        ElMessage.error('提交岗位信息发生异常')
      } finally {
        saving.value = false
      }
    }
  })
}

/**
 * 物理或逻辑删除选中记录，包含二次提示层
 * @param {String} ids 逗号拼接的ID字符串
 * @param {String} msg 确认消息提示
 */
function executeRemove(ids, msg) {
  ElMessageBox.confirm(msg, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await removePosts(ids)
      if (isSuccess(res)) {
        ElMessage.success('删除成功')
        loadData()
      } else {
        ElMessage.error(res?.msg || '删除失败')
      }
    } catch (error) {
      console.error('删除岗位失败:', error)
      ElMessage.error('删除岗位操作异常')
    }
  }).catch(() => {})
}

/**
 * 行内删除单条岗位
 * @param {Object} row 目标行
 */
function handleSingleRemove(row) {
  executeRemove(String(row.id), `确定删除岗位 [${row.postName}] 吗？`)
}

/**
 * 批量删除选中的所有岗位行
 */
function handleBatchRemove() {
  if (selectedRows.value.length === 0) return
  const ids = joinIds(selectedRows.value)
  executeRemove(ids, `确定删除选中的 ${selectedRows.value.length} 个岗位吗？`)
}

// 挂载加载数据
onMounted(() => {
  loadData()
})
</script>

<style scoped>
.operation-buttons {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
}

.operation-btn {
  height: 28px !important;
  padding: 0 12px !important;
  font-size: 12px !important;
  border-radius: 14px !important;
  font-weight: 500 !important;
}

.edit-btn {
  background: #ffffff !important;
  color: #1A53FF !important;
  border: 1px solid #1A53FF !important;
}

.edit-btn:hover {
  background: #f0f4ff !important;
}

.delete-btn {
  background: #ffffff !important;
  color: #f56c6c !important;
  border: 1px solid #d9d9d9 !important;
}

.delete-btn:hover {
  border-color: #f56c6c !important;
  color: #f56c6c !important;
}

.toolbar-left {
  display: flex;
  align-items: center;
}

.el-form-item {
  margin-bottom: 0;
  margin-right: 16px;
}
</style>
