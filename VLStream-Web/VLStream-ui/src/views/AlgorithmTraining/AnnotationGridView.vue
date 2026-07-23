<template>
  <div class="annotation-grid-view">
    <!-- 顶部筛选标签 -->
    <el-tabs
      v-model="activeFilter"
      class="tenanat-tabs"
      @tab-change="handleFilterChange"
    >
      <el-tab-pane
        v-for="filter in annotationFilters"
        :key="filter.key"
        :label="filter.label"
        :name="filter.key"
      />
    </el-tabs>

    <div class="grid-container tableTenBox flexRowAC">
      <!-- 左侧标签面板 -->
      <div v-yResize class="police_aside_use">
        <AnnotationLabelPanel
          :labels="annotationLabels"
          :selected-label-id="selectedLabelId"
          @label-selected="selectLabel"
          @add-label="handleAddLabel"
          @edit-label="handleEditLabel"
          @delete-label="handleDeleteLabel"
        />
      </div>

      <!-- 右侧主内容区域 -->
      <div class="tableTenItU main-content">
        <div class="depNameBox_out flexRowAC">
          <div class="depNameBox flexRowAC">
            <div class="exportBtnBox flexRowAC">
              <button-group :button-list="toolbarButtonList" />
            </div>
          </div>
        </div>

        <!-- 图片网格区域 -->
        <div class="images-grid-wrapper" @scroll="handleGridScroll">
          <div class="images-grid">
            <div
              v-for="image in displayedImages"
              :key="image.id"
              class="image-card"
              :class="{ selected: selectedImages.includes(image.id) }"
              @click="handleImageCardClick(image.id, $event)"
            >
              <div class="image-container">
                <img
                  :src="image.url"
                  :alt="image.name"
                  class="image-preview"
                  @error="handleImageError"
                  loading="lazy"
                />
                <div class="image-corner-icon">
                  <el-icon><Crop /></el-icon>
                </div>
              </div>
              <div class="image-info flexRowAC">
                <div class="image-name">{{ getImageDisplayName(image) }}</div>
                <div class="image-card-actions flexRowAC">
                  <el-icon class="card-action-icon" title="标注" @click.stop="handleAnnotateImage(image.id)">
                    <Crop />
                  </el-icon>
                  <el-icon class="card-action-icon" title="预览" @click.stop="handlePreviewImage(image)">
                    <FullScreen />
                  </el-icon>
                </div>
              </div>
            </div>

            <!-- 空状态 -->
            <div v-if="filteredImages.length === 0" class="empty-state">
              <el-icon class="empty-icon"><Picture /></el-icon>
              <p class="empty-text">暂无图片</p>
              <button type="button" class="exportBtn newBtn flexRowAC" @click="handleImportImages">
                <el-icon class="BtnImg"><Upload /></el-icon>
                导入图片
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 标签编辑弹窗 -->
    <el-dialog
      v-model="showLabelDialog"
      :title="editingLabel ? '编辑标签' : '新增标签'"
      width="25%"
    >
      <el-form :model="labelForm" :rules="labelRules" ref="labelFormRef" label-width="80px">
        <el-form-item label="标签名" prop="name">
          <el-input v-model="labelForm.name" placeholder="请输入标签名称" />
        </el-form-item>
        <el-form-item label="颜色" prop="color">
          <el-color-picker v-model="labelForm.color" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showLabelDialog = false" class="common_btn">取消</el-button>
        <el-button type="primary" @click="handleSaveLabel" class="common_btn">保存</el-button>
      </template>
    </el-dialog>

    <!-- 图片上传弹窗 -->
    <el-dialog
      v-model="showUploadDialog"
      title="导入图片"
      width="45%"
      class="import-dialog"
    >
      <div class="import-dialog-content">
        <!-- 左侧表单区域 -->
        <div class="import-form-section">
          <!-- 标注状态 -->
          <div class="form-group">
            <label class="form-label">标注状态</label>
            <div class="radio-group">
              <el-radio v-model="importForm.annotationStatus" label="none" class="custom-radio">
                无标注信息
              </el-radio>
              <el-radio v-model="importForm.annotationStatus" label="exist" class="custom-radio">
                有标注信息
              </el-radio>
            </div>
          </div>

          <!-- 导入路径 -->
          <div class="form-group">
            <label class="form-label">导入路径<span class="required">*</span></label>
            <div class="path-input-group">
              <el-button @click="handleSelectDirectory" class="select-directory-btn">
                <el-icon><Folder /></el-icon>
                选择目录
              </el-button>
            </div>

            <!-- 提示信息 -->
            <div class="import-tips">
              <div class="tip-item">
                <el-icon class="tip-icon"><InfoFilled /></el-icon>
                <div class="tip-content">
                  <p><strong>提示：</strong>1.导入后请避免改动本地路径数据，以免影响数据标注、训练功能正常使用</p>
                  <p>2.每次导入仅支持选择同一目录，如您想选择体验一站式功能，可联网下载已标注训练数据样例</p>
                  <div class="link-wrapper">
                    <a href="#" class="help-link">实例分享训练数据集（coco格式）</a>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧帮助区域 -->
        <div class="help-section">
          <!-- 如何设计标签 -->
          <div class="help-item">
            <div class="help-header" @click="toggleHelpItem('labels')">
              <span class="help-title">1. 如何设计标签</span>
              <el-icon class="toggle-icon" :class="{ expanded: expandedHelp.labels }">
                <ArrowDown />
              </el-icon>
            </div>
            <div v-show="expandedHelp.labels" class="help-content">
              <p>每种需要识别的目标都需要一个标签，一张图片中可以有多种目标出现。</p>
              <p>标签的上限为1000种，标签名由数字、中英文、中/下划线组成，长度上限256字符。</p>
            </div>
          </div>

          <!-- 图片内容要求 -->
          <div class="help-item">
            <div class="help-header" @click="toggleHelpItem('content')">
              <span class="help-title">2. 图片内容要求</span>
              <el-icon class="toggle-icon" :class="{ expanded: expandedHelp.content }">
                <ArrowDown />
              </el-icon>
            </div>
            <div v-show="expandedHelp.content" class="help-content">
              <p>训练图片和实际场景要尽可能的相似图片拍摄环境一致，尽量如实采集标记场景的同期图片是能够头绪的图像做好准备，训练图片数量不低于同期标记日标正确图片。</p>
              <p>每个标签的图片需要覆盖实际场景里面的可能性，如拍摄角度、光线明暗的变化，训练集要涵盖的场景越多，模型的泛化能力越强。</p>
              <p>每个模型训练图片量不得低于4张，每个标签建议标注50个框以上。</p>
            </div>
          </div>

          <!-- 导入格式要求 -->
          <div class="help-item">
            <div class="help-header" @click="toggleHelpItem('format')">
              <span class="help-title">3. 导入格式要求</span>
              <el-icon class="toggle-icon" :class="{ expanded: expandedHelp.format }">
                <ArrowDown />
              </el-icon>
            </div>
            <div v-show="expandedHelp.format" class="help-content">
              <p>支持导入常见图片格式：jpg、png、jpeg、bmp等。</p>
              <p>如有标注信息，请确保标注文件格式正确。</p>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showUploadDialog = false" class="cancel-btn common_btn">取消</el-button>
          <el-button type="primary" @click="handleConfirmUpload" class="confirm-btn common_btn">确认</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 图片预览弹窗 -->
    <el-dialog
      v-model="showPreviewDialog"
      :title="previewImage?.name || '图片预览'"
      width="70%"
      class="image-preview-dialog"
      @close="handleClosePreview"
    >
      <div v-if="previewImage" class="preview-wrapper">
        <img
          :src="previewImage.url"
          :alt="previewImage.name"
          class="preview-image-large"
          @error="handleImageError"
        />
        <div class="preview-meta">
          <div class="meta-name" v-if="previewImage.name">文件名：{{ previewImage.name }}</div>
          <div class="meta-remark" v-if="previewImage.originalName">原始名：{{ previewImage.originalName }}</div>
        </div>
      </div>
      <div v-else class="preview-placeholder">
        <el-icon class="preview-placeholder-icon"><Picture /></el-icon>
        <p>无法加载图片</p>
      </div>
      <template #footer>
        <el-button @click="handleClosePreview" class="common_btn">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import {computed, onMounted, reactive, ref, watch} from 'vue'
import {ElLoading, ElMessage, ElMessageBox} from 'element-plus'
import {ArrowDown, Crop, Delete, Edit, Folder, FullScreen, InfoFilled, Picture, Upload} from '@element-plus/icons-vue'
import {getAllAnnotationInstances} from '@/api/annotationInstances'
import {uploadAnnotationImages} from '@/api/annotationImage'
import {importAnnotationData} from '@/api/algorithmAnnotation'
import AnnotationLabelPanel from '@/components/AnnotationLabelPanel.vue'

// 定义props
const props = defineProps({
  annotationData: {
    type: Object,
    default: () => ({})
  }
})

// 定义emit
const emit = defineEmits(['back-to-list', 'start-annotation', 'images-imported', 'add-label', 'update-label', 'delete-label', 'batch-upload-images', 'import-annotation-zip', 'delete-image-and-data'])

// 使用从父组件传递的真实标签数据
const annotationLabels = computed(() => {
  // 如果父组件传递了标注数据，使用其中的标签数据
  if (props.annotationData && props.annotationData.annotationLabels) {
    return props.annotationData.annotationLabels
  }

  // 否则返回空数组
  return []
})

// 使用从父组件传递的真实图片数据
const uploadedImages = computed(() => {
  // 如果父组件传递了标注数据，使用其中的图片数据
  if (props.annotationData && props.annotationData.uploadedImages) {
    return props.annotationData.uploadedImages
  }

  // 否则返回空数组
  return []
})

// 筛选相关
const annotationFilters = ref([
  { key: 'all', label: '全部', count: 0 },
  { key: 'annotated', label: '有标注信息', count: 0 },
  { key: 'unannotated', label: '无标注信息', count: 0 }
])

// 标注实例集合（支持 imageId / name 匹配）
const buildAnnotatedSets = () => {
  const ids = new Set()
  const names = new Set()

  annotationInstances.value.forEach(instance => {
    if (instance.imageId != null) ids.add(instance.imageId)
    if (instance.imageName) names.add(instance.imageName)
    if (instance.originalName) names.add(instance.originalName)
  })

  return { ids, names }
}

const isImageAnnotated = (image, annotatedSets) => {
  const { ids, names } = annotatedSets
  const hitById = image?.id != null && ids.has(image.id)
  const hitByName =
    (image?.name && names.has(image.name)) ||
    (image?.originalName && names.has(image.originalName))
  return hitById || hitByName
}

const activeFilter = ref('all')

const selectedLabelId = ref(null)
const selectedImages = ref([])
const showLabelDialog = ref(false)
const showUploadDialog = ref(false)
const editingLabel = ref(null)
const showPreviewDialog = ref(false)
const previewImage = ref(null)

const getImageDisplayName = (image) => {
  if (selectedLabelId.value) {
    const label = annotationLabels.value.find(item => item.id === selectedLabelId.value)
    if (label?.name) return label.name
  }
  const annotations = image?.annotations || []
  if (annotations.length > 0 && annotations[0].labelName) {
    return annotations[0].labelName
  }
  return image?.name || '未命名'
}

// 表单数据
const labelForm = reactive({
  name: '',
  color: '#409eff'
})

// 导入表单数据
const importForm = reactive({
  annotationStatus: 'none'
})
const pendingUploadFiles = ref([])
const pendingZipFile = ref(null)
const isUploading = ref(false)

// 帮助区域展开状态
const expandedHelp = reactive({
  labels: false,
  content: false,
  format: false
})

const labelRules = {
  name: [{ required: true, message: '请输入标签名称', trigger: 'blur' }]
}

// 存储标注实例数据用于筛选
const annotationInstances = ref([])

// 计算属性 - 筛选后的图片
const filteredImages = computed(() => {
  const annotatedSets = buildAnnotatedSets()
  switch (activeFilter.value) {
    case 'annotated':
      return uploadedImages.value.filter(image => isImageAnnotated(image, annotatedSets))
    case 'unannotated':
      return uploadedImages.value.filter(image => !isImageAnnotated(image, annotatedSets))
    default:
      return uploadedImages.value
  }
})

// 限制初次渲染数量，滚动时逐步追加，避免一次性渲染过多图片卡顿
const INITIAL_RENDER_COUNT = 40
const RENDER_INCREMENT = 20
const renderCount = ref(INITIAL_RENDER_COUNT)
const displayedImages = computed(() =>
  filteredImages.value.slice(0, renderCount.value)
)

const resetRenderCount = () => {
  renderCount.value = Math.min(filteredImages.value.length, INITIAL_RENDER_COUNT)
}

// 更新筛选器统计数量
const updateFilterCounts = async () => {
  const total = uploadedImages.value.length

  try {
    // 获取所有标注实例数据来统计
    if (props.annotationData && props.annotationData.id) {
      const response = await getAllAnnotationInstances(props.annotationData.id)

      if (response.code === 200) {
        const instances = response.data || []

        // 更新标注实例数据用于筛选
        annotationInstances.value = instances

        // 统计有标注的图片数量（根据图片名称去重）
        const annotatedSets = buildAnnotatedSets()
        const annotated = uploadedImages.value.filter(image => isImageAnnotated(image, annotatedSets)).length
        const unannotated = total - annotated

        annotationFilters.value[0].count = total
        annotationFilters.value[1].count = annotated
        annotationFilters.value[2].count = unannotated

        console.log('统计结果:', { total, annotated, unannotated })
      } else {
        // API调用失败，使用默认统计
        console.warn('获取标注实例失败，使用默认统计')
        annotationFilters.value[0].count = total
        annotationFilters.value[1].count = 0
        annotationFilters.value[2].count = total
      }
    } else {
      // 没有标注数据，所有图片都是无标注
      annotationFilters.value[0].count = total
      annotationFilters.value[1].count = 0
      annotationFilters.value[2].count = total
    }
  } catch (error) {
    console.error('更新筛选器统计失败:', error)
    // 出错时使用默认统计
    annotationFilters.value[0].count = total
    annotationFilters.value[1].count = 0
    annotationFilters.value[2].count = total
  }
}

// 方法
const selectLabel = (labelId) => {
  selectedLabelId.value = labelId
}

const handleFilterChange = (filterKey) => {
  activeFilter.value = filterKey
  console.log('切换筛选器:', filterKey)
}

const handleGridScroll = (event) => {
  const target = event?.target
  if (!target) return

  const isNearBottom = target.scrollTop + target.clientHeight >= target.scrollHeight
  if (isNearBottom && renderCount.value < filteredImages.value.length) {
    renderCount.value = Math.min(
      filteredImages.value.length,
      renderCount.value + RENDER_INCREMENT
    )
  }
}

const handleBackToList = () => {
  // 触发父组件的返回事件
  emit('back-to-list')
}

const handleAddLabel = (labelData) => {
  // 新组件传递的是标签数据对象
  if (labelData && labelData.name) {
    // 处理新增标签
    emit('add-label', labelData)
  } else {
    // 兼容原有调用方式
    editingLabel.value = null
    labelForm.name = ''
    labelForm.color = '#409eff'
    showLabelDialog.value = true
  }
}

const handleEditLabel = (labelData) => {
  // 新组件传递的是标签数据对象
  if (labelData && labelData.id) {
    // 处理编辑标签
    emit('edit-label', labelData)
  } else {
    // 兼容原有调用方式
    editingLabel.value = labelData
    labelForm.name = labelData.name
    labelForm.color = labelData.color
    showLabelDialog.value = true
  }
}

const handleDeleteLabel = (labelId) => {
  // 直接传递给父组件处理，删除确认由公共组件处理
  emit('delete-label', labelId)
}

const handleSaveLabel = () => {
  // 保存标签逻辑
  if (editingLabel.value) {
    // 编辑标签 - 通知父组件
    emit('update-label', {
      id: editingLabel.value.id,
      name: labelForm.name,
      color: labelForm.color
    })
    ElMessage.success('标签更新成功')
  } else {
    // 新增标签 - 通知父组件
    const newLabel = {
      name: labelForm.name,
      color: labelForm.color,
      usageCount: 0
    }
    emit('add-label', newLabel)
    ElMessage.success('标签添加成功')
  }
  showLabelDialog.value = false
}

const handleImportImages = () => {
  pendingUploadFiles.value = []
  pendingZipFile.value = null
  showUploadDialog.value = true
}

const handleBatchAnnotate = () => {
  if (selectedImages.value.length === 0) {
    ElMessage.warning('请先选择要标注的图片')
    return
  }

  console.log(`开始批量标注 ${selectedImages.value.length} 张图片`)

  // 传递所有选中的图片到标注页面
  emit('start-annotation', {
    type: 'batch',
    images: selectedImages.value.map(id =>
      uploadedImages.value.find(img => img.id === id)
    ).filter(Boolean)
  })
}

const handleDeleteSelected = async () => {
  if (selectedImages.value.length === 0) {
    ElMessage.warning('请先选择要删除的图片')
    return
  }

  console.log('=== 开始删除选中的图片 ===')
  console.log('选中的图片ID:', selectedImages.value)
  console.log('uploadedImages数组:', uploadedImages.value)

  ElMessageBox.confirm(`确定要删除选中的 ${selectedImages.value.length} 张图片吗？这将同时删除图片文件和相关的标注数据。`, '确认删除', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      // 获取要删除的图片信息，根据图片ID查找
      const validImages = []
      for (const imageId of selectedImages.value) {
        const image = uploadedImages.value.find(img => img.id === imageId)
        if (image) {
          validImages.push(image)
          console.log(`找到要删除的图片: ID=${imageId}, name=${image.name}`)
        } else {
          console.warn(`未找到图片: ID=${imageId}`)
        }
      }

      console.log('要删除的图片信息:', validImages)
      console.log('标注数据ID:', props.annotationData?.id)

      if (validImages.length === 0) {
        console.warn('没有找到有效的图片进行删除')
        ElMessage.warning('没有找到有效的图片进行删除')
        return
      }

      const imageIds = validImages.map(img => img.id)

      // 调用完整的删除逻辑（删除图片文件和标注数据）
      if (props.annotationData && props.annotationData.id) {
        await deleteImageAndRelatedData(props.annotationData.id, imageIds)
      }

      // 从前端数组中删除图片（根据ID删除）
      for (const imageId of selectedImages.value) {
        const index = uploadedImages.value.findIndex(img => img.id === imageId)
        if (index > -1) {
          uploadedImages.value.splice(index, 1)
        }
      }
      selectedImages.value = []

      console.log('=== 图片删除流程完成 ===')
      ElMessage.success('图片及相关标注数据删除成功')
    } catch (error) {
      console.error('删除图片失败:', error)
      ElMessage.error('删除图片失败: ' + error.message)
    }
  })
}

const toolbarButtonList = computed(() => [
  { name: '导入', svg: 'table_incoming', clickFn: handleImportImages },
  { name: '标注', svg: 'table_edit', clickFn: handleBatchAnnotate },
  { name: '删除', svg: 'table_del', clickFn: handleDeleteSelected }
])

const deleteImageAndRelatedData = async (annotationId, imageIds) => {
  console.log('deleteImageAndRelatedData :', { annotationId, imageIds: imageIds })

  // 由于这是子组件，我们通过emit通知父组件处理
  return new Promise((resolve) => {
    emit('delete-image-and-data', { annotationId, imageIds: imageIds })
    setTimeout(resolve, 50)
  })
}

const handleImageCardClick = (imageId, event) => {
  if (event.ctrlKey || event.metaKey) {
    // Ctrl/Cmd + 点击：多选
    const selectedIndex = selectedImages.value.indexOf(imageId)
    if (selectedIndex > -1) {
      selectedImages.value.splice(selectedIndex, 1)
    } else {
      selectedImages.value.push(imageId)
    }
  } else {
    // 普通点击：单选
    selectedImages.value = [imageId]
  }
}

const handleAnnotateImage = (imageId) => {
  // 找到对应的图片
  const image = filteredImages.value.find(img => img.id === imageId)
  if (image) {
    console.log(`开始标注图片: ${image.name}`)
    // 触发父组件切换到标注模式
    emit('start-annotation', image)
  }
}

const handlePreviewImage = (image) => {
  if (!image || !image.url) {
    ElMessage.warning('未找到图片资源，无法预览')
    return
  }
  previewImage.value = image
  showPreviewDialog.value = true
}

const handleClosePreview = () => {
  showPreviewDialog.value = false
  previewImage.value = null
}

const handleSelectDirectory = () => {
  const isZipImport = importForm.annotationStatus === 'exist'
  const input = document.createElement('input')
  input.type = 'file'
  input.multiple = !isZipImport

  if (isZipImport) {
    input.accept = '.zip'
  } else {
    input.webkitdirectory = true
  }

  input.onchange = async (event) => {
    const files = Array.from(event.target.files || [])
    if (isZipImport) {
      const zipFile = files[0]
      if (!zipFile) return
      if (!zipFile.name.toLowerCase().endsWith('.zip')) {
        ElMessage.warning('请选择zip文件')
        pendingZipFile.value = null
        return
      }
      pendingZipFile.value = zipFile
      pendingUploadFiles.value = []
      ElMessage.success(`选择zip文件: ${zipFile.name}`)
      return
    }

    const imageFiles = files.filter(file => file.type.startsWith('image/'))

    if (imageFiles.length === 0) {
      ElMessage.warning('所选文件夹中没有找到图片文件')
      return
    }

    pendingUploadFiles.value = imageFiles
    pendingZipFile.value = null

    if (files.length > 0) {
      const firstFile = files[0]
      const fullPath = firstFile.webkitRelativePath
      const folderPath = fullPath.substring(0, fullPath.lastIndexOf('/'))

      ElMessage.success(`已选择文件夹：${folderPath}，找到 ${imageFiles.length} 个图片文件`)
    }

  }

  input.click()
}
const batchProcessImages = async (files) => {
  const annotationId = props.annotationData?.id
  if (!annotationId) {
    throw new Error('Missing annotation ID, cannot batch upload')
  }

  const uploadResp = await uploadAnnotationImages(files, annotationId)
  const isSuccess = uploadResp?.success || uploadResp?.code === 200

  if (!isSuccess) {
    throw new Error(uploadResp?.message || 'Batch upload images failed')
  }

  emit('batch-upload-images', files, uploadResp)
  return uploadResp
}
const toggleHelpItem = (item) => {
  expandedHelp[item] = !expandedHelp[item]
}

const handleFileChange = (file, fileList) => {
  console.log('文件选择:', fileList)
}

const handleConfirmUpload = async () => {
  if (isUploading.value) return
  const isZipImport = importForm.annotationStatus === 'exist'
  if (isZipImport) {
    if (!pendingZipFile.value) {
      ElMessage.warning('Please select a .zip file.')
      return
    }
  } else if (pendingUploadFiles.value.length === 0) {
    ElMessage.warning('Please select images to upload.')
    return
  }

  const loading = ElLoading.service({
    lock: true,
    fullscreen: true,
    text: isZipImport ? 'Uploading zip...' : 'Uploading images...',
    background: 'rgba(0, 0, 0, 0.6)'
  })
  isUploading.value = true

  try {
    if (isZipImport) {
      const annotationId = props.annotationData?.id
      if (!annotationId) {
        throw new Error('Missing annotation ID, cannot import zip')
      }
      const response = await importAnnotationData(annotationId, pendingZipFile.value)
      const isSuccess = response?.success || response?.code === 200
      if (!isSuccess) {
        throw new Error(response?.message || 'Zip import failed')
      }
      emit('import-annotation-zip', response)
      pendingZipFile.value = null
    } else {
      await batchProcessImages(pendingUploadFiles.value)
      pendingUploadFiles.value = []
    }

    if (importForm.annotationStatus === 'none') {
      ElMessage.success('导入无标注图片成功')
    } else {
      ElMessage.success('导入有标注图片成功')
    }

    showUploadDialog.value = false
  } catch (error) {
    console.error('Upload failed:', error)
    ElMessage.error(error?.message || 'Upload failed')
  } finally {
    isUploading.value = false
    loading.close()
  }
}
const handleImageError = (event) => {
  console.error('网格图片加载失败:', event.target.src)
  // 设置默认占位图片
  event.target.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjE1MCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMjAwIiBoZWlnaHQ9IjE1MCIgZmlsbD0iI2Y1ZjVmNSIvPjx0ZXh0IHg9IjEwMCIgeT0iNzUiIGZvbnQtZmFtaWx5PSJBcmlhbCIgZm9udC1zaXplPSIxNCIgZmlsbD0iIzk5OSIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZHk9Ii4zZW0iPuWbvueJh+S4jeWtmOWcqDwvdGV4dD48L3N2Zz4='
}

watch(filteredImages, () => {
  resetRenderCount()
}, { deep: true, immediate: true })

// 监听图片数据变化，更新统计
watch(() => uploadedImages.value, () => {
  updateFilterCounts()
}, { deep: true })

// 监听标注数据变化，更新统计
watch(() => props.annotationData, () => {
  updateFilterCounts()
}, { deep: true })

onMounted(() => {
  console.log('网格标注视图已加载')
  resetRenderCount()
  updateFilterCounts()
})
</script>

<style scoped lang="scss">
.annotation-grid-view {
  height: 100%;
  background: #fff;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.grid-container {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
  align-items: flex-start;
  background: #fff;
  padding: 16px 20px 0;
}

.police_aside_use {
  width: 280px;
  padding-right: 20px;
  flex-shrink: 0;
  height: 100%;
  overflow: hidden;
  display: flex;
  flex-direction: column;

  :deep(.annotation-label-panel) {
    flex: 1;
    min-height: 0;
  }
}

.tableTenItU.main-content {
  flex: 1;
  min-width: 0;
  height: 100%;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.depNameBox_out {
  flex-shrink: 0;
  padding-bottom: 12px;
}

.images-grid-wrapper {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 0 0 40px;
}

.images-grid {
  min-height: 0;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
  align-content: start;
}

.image-card {
  background: #fff;
  border-radius: 4px;
  border: 2px solid transparent;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.image-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
}

.image-card.selected {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 1px var(--el-color-primary);
}

.image-container {
  position: relative;
  width: 100%;
  height: 160px;
  overflow: hidden;
  background: #f5f5f5;
}

.image-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-corner-icon {
  position: absolute;
  top: 8px;
  left: 8px;
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 2px;
  color: #666;
  font-size: 14px;
}

.image-info {
  padding: 10px 12px;
  justify-content: space-between;
  gap: 8px;
  background: #fff;
  border-top: 1px solid #f0f0f0;
}

.image-name {
  font-size: 14px;
  font-weight: 400;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  min-width: 0;
}

.image-card-actions {
  gap: 8px;
  flex-shrink: 0;
  opacity: 0;
  transition: opacity 0.2s;
}

.image-card:hover .image-card-actions,
.image-card.selected .image-card-actions {
  opacity: 1;
}

.card-action-icon {
  font-size: 16px;
  color: var(--el-color-primary);
  cursor: pointer;

  &:hover {
    opacity: 0.8;
  }
}

.empty-state {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #8c8c8c;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  color: #d9d9d9;
}

.empty-text {
  font-size: 16px;
  margin-bottom: 20px;
}

/* 导入弹窗样式 */
.import-dialog .el-dialog__body {
  padding: 20px;
}

.import-dialog-content {
  display: flex;
  gap: 40px;
  min-height: 400px;
}

.import-form-section {
  flex: 1;
  max-width: 350px;
}

.help-section {
  flex: 1;
  max-width: 350px;
}

/* 表单样式 */
.form-group {
  margin-bottom: 24px;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #262626;
  margin-bottom: 8px;
}

.required {
  color: #ff4d4f;
  margin-left: 2px;
}

.radio-group {
  display: flex;
  gap: 16px;
}

.custom-radio {
  margin-right: 0;
}

.path-input-group {
  margin-bottom: 12px;
}

.select-directory-btn {
  width: 100px;
  height: 32px;
  border-radius: 6px;
  border: 1px solid #d9d9d9;
  background: white;
  color: #666;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.select-directory-btn:hover {
  border-color: #40a9ff;
  color: #40a9ff;
}

/* 提示信息样式 */
.import-tips {
  background: #fffbe6;
  border: 1px solid #ffe58f;
  border-radius: 6px;
  padding: 12px;
}

.tip-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.tip-icon {
  color: #faad14;
  font-size: 16px;
  margin-top: 2px;
  flex-shrink: 0;
}

.tip-content p {
  margin: 0 0 8px 0;
  font-size: 13px;
  color: #666;
  line-height: 1.5;
}

.tip-content p:last-child {
  margin-bottom: 0;
}

.link-wrapper {
  margin-top: 8px;
}

.help-link {
  color: #1890ff;
  text-decoration: none;
  font-size: 13px;
}

.help-link:hover {
  text-decoration: underline;
}

/* 帮助区域样式 */
.help-item {
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  margin-bottom: 12px;
  overflow: hidden;
}

.help-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #fafafa;
  cursor: pointer;
  transition: background 0.2s;
}

.help-header:hover {
  background: #f5f5f5;
}

.help-title {
  font-size: 14px;
  font-weight: 500;
  color: #262626;
}

.toggle-icon {
  font-size: 14px;
  color: #8c8c8c;
  transition: transform 0.2s;
}

.toggle-icon.expanded {
  transform: rotate(180deg);
}

.help-content {
  padding: 16px;
  background: white;
  border-top: 1px solid #f0f0f0;
}

.help-content p {
  margin: 0 0 12px 0;
  font-size: 13px;
  color: #666;
  line-height: 1.6;
}

.help-content p:last-child {
  margin-bottom: 0;
}

/* 弹窗底部按钮 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 图片预览 */
.image-preview-dialog :deep(.el-dialog__body) {
  padding: 10px 20px 20px;
}

.preview-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.preview-image-large {
  max-width: 100%;
  max-height: 70vh;
  border-radius: 8px;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
  object-fit: contain;
}

.preview-meta {
  width: 100%;
  text-align: center;
  color: #666;
  font-size: 13px;
  display: flex;
  justify-content: center;
  gap: 12px;
}

.preview-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #999;
  min-height: 200px;
}

.preview-placeholder-icon {
  font-size: 48px;
  margin-bottom: 8px;
  color: #d9d9d9;
}
</style>
