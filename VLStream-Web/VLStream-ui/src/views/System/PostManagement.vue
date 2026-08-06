<template>
  <div class="post-management tenant_Page draHeaPB">
    <div class="tenant_content">
      <div class="tableTenBox flexRowAC">
        <div class="tableTenItU">
          <div class="depNameBox_out flexRowAC">
            <div class="depNameBox flexRowAC">
              <div class="exportBtnBox flexRowAC">
                <button type="button" class="exportBtn newBtn flexRowAC" @click="handleCreate">
                  <el-icon class="BtnImg"><Plus /></el-icon>
                  新建
                </button>
                <button-group :button-list="toolbarButtonList" />
              </div>
            </div>
            <div class="searchHeight_out flexRowAC">
              <search-height-box
                keyword="keyword"
                placeholder="搜索"
                :data="searchData"
                @handle="searchResetFn"
              />
              <export-excel-pdf :item="exportItem" @handle="handleExport" />
            </div>
          </div>

          <TableSelf
            class="new_table"
            header-cell-class-name="header_tenant_cell"
            stripe
            v-loading="loading"
            :data="tableData"
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" :width="clacPXToVW(55)" />
            <el-table-column label="序号" :width="clacPXToVW(65)">
              <template #default="scope">
                {{ scope.$index + (pagination.current - 1) * pagination.size + 1 }}
              </template>
            </el-table-column>
            <el-table-column prop="postCode" label="岗位编码" min-width="120" />
            <el-table-column prop="postName" label="岗位名称" min-width="150" />
            <el-table-column prop="category" label="岗位分类" min-width="100" align="center">
              <template #default="scope">
                <span>{{ scope.row.category === 1 ? '高管' : scope.row.category === 2 ? '经理' : '员工' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="sort" label="排序" :width="clacPXToVW(80)" align="center" />
            <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
            <el-table-column label="操作" :width="clacPXToVW(180)" fixed="right" align="right">
              <template #default="scope">
                <div class="operateAppBox flexRowAC" @click.stop>
                  <div class="new_table_svg_group" @click="handleEdit(scope.row)">
                    <oort-svg-icon width="20" height="20" name="edit_icon" class="new_table_svg_group_svg" />
                    <span>编辑</span>
                  </div>
                  <div class="new_table_svg_group" @click="handleSingleRemove(scope.row)">
                    <oort-svg-icon color="red" width="20" height="20" name="delete_icon" class="new_table_svg_group_svg" />
                    <span>删除</span>
                  </div>
                </div>
              </template>
            </el-table-column>
          </TableSelf>

          <div class="paginationBox flexRowAC">
            <el-pagination
              background
              :current-page="pagination.current"
              :page-size="pagination.size"
              :page-sizes="[10, 20, 50, 100]"
              :total="pagination.total"
              layout="total, prev, pager, next, sizes"
              class="justifyAlign"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 新增/编辑岗位对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="30%"
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
        <el-button @click="dialogVisible = false" class="common_btn">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSaveSubmit" class="common_btn">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { clacPXToVW } from '@/utils/index'
import { getPostList, submitPost, removePosts } from '@/api/system/post'
import { getPayload, getRecords, getTotal, joinIds, isSuccess } from './utils/response'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增岗位')
const selectedRows = ref([])
const tableData = ref([])
const exportItem = ref({ isDisabledExcel: false })
const searchData = ref([
  { label: '岗位编码', value: 'postCode', type: 'text', default: '' },
  { label: '岗位名称', value: 'postName', type: 'text', default: '' }
])

const toolbarButtonList = computed(() => [
  { name: '编辑', svg: 'table_edit', clickFn: handleToolbarEdit },
  { name: '删除', svg: 'table_del', clickFn: handleBatchRemove }
])

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const queryParams = reactive({
  postCode: '',
  postName: ''
})

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

function handleSearch() {
  pagination.current = 1
  loadData()
}

function handleReset() {
  queryParams.postCode = ''
  queryParams.postName = ''
  pagination.current = 1
  loadData()
}

const searchResetFn = (val, reset) => {
  if (reset && !(val && (val.keyword || val.postCode || val.postName))) {
    handleReset()
    return
  }
  queryParams.postCode = val?.postCode || val?.keyword || ''
  queryParams.postName = val?.postName || ''
  handleSearch()
}

const handleExport = () => {
  ElMessage.success('导出数据')
}

function handleToolbarEdit() {
  if (selectedRows.value.length !== 1) {
    ElMessage.warning('请选择一条记录进行编辑')
    return
  }
  handleEdit(selectedRows.value[0])
}

function handleSizeChange(size) {
  pagination.size = size
  pagination.current = 1
  loadData()
}

function handleCurrentChange(current) {
  pagination.current = current
  loadData()
}

function handleSelectionChange(rows) {
  selectedRows.value = rows
}

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

function handleEdit(row) {
  if (!row) return
  form.value = { ...row }
  dialogTitle.value = '编辑岗位'
  dialogVisible.value = true
}

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

function handleSingleRemove(row) {
  executeRemove(String(row.id), `确定删除岗位 [${row.postName}] 吗？`)
}

function handleBatchRemove() {
  if (selectedRows.value.length === 0) return
  const ids = joinIds(selectedRows.value)
  executeRemove(ids, `确定删除选中的 ${selectedRows.value.length} 个岗位吗？`)
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.tenant_Page {
  height: 100%;
  width: 100%;
  border-radius: var(--common-border-radius) var(--common-border-radius) 0 0;
  background: #f0f2f5;
  display: flex;
  flex-direction: column;

  .tenant_content {
    width: 100%;
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;
    border-radius: 8px;
  }

  .tableTenBox {
    padding: 20px;
    width: 100%;
    height: 100%;
    flex: 1;
    background: #fff;
    align-items: flex-start;
    min-height: 0;
    border-radius: var(--common-border-radius) var(--common-border-radius) 0 0;
  }
}

.tableTenItU {
  flex: 1;
  height: 100%;
  overflow: auto;
  min-width: 0;

  :deep(.header_tenant_cell) {
    background: #F8F8F9;
  }
}

.paginationBox {
  justify-content: center;
  height: 100px;
}

.operateAppBox {
  justify-content: flex-end;
  gap: 2px;
}

.post-management {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f0f2f5;
  overflow: hidden;
}

:deep(.el-dialog) {
  border-radius: 8px;
}
</style>
