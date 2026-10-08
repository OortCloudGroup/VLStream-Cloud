<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
  Created by: ChaoQun Lei
  Updated by: ChaoQun Lei
-->

<template>
  <div class="algorithm-model tenant_Page draHeaPB">
    <div class="tenant_content">
    <!--  -->
    <div v-if="!showDetailView" class="tableTenBox flexRowAC">
      <div class="tableTenItU">
        <div class="depNameBox_out flexRowAC">
          <div class="depNameBox flexRowAC">
            <div class="exportBtnBox flexRowAC">
                <button type="button" class="exportBtn newBtn flexRowAC" @click="handleImportOpen">
                  <el-icon class="BtnImg">
                    <Plus />
                  </el-icon>
                  {{ $tp('导入模型') }}
                </button>
                <button-group :button-list="toolbarButtonList" />
              </div>
          </div>
          <div class="searchHeight_out flexRowAC">
            <search-height-box
              keyword="keyword"
              :placeholder="$tp('搜索')"
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
          :data="currentPageData"
          @selection-change="handleSelectionChange"
          @row-click="handleRowClick"
        >
          <el-table-column type="selection" :width="clacPXToVW(55)" align="center" />
          <el-table-column prop="name" :label="$tp('模型名称')" show-overflow-tooltip />
          <el-table-column prop="source" :label="$tp('模型来源')" align="center" />
          <el-table-column prop="annotationTypeLabel" :label="$tp('模型类型')" align="center" />
          <el-table-column prop="version" :label="$tp('版本')" align="center" />
          <el-table-column prop="downloadCount" :label="$tp('下载次数')" align="center" />
          <el-table-column prop="createTime" :label="$tp('创建时间')" />
          <el-table-column :label="$tp('操作')" :min-width="clacPXToVW(260)" :width="clacPXToVW(260)" align="right" fixed="right">
            <template #default="scope">
              <div class="operateAppBox flexRowAC" @click.stop>
                <div class="new_table_svg_group" @click="handleView(scope.row)">
                  <oort-svg-icon width="14" height="14" name="detail_icon" class="new_table_svg_group_svg" />
                  <span>{{ $tp('查看') }}</span>
                </div>
                <div class="new_table_svg_group" @click="handleDownloadModel(scope.row)">
                  <oort-svg-icon width="14" height="14" name="export" class="new_table_svg_group_svg" />
                  <span>{{ $tp('下载') }}</span>
                </div>
                <div class="new_table_svg_group" @click="handleDeleteItem(scope.row)">
                  <oort-svg-icon color="red" width="14" height="14" name="delete_icon" class="new_table_svg_group_svg" />
                  <span>{{ $tp('删除') }}</span>
                </div>
              </div>
            </template>
          </el-table-column>
        </TableSelf>

        <div class="paginationBox flexRowAC">
          <el-pagination
            background
            :current-page="currentPage"
            :page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, prev, pager, next, sizes"
            class="justifyAlign"
            :total="total"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>
    </div>

    <!--  -->
    <div v-if="showDetailView" class="detail-view">
      <!--  -->
      <div class="breadcrumb-section">
        <div class="breadcrumb">
          <span class="breadcrumb-item" @click="handleBackToList">{{ $tp('算法模型') }}</span>
          <span class="breadcrumb-separator">></span>
          <span class="breadcrumb-item active">{{ $tp('详情') }}</span>
        </div>
      </div>

      <!-- info -->
      <div class="detail-info-section">
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">{{ $tp('模型名称：') }}</span>
            <span class="info-value">{{ currentModel?.name || $tp('螺丝螺母识别') }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">{{ $tp('模型ID：') }}</span>
            <span class="info-value">{{ currentModel?.id || '1' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">{{ $tp('模型类型：') }}</span>
            <span class="info-value">{{ currentModel?.type || $tp('物体检测') }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">{{ $tp('模型来源：') }}</span>
            <span class="info-value">{{ currentModel?.source || $tp('零代码训练') }}</span>
          </div>
        </div>
      </div>

      <!--  -->
      <div class="version-section">
        <div class="version-table">
          <el-table :data="versionData" stripe style="width: 100%">
            <el-table-column prop="version" :label="$tp('版本')" align="center" />
            <el-table-column prop="taskName" :label="$tp('对应训练任务名')" min-width="200" />
            <el-table-column prop="trainMethod" :label="$tp('训练方式')" min-width="120" align="center" />
            <el-table-column prop="description" :label="$tp('描述')" min-width="100" align="center" />
            <el-table-column prop="createTime" :label="$tp('创建时间')" />
            <el-table-column :label="$tp('操作')" width="150" align="right">
              <template #default="scope">
                <el-button
                  type="primary"
                  link
                  size="small"
                  @click="handleExportModel(scope.row)"
                > {{ $tp('下载') }} {{ scope.row.modelName || $tp('模型文件') }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>

    <el-dialog v-model="showImportDialog" :title="$tp('导入算法模型')" width="min(520px, 94vw)"
               :close-on-click-modal="false" :close-on-press-escape="!importing" :show-close="!importing">
      <el-form label-width="110px" label-position="left" v-loading="importing">
        <el-form-item :label="$tp('模型类型')" required>
          <el-select v-model="importForm.annotationType" :placeholder="$tp('请选择模型类型')" style="width: 100%" @change="handleImportTypeChange">
            <el-option v-for="item in importTaskTypes" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$tp('所属算法')" required>
          <el-select v-model="importForm.algorithmId" filterable remote :remote-method="searchImportAlgorithms"
                     :loading="importAlgorithmsLoading" :disabled="!importForm.annotationType"
                     :placeholder="$tp('先选择模型类型，再搜索算法')" style="width: 100%">
            <el-option v-for="item in importAlgorithmOptions" :key="item.id" :label="item.name" :value="String(item.id)" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$tp('模型名称')" required>
          <el-input v-model="importForm.modelName" maxlength="100" :placeholder="$tp('请输入模型名称')" />
        </el-form-item>
        <el-form-item :label="$tp('模型版本')" required>
          <el-input v-model="importForm.version" :placeholder="$tp('正整数，例如 1')" />
        </el-form-item>
        <el-form-item :label="$tp('导入方式')" required>
          <el-radio-group v-model="importForm.mode" @change="handleImportModeChange">
            <el-radio value="zip">{{ $tp('ZIP 压缩包') }}</el-radio>
            <el-radio value="files">{{ $tp('分别选择文件') }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="importForm.mode === 'zip'" :label="$tp('ZIP 文件')" required>
          <input :key="importFileInputKey" type="file" accept=".zip" @change="selectImportArchive" />
        </el-form-item>
        <el-form-item v-else :label="$tp('PT 文件')" required>
          <input :key="importFileInputKey" type="file" accept=".pt" @change="selectImportFile" />
        </el-form-item>
        <el-form-item v-if="importForm.mode === 'files'" :label="$tp('类别配置')" required>
          <input :key="importFileInputKey" type="file" accept=".yaml,.yml" @change="selectImportYaml" />
        </el-form-item>
        <el-form-item :label="$tp('描述')">
          <el-input v-model="importForm.description" type="textarea" :rows="2" maxlength="200" />
        </el-form-item>
        <el-alert type="info" :closable="false"
                  :title="$tp('ZIP 中放一份 .pt 和一份类别 YAML；也可分别选择两个文件。类别编号和顺序须与模型一致。导入后可下载 PT，转换需另行发起。')" />
        <el-progress v-if="importing" :percentage="importProgress" style="margin-top: 16px" />
      </el-form>
      <template #footer>
        <el-button :disabled="importing" @click="showImportDialog = false">{{ $tp('取消') }}</el-button>
        <el-button type="primary" :loading="importing" @click="handleImportSubmit">{{ $tp('导入') }}</el-button>
      </template>
    </el-dialog>

    <!-- Add / modeldialog -->
    <el-dialog
      v-model="showModelDialog"
      :title="isEditingModel ? $tp('编辑算法模型') : $tp('新增算法模型')"
      width="35%"
      :close-on-click-modal="false"
      @close="handleModelDialogClose"
    >
      <el-form
        ref="modelFormRef"
        :model="modelForm"
        :rules="modelFormRules"
        label-width="110px"
        label-position="left"
        v-loading="modelDialogLoading"
      >
        <el-form-item :label="$tp('模型名称')" prop="modelName">
          <el-input v-model="modelForm.modelName" :placeholder="$tp('请输入模型名称')" clearable />
        </el-form-item>

        <el-form-item :label="$tp('模型版本')" prop="version">
          <el-input v-model="modelForm.version" :placeholder="$tp('例如1')" clearable />
        </el-form-item>

        <el-form-item :label="$tp('模型格式')" prop="modelFormat">
          <el-select v-model="modelForm.modelFormat" :placeholder="$tp('请选择模型格式')" style="width: 100%">
            <el-option
              v-for="item in modelFormatOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item :label="$tp('模型路径')" prop="modelPath">
          <el-input v-model="modelForm.modelPath" :placeholder="$tp('请输入模型文件存储路径')" clearable />
        </el-form-item>

        <el-form-item :label="$tp('算法ID')">
          <el-input v-model="modelForm.algorithmId" :placeholder="$tp('可选，关联算法ID')" clearable />
        </el-form-item>

        <el-form-item :label="$tp('训练任务ID')">
          <el-input v-model="modelForm.trainingId" :placeholder="$tp('可选，关联训练任务ID')" clearable />
        </el-form-item>

        <el-form-item :label="$tp('模型状态')" prop="status">
          <el-select v-model="modelForm.status" :placeholder="$tp('请选择模型状态')" style="width: 100%">
            <el-option
              v-for="item in modelStatusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item :label="$tp('精度(%)')" prop="accuracy">
          <el-input v-model="modelForm.accuracy" :placeholder="$tp('可选，0-100')" clearable />
        </el-form-item>

        <el-form-item :label="$tp('模型描述')">
          <el-input
            v-model="modelForm.description"
            type="textarea"
            :rows="3"
            :placeholder="$tp('请输入模型描述')"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleModelDialogClose" class="common_btn">{{ $tp('取消') }}</el-button>
          <el-button type="primary" @click="handleSubmitModel" class="common_btn">{{ $tp('保存') }}</el-button>
        </div>
      </template>
    </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { translatePhrase as uiText } from '@/i18n'

import {computed, h, onMounted, ref} from 'vue'
import { clacPXToVW } from '@/utils/index'
import {Delete, Edit, Plus} from '@element-plus/icons-vue'
import {ElMessage, ElMessageBox, ElRadio, ElRadioGroup} from 'element-plus'
import {
  batchDeleteModel,
  createModel,
  deleteModel,
  getModelById,
  getModelPage,
  importModelArchive,
  importModelFiles,
  updateModel
} from '@/api/algorithmModel'
import { getAlgorithmPage } from '@/api/algorithmManagement'
import request from "@/utils/request";

// form
const searchForm = ref({
  modelName: '',
  modelSource: '',
  modelType: '',
  createTime: []
})

// tabledata
const tableData = ref([])
const loading = ref(false)

// related
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// current data
const currentPageData = computed(() => {
  return tableData.value
})

// in
const selectedRow = ref(null)
const selectedRows = ref([])

//
const showDetailView = ref(false)
const currentModel = ref(null)

// data
const versionData = ref([])

const showImportDialog = ref(false)
const importing = ref(false)
const importProgress = ref(0)
const importAlgorithmsLoading = ref(false)
const importAlgorithmOptions = ref([])
const importFileInputKey = ref(0)
const importTaskTypes = [
  { value: 'image_classification', label: '图像分类', categories: ['classify'] },
  { value: 'object_detection', label: '物体检测', categories: ['detect', 'personDetect'] },
  { value: 'instance_segmentation', label: '实例分割', categories: ['segment'] },
  { value: 'semantic_segmentation', label: '语义分割', categories: ['semanticSeg'] }
]
const newImportForm = () => ({ annotationType: '', algorithmId: '', modelName: '', version: '1',
  description: '', mode: 'zip', file: null, dataYaml: null, archive: null })
const importForm = ref(newImportForm())

const searchImportAlgorithms = async (query = '') => {
  const selectedType = importTaskTypes.find(item => item.value === importForm.value.annotationType)
  if (!selectedType) { importAlgorithmOptions.value = []; return }
  importAlgorithmsLoading.value = true
  try {
    const responses = await Promise.all(selectedType.categories.map(category =>
      getAlgorithmPage({ current: 1, size: 100, name: query, category })))
    const records = responses.flatMap(response => response?.data?.records || [])
    if (importForm.value.annotationType === selectedType.value) {
      importAlgorithmOptions.value = records.filter(item => selectedType.categories.includes(item.category))
    }
  } catch (error) {
    ElMessage.error(uiText('加载算法失败：') + error.message)
  } finally {
    importAlgorithmsLoading.value = false
  }
}

const handleImportOpen = () => {
  importForm.value = newImportForm()
  importAlgorithmOptions.value = []
  importProgress.value = 0
  importFileInputKey.value += 1
  showImportDialog.value = true
}

const handleImportTypeChange = () => {
  importForm.value.algorithmId = ''
  importAlgorithmOptions.value = []
  searchImportAlgorithms()
}
const handleImportModeChange = () => {
  importForm.value.file = null
  importForm.value.dataYaml = null
  importForm.value.archive = null
  importFileInputKey.value += 1
}
const selectImportFile = (event) => { importForm.value.file = event.target.files?.[0] || null }
const selectImportYaml = (event) => { importForm.value.dataYaml = event.target.files?.[0] || null }
const selectImportArchive = (event) => { importForm.value.archive = event.target.files?.[0] || null }

const handleImportSubmit = async () => {
  const form = importForm.value
  if (!form.annotationType || !form.algorithmId || !form.modelName.trim() || !/^[1-9]\d*$/.test(String(form.version))) {
    ElMessage.warning(uiText('请填写模型类型、所属算法、名称和正整数版本'))
    return
  }
  if (form.mode === 'zip' && (!form.archive || !form.archive.name.toLowerCase().endsWith('.zip'))) {
    ElMessage.warning(uiText('请选择包含一份 PT 和一份类别 YAML 的 ZIP 压缩包'))
    return
  }
  if (form.mode === 'files' && (!form.file || !form.dataYaml || !form.file.name.toLowerCase().endsWith('.pt')
    || !/\.ya?ml$/i.test(form.dataYaml.name))) {
    ElMessage.warning(uiText('请选择 .pt 模型和 .yaml/.yml 类别配置'))
    return
  }
  const modelFile = form.mode === 'zip' ? form.archive : form.file
  if (modelFile.size > 500 * 1024 * 1024 || (form.mode === 'files' && form.dataYaml.size > 1024 * 1024)) {
    ElMessage.warning(uiText('模型或 ZIP 不能超过 500 MiB，类别 YAML 不能超过 1 MiB'))
    return
  }
  const payload = new FormData()
  payload.append('algorithmId', form.algorithmId)
  payload.append('annotationType', form.annotationType)
  payload.append('modelName', form.modelName.trim())
  payload.append('version', form.version)
  payload.append('description', form.description)
  if (form.mode === 'zip') payload.append('archive', form.archive)
  else {
    payload.append('file', form.file)
    payload.append('dataYaml', form.dataYaml)
  }
  importing.value = true
  try {
    const send = form.mode === 'zip' ? importModelArchive : importModelFiles
    await send(payload, event => {
      if (event.total) importProgress.value = Math.min(99, Math.round(event.loaded * 100 / event.total))
    })
    importProgress.value = 100
    showImportDialog.value = false
    ElMessage.success(uiText('模型与类别配置已导入'))
    currentPage.value = 1
    await loadModelData()
  } catch (error) {
    ElMessage.error(uiText('导入失败：') + (error?.message || uiText('请稍后重试')))
  } finally {
    importing.value = false
  }
}

// modelAdd /
const showModelDialog = ref(false)
const isEditingModel = ref(false)
const modelDialogLoading = ref(false)
const modelFormRef = ref(null)
const modelForm = ref({
  id: null,
  modelName: '',
  version: '1',
  modelFormat: 'pt',
  modelPath: '',
  algorithmId: '',
  trainingId: '',
  status: 'published',
  accuracy: null,
  description: ''
})

const modelFormatOptions = [
  { label: 'onnx', value: 'onnx' },
  { label: 'pt', value: 'pt' },
  { label: 'other', value: 'other' }
]

const modelStatusOptions = [
  { label: '已发布', value: 'published' },
  { label: '草稿', value: 'draft' },
  { label: '测试中', value: 'testing' }
]

const validateAccuracy = (rule, value, callback) => {
  if (value === null || value === undefined || value === '') {
    callback()
    return
  }
  const numberValue = Number(value)
  if (Number.isNaN(numberValue) || numberValue < 0 || numberValue > 100) {
    callback(new Error('精度请输入 0 - 100 之间的数字'))
  } else {
    callback()
  }
}

const modelFormRules = {
  modelName: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
  version: [{ required: true, message: '请输入模型版本号', trigger: 'blur' }],
  modelFormat: [{ required: true, message: '请选择模型格式', trigger: 'change' }],
  modelPath: [{ required: true, message: '请输入模型路径', trigger: 'blur' }],
  status: [{ required: true, message: '请选择模型状态', trigger: 'change' }],
  accuracy: [{ validator: validateAccuracy, trigger: 'blur' }]
}

const resetModelForm = () => {
  modelDialogLoading.value = false
  modelForm.value = {
    id: null,
    modelName: '',
    version: '1',
    modelFormat: 'pt',
    modelPath: '',
    algorithmId: '',
    trainingId: '',
    status: 'published',
    accuracy: null,
    description: ''
  }
  if (modelFormRef.value) {
    modelFormRef.value.clearValidate()
  }
}

const fillModelForm = (data) => {
  if (!data) return
  Object.assign(modelForm.value, {
    id: data.id ?? null,
    modelName: data.modelName || data.name || '',
    version: data.version || data.modelVersion,
    modelFormat: data.modelFormat || data.format || 'pt',
    modelPath: data.modelPath || '',
    algorithmId: data.algorithmId ?? '',
    trainingId: data.trainingId ?? '',
    status: data.status || 'published',
    accuracy: data.accuracy ?? null,
    description: data.description || ''
  })
}

const toNumberOrNull = (value) => {
  if (value === '' || value === null || value === undefined) return null
  const num = Number(value)
  return Number.isNaN(num) ? null : num
}

// Generate data
const generateVersionData = (modelData) => {
  if (!modelData) return []

  const originalData = modelData.originalData
  return [
    {
      version: originalData.version || 'V1',
      taskName: originalData.modelName || modelData.name,
      trainMethod: originalData.trainingId ? '零代码训练' : '导入模型',
      description: originalData.description || '-',
      createTime: originalData.createTime || modelData.createTime,
      modelPath: originalData.modelPath,
      modelName: `${modelData.name}.pt`
    }
  ]
}

// Load modeldata
const loadModelData = async () => {
  try {
    loading.value = true
    const params = {
      current: currentPage.value,
      size: pageSize.value,
      modelName: searchForm.value.modelName,
      status: 'published' // only already model
    }

    // Process
    if (searchForm.value.createTime && searchForm.value.createTime.length === 2) {
      params.createdTimeBegin = searchForm.value.createTime[0]
      params.createdTimeEnd = searchForm.value.createTime[1]
    }

    const response = await getModelPage(params)
    if (response.data) {
      tableData.value = response.data.records.map(item => ({
        id: item.id,
        name: item.modelName,
        source: getModelSource(item.trainingId),
        annotationTypeLabel: importTaskTypes.find(type => type.value === item.annotationType)?.label || '—',
        version: item.version,
        downloadCount: item.downloadCount,
        createTime: item.createTime,
        originalData: item
      }))
      total.value = response.data.total
    }
  } catch (error) {
    ElMessage.error(uiText('加载模型数据失败：') + error.message)
  } finally {
    loading.value = false
  }
}

// model
const getModelSource = (trainingId) => {
  return trainingId ? uiText('零代码训练') : uiText('导入模型')
}

// eventProcess
const handleSearch = () => {
  currentPage.value = 1
  loadModelData()
}

const handleReset = () => {
  searchForm.value = {
    modelName: '',
    modelSource: '',
    modelType: '',
    createTime: []
  }
  currentPage.value = 1
  loadModelData()
}

const handleEdit = async () => {
  if (selectedRows.value.length !== 1) {
    ElMessage.warning(uiText('请选择一条模型进行编辑'))
    return
  }

  resetModelForm()
  const row = selectedRows.value[0]
  isEditingModel.value = true
  showModelDialog.value = true
  modelDialogLoading.value = true

  try {
    const response = await getModelById(row.originalData.id)
    fillModelForm(response.data || row.originalData)
  } catch (error) {
    ElMessage.error(uiText('加载模型信息失败：') + error.message)
    showModelDialog.value = false
  } finally {
    modelDialogLoading.value = false
  }
}

const handleDelete = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning(uiText('请选择要删除的模型'))
    return
  }

  try {
    const message = selectedRows.value.length === 1
      ? `确认要删除模型"${selectedRows.value[0].name}"吗？`
      : `确认要删除选中的${selectedRows.value.length}个模型吗？`

    await ElMessageBox.confirm(message, uiText('确认删除'), {
      type: 'warning'
    })

    if (selectedRows.value.length === 1) {
      await deleteModel(selectedRows.value[0].originalData.id)
    } else {
      const ids = selectedRows.value.map(row => row.originalData.id)
      await batchDeleteModel(ids)
    }

    ElMessage.success(uiText('删除成功'))
    selectedRows.value = []
    await loadModelData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(uiText('删除失败：') + error.message)
    }
  }
}

const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

const handleRowClick = (row) => {
  selectedRow.value = row
}

const handleView = (row) => {
  console.log('查看模型', row)
  currentModel.value = row

  // Generate data
  versionData.value = generateVersionData(row)

  showDetailView.value = true
}

const handleDownloadModel = async (row) => {
  try {
    console.log('下载模型', row)

    // model
    await downloadModelFile(row)

  } catch (error) {
    console.error('下载模型失败:', error)
    ElMessage.error(uiText('下载模型失败：') + error.message)
  }
}

const handleDeleteItem = async (row) => {
  try {
    await ElMessageBox.confirm(uiText('确认要删除模型"{value0}"吗？', { value0: row.name }), uiText('确认删除'), {
      type: 'warning'
    })

    await deleteModel(row.originalData.id)
    ElMessage.success(uiText('删除成功'))
    await loadModelData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(uiText('删除失败：') + error.message)
    }
  }
}

// related method
const handleBackToList = () => {
  showDetailView.value = false
  currentModel.value = null
}

// model
const promptDownloadModelType = async (modelRow) => {
  const modelData = modelRow?.originalData || modelRow || {}
  const modelTypes = [
    { type: 'pt', path: modelData.modelPath },
    ...(String(modelData.modelPath || '').startsWith('cloud-training/') ? [{ type: 'classes', label: '类别 YAML', path: modelData.modelPath }] : []),
    { type: 'onnx', path: modelData.onnxModelPath },
    { type: 'rknn', path: modelData.rknnModelPath },
    { type: 'int8-rknn', path: modelData.int8RknnModelOutputPath },
    { type: 'om', path: modelData.omModelOutputPath }
  ]
  const firstAvailableType = modelTypes.find(item => item.path)?.type
  if (!firstAvailableType) {
    ElMessage.warning(uiText('当前模型还没有可下载的文件'))
    return null
  }
  let chosenType = firstAvailableType

  const TypeSelector = {
    name: 'DownloadModelTypeSelector',
    setup() {
      const localType = ref(chosenType)
      const updateType = (value) => {
        localType.value = value
        chosenType = value
      }

      return () => h('div', [
        h(
          ElRadioGroup,
          {
            modelValue: localType.value,
            'onUpdate:modelValue': updateType
          },
          () => modelTypes.map(item => h(
            ElRadio,
            { label: item.type, disabled: !item.path },
            () => item.path ? (item.label || item.type) : uiText('{value0}（未生成）', { value0: item.type })
          ))
        )
      ])
    }
  }

  try {
    await ElMessageBox({
      title: '模型下载',
      message: h(TypeSelector),
      showCancelButton: true,
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      closeOnClickModal: false
    })
    return chosenType
  } catch (error) {
    return null
  }
}

const downloadModelFile = async (modelRow) => {
  try {
    console.log('下载模型文件 - 模型数据:', modelRow)

    const originalData = modelRow?.originalData || modelRow
    if (!originalData) {
      ElMessage.error(uiText('无法获取模型数据'))
      return
    }

    const modelId = originalData.id ?? modelRow?.id
    if (!modelId) {
      ElMessage.error(uiText('没有模型ID'))
      return
    }

    const downloadType = await promptDownloadModelType(modelRow)
    if (!downloadType) {
      return
    }

    const fallbackName = modelRow?.name || originalData?.modelName || 'model'
    const fileName = `${fallbackName}.${downloadType === 'classes' ? 'classes.yaml' : downloadType}`

    const blob = await request({
      url: `/vlsAlgorithmModel/${modelId}/download-file`,
      method: 'get',
      params: { type: downloadType },
      responseType: 'blob'
    })

    if (blob instanceof Blob && blob.type && blob.type.includes('application/json')) {
      const text = await blob.text()
      let message = 'Failed to download model file'
      try {
        const payload = JSON.parse(text)
        message = payload?.message || payload?.msg || message
      } catch (parseError) {
        message = text || message
      }
      throw new Error(message)
    }

    const blobUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = blobUrl
    link.download = fileName
    link.style.display = 'none'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(blobUrl)

    ElMessage.success(`Download started${fileName}`)

  } catch (error) {
    console.error('下载模型文件失败：', error)
    ElMessage.error(uiText('下载模型文件失败：') + error.message)
  }
}

const handleExportModel = async (version) => {
  try {
    console.log('导出模型版本:', version)

    // current model
    await downloadModelFile(currentModel.value)

  } catch (error) {
    ElMessage.error(uiText('导出模型文件失败：') + error.message)
  }
}

const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
  loadModelData()
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  loadModelData()
}

const toolbarButtonList = [
  { name: '编辑', svg: 'table_edit', clickFn: handleEdit },
  { name: '删除', svg: 'table_del', clickFn: handleDelete },
]
const exportItem = ref({ isDisabledExcel: false })
const searchData = ref([
  { label: '关键词', value: 'keyword', type: 'text', default: '' }
])

const searchResetFn = (val, reset) => {
  if (reset && !(val && val.keyword)) {
    handleAdvancedSearchReset()
    return
  }
  handleAdvancedSearch(val || {})
}

// related method
const handleAdvancedSearch = (searchData) => {
  console.log('高级搜索:', searchData)

  // new form
  if (searchData.keyword) {
    searchForm.modelName = searchData.keyword
  }
  if (searchData.modelName) {
    searchForm.modelName = searchData.modelName
  }
  if (searchData.modelSource) {
    searchForm.modelSource = searchData.modelSource
  }
  if (searchData.modelType) {
    searchForm.modelType = searchData.modelType
  }
  if (searchData.dateRange && searchData.dateRange.length > 0) {
    searchForm.createTime = searchData.dateRange
  }

  handleSearch()
}

const handleAdvancedSearchReset = () => {
  console.log('重置高级搜索')
  searchForm.modelName = ''
  searchForm.modelSource = ''
  searchForm.modelType = ''
  searchForm.createTime = []
  handleReset()
}

const handleExport = () => {
  console.log('导出数据')
  ElMessage.success(uiText('导出数据'))
}

const handleUpload = () => {
  console.log('上传文件')
  ElMessage.success(uiText('上传功能'))
}

const handleDownloadTemplate = () => {
  console.log('下载模板')
  ElMessage.success(uiText('下载模板'))
}

const handleBatchOperation = () => {
  console.log('批量操作')
  ElMessage.success(uiText('批量操作'))
}

const handleModelDialogClose = () => {
  showModelDialog.value = false
  isEditingModel.value = false
  resetModelForm()
}

const handleSubmitModel = async () => {
  if (!modelFormRef.value) return
  try {
    await modelFormRef.value.validate()
    const accuracyValue = toNumberOrNull(modelForm.value.accuracy)
    const payload = {
      id: modelForm.value.id,
      modelName: modelForm.value.modelName,
      version: modelForm.value.version,
      modelFormat: modelForm.value.modelFormat,
      algorithmId: toNumberOrNull(modelForm.value.algorithmId),
      trainingId: toNumberOrNull(modelForm.value.trainingId),
      accuracy: accuracyValue
    }
    if (isEditingModel.value) {
      await updateModel(payload)
      ElMessage.success(uiText('编辑算法模型成功'))
    } else {
      await createModel(payload)
      ElMessage.success(uiText('新增算法模型成功'))
    }
    handleModelDialogClose()
    selectedRows.value = []
    await loadModelData()
  } catch (error) {
    const message = error?.message || '保存模型信息失败'
    ElMessage.error(message)
  }
}

// page Load data
onMounted(() => {
  loadModelData()
})
</script>

<style scoped lang="scss">

.tenant_Page {
  height: 100%;
  width: 100%;
  border-radius: var(--common-border-radius) var(--common-border-radius) 0 0;
  background: #f0f2f5;
  .tenant_content {
    width: 100%;
    height: 100%;
    background: #fff;
    border-radius: var(--common-border-radius) var(--common-border-radius) 0 0;
    overflow: hidden;
    display: flex;
    flex-direction: column;
  }
  .tableTenBox {
    padding: 20px;
    width: 100%;
    height: 100%;
    border-radius: var(--common-border-radius) var(--common-border-radius) 0 0;
    flex: 1;
    background: #fff;
    align-items: flex-start;
  }
}
.tableTenItU {
  flex: 1;
  min-width: 0;
  height: 100%;
  overflow: auto;
  :deep(.header_tenant_cell) { background: #F8F8F9; }
}
.paginationBox { justify-content: center; height: 100px; }
.operateAppBox {
  justify-content: flex-end;
  gap: 2px;
  flex-wrap: nowrap;
  white-space: nowrap;
}

.algorithm-model {
  height: 100%;
  overflow: hidden;
}

/*  */
.search-section {
  background: #F5F5F5;
  border-radius: 8px 8px 0 0;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.search-form {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.search-input {
  width: 200px;
}

/*  */
.toolbar-section {
  background: white;
  border-radius: 0;
  padding: 16px 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border-top: 1px solid #e8e8e8;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toolbar-left {
  display: flex;
  gap: 12px;
}

.toolbar-right {
  display: flex;
  align-items: center;
}

/* Add buttonCustom */
.add-btn-custom {
  width: 82px !important;
  height: 36px !important;
  border-radius: 18px !important;
  background: #1A53FF !important;
  border-color: #1A53FF !important;
  padding: 0 !important;
  font-size: 14px;
  font-weight: 500;
  color: white !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  gap: 6px !important;
}

.add-btn-custom:hover,
.add-btn-custom:focus {
  background: #1A53FF !important;
  border-color: #1A53FF !important;
  color: white !important;
  opacity: 0.8;
}

.add-btn-custom:active {
  background: #1A53FF !important;
  border-color: #1A53FF !important;
  color: white !important;
  opacity: 0.9;
}

/* Delete button */
.edit-delete-group {
  display: flex;
  align-items: center;
  margin: 0;
  padding: 0;
}

.edit-delete-group .el-button + .el-button {
  margin-left: 0 !important;
}

/* buttonCustom */
.edit-btn-custom {
  height: 36px !important;
  border-radius: 18px 0 0 18px !important;
  border-right: none !important;
  padding: 0 16px !important;
  font-size: 14px;
  font-weight: 500;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  gap: 6px !important;
  margin-right: 0 !important;
  border-color: #d9d9d9 !important;
}

.edit-btn-custom:hover,
.edit-btn-custom:focus {
  border-right: none !important;
  border-color: #409eff !important;
}

.edit-btn-custom:disabled {
  border-right: none !important;
  border-color: #e4e7ed !important;
}

/* Delete buttonCustom */
.delete-btn-custom {
  height: 36px !important;
  border-radius: 0 18px 18px 0 !important;
  border-left: none !important;
  padding: 0 16px !important;
  font-size: 14px;
  font-weight: 500;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  gap: 6px !important;
  background: white !important;
  color: #f56c6c !important;
  border-color: #d9d9d9 !important;
  margin-left: 0 !important;
}

.delete-btn-custom:hover,
.delete-btn-custom:focus {
  border-left: none !important;
  background: white !important;
  color: #f56c6c !important;
  border-color: #f56c6c !important;
}

.delete-btn-custom:active {
  background: white !important;
  color: #f56c6c !important;
  border-color: #f56c6c !important;
  border-left: none !important;
}

.delete-btn-custom:disabled {
  border-left: none !important;
  background: #f5f5f5 !important;
  color: #c0c4cc !important;
  border-color: #e4e7ed !important;
}

/* table */
.table-section {
  background: white;
  border-radius: 0;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  border-top: 1px solid #e8e8e8;
}

:deep(.el-table) {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  width: 100%;
}

:deep(.el-table__fixed-right) {
  border-left: 1px solid #ebeef5;
}

:deep(.el-table th) {
  background-color: #fafafa;
  font-weight: 600;
  color: #262626;
  border-bottom: 1px solid #ebeef5;
}

:deep(.el-table .el-table__row:hover > td) {
  background-color: #f5f7fa;
}

:deep(.el-table td) {
  border-bottom: 1px solid #ebeef5;
}

/* operationbutton */
.action-buttons {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
  flex-wrap: nowrap;
}

/*  */
.pagination-section {
  display: flex;
  justify-content: center;
  background: white;
  border-radius: 0 0 8px 8px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border-top: 1px solid #e8e8e8;
}

/*  */
.detail-view {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: var(--common-border-radius) var(--common-border-radius) 0 0;
  overflow: hidden;
  min-height: 0;
}

/*  */
.breadcrumb-section {
  background: #fff;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  box-shadow: none;
}

.breadcrumb {
  display: flex;
  align-items: center;
  font-size: 14px;
  color: #606266;
}

.breadcrumb-item {
  color: var(--el-color-primary);
  cursor: pointer;
  transition: color 0.3s;
}

.breadcrumb-item:hover {
  color: #3d70ff;
}

.breadcrumb-item.active {
  color: #303133;
  cursor: default;
}

.breadcrumb-separator {
  margin: 0 8px;
  color: #c0c4cc;
}

/* info */
.detail-info-section {
  background: #fff;
  border-radius: 0;
  padding: 20px;
  box-shadow: none;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
  max-width: 800px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 16px;
}

.info-label {
  font-size: 14px;
  color: #666;
  min-width: 80px;
  font-weight: 500;
}

.info-value {
  font-size: 14px;
  color: #262626;
  font-weight: 500;
}

/*  */
.version-section {
  background: #fff;
  border-radius: 0;
  padding: 0 20px 20px;
  box-shadow: none;
  flex: 1;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
  margin: 0 0 20px 0;
}

.version-table {
  margin-top: 16px;
}

.version-table :deep(.el-table) {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  width: 100%;
}

.version-table :deep(.el-table th) {
  background-color: #fafafa;
  font-weight: 600;
  color: #262626;
  border-bottom: 1px solid #ebeef5;
}

.version-table :deep(.el-table .el-table__row:hover > td) {
  background-color: #f5f7fa;
}

.version-table :deep(.el-table td) {
  border-bottom: 1px solid #ebeef5;
}
</style>
