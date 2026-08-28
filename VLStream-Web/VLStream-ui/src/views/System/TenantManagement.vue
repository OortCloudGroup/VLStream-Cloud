<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
  Created by: ChaoQun Lei
  Updated by: ChaoQun Lei
-->

<template>
  <SystemPageShell>
    <!--  -->
    <template #toolbar>
      <div class="toolbar-left">
        <el-form :inline="true" :model="queryParams" size="default">
          <el-form-item label="租户ID">
            <el-input v-model="queryParams.tenantId" placeholder="请输入租户ID" clearable @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="租户名称">
            <el-input v-model="queryParams.tenantName" placeholder="请输入租户名称" clearable @keyup.enter="handleSearch" />
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
        <!-- buttonoperation -->
        <ActionButtonGroup
          :selected-count="selectedRows.length"
          @add="handleCreate"
          @edit="handleEdit(selectedRows[0])"
          @delete="handleBatchRemove"
        />
      </div>
    </template>

    <!-- data table -->
    <el-table
      v-loading="loading"
      :data="tableData"
      stripe
      style="width: 100%; height: 100%"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" />
      <el-table-column prop="tenantId" label="租户ID" min-width="120" />
      <el-table-column prop="tenantName" label="租户名称" min-width="150" />
      <el-table-column prop="domain" label="域名地址" min-width="150" show-overflow-tooltip />
      <el-table-column prop="linkman" label="联系人" min-width="120" />
      <el-table-column prop="contactNumber" label="联系电话" min-width="120" />
      <el-table-column prop="address" label="联系地址" min-width="200" show-overflow-tooltip />
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

    <!--  -->
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

  <!-- Add / -->
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
      <el-form-item label="租户ID" prop="tenantId">
        <el-input v-model="form.tenantId" placeholder="请输入六位租户ID" :disabled="form.id !== undefined" />
      </el-form-item>

      <el-form-item label="租户名称" prop="tenantName">
        <el-input v-model="form.tenantName" placeholder="请输入租户名称" />
      </el-form-item>

      <el-form-item label="绑定域名" prop="domain">
        <el-input v-model="form.domain" placeholder="请输入域名地址" />
      </el-form-item>

      <el-form-item label="联系人" prop="linkman">
        <el-input v-model="form.linkman" placeholder="请输入联系人姓名" />
      </el-form-item>

      <el-form-item label="联系电话" prop="contactNumber">
        <el-input v-model="form.contactNumber" placeholder="请输入联系电话" />
      </el-form-item>

      <el-form-item label="联系地址" prop="address">
        <el-input v-model="form.address" type="textarea" placeholder="请输入联系地址" :rows="2" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false" class="common_btn">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSaveSubmit" class="common_btn">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import SystemPageShell from './components/SystemPageShell.vue'
import ActionButtonGroup from '@/components/ActionButtonGroup.vue'
import { getTenantList, submitTenant, removeTenants } from '@/api/system/tenant'
import { getPayload, getRecords, getTotal, joinIds, isSuccess } from './utils/response'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增租户')
const selectedRows = ref([])
const tableData = ref([])

// data
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

// parameter
const queryParams = reactive({
  tenantId: '',
  tenantName: ''
})

// formobject and formValidate
const formRef = ref()
const form = ref({
  id: undefined,
  tenantId: '',
  tenantName: '',
  domain: '',
  linkman: '',
  contactNumber: '',
  address: ''
})

const rules = {
  tenantId: [
    { required: true, message: '请输入租户ID', trigger: 'blur' },
    { min: 3, max: 8, message: '长度在 3 到 8 个字符之间', trigger: 'blur' }
  ],
  tenantName: [{ required: true, message: '请输入租户名称', trigger: 'blur' }]
}

/**
 * Query listdata
 */
async function loadData() {
  loading.value = true
  try {
    const res = await getTenantList({
      current: pagination.current,
      size: pagination.size,
      tenantId: queryParams.tenantId,
      tenantName: queryParams.tenantName
    })
    tableData.value = getRecords(res)
    pagination.total = getTotal(res)
  } catch (error) {
    console.error('加载租户列表数据失败:', error)
    ElMessage.error('加载租户列表失败')
  } finally {
    loading.value = false
  }
}

/**
 *
 */
function handleSearch() {
  pagination.current = 1
  loadData()
}

/**
 * , new Load
 */
function handleReset() {
  queryParams.tenantId = ''
  queryParams.tenantName = ''
  pagination.current = 1
  loadData()
}

/**
 * new data
 * @param {Number} size
 */
function handleSizeChange(size) {
  pagination.size = size
  pagination.current = 1
  loadData()
}

/**
 * new data
 * @param {Number} current
 */
function handleCurrentChange(current) {
  pagination.current = current
  loadData()
}

/**
 * table in
 * @param {Array} rows in all object
 */
function handleSelectionChange(rows) {
  selectedRows.value = rows
}

/**
 * Add dialog
 */
function handleCreate() {
  dialogTitle.value = '新增租户'
  form.value = {
    id: undefined,
    tenantId: '',
    tenantName: '',
    domain: '',
    linkman: '',
    contactNumber: '',
    address: ''
  }
  dialogVisible.value = true
}

/**
 * Update , data
 * @param {Object} row Update
 */
function handleEdit(row) {
  if (!row) return
  form.value = { ...row }
  dialogTitle.value = '编辑租户'
  dialogVisible.value = true
}

/**
 * Add Update data
 */
async function handleSaveSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      saving.value = true
      try {
        const res = await submitTenant(form.value)
        if (isSuccess(res)) {
          ElMessage.success('保存成功')
          dialogVisible.value = false
          loadData()
        } else {
          ElMessage.error(res?.msg || '保存失败')
        }
      } catch (error) {
        console.error('提交租户信息失败:', error)
        ElMessage.error('保存租户数据发生异常')
      } finally {
        saving.value = false
      }
    }
  })
}

/**
 * after API Delete configuration, prompt / tip layer
 * @param {String} ids tenant ID
 * @param {String} msg prompt / tip
 */
function executeRemove(ids, msg) {
  ElMessageBox.confirm(msg, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await removeTenants(ids)
      if (isSuccess(res)) {
        ElMessage.success('删除成功')
        loadData()
      } else {
        ElMessage.error(res?.msg || '删除失败')
      }
    } catch (error) {
      console.error('删除租户失败:', error)
      ElMessage.error('删除租户操作发生异常')
    }
  }).catch(() => {})
}

/**
 * Delete data
 * @param {Object} row data
 */
function handleSingleRemove(row) {
  executeRemove(String(row.id), `确定删除租户 [${row.tenantName}] 吗？`)
}

/**
 * Batch delete in all
 */
function handleBatchRemove() {
  if (selectedRows.value.length === 0) return
  const ids = joinIds(selectedRows.value)
  executeRemove(ids, `确定删除选中的 ${selectedRows.value.length} 个租户吗？`)
}

// Load data
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
