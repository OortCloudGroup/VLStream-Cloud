<template>
  <div class="scene-governance">
    <!-- 列表视图 -->
    <div v-if="!showEditView" class="list-view">

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
          
          <div class="toolbar-right">
            <AdvancedSearch 
              @search="handleAdvancedSearch"
              @reset="handleAdvancedSearchReset"
              @export="handleExport"
              @upload="handleUpload"
              @template="handleDownloadTemplate"
              @batch="handleBatchOperation"
            />
          </div>
        </div>

        <!-- 表格内容 -->
        <div class="table-content">
          <el-table 
            :data="currentPageData" 
            stripe
            v-loading="tableLoading"
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" width="55" />
            <el-table-column type="index" label="序号" width="80" align="center" />
            <el-table-column prop="name" label="场景名称" min-width="150" />
            <el-table-column prop="description" label="场景描述" min-width="200" />
<!--            <el-table-column prop="algorithmName" label="关联算法" min-width="120" />-->
            <el-table-column prop="camerasName" label="关联设备" min-width="120" />
<!--            <el-table-column prop="rules" label="治理规则" min-width="120" />-->
            <el-table-column prop="status" label="状态" min-width="100" align="center">
              <template #default="scope">
                <el-tag 
                  :type="scope.row.status === 1 ? 'success' : 'danger'"
                  size="small"
                >
                  {{ scope.row.status === 1 ? '启用' : '禁用' }}
                </el-tag>
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
          
          <!-- 分页 - 紧贴表格数据 -->
          <div class="table-pagination">
                      <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
          </div>
        </div>
      </div>
    </div>

    <!-- 编辑视图 -->
    <div v-if="showEditView" class="edit-view">
      <!-- 面包屑导航 -->
      <div class="breadcrumb-nav">
        <span class="breadcrumb-item" @click="showListView">场景列表</span>
        <span class="breadcrumb-separator">></span>
        <span class="breadcrumb-item active">{{ editTitle }}</span>
      </div>

      <!-- 编辑内容 -->
      <div class="edit-content">
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="80px"
          class="scene-form">
          <el-form-item label="场景名称" prop="name">
            <el-input 
              v-model="form.name" 
              placeholder="请输入场景名称"
              style="width: 480px"
            />
          </el-form-item>
          
          <el-form-item label="Cron表达式" prop="cronExpression" label-width="120">
            <CronExpressionBuilder v-model="form.cronExpression" />
          </el-form-item>
          
<!--          <el-form-item label="AI算法" prop="algorithm">-->
<!--            <div class="selector-item" @click="showAlgorithmSelector = true" style="width: 480px">-->
<!--              <span class="selector-text">{{ form.algorithm || '请选择' }}</span>-->
<!--              <el-icon class="selector-arrow"><ArrowRight /></el-icon>-->
<!--            </div>-->
<!--          </el-form-item>-->
          
          <el-form-item label="区划地点" prop="location">
            <div class="selector-item" @click="showLocationSelector = true" style="width: 480px">
              <span class="selector-text">{{ form.location || '请选择' }}</span>
              <el-icon class="selector-arrow"><ArrowRight /></el-icon>
            </div>
          </el-form-item>
          
          <el-form-item label="摄像头" prop="cameras">
            <div class="selector-item" @click="showCameraSelector = true" style="width: 480px">
              <span class="selector-text">{{ form.cameras || '请选择' }}</span>
              <el-icon class="selector-arrow"><ArrowRight /></el-icon>
            </div>
          </el-form-item>

          <!-- 操作按钮 -->
          <div class="form-actions">
            <el-button @click="showListView">取消</el-button>
            <el-button type="primary" @click="handleSubmit">保存</el-button>
          </div>
        </el-form>
      </div>
    </div>

    <!-- 新增场景弹窗 -->
    <el-dialog
      v-model="showAddDialog"
      title="新增场景"
      width="800px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="true"
      class="scene-dialog"
    >
      <el-form :model="addForm" label-width="100px" class="scene-dialog-form">
        <!-- 场景名称 -->
        <el-form-item label="场景名称" required>
          <el-input
            v-model="addForm.name"
            placeholder="告警场景"
            clearable
            style="width: 100%"
          />
        </el-form-item>
        
        <!-- Cron表达式 -->
        <el-form-item label="Cron表达式" required>
          <CronExpressionBuilder v-model="addForm.cronExpression" />
        </el-form-item>
        
        <!-- AI算法 -->
<!--        <el-form-item label="AI算法">-->
<!--          <div class="selector-item" @click="showAlgorithmDialog = true">-->
<!--            <span class="selector-text">{{ addForm.algorithm || '请选择' }}</span>-->
<!--            <el-icon class="selector-arrow"><ArrowRight /></el-icon>-->
<!--          </div>-->
<!--        </el-form-item>-->
        
        <!-- 区划地点 -->
        <el-form-item label="区划地点">
          <div class="selector-item" @click="showLocationDialog = true">
            <span class="selector-text">{{ addForm.location || '请选择' }}</span>
            <el-icon class="selector-arrow"><ArrowRight /></el-icon>
          </div>
        </el-form-item>
        
        <!-- 摄像头 -->
        <el-form-item label="摄像头">
          <div class="selector-item" @click="showCameraDialog = true">
            <span class="selector-text">{{ addForm.cameras || '请选择' }}</span>
            <el-icon class="selector-arrow"><ArrowRight /></el-icon>
          </div>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleCancelAdd" size="large">取消</el-button>
          <el-button type="primary" @click="handleConfirmAdd" size="large">确定</el-button>
        </div>
      </template>
    </el-dialog>

<!--    <el-dialog-->
<!--      v-model="showAlgorithmSelector"-->
<!--      title="选择AI算法"-->
<!--      width="520px"-->
<!--      class="selector-dialog"-->
<!--    >-->
<!--      <div class="dialog-content">-->
<!--        <el-input-->
<!--          v-model="algorithmSearch"-->
<!--          placeholder="搜索算法"-->
<!--          clearable-->
<!--          class="dialog-search"-->
<!--        />-->
<!--        <el-scrollbar height="360px">-->
<!--          <el-checkbox-group v-model="tempSelectedAlgorithmIds">-->
<!--            <div class="checkbox-grid">-->
<!--              <el-checkbox-->
<!--                v-for="algorithm in filteredAlgorithmOptions"-->
<!--                :key="algorithm.id"-->
<!--                :label="algorithm.id"-->
<!--                class="option-checkbox"-->
<!--              >-->
<!--                {{ algorithm.name }}-->
<!--              </el-checkbox>-->
<!--            </div>-->
<!--          </el-checkbox-group>-->
<!--        </el-scrollbar>-->
<!--      </div>-->
<!--      <template #footer>-->
<!--        <div class="dialog-footer">-->
<!--          <el-button @click="showAlgorithmSelector = false">取消</el-button>-->
<!--          <el-button type="primary" @click="confirmAlgorithmSelector">确定</el-button>-->
<!--        </div>-->
<!--      </template>-->
<!--    </el-dialog>-->

    <el-dialog
      v-model="showCameraSelector"
      title="选择摄像头"
      width="520px"
      class="selector-dialog"
    >
      <div class="dialog-content">
        <el-input
          v-model="cameraSearch"
          placeholder="搜索摄像头"
          clearable
          class="dialog-search"
        />
        <el-scrollbar height="360px">
          <el-checkbox-group v-model="tempSelectedCameraIds">
            <div class="checkbox-grid">
              <el-checkbox
                v-for="camera in filteredCameraOptions"
                :key="camera.id"
                :label="camera.id"
                class="option-checkbox"
              >
                {{ camera.name }}
              </el-checkbox>
            </div>
          </el-checkbox-group>
        </el-scrollbar>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showCameraSelector = false">取消</el-button>
          <el-button type="primary" @click="confirmCameraSelector">确定</el-button>
        </div>
      </template>
    </el-dialog>

<!--    <el-dialog-->
<!--      v-model="showAlgorithmDialog"-->
<!--      title="选择AI算法"-->
<!--      width="520px"-->
<!--      class="selector-dialog"-->
<!--    >-->
<!--      <div class="dialog-content">-->
<!--        <el-input-->
<!--          v-model="algorithmSearch"-->
<!--          placeholder="搜索算法"-->
<!--          clearable-->
<!--          class="dialog-search"-->
<!--        />-->
<!--        <el-scrollbar height="360px">-->
<!--          <el-checkbox-group v-model="tempSelectedAlgorithmDialogIds">-->
<!--            <div class="checkbox-grid">-->
<!--              <el-checkbox-->
<!--                v-for="algorithm in filteredAlgorithmOptions"-->
<!--                :key="algorithm.id"-->
<!--                :label="algorithm.id"-->
<!--                class="option-checkbox"-->
<!--              >-->
<!--                {{ algorithm.name }}-->
<!--              </el-checkbox>-->
<!--            </div>-->
<!--          </el-checkbox-group>-->
<!--        </el-scrollbar>-->
<!--      </div>-->
<!--      <template #footer>-->
<!--        <div class="dialog-footer">-->
<!--          <el-button @click="showAlgorithmDialog = false">取消</el-button>-->
<!--          <el-button type="primary" @click="confirmAlgorithmDialog">确定</el-button>-->
<!--        </div>-->
<!--      </template>-->
<!--    </el-dialog>-->

    <el-dialog
      v-model="showCameraDialog"
      title="选择摄像头"
      width="520px"
      class="selector-dialog"
    >
      <div class="dialog-content">
        <el-input
          v-model="cameraSearch"
          placeholder="搜索摄像头"
          clearable
          class="dialog-search"
        />
        <el-scrollbar height="360px">
          <el-checkbox-group v-model="tempSelectedCameraDialogIds">
            <div class="checkbox-grid">
              <el-checkbox
                v-for="camera in filteredCameraOptions"
                :key="camera.id"
                :label="camera.id"
                class="option-checkbox"
              >
                {{ camera.name }}
              </el-checkbox>
            </div>
          </el-checkbox-group>
        </el-scrollbar>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showCameraDialog = false">取消</el-button>
          <el-button type="primary" @click="confirmCameraDialog">确定</el-button>
        </div>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowRight } from '@element-plus/icons-vue'
import ActionButtonGroup from '@/components/ActionButtonGroup.vue'
import AdvancedSearch from '@/components/AdvancedSearch.vue'
import CronExpressionBuilder from '@/components/CronExpressionBuilder.vue'
import {getList, add, update, remove} from '@/api/sceneGovernance'
import { getAlgorithmPage } from '@/api/algorithmManagement'
import { getDeviceList } from '@/api/device'

const router = useRouter()

// 搜索表单
const queryForm = reactive({
  name: '',
  dateRange: null
})

// 分页相关
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 选中的行数据
const selectedRows = ref([])

// 加载状态
const loading = ref(false)
const tableLoading = ref(false)

// 视图控制
const showEditView = ref(false)
const editingScene = ref(null)
const editTitle = ref('新增场景')

const DEFAULT_CRON_EXPRESSION = '0 0 0 * * ? *'

// 新增弹窗控制
const showAddDialog = ref(false)
const addForm = ref({
  name: '',
  cronExpression: DEFAULT_CRON_EXPRESSION,
  algorithm: '',
  algorithmIds: [],
  location: '',
  cameras: '',
  cameraIds: []
})

// 场景表单
const form = ref({
  name: '',
  cronExpression: DEFAULT_CRON_EXPRESSION,
  algorithm: '',
  algorithmIds: [],
  location: '',
  cameras: '',
  cameraIds: []
})

// 选择器显示状态
const showAlgorithmSelector = ref(false)
const showLocationSelector = ref(false)
const showCameraSelector = ref(false)

// 新增弹窗的选择器状态
const showAlgorithmDialog = ref(false)
const showLocationDialog = ref(false)
const showCameraDialog = ref(false)

const algorithmOptions = ref([])
const algorithmLoading = ref(false)
const algorithmSearch = ref('')
const cameraOptions = ref([])
const cameraLoading = ref(false)
const cameraSearch = ref('')

const tempSelectedAlgorithmIds = ref([])
const tempSelectedAlgorithmDialogIds = ref([])
const tempSelectedCameraIds = ref([])
const tempSelectedCameraDialogIds = ref([])

// 表单验证规则
const rules = {
  name: [
    { required: true, message: '请输入场景名称', trigger: 'blur' }
  ]
}

// 表格数据
const tableData = ref([])

// 直接使用API返回的数据（因为分页和筛选在后端完成）
const currentPageData = computed(() => {
  return tableData.value
})

const splitTokens = (value) => {
  if (!value) return []
  if (Array.isArray(value)) {
    return value.map(item => String(item).trim()).filter(Boolean)
  }
  return String(value)
    .split(/[,，、]/)
    .map(token => token.trim())
    .filter(Boolean)
}

const normalizeIdArray = (value) => {
  if (!value) return []
  if (Array.isArray(value)) {
    return value.map(item => String(item).trim()).filter(Boolean)
  }
  return splitTokens(value)
}

const getCronExpressionValue = (row) => {
  if (!row) return ''
  return row.cronExpression || row.cron || row.cronExpr || ''
}

const resolveCronExpression = (row) => {
  return getCronExpressionValue(row) || DEFAULT_CRON_EXPRESSION
}

const isValidCronExpression = (value) => {
  const text = String(value || '').trim()
  if (!text) return false
  const length = text.split(/\s+/).length
  return length === 6 || length === 7
}

const formatCronDescription = (cronExpression) => {
  const text = String(cronExpression || '').trim()
  return text ? `Cron表达式: ${text}` : '-'
}

const normalizeAlgorithmOption = (algorithm) => {
  const id = algorithm?.id ?? algorithm?.algorithmId ?? algorithm?.value ?? algorithm?.key
  const name = algorithm?.name ?? algorithm?.algorithmName ?? algorithm?.label ?? algorithm?.title ?? ''
  if (id === undefined || id === null || name === '') return null
  return { id: String(id), name: String(name).trim() }
}

const normalizeCameraOption = (device) => {
  const id = device?.id ?? device?.deviceId ?? device?.deviceCode ?? device?.name ?? device?.deviceName
  const name = device?.deviceName ?? device?.name ?? device?.deviceCode ?? device?.deviceId ?? ''
  if (!id || !name) return null
  return { id: String(id), name: String(name).trim() }
}

const resolveIdsFromTokens = (options, tokens) => {
  if (!tokens.length || !options.length) return []
  const mapById = new Map(options.map(option => [String(option.id), option]))
  const mapByName = new Map(options.map(option => [String(option.name).trim(), option]))
  const ids = []
  tokens.forEach(token => {
    const normalized = String(token).trim()
    if (!normalized) return
    const match = mapById.get(normalized) || mapByName.get(normalized)
    if (match?.id !== undefined && match?.id !== null) {
      ids.push(String(match.id))
    }
  })
  return Array.from(new Set(ids))
}

const resolveNamesByIds = (options, ids) => {
  if (!ids.length || !options.length) return []
  const map = new Map(options.map(option => [String(option.id), option.name]))
  return ids.map(id => map.get(String(id))).filter(Boolean)
}

const buildSelectionText = (options, ids, fallback) => {
  const names = resolveNamesByIds(options, ids)
  if (names.length > 0) return names.join('、')
  if (Array.isArray(fallback)) {
    return fallback.map(item => String(item).trim()).filter(Boolean).join('、')
  }
  return fallback ? String(fallback).trim() : ''
}

const resolveIdsForSubmit = (idsValue, nameValue, options) => {
  const optionIds = new Set((options || []).map(option => String(option.id)))
  const rawIds = normalizeIdArray(idsValue)
  if (rawIds.length > 0 && optionIds.size > 0) {
    const matched = rawIds.filter(id => optionIds.has(String(id)))
    if (matched.length > 0) {
      return matched
    }
  }
  const tokens = splitTokens(nameValue)
  if (tokens.length > 0 && options && options.length > 0) {
    const resolved = resolveIdsFromTokens(options, tokens)
    if (resolved.length > 0) {
      return resolved
    }
  }
  return rawIds
}

const loadAlgorithmOptions = async () => {
  if (algorithmLoading.value) return
  try {
    algorithmLoading.value = true
    const response = await getAlgorithmPage({ current: 1, size: 500 })
    if (response?.code === 200) {
      const records = response?.data?.records || []
      algorithmOptions.value = records.map(normalizeAlgorithmOption).filter(Boolean)
    } else {
      algorithmOptions.value = []
      ElMessage.error(response?.message || '加载算法列表失败')
    }
  } catch (error) {
    console.error('加载算法列表失败:', error)
    algorithmOptions.value = []
    ElMessage.error('加载算法列表失败')
  } finally {
    algorithmLoading.value = false
  }
}

const ensureAlgorithmOptions = async () => {
  if (algorithmOptions.value.length > 0 || algorithmLoading.value) return
  await loadAlgorithmOptions()
}

const loadCameraOptions = async () => {
  if (cameraLoading.value) return
  try {
    cameraLoading.value = true
    const response = await getDeviceList({ page: 1, size: 500 })
    const records = response?.data?.records || []
    cameraOptions.value = records.map(normalizeCameraOption).filter(Boolean)
  } catch (error) {
    console.error('加载摄像头列表失败:', error)
    cameraOptions.value = []
    ElMessage.error('加载摄像头列表失败')
  } finally {
    cameraLoading.value = false
  }
}

const ensureCameraOptions = async () => {
  if (cameraOptions.value.length > 0 || cameraLoading.value) return
  await loadCameraOptions()
}

const filteredAlgorithmOptions = computed(() => {
  const keyword = algorithmSearch.value.trim().toLowerCase()
  if (!keyword) return algorithmOptions.value
  return algorithmOptions.value.filter(option =>
    option.name.toLowerCase().includes(keyword)
  )
})

const filteredCameraOptions = computed(() => {
  const keyword = cameraSearch.value.trim().toLowerCase()
  if (!keyword) return cameraOptions.value
  return cameraOptions.value.filter(option =>
    option.name.toLowerCase().includes(keyword)
  )
})

const applyAlgorithmSelection = (target, ids) => {
  const uniqueIds = Array.from(new Set(ids.map(id => String(id))))
  const names = resolveNamesByIds(algorithmOptions.value, uniqueIds)
  const text = names.join('、')
  if (target === 'edit') {
    form.value.algorithmIds = uniqueIds
    form.value.algorithm = text
  } else {
    addForm.value.algorithmIds = uniqueIds
    addForm.value.algorithm = text
  }
}

const applyCameraSelection = (target, ids) => {
  const uniqueIds = Array.from(new Set(ids.map(id => String(id))))
  const names = resolveNamesByIds(cameraOptions.value, uniqueIds)
  const text = names.join('、')
  if (target === 'edit') {
    form.value.cameraIds = uniqueIds
    form.value.cameras = text
  } else {
    addForm.value.cameraIds = uniqueIds
    addForm.value.cameras = text
  }
}

const initAlgorithmTempSelection = (target) => {
  const source = target === 'edit' ? form.value : addForm.value
  const ids = Array.isArray(source.algorithmIds) && source.algorithmIds.length > 0
    ? source.algorithmIds
    : resolveIdsFromTokens(algorithmOptions.value, splitTokens(source.algorithm))
  if (target === 'edit') {
    tempSelectedAlgorithmIds.value = [...ids]
  } else {
    tempSelectedAlgorithmDialogIds.value = [...ids]
  }
}

const initCameraTempSelection = (target) => {
  const source = target === 'edit' ? form.value : addForm.value
  const ids = Array.isArray(source.cameraIds) && source.cameraIds.length > 0
    ? source.cameraIds
    : resolveIdsFromTokens(cameraOptions.value, splitTokens(source.cameras))
  if (target === 'edit') {
    tempSelectedCameraIds.value = [...ids]
  } else {
    tempSelectedCameraDialogIds.value = [...ids]
  }
}

const confirmAlgorithmSelector = () => {
  applyAlgorithmSelection('edit', tempSelectedAlgorithmIds.value)
  showAlgorithmSelector.value = false
}

const confirmAlgorithmDialog = () => {
  applyAlgorithmSelection('add', tempSelectedAlgorithmDialogIds.value)
  showAlgorithmDialog.value = false
}

const confirmCameraSelector = () => {
  applyCameraSelection('edit', tempSelectedCameraIds.value)
  showCameraSelector.value = false
}

const confirmCameraDialog = () => {
  applyCameraSelection('add', tempSelectedCameraDialogIds.value)
  showCameraDialog.value = false
}

const syncEditSelection = async (row) => {
  if (!row) return
  await Promise.all([ensureAlgorithmOptions(), ensureCameraOptions()])
  const algorithmIds = resolveIdsForSubmit(
    row.algorithmIds || row.algorithm,
    row.algorithmName || row.rules || row.algorithm,
    algorithmOptions.value
  )
  const cameraIds = resolveIdsForSubmit(
    row.cameraIds || row.cameras,
    row.camerasName || row.devices || row.cameras,
    cameraOptions.value
  )
  if (algorithmIds.length > 0) {
    form.value.algorithmIds = algorithmIds
    form.value.algorithm = buildSelectionText(
      algorithmOptions.value,
      algorithmIds,
      form.value.algorithm
    )
  }
  if (cameraIds.length > 0) {
    form.value.cameraIds = cameraIds
    form.value.cameras = buildSelectionText(
      cameraOptions.value,
      cameraIds,
      form.value.cameras
    )
  }
}

watch(showAlgorithmSelector, async (visible) => {
  if (!visible) return
  algorithmSearch.value = ''
  await ensureAlgorithmOptions()
  initAlgorithmTempSelection('edit')
})

watch(showAlgorithmDialog, async (visible) => {
  if (!visible) return
  algorithmSearch.value = ''
  await ensureAlgorithmOptions()
  initAlgorithmTempSelection('add')
})

watch(showCameraSelector, async (visible) => {
  if (!visible) return
  cameraSearch.value = ''
  await ensureCameraOptions()
  initCameraTempSelection('edit')
})

watch(showCameraDialog, async (visible) => {
  if (!visible) return
  cameraSearch.value = ''
  await ensureCameraOptions()
  initCameraTempSelection('add')
})


// 加载场景治理列表
const loadSceneGovernanceList = async () => {
  try {
    tableLoading.value = true
    
    const params = {
      name: queryForm.name || undefined,
      startDate: queryForm.dateRange ? queryForm.dateRange[0] : undefined,
      endDate: queryForm.dateRange ? queryForm.dateRange[1] : undefined
    }
    
    const response = await getList(currentPage.value, pageSize.value, params)
    
    if (response.code === 200) {
      tableData.value = response.data.records || []
      total.value = response.data.total || 0
      
      // 为每条记录添加序号
      tableData.value.forEach((item, index) => {
        item.index = (currentPage.value - 1) * pageSize.value + index + 1
        // 格式化时间显示
        if (item.createdAt) {
          item.createTime = new Date(item.createdAt).toLocaleString('zh-CN', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
          }).replace(/\//g, '-')
        }
        
        // 处理selectedDays为JSON字符串的情况
        if (typeof item.selectedDays === 'string') {
          try {
            item.selectedDays = JSON.parse(item.selectedDays)
          } catch (e) {
            item.selectedDays = ['monday']
          }
        }
        
        // 设置默认值
        item.devices = item.devices || '-'
        item.camerasName = item.camerasName || item.devices || '-'
        item.rules = item.rules || '-'
        item.algorithmName = item.algorithmName || item.rules || '-'
        item.description = item.description || generateDescriptionText(item)
      })
    } else {
      ElMessage.error(response.message || '加载数据失败')
    }
  } catch (error) {
    console.error('加载场景治理列表失败:', error)
    ElMessage.error('加载数据失败，请重试')
  } finally {
    tableLoading.value = false
  }
}

// 生成描述文本
const generateDescriptionText = (item) => {
  const cronExpression = getCronExpressionValue(item)
  if (cronExpression) return formatCronDescription(cronExpression)

  if (!item.executeType) return '-'

  const executeTypeMap = {
    'daily': '每天',
    'alternate': '隔天',
    'weekly': '每周',
    'monthly': '每月'
  }

  let text = executeTypeMap[item.executeType] || '每周'

  if (item.executeType === 'weekly' && item.selectedDays && item.selectedDays.length > 0) {
    const dayMap = {
      'sunday': '星期日',
      'monday': '星期一',
      'tuesday': '星期二',
      'wednesday': '星期三',
      'thursday': '星期四',
      'friday': '星期五',
      'saturday': '星期六'
    }

    const dayTexts = item.selectedDays.map(day => dayMap[day] || day)
    text += '：' + dayTexts.join('、')
  }

  return text
}

// 方法
const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

const handleSearch = async () => {
  currentPage.value = 1 // 重置到第一页
  await loadSceneGovernanceList()
  ElMessage.success('搜索完成')
}

const handleReset = async () => {
  queryForm.name = ''
  queryForm.dateRange = null
  currentPage.value = 1 // 重置到第一页
  await loadSceneGovernanceList()
  ElMessage.info('搜索条件已重置')
}

const handleAdd = () => {
  // 重置新增表单
  addForm.value = {
    name: '',
    cronExpression: DEFAULT_CRON_EXPRESSION,
    algorithm: '',
    algorithmIds: [],
    location: '',
    cameras: '',
    cameraIds: []
  }
  showAddDialog.value = true
}

// 确认新增
const handleConfirmAdd = async () => {
  if (!addForm.value.name.trim()) {
    ElMessage.warning('请输入场景名称')
    return
  }

  const cronExpression = String(addForm.value.cronExpression || '').trim()
  if (!cronExpression) {
    ElMessage.warning('请配置Cron表达式')
    return
  }
  if (!isValidCronExpression(cronExpression)) {
    ElMessage.warning('Cron表达式格式不正确')
    return
  }
  
  try {
    loading.value = true
    
    await Promise.all([ensureAlgorithmOptions(), ensureCameraOptions()])
    const algorithmIds = resolveIdsForSubmit(
      addForm.value.algorithmIds,
      addForm.value.algorithm,
      algorithmOptions.value
    )
    const cameraIds = resolveIdsForSubmit(
      addForm.value.cameraIds,
      addForm.value.cameras,
      cameraOptions.value
    )
    const algorithmText = buildSelectionText(
      algorithmOptions.value,
      algorithmIds,
      addForm.value.algorithm
    )
    const cameraText = buildSelectionText(
      cameraOptions.value,
      cameraIds,
      addForm.value.cameras
    )

    const sceneData = {
      name: addForm.value.name,
      description: generateDescriptionFromForm(addForm.value),
      status: 'enabled',
      cronExpression: cronExpression,
      algorithm: formatIdsParam(algorithmIds),
      algorithmIds: algorithmIds,
      algorithmName: algorithmText,
      location: addForm.value.location || '',
      cameras: formatIdsParam(cameraIds),
      cameraIds: cameraIds,
      camerasName: cameraText,
      devices: cameraText || '-',
      rules: algorithmText || '-'
    }
    
    const response = await add(sceneData)
    
    if (response.code === 200) {
      showAddDialog.value = false
      ElMessage.success('新增场景成功')
      // 重新加载列表数据
      await loadSceneGovernanceList()
    } else {
      ElMessage.error(response.message || '新增失败')
    }
  } catch (error) {
    console.error('新增场景失败:', error)
    ElMessage.error('新增失败，请重试')
  } finally {
    loading.value = false
  }
}

// 从表单生成描述文本
const generateDescriptionFromForm = (formData) => {
  return formatCronDescription(formData.cronExpression)
}

const formatIdsParam = (ids) => {
  if (Array.isArray(ids)) {
    return ids.join(',')
  }
  return ids
}

// 取消新增
const handleCancelAdd = () => {
  showAddDialog.value = false
}

const handleEdit = () => {
  if (selectedRows.value.length !== 1) {
    ElMessage.warning('请选择一条记录进行编辑')
    return
  }
  
  const row = selectedRows.value[0]
  const cronExpression = resolveCronExpression(row)
  const algorithmText = row.algorithmName || row.rules || row.algorithm || ''
  const algorithmIds = normalizeIdArray(row.algorithmIds || row.algorithm)
  const cameraText = row.camerasName || row.devices || row.cameras || ''
  const cameraIds = normalizeIdArray(row.cameraIds || row.cameras)
  editingScene.value = row
  form.value = {
    name: row.name,
    cronExpression: cronExpression,
    algorithm: algorithmText,
    algorithmIds: algorithmIds,
    location: row.location || '',
    cameras: cameraText,
    cameraIds: cameraIds
  }
  editTitle.value = '编辑场景'
  void syncEditSelection(row)
  showEditView.value = true
}

const handleDelete = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择要删除的记录')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedRows.value.length} 条记录吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    loading.value = true
    
    // 批量删除
    const idsToDelete = selectedRows.value.map(row => row.id)
    const response = await remove(formatIdsParam(idsToDelete))
    
    if (response.code === 200) {
      selectedRows.value = []
      ElMessage.success('删除成功')
      // 重新加载列表数据
      await loadSceneGovernanceList()
    } else {
      ElMessage.error(response.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除失败:', error)
      ElMessage.error('删除失败，请重试')
    } else {
      ElMessage.info('已取消删除')
    }
  } finally {
    loading.value = false
  }
}

const handleDetailRow = (row) => {
  ElMessage.info(`查看场景详情: ${row.name}`)
  // 实际项目中这里会打开详情页面或弹窗
}

const handleEditRow = (row) => {
  editingScene.value = row
  
  // 处理Cron表达式回显
  const cronExpression = resolveCronExpression(row)
  const algorithmText = row.algorithmName || row.rules || row.algorithm || ''
  const algorithmIds = normalizeIdArray(row.algorithmIds || row.algorithm)
  const cameraText = row.camerasName || row.devices || row.cameras || ''
  const cameraIds = normalizeIdArray(row.cameraIds || row.cameras)
  form.value = {
    name: row.name,
    cronExpression: cronExpression,
    algorithm: algorithmText,
    algorithmIds: algorithmIds,
    location: row.location || '',
    cameras: cameraText,
    cameraIds: cameraIds
  }
  editTitle.value = '编辑场景'
  void syncEditSelection(row)
  showEditView.value = true
}

const handleDeleteRow = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除场景"${row.name}"吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    loading.value = true
    
    const response = await remove(formatIdsParam([row.id]))
    
    if (response.code === 200) {
      ElMessage.success('删除成功')
      // 重新加载列表数据
      await loadSceneGovernanceList()
    } else {
      ElMessage.error(response.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除场景失败:', error)
      ElMessage.error('删除失败，请重试')
    } else {
      ElMessage.info('已取消删除')
    }
  } finally {
    loading.value = false
  }
}

const handleStatusChange = (row) => {
  const status = row.status === 'enabled' ? '启用' : '禁用'
  ElMessage.success(`场景"${row.name}"已${status}`)
}

const resetForm = () => {
  form.value = {
    name: '',
    cronExpression: DEFAULT_CRON_EXPRESSION,
    algorithm: '',
    algorithmIds: [],
    location: '',
    cameras: '',
    cameraIds: []
  }
}

const handleSubmit = async () => {
  if (!form.value.name.trim()) {
    ElMessage.warning('请输入场景名称')
    return
  }

  const cronExpression = String(form.value.cronExpression || '').trim()
  if (!cronExpression) {
    ElMessage.warning('请配置Cron表达式')
    return
  }
  if (!isValidCronExpression(cronExpression)) {
    ElMessage.warning('Cron表达式格式不正确')
    return
  }

  try {
    loading.value = true
    
    await Promise.all([ensureAlgorithmOptions(), ensureCameraOptions()])
    const algorithmIds = resolveIdsForSubmit(
      form.value.algorithmIds,
      form.value.algorithm,
      algorithmOptions.value
    )
    const cameraIds = resolveIdsForSubmit(
      form.value.cameraIds,
      form.value.cameras,
      cameraOptions.value
    )
    const algorithmText = buildSelectionText(
      algorithmOptions.value,
      algorithmIds,
      form.value.algorithm
    )
    const cameraText = buildSelectionText(
      cameraOptions.value,
      cameraIds,
      form.value.cameras
    )

    const sceneData = {
      name: form.value.name,
      description: getExecuteTimeText(form.value),
      cronExpression: cronExpression,
      devices: cameraText || '-',
      rules: algorithmText || '-',
      algorithm: formatIdsParam(algorithmIds),
      algorithmIds: algorithmIds,
      algorithmName: algorithmText,
      location: form.value.location || '',
      cameras: formatIdsParam(cameraIds),
      cameraIds: cameraIds,
      camerasName: cameraText,
    }

    if (editingScene.value) {
      const response = await update({ ...sceneData, id: editingScene.value.id })
      
      if (response.code === 200) {
        ElMessage.success('更新成功')
        showEditView.value = false
        selectedRows.value = []
        // 重新加载列表数据
        await loadSceneGovernanceList()
      } else {
        ElMessage.error(response.message || '更新失败')
      }
    } else {
      const response = await add(sceneData)
      
      if (response.code === 200) {
        ElMessage.success('创建成功')
        showEditView.value = false
        selectedRows.value = []
        // 重新加载列表数据
        await loadSceneGovernanceList()
      } else {
        ElMessage.error(response.message || '创建失败')
      }
    }
  } catch (error) {
    console.error('提交失败:', error)
    ElMessage.error('操作失败，请重试')
  } finally {
    loading.value = false
  }
}

const showListView = () => {
  showEditView.value = false
  resetForm()
}

const handleSizeChange = async (val) => {
  pageSize.value = val
  currentPage.value = 1
  await loadSceneGovernanceList()
}

const handleCurrentChange = async (val) => {
  currentPage.value = val
  await loadSceneGovernanceList()
}

// 获取执行时间文本描述
const getExecuteTimeText = (formData) => {
  return formatCronDescription(formData.cronExpression)
}

// 高级搜索相关方法
const handleAdvancedSearch = (searchData) => {
  console.log('高级搜索:', searchData)
  
  // 更新搜索表单
  if (searchData.keyword) {
    queryForm.name = searchData.keyword
  }
  if (searchData.sceneName) {
    queryForm.name = searchData.sceneName
  }
  if (searchData.dateRange && searchData.dateRange.length > 0) {
    queryForm.dateRange = searchData.dateRange
  }
  
  handleSearch()
}

const handleAdvancedSearchReset = () => {
  console.log('重置高级搜索')
  queryForm.name = ''
  queryForm.dateRange = []
  handleReset()
}

const handleExport = () => {
  console.log('导出数据')
  ElMessage.success('导出数据')
}

const handleUpload = () => {
  console.log('上传文件')
  ElMessage.success('上传功能')
}

const handleDownloadTemplate = () => {
  console.log('下载模板')
  ElMessage.success('下载模板')
}

const handleBatchOperation = () => {
  console.log('批量操作')
  ElMessage.success('批量操作')
}

// 初始化加载数据
onMounted(() => {
  loadSceneGovernanceList()
})
</script>

<style scoped>
.scene-governance {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 0;
  background-color: #f5f7fa;
  padding: 20px;
  overflow: hidden;
}

.list-view {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 0;
}

/* 查询栏 */
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
  min-width: 300px;
}

.search-buttons {
  display: flex;
  gap: 12px;
  flex-shrink: 0;
}

/* 主内容区域 */
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

/* 工具栏样式 */
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toolbar-left {
  display: flex;
  align-items: center;
}

.toolbar-right {
  display: flex;
  align-items: center;
}

/* 表格内容区域 */
.table-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 表格操作按钮样式 */
.table-action-buttons {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

/* 主题色更新 */
:deep(.el-button--primary) {
  background-color: #1A53FF;
  border-color: #1A53FF;
}

:deep(.el-button--primary:hover) {
  background-color: #3d70ff;
  border-color: #3d70ff;
}

/* 表格操作按钮 - 纯文字样式 */
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

.table-action-buttons :deep(.el-button--primary.is-text:focus) {
  color: #1A53FF !important;
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

.table-action-buttons :deep(.el-button--danger.is-text:focus) {
  color: #f56c6c !important;
  background: transparent !important;
  border: none !important;
}

/* 分页样式 */
.table-pagination {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

/* 编辑视图样式 */
.edit-view {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 0;
  background-color: #f5f7fa;
  padding: 20px;
  overflow: hidden;
}

.breadcrumb-nav {
  background: #F0F2F5;
  border-radius: 8px 8px 0 0;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.breadcrumb-item {
  color: #606266;
  cursor: pointer;
  font-size: 14px;
}

.breadcrumb-item:hover {
  color: #1A53FF;
}

.breadcrumb-item.active {
  color: #303133;
  font-weight: 500;
}

.breadcrumb-separator {
  color: #c0c4cc;
  margin: 0 8px;
}

.edit-content {
  flex: 1;
  background: white;
  border-radius: 0 0 8px 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 20px;
  overflow-y: auto;
}

.scene-form {
  max-width: 600px;
}

.execute-time-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.time-btn {
  min-width: 60px;
  height: 32px;
  border-radius: 16px;
  border: 1px solid #d9d9d9;
  background: white;
  color: #606266;
}

.time-btn.el-button--primary {
  background: #1A53FF;
  border-color: #1A53FF;
  color: white;
}

.time-config {
  margin-top: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  padding: 16px;
}

.time-section {
  margin-bottom: 16px;
}

.time-section:last-child {
  margin-bottom: 0;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.required {
  color: #606266;
  font-size: 14px;
}

.required::before {
  content: '*';
  color: #f56c6c;
  margin-right: 4px;
}

.select-date-btn {
  color: #1A53FF;
  padding: 0;
  height: auto;
}

.week-selection {
  margin-bottom: 16px;
}

.week-label {
  color: #606266;
  font-size: 14px;
  margin-right: 12px;
}

.week-checkboxes {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  margin-top: 8px;
}

.week-checkbox {
  margin-right: 0;
}

.interval-setting {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #606266;
  font-size: 14px;
}

.interval-input {
  width: 80px;
}

.time-hint {
  background: #fff3cd;
  border: 1px solid #ffeaa7;
  border-radius: 4px;
  padding: 8px 12px;
  font-size: 12px;
  color: #856404;
  line-height: 1.4;
}

.selector-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 32px;
  padding: 0 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: white;
  cursor: pointer;
  transition: border-color 0.2s;
}

.selector-item:hover {
  border-color: #1A53FF;
}

.selector-text {
  color: #606266;
  font-size: 14px;
}

.selector-arrow {
  color: #c0c4cc;
  font-size: 12px;
}

.form-actions {
  margin-top: 32px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  gap: 12px;
}

/* 弹窗样式 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.time-input {
  width: 200px;
}

.selector-dialog .dialog-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.selector-dialog .dialog-search {
  width: 100%;
}

.selector-dialog .checkbox-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.selector-dialog .option-checkbox {
  width: 100%;
}

/* 弹窗表单样式 */
:deep(.el-dialog__body) {
  padding: 20px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
  color: #303133;
}

:deep(.el-form-item__content) {
  flex: 1;
}

/* 弹窗按钮样式 */
.dialog-footer .el-button {
  min-width: 80px;
  height: 32px;
}

.dialog-footer .el-button--primary {
  background-color: #1A53FF;
  border-color: #1A53FF;
}

.dialog-footer .el-button--primary:hover {
  background-color: #3d70ff;
  border-color: #3d70ff;
}

/* 新增场景弹窗样式 */
.scene-dialog :deep(.el-dialog__header) {
  padding: 20px 20px 0 20px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 0;
}

.scene-dialog :deep(.el-dialog__title) {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.scene-dialog :deep(.el-dialog__body) {
  padding: 20px;
  max-height: 80vh;
  overflow-y: auto;
}

.scene-dialog :deep(.el-dialog__footer) {
  padding: 0 20px 20px 20px;
  border-top: 1px solid #f0f0f0;
  margin-top: 0;
}

.scene-dialog-form {
  .el-form-item {
    margin-bottom: 20px;
  }
  
  .el-form-item__label {
    font-weight: 500;
    color: #303133;
  }
}

/* 执行时间选择器样式 */
.execute-time-selector {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.time-option-btn {
  min-width: 60px;
  height: 36px;
  border-radius: 18px;
  border: 1px solid #d9d9d9;
  background: white;
  color: #606266;
  font-size: 14px;
}

.time-option-btn.el-button--primary {
  background: #1A53FF;
  border-color: #1A53FF;
  color: white;
}

/* 时间选择器组样式 */
.time-item {
  .el-form-item__content {
    flex-direction: column;
    align-items: flex-start !important;
  }
}

.time-selector-group {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  margin-bottom: 12px;
}

.required-star {
  color: #f56c6c;
  font-size: 14px;
  font-weight: 500;
}

.select-time-btn {
  color: #1A53FF;
  font-size: 14px;
  padding: 0;
  height: auto;
}

.select-time-btn .el-icon {
  margin-right: 4px;
}

/* 星期选择器样式 */
.week-selector {
  width: 100%;
  margin-top: 8px;
}

.week-trigger-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}

.week-label {
  font-size: 14px;
  color: #606266;
  min-width: 60px;
  line-height: 32px;
}

.week-checkboxes {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  flex: 1;
}

.week-checkbox {
  margin-right: 0 !important;
}

.week-checkbox :deep(.el-checkbox__label) {
  font-size: 14px;
  color: #606266;
}

.week-checkbox :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: #1A53FF;
  border-color: #1A53FF;
}

/* 间隔设置样式 */
.interval-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.interval-label {
  font-size: 14px;
  color: #606266;
}

.interval-input {
  width: 80px;
}

.interval-input :deep(.el-input-number__decrease),
.interval-input :deep(.el-input-number__increase) {
  width: 24px;
  height: 24px;
}

.interval-unit {
  font-size: 14px;
  color: #606266;
}

/* 时间提示样式 */
.time-hint {
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
  margin-top: 8px;
}

/* 选择器样式 */
.selector-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
  background: white;
  transition: all 0.2s;
  width: 100%;
}

.selector-item:hover {
  border-color: #1A53FF;
}

.selector-text {
  font-size: 14px;
  color: #606266;
  flex: 1;
}

.selector-arrow {
  color: #c0c4cc;
  font-size: 14px;
  transition: transform 0.2s;
}

.selector-item:hover .selector-arrow {
  color: #1A53FF;
}

/* 弹窗底部按钮样式覆盖 */
.scene-dialog .dialog-footer .el-button {
  min-width: 100px;
  height: 40px;
  border-radius: 4px;
  font-size: 14px;
}
</style> 
