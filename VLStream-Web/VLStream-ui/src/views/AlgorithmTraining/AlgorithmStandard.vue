<template>
  <div class="page-container">
    <!-- 页面标题 -->
    <div class="page-header" v-if="!showAnnotationView">
      <h1>算法标注</h1>
      <p>管理和创建算法训练所需的标注数据集</p>
    </div>

    <!-- 导航栏（标注视图时显示） -->
    <div v-if="showAnnotationView" class="content-header">
      <div class="breadcrumb">
        <span class="breadcrumb-item" @click="showTableView">算法标注</span>
        <span class="breadcrumb-separator">></span>
        <span class="breadcrumb-item active">查看与标注</span>
      </div>
    </div>

    <!-- 列表视图 -->
    <div v-if="!showAnnotationView" class="list-view">
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
            >
            </ActionButtonGroup>
          </div>
          
          <div class="toolbar-right">
            <AdvancedSearch 
              @search="handleAdvancedSearch"
              @reset="handleAdvancedSearchReset"
              @export="handleExport"
              @upload="handleImport"
              @template="handleDownloadTemplate"
              @batch="handleBatchOperation"
            />
          </div>
        </div>

        <!-- 表格内容 -->
        <div class="table-content">
          <el-table 
            :data="tableData" 
            stripe 
            style="width: 100%"
            v-loading="loading"
            @selection-change="handleSelectionChange"
            @row-click="handleRowClick"
          >
            <el-table-column type="selection" width="55" />
            <el-table-column prop="name" label="标注名称" min-width="200" />
            <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
            <el-table-column prop="type" label="标注类型" width="120" align="center" />
            <el-table-column prop="status" label="标注状态" width="120" align="center">
              <template #default="scope">
                <el-tag 
                  :type="getStatusTagType(scope.row.annotationStatus)" 
                  size="small"
                >
                  {{ scope.row.status }} ({{ scope.row.progress }})
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="datasetPath" label="数据集路径" width="200">
              <template #default="scope">
                <span
                  v-if="scope.row.datasetPath"
                  class="dataset-path clickable"
                  :title="scope.row.datasetPath"
                >
                  {{ scope.row.datasetPath }}
                </span>
                <span v-else class="no-dataset">未设置</span>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="180" />
                            <el-table-column label="操作" width="360" align="right" fixed="right">
              <template #default="scope">
                <div class="action-buttons">
                  <el-button 
                    type="primary" 
                    link 
                    size="small" 
                  @click="handleView(scope.row)"
                >
                  查看与标注
                </el-button>
                  <el-button
                    type="primary"
                    link
                    size="small"
                    @click="handleSaveDataset(scope.row)"
                  >
                    生成数据集
                  </el-button>

                  <el-button 
                    type="primary" 
                    link 
                    size="small" 
                    @click="handleImportData(scope.row)"
                  >
                    导入
                  </el-button>
                  <el-button 
                    type="primary" 
                    link 
                    size="small" 
                    @click="handleExportData(scope.row)"
                  >
                    导出
                  </el-button>
                  <el-button 
                    type="danger" 
                    link 
                    size="small" 
                    @click="handleDeleteItem(scope.row)"
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
              layout="total, sizes, prev, pager, next, jumper"
              :total="total"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 网格视图 (第二层) -->
    <div v-if="showAnnotationView && !showDetailAnnotation" class="annotation-view-container">
      <!-- 使用网格视图组件 -->
      <AnnotationGridView
        :annotation-data="{
          ...currentAnnotationData,
          uploadedImages: uploadedImages,
          annotationLabels: annotationLabels
        }"
        @back-to-list="handleBackToList"
        @start-annotation="handleStartAnnotation"
        @images-imported="handleImagesImported"
        @add-label="handleAddLabelFromGrid"
        @update-label="handleUpdateLabelFromGrid"
        @delete-label="handleDeleteLabelFromGrid"
        @batch-upload-images="handleBatchUploadFromGrid"
        @import-annotation-zip="handleImportAnnotationZipFromGrid"
        @delete-image-and-data="handleDeleteImageAndDataFromGrid"
      />
    </div>

    <!-- 详细标注页面 (第三层) - 完整的标注界面 -->
    <div v-if="showDetailAnnotation" class="annotation-view-grid">
      <div class="grid-container">
        <!-- 左侧标签面板 -->
        <div class="left-sidebar">
          <AnnotationLabelPanel
            :labels="annotationLabels"
            :selected-label-id="selectedAnnotationLabel"
            @label-selected="selectAnnotationLabel"
            @add-label="handleAddAnnotationLabel"
            @edit-label="handleEditAnnotationLabel"
            @delete-label="handleDeleteAnnotationLabel"
          />
        </div>

        <!-- 右侧图像区域 -->
        <div class="image-annotation-area">
          <!-- 保存按钮 - 顶部单独一行 -->
          <div class="save-annotation-section">
            <div class="save-buttons-left">
              <el-button
                type="primary"
                @click="handleSaveAnnotation"
                class="save-annotation-btn"
              >
                <el-icon><Download /></el-icon>
                保存当前标注
              </el-button>
            </div>
            <div class="save-buttons-right">
              <el-button @click="handleBackToGrid" type="primary" plain>
                <el-icon><ArrowLeft /></el-icon>
                返回网格
              </el-button>
              <el-button
                type="warning"
                @click="handleClearAllImages"
                class="clear-all-btn"
                :disabled="uploadedImages.length === 0"
              >
                <el-icon><Delete /></el-icon>
                清空所有图片
              </el-button>
            </div>
          </div>

          <!-- 主要编辑区域 -->
          <div class="image-edit-main">
            <!-- 左侧工具栏 -->
            <div class="annotation-toolbar-left">
              <div class="tool-group-vertical">
                <el-button
                  class="tool-btn-left"
                  :class="{ active: selectedTool === 'rect' }"
                  @click="selectTool('rect')"
                  title="矩形标注"
                >
                  <el-icon><Crop /></el-icon>
                </el-button>
                <el-button
                  class="tool-btn-left"
                  :class="{ active: selectedTool === 'circle' }"
                  @click="selectTool('circle')"
                  title="圆形标注"
                >
                  <el-icon><CirclePlus /></el-icon>
                </el-button>
                <el-button
                  class="tool-btn-left"
                  @click="handleZoomIn"
                  title="放大"
                >
                  <el-icon><ZoomIn /></el-icon>
                </el-button>
                <el-button
                  class="tool-btn-left"
                  @click="handleZoomOut"
                  title="缩小"
                >
                  <el-icon><ZoomOut /></el-icon>
                </el-button>
                <el-button
                  class="tool-btn-left"
                  @click="handleRotateLeft"
                  title="逆时针旋转90°"
                >
                  <el-icon><RefreshLeft /></el-icon>
                </el-button>
                <el-button
                  class="tool-btn-left"
                  @click="handleRotateRight"
                  title="顺时针旋转90°"
                >
                  <el-icon><RefreshRight /></el-icon>
                </el-button>
                <el-button
                  class="tool-btn-left"
                  @click="handleDeleteAnnotation"
                  title="删除标注"
                >
                  <el-icon><Delete /></el-icon>
                </el-button>
                <el-button
                  class="tool-btn-left"
                  @click="handleFullScreenPreview"
                  title="全屏预览"
                >
                  <el-icon><FullScreen /></el-icon>
                </el-button>
              </div>
            </div>

            <!-- 图片与上传区域 -->
            <div class="image-content-area">
              <div class="image-canvas-wrapper">
                <!-- 左侧切换按钮 -->
                <div class="image-nav-button image-nav-left"
                     v-if="uploadedImages.length > 1"
                     @click="previousImage"
                     :class="{ disabled: currentImageIndex === 0 }">
                  <el-icon><ArrowLeft /></el-icon>
                </div>

                <!-- 右侧切换按钮 -->
                <div class="image-nav-button image-nav-right"
                     v-if="uploadedImages.length > 1"
                     @click="nextImage"
                     :class="{ disabled: currentImageIndex === uploadedImages.length - 1 }">
                  <el-icon><ArrowRight /></el-icon>
                </div>

                <div class="image-canvas"
                :data-tool="selectedTool"
                @click="handleCanvasClick"
                @contextmenu.prevent="handleCanvasRightClick"
                @mousedown="handleMouseDown"
                @mousemove="handleMouseMove"
                @mouseup="handleMouseUp"
              >
                <!-- 真实图片显示 -->
                <div v-if="currentImage" class="image-container"
                  :style="{ transform: `rotate(${imageRotation}deg)` }"
                >
                  <img
                    :src="currentImage.url"
                    :alt="currentImage.name"
                    class="main-image"
                    @load="handleImageLoad"
                    @error="handleImageError"
                    draggable="false"
                  />

                  <!-- SVG标注层 -->
                  <svg
                    class="annotation-overlay"
                    xmlns="http://www.w3.org/2000/svg"
                    :viewBox="annotationViewBox"
                    preserveAspectRatio="xMidYMid meet"
                  >
                    <!-- 已有标注 -->
                    <g v-for="annotation in imageAnnotations" :key="annotation.id" class="annotation-group">
                      <rect
                        v-if="annotation.type === 'rect'"
                        :x="annotation.x"
                        :y="annotation.y"
                        :width="annotation.width"
                        :height="annotation.height"
                        :fill="convertToRgba(annotation.labelColor, 0.2)"
                        :stroke="annotation.labelColor"
                        stroke-width="2"
                        :class="{ selected: selectedAnnotation === annotation.id }"
                        @click="selectAnnotation(annotation.id)"
                      />

                      <circle
                        v-if="annotation.type === 'circle'"
                        :cx="annotation.cx"
                        :cy="annotation.cy"
                        :r="annotation.r"
                        :fill="convertToRgba(annotation.labelColor, 0.2)"
                        :stroke="annotation.labelColor"
                        stroke-width="2"
                        :class="{ selected: selectedAnnotation === annotation.id }"
                        @click="selectAnnotation(annotation.id)"
                      />

                      <!-- 标签文本显示 -->
                      <text
                        v-if="annotation.labelName"
                        :x="annotation.type === 'rect' ? annotation.x + annotation.width/2 : annotation.cx"
                        :y="annotation.type === 'rect' ? annotation.y + annotation.height/2 + 5 : annotation.cy + 5"
                        text-anchor="middle"
                        :fill="annotation.labelColor"
                        font-size="14"
                        font-weight="bold"
                        font-family="Arial"
                        stroke="white"
                        stroke-width="2"
                        paint-order="stroke fill"
                      >
                        {{ annotation.labelName }}
                      </text>
                    </g>

                    <!-- 正在绘制的标注 -->
                    <g v-if="currentDrawing" class="drawing-annotation">
                      <rect
                        v-if="selectedTool === 'rect'"
                        :x="Math.min(currentDrawing.startX, currentDrawing.endX)"
                        :y="Math.min(currentDrawing.startY, currentDrawing.endY)"
                        :width="Math.abs(currentDrawing.endX - currentDrawing.startX)"
                        :height="Math.abs(currentDrawing.endY - currentDrawing.startY)"
                        fill="rgba(64, 158, 255, 0.2)"
                        stroke="#409eff"
                        stroke-width="2"
                        stroke-dasharray="5,5"
                      />

                      <circle
                        v-if="selectedTool === 'circle'"
                        :cx="currentDrawing.startX"
                        :cy="currentDrawing.startY"
                        :r="Math.sqrt(Math.pow(currentDrawing.endX - currentDrawing.startX, 2) + Math.pow(currentDrawing.endY - currentDrawing.startY, 2))"
                        fill="rgba(64, 158, 255, 0.2)"
                        stroke="#409eff"
                        stroke-width="2"
                        stroke-dasharray="5,5"
                      />
                    </g>
                  </svg>
                </div>

                <!-- 无图片占位符 -->
                <div v-else class="no-image-placeholder">
                  <el-icon class="placeholder-icon"><Picture /></el-icon>
                  <p class="placeholder-text">请上传图片开始标注</p>
                  <el-upload
                    :show-file-list="false"
                    :on-change="handleImageUpload"
                    :auto-upload="false"
                    accept="image/*"
                    multiple
                  >
                    <el-button type="primary">
                      <el-icon><Upload /></el-icon>
                      上传图片
                    </el-button>
                  </el-upload>
                </div>
              </div>
              </div>

              <!-- 缩略图导航 - 移到图片区域下方 -->
              <div v-if="uploadedImages.length > 0" class="thumbnail-navigation">
                <div class="thumbnail-container">
                  <div
                    v-for="(image, index) in uploadedImages"
                    :key="index"
                    class="thumbnail-item"
                    :class="{ active: currentImageIndex === index }"
                    @click="switchImage(index)"
                  >
                    <img :src="image.url"
                      :alt="image.name" 
                      class="thumbnail-image" 
                      @error="handleThumbnailError" 
                    />
                    <div class="thumbnail-overlay">
                      <span class="thumbnail-index">{{ index + 1 }}</span>
                      <div class="annotation-indicator" v-if="(image.annotations || []).length > 0">
                        {{ (image.annotations || []).length }}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 右键菜单 -->
    <div
      v-if="showContextMenu"
      class="context-menu"
      :style="{ left: contextMenuX + 'px', top: contextMenuY + 'px' }"
      @click.stop
    >
      <div class="context-menu-header">选择标签</div>
      <div class="context-menu-items">
        <div
          v-for="label in annotationLabels"
          :key="label.id"
          class="context-menu-item"
          @click="selectLabelForAnnotation(label)"
        >
          <div class="label-color-dot" :style="{ backgroundColor: label.color }"></div>
          <span class="label-name">{{ label.name }}</span>
        </div>
      </div>
      <div v-if="annotationLabels.length === 0" class="context-menu-empty">
        暂无标签，请先添加标签
      </div>
    </div>



    <AnnotationAddDialog
      v-model="showAddDialog"
      :initial-data="annotationDialogData"
      :annotation-types="annotationTypes"
      @submit="handleAnnotationDialogSubmit"/>

    <AnnotationLabelDialog
      v-model="showLabelDialog"
      :initial-data="labelDialogData"
      :predefine-colors="predefineColors"
      @submit="handleLabelDialogSubmit"/>

    <!-- 数据集文件校验弹窗 -->
    <el-dialog
      v-model="showDatasetDialog"
      title="数据集文件校验"
      width="600px"
      :before-close="handleDatasetDialogClose"
      class="dataset-validation-dialog"
    >
      <div class="dataset-validation-content">
        <!-- 当前标注项目信息 -->
        <div class="annotation-info">
          <h4>{{ currentAnnotationRow?.name || '未选择' }}</h4>
          <div class="path-info">
            <div class="path-item">
              <span class="label">完整路径：</span>
              <span class="path-value">{{ currentAnnotationRow?.datasetPath || '未设置' }}</span>
            </div>
            <div class="path-item">
              <span class="label">文件名：</span>
              <span class="file-name">{{ extractDatasetFileName(currentAnnotationRow?.datasetPath) }}</span>
            </div>
          </div>
        </div>

        <!-- 校验结果显示 -->
        <div class="validation-section">
          <div class="validation-status" v-if="pathValidationResult">
            <div v-if="pathValidationResult.valid" class="status-success">
              <el-icon><CircleCheck /></el-icon>
              <span>数据集文件存在</span>
            </div>
            <div v-else class="status-error">
              <el-icon><CircleClose /></el-icon>
              <span>数据集文件不存在</span>
            </div>
            <p class="validation-message">{{ pathValidationResult.message }}</p>
          </div>

          <div v-if="validatingPath" class="validation-loading">
            <el-icon class="is-loading"><Loading /></el-icon>
            <p>正在校验数据集文件...</p>
          </div>
        </div>

        <!-- 路径验证结果 -->
        <div class="path-validation-section" v-if="pathValidationResult">
          <div class="section-header">
            <h3>路径验证结果</h3>
            <el-tag :type="pathValidationResult.valid ? 'success' : 'danger'">
              {{ pathValidationResult.valid ? '路径有效' : '路径无效' }}
            </el-tag>
          </div>
          <div class="validation-details">
            <div class="validation-item">
              <span class="label">检测到的图片数量：</span>
              <span class="value">{{ pathValidationResult.imageCount || 0 }}</span>
            </div>
            <div class="validation-item">
              <span class="label">支持的格式：</span>
              <span class="value">{{ pathValidationResult.supportedFormats?.join(', ') || 'jpg, png, jpeg, bmp' }}</span>
            </div>
            <div class="validation-item" v-if="pathValidationResult.message">
              <span class="label">验证信息：</span>
              <span class="value">{{ pathValidationResult.message }}</span>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="handleDatasetDialogClose">关闭</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 全屏图片预览弹窗 -->
    <el-dialog
      v-model="showFullScreenPreview"
      title="图片全屏预览"
      width="95%"
      :before-close="handleCloseFullScreenPreview"
      class="fullscreen-preview-dialog"
      :show-close="false"
      :close-on-click-modal="true"
      :close-on-press-escape="true"
    >
      <div class="fullscreen-preview-content" v-if="currentImage">
        <!-- 顶部工具栏 -->
        <div class="preview-toolbar">
          <div class="toolbar-left">
            <span class="image-info">
              {{ currentImage.name }} ({{ currentImageIndex + 1 }} / {{ uploadedImages.length }})
            </span>
          </div>
          <div class="toolbar-right">
            <el-button
              size="small"
              @click="previousImageInPreview"
              :disabled="currentImageIndex <= 0"
              title="上一张 (←)"
            >
              <el-icon><ArrowLeft /></el-icon>
            </el-button>
            <el-button
              size="small"
              @click="nextImageInPreview"
              :disabled="currentImageIndex >= uploadedImages.length - 1"
              title="下一张 (→)"
            >
              <el-icon><ArrowRight /></el-icon>
            </el-button>
            <el-button
              size="small"
              @click="handleCloseFullScreenPreview"
              title="关闭 (ESC)"
            >
              关闭
            </el-button>
          </div>
        </div>

        <!-- 图片显示区域 -->
        <div class="preview-image-container">
          <img
            :src="currentImage.url"
            :alt="currentImage.name"
            class="preview-image"
            @load="handlePreviewImageLoad"
          />

          <!-- 标注层 -->
          <svg
            class="preview-annotation-overlay"
            xmlns="http://www.w3.org/2000/svg"
            v-if="showAnnotationsInPreview"
          >
            <g v-for="annotation in imageAnnotations" :key="annotation.id" class="annotation-group">
              <rect
                v-if="annotation.type === 'rect'"
                :x="annotation.x * previewImageScale"
                :y="annotation.y * previewImageScale"
                :width="annotation.width * previewImageScale"
                :height="annotation.height * previewImageScale"
                :fill="convertToRgba(annotation.labelColor, 0.2)"
                :stroke="annotation.labelColor"
                stroke-width="2"
              />
              <circle
                v-else-if="annotation.type === 'circle'"
                :cx="annotation.cx * previewImageScale"
                :cy="annotation.cy * previewImageScale"
                :r="annotation.radius * previewImageScale"
                :fill="convertToRgba(annotation.labelColor, 0.2)"
                :stroke="annotation.labelColor"
                stroke-width="2"
              />
            </g>
          </svg>
        </div>

        <!-- 底部控制栏 -->
        <div class="preview-controls">
          <el-switch
            v-model="showAnnotationsInPreview"
            active-text="显示标注"
            inactive-text="隐藏标注"
          />
        </div>
      </div>
    </el-dialog>

    <!-- 导入图片弹窗 -->
    <el-dialog
      v-model="showImportDialog"
      title="导入图片"
      width="800px"
      :before-close="handleCloseImportDialog"
    >
      <div class="import-dialog-content">
        <div class="import-form">
          <div class="form-item">
            <label class="form-label">标注状态</label>
            <div class="radio-group">
              <el-radio v-model="importForm.annotationStatus" label="none">
                无标注信息
              </el-radio>
              <el-radio v-model="importForm.annotationStatus" label="exist">
                有标注信息
              </el-radio>
            </div>
          </div>

          <div class="form-item">
            <label class="form-label">导入路径 <span class="required">*</span></label>
            <div class="path-selector">
              <el-button @click="handleSelectDirectory" class="select-btn">
                <el-icon><Folder /></el-icon>
                选择目录
              </el-button>
              <span v-if="importForm.directoryPath" class="selected-path">
                已选择：{{ importForm.directoryPath }}
              </span>
            </div>
            <div class="import-tips">
              <el-icon class="tip-icon"><InfoFilled /></el-icon>
              <div class="tip-content">
                <p>提示：1.导入的图像及文件夹标签数据，以免影响图像标记工作，重复出现功能已单独设置；</p>
                <p>2.每次导入符合允许检测的同一目录，如果组织标记会自动分发功能，可能影响下层已经选则的函数检测时间。</p>
                <div class="link-wrapper">
                  <a href="#" class="help-link">实例分享训练数据集（coco格式）</a>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleCloseImportDialog">取消</el-button>
          <el-button type="primary" @click="handleConfirmImport">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import {computed, onMounted, onUnmounted, ref, watch} from 'vue'
import ActionButtonGroup from '@/components/ActionButtonGroup.vue'
import AnnotationGridView from './AnnotationGridView.vue'
import AnnotationLabelPanel from '@/components/AnnotationLabelPanel.vue'
import AnnotationAddDialog from './components/AnnotationAddDialog.vue'
import AnnotationLabelDialog from './components/AnnotationLabelDialog.vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {
  ArrowLeft,
  ArrowRight,
  CircleCheck,
  CircleClose,
  CirclePlus,
  Crop,
  Delete,
  Download,
  Folder,
  FullScreen,
  InfoFilled,
  Loading,
  RefreshLeft,
  RefreshRight,
  Upload,
  ZoomIn,
  ZoomOut
} from '@element-plus/icons-vue'
import AdvancedSearch from '@/components/AdvancedSearch.vue'
import {
  ANNOTATION_STATUS,
  ANNOTATION_TYPE_LABELS,
  ANNOTATION_TYPES,
  batchDeleteAlgorithmAnnotation,
  createAlgorithmAnnotation,
  deleteAlgorithmAnnotation,
  deleteImage,
  exportAnnotationData,
  importAnnotationData,
  getAlgorithmAnnotationPage,
  getProgressPercentage,
  getStatusTagType,
  updateAlgorithmAnnotation
} from '@/api/algorithmAnnotation'

// 导入标注标签API
import {
  createAnnotationLabel,
  deleteAnnotationLabel,
  getAnnotationLabels,
  updateAnnotationLabel
} from '@/api/annotationLabel'
import {
  batchDeleteAnnotationInstances,
  batchSaveAnnotationInstances,
  deleteAnnotationInstance,
  deleteAnnotationInstancesByImage,
  getAllAnnotationInstances,
} from '@/api/annotationInstances'
import request, {getBaseURL, imageUploadRequest} from '@/utils/request'
import {batchSaveAnnotationImages, getAnnotationImages} from '@/api/annotationImage'

// 视图控制
const showAnnotationView = ref(false)
const currentAnnotationData = ref(null)
const showDetailAnnotation = ref(false) // 是否显示详细标注页面
const currentAnnotationImage = ref(null) // 当前标注的图片

// 搜索表单
const searchForm = ref({
  keyword: '',
  type: '',
  dateRange: []
})

// 表格数据
const tableData = ref([])
const loading = ref(false)

// 分页相关
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 选中行
const selectedRow = ref(null)
const selectedRows = ref([])

// 导入弹窗相关
const showImportDialog = ref(false)
const importForm = ref({
  annotationStatus: 'none',
  directoryPath: ''
})
const importTargetId = ref(null)
const pendingImportZipFile = ref(null)


// 帮助面板展开状态
const expandedSections = ref({
  howToAnnotate: true,
  imageRequirements: false,
  importRequirements: false
})

// 标注视图相关数据
const annotationFilters = ref([
  { key: 'all', label: '全部', count: 8 },
  { key: 'labeled', label: '有标注信息', count: 4 },
  { key: 'unlabeled', label: '无标注信息', count: 4 }
])

const activeAnnotationFilter = ref('all')

// 标签数据（从API获取）
const annotationLabels = ref([])
const labelsLoading = ref(false)

// 数据集文件校验相关
const showDatasetDialog = ref(false)
const currentAnnotationRow = ref(null)
const pathValidationResult = ref(null)
const validatingPath = ref(false)

// 全屏预览相关
const showFullScreenPreview = ref(false)
const showAnnotationsInPreview = ref(true)
const previewImageScale = ref(1)

const selectedAnnotationLabel = ref(1)



// 标签搜索
const labelSearchKeyword = ref('')

// 图像标注编辑相关数据
const currentImageIndex = ref(0)
const selectedTool = ref('rect')

// 标签管理相关
const showLabelDialog = ref(false)
const labelDialogData = ref({
  id: null,
  name: '',
  color: '#409EFF'
})

// 预定义颜色
const predefineColors = [
  '#409EFF',
  '#67C23A', 
  '#E6A23C',
  '#F56C6C',
  '#909399',
  '#9C27B0',
  '#FF9800',
  '#4CAF50',
  '#2196F3',
  '#FF5722'
]

// 图片上传相关
const uploadedImages = ref([])
const imageWidth = ref(0)
const imageHeight = ref(0)
const imageNaturalWidth = ref(0)
const imageNaturalHeight = ref(0)
const imageRotation = ref(0) // 图片旋转角度，每次90度

// 计算属性 - 当前图片
const currentImage = computed(() => {
  if (currentImageIndex.value >= 0 && currentImageIndex.value < uploadedImages.value.length) {
    const image = uploadedImages.value[currentImageIndex.value]

    // 确保图片对象有效且URL存在
    if (image && image.url && typeof image.url === 'string') {
      return image
    }
    console.warn('当前图片无效:', image)
    return null
  }
  return null
})

// 标注相关数据（当前显示的标注，来自当前选中的图片）
const imageAnnotations = ref([])
const getAnnotationCanvasSize = () => {
  const annotationWithSize = imageAnnotations.value.find((item) =>
    Number.isFinite(item?.imageWidth) && Number.isFinite(item?.imageHeight)
  )
  const width = annotationWithSize?.imageWidth || imageNaturalWidth.value || imageWidth.value || 0
  const height = annotationWithSize?.imageHeight || imageNaturalHeight.value || imageHeight.value || 0
  return { width, height }
}

const annotationViewBox = computed(() => {
  const { width, height } = getAnnotationCanvasSize()
  const safeWidth = width > 0 ? width : 1
  const safeHeight = height > 0 ? height : 1
  return `0 0 ${safeWidth} ${safeHeight}`
})


const selectedAnnotation = ref(null)

// 绘制相关
const isDrawing = ref(false)
const currentDrawing = ref(null)

// 右键菜单
const showContextMenu = ref(false)
const contextMenuX = ref(0)
const contextMenuY = ref(0)
const pendingAnnotation = ref(null)
const justFinishedDrawing = ref(false) // 防止绘制完成后立即触发click事件

// 计算属性 - 过滤标签（使用服务器端的count数据）
const filteredAnnotationLabels = computed(() => {
  if (!labelSearchKeyword.value.trim()) {
    return annotationLabels.value
  }
  return annotationLabels.value.filter(label => 
    label.name.toLowerCase().includes(labelSearchKeyword.value.toLowerCase())
  )
})



// 新增弹窗相关
const showAddDialog = ref(false)
const annotationDialogData = ref(null)

// 标注类型选项
const annotationTypes = ref([
  {
    value: ANNOTATION_TYPES.IMAGE_CLASSIFICATION,
    label: ANNOTATION_TYPE_LABELS[ANNOTATION_TYPES.IMAGE_CLASSIFICATION],
    iconClass: 'icon-image-classification'
  },
  {
    value: ANNOTATION_TYPES.OBJECT_DETECTION,
    label: ANNOTATION_TYPE_LABELS[ANNOTATION_TYPES.OBJECT_DETECTION],
    iconClass: 'icon-object-detection'
  },
  {
    value: ANNOTATION_TYPES.INSTANCE_SEGMENTATION,
    label: ANNOTATION_TYPE_LABELS[ANNOTATION_TYPES.INSTANCE_SEGMENTATION],
    iconClass: 'icon-instance-segmentation'
  },
  {
    value: ANNOTATION_TYPES.SEMANTIC_SEGMENTATION,
    label: ANNOTATION_TYPE_LABELS[ANNOTATION_TYPES.SEMANTIC_SEGMENTATION],
    iconClass: 'icon-semantic-segmentation'
  }
])

// 工具函数
// 验证图片对象是否有效
const isValidImage = (image) => {
  return image && 
         image.name && 
         typeof image.name === 'string' && 
         image.url && 
         typeof image.url === 'string' &&
         image.url.trim() !== ''
}

// 检查图片是否存在
const checkImageExists = async (imageUrl) => {
  try {
    const response = await fetch(imageUrl, { method: 'HEAD' })
    return response.ok
  } catch (error) {
    console.warn('检查图片存在性失败:', imageUrl, error)
    return false
  }
}

// 将RGB颜色转换为RGBA格式
const convertToRgba = (rgbColor, alpha = 0.2) => {
  if (!rgbColor) return 'rgba(64, 158, 255, 0.2)'
  
  // 如果已经是rgba格式，直接返回
  if (rgbColor.startsWith('rgba')) {
    return rgbColor
  }
  
  // 提取rgb值
  const rgbMatch = rgbColor.match(/rgb\((\d+),\s*(\d+),\s*(\d+)\)/)
  if (rgbMatch) {
    const [, r, g, b] = rgbMatch
    return `rgba(${r}, ${g}, ${b}, ${alpha})`
  }
  
  // 如果是十六进制格式
  if (rgbColor.startsWith('#')) {
    const hex = rgbColor.slice(1)
    const r = parseInt(hex.slice(0, 2), 16)
    const g = parseInt(hex.slice(2, 4), 16)
    const b = parseInt(hex.slice(4, 6), 16)
    return `rgba(${r}, ${g}, ${b}, ${alpha})`
  }
  
  // 默认返回蓝色透明
  return 'rgba(64, 158, 255, 0.2)'
}

// 数据加载
const loadData = async () => {
  loading.value = true
  try {
    const params = {
      current: currentPage.value,
      size: pageSize.value,
      annotationName: searchForm.value.keyword,
      annotationType: searchForm.value.type,
      startTime: searchForm.value.dateRange?.[0],
      endTime: searchForm.value.dateRange?.[1]
    }
    
    const response = await getAlgorithmAnnotationPage(params)
    console.log('API响应:', response)
    if (response.code === 200) {
      tableData.value = response.data.records.map(item => ({
        id: item.id,
        name: item.annotationName,
        remark: item.remark || '-',
        type: ANNOTATION_TYPE_LABELS[item.annotationType],
        status: `${getProgressPercentage(item.annotatedCount, item.totalCount)}%`,
        progress: `${item.annotatedCount}/${item.totalCount}`,
        createTime: item.createTime,
        annotationStatus: item.annotationStatus,
        annotatedCount: item.annotatedCount,
        totalCount: item.totalCount,
        annotationType: item.annotationType,
        datasetPath: item.datasetPath // 添加datasetPath字段
      }))
      total.value = response.data.total
    }
  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// 事件处理
const handleSearch = () => {
  currentPage.value = 1
  loadData()
}

const handleReset = () => {
  searchForm.value = {
    keyword: '',
    type: '',
    dateRange: []
  }
  currentPage.value = 1
  loadData()
}

const handleAdd = () => {
  annotationDialogData.value = {
    id: null,
    name: '',
    remark: '',
    type: '',
    datasetPath: ''
  }
  showAddDialog.value = true
}

const handleEdit = async () => {
  if (selectedRows.value.length === 1) {
    const row = selectedRows.value[0]
    // 填充编辑数据
    annotationDialogData.value = {
      id: row.id,
      name: row.name,
      remark: row.remark === '-' ? '' : row.remark,
      type: row.annotationType,
      datasetPath: row.datasetPath
    }
    showAddDialog.value = true
  } else {
    ElMessage.warning('请选择一行数据进行编辑')
  }
}

const handleImport = () => {
  if (selectedRows.value.length !== 1) {
    ElMessage.warning('请选择一条标注任务进行导入')
    return
  }
  importTargetId.value = selectedRows.value[0].id
  showImportDialog.value = true
  importForm.value = {
    annotationStatus: 'none',
    directoryPath: ''
  }
  pendingImportZipFile.value = null
}
const handleExport = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择要导出的数据')
    return
  }
  
  try {
    for (const row of selectedRows.value) {
      const blob = await exportAnnotationData(row.id)
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `${row.name}_annotation.zip`
      a.click()
      window.URL.revokeObjectURL(url)
    }
    ElMessage.success('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败')
  }
}

const handleDelete = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择要删除的数据')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的${selectedRows.value.length}个标注吗？\n注意：删除后将同时删除标注数据和相关图片文件！`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    if (selectedRows.value.length === 1) {
      await deleteAlgorithmAnnotation(selectedRows.value[0].id)
    } else {
      const ids = selectedRows.value.map(row => row.id)
      await batchDeleteAlgorithmAnnotation(ids)
    }
    
    ElMessage.success('删除成功')
    selectedRows.value = []
    await loadData()
  } catch (error) {
  if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

const handleRowClick = (row) => {
  selectedRow.value = row
}

const handleView = async (row) => {
  console.log('查看与标注', row)
  
  // 清理之前的数据
  uploadedImages.value = []
  annotationLabels.value = []
  imageAnnotations.value = []
  currentImageIndex.value = -1
  selectedAnnotation.value = null
  selectedAnnotationLabel.value = null
  selectedTool.value = 'rect'
  isDrawing.value = false
  currentDrawing.value = null
  showContextMenu.value = false
  pendingAnnotation.value = null
  justFinishedDrawing.value = false
  imageWidth.value = 0
  imageHeight.value = 0
  imageNaturalWidth.value = 0
  imageNaturalHeight.value = 0
  imageRotation.value = 0
  labelSearchKeyword.value = ''
  
  // 切换到标注视图
  currentAnnotationData.value = row
  showAnnotationView.value = true
  
  // 进入标注页面，获取所有标注数据
  console.log('进入标注页面，获取所有标注数据')
  try {
    await loadAllAnnotationData()
  } catch (error) {
    console.error('加载标注数据失败，尝试单独加载标签:', error)
    // 如果加载失败，至少尝试单独加载标签
    try {
      await loadAnnotationLabels()
    } catch (labelError) {
      console.error('单独加载标签也失败:', labelError)
      ElMessage.error('加载标签失败，请刷新页面重试')
    }
  }
}

// 生成数据集文件
const handleSaveDataset = async (row) => {
  const target = row || currentAnnotationData.value
  if (!target?.id) {
    ElMessage.warning('缺少标注任务ID，无法生成数据集')
    return
  }

  try {
    const response = await request.post(`/vlsAlgorithmAnnotation/${target.id}/save-dataset`, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })

    if (response.code === 200) {
      ElMessage.success('标注数据已保存到数据集文件')
      console.log('数据集文件创建成功')
    } else {
      ElMessage.error('保存到数据集文件失败')
      console.error('保存到数据集文件失败:', response.message)
    }
  } catch (error) {
    console.error('生成数据集失败:', error)
    ElMessage.error('生成数据集失败')
  }
}

const handleImportData = (row) => {
  console.log('Import dataset', row)
  showImportDialog.value = true
  importTargetId.value = row?.id || null
  importForm.value = {
    annotationStatus: 'none',
    directoryPath: ''
  }
  pendingImportZipFile.value = null
}
const handleExportData = async (row) => {
  try {
    const blob = await exportAnnotationData(row.id)
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${row.name}_annotation.zip`
    a.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败')
  }
}

const handleDeleteItem = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除标注"${row.name}"吗？\n注意：删除后将同时删除标注数据和相关图片文件！`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await deleteAlgorithmAnnotation(row.id)
    ElMessage.success('删除成功')
    await loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 提取数据集文件名的方法
const extractDatasetFileName = (datasetPath) => {
  if (!datasetPath) return '未设置'

  // 提取文件名，如：//192.168.88.173/data/work/ultralytics_yolov8-main/datasets/vls/1756697884961.yaml
  // 返回：1756697884961.yaml
  const parts = datasetPath.split('/')
  return parts[parts.length - 1] || '未知文件'
}

// 数据集文件校验相关方法
const handleViewDataset = async (row) => {
  console.log('校验数据集文件', row)
  console.log('当前datasetPath:', row.datasetPath)

  // 保存当前行数据
  currentAnnotationRow.value = row

  // 清除之前的验证结果
  pathValidationResult.value = null

  // 显示弹窗
  showDatasetDialog.value = true

  // 自动开始校验
  await handleValidateDatasetPath()
}

// 浏览选择数据集路径
const handleBrowseDatasetPath = () => {
  // 创建隐藏的文件输入元素，支持文件夹选择
  const input = document.createElement('input')
  input.type = 'file'
  input.webkitdirectory = true // 允许选择文件夹
  input.multiple = true

  input.onchange = (event) => {
    const files = Array.from(event.target.files)
    if (files.length > 0) {
      // 获取选择的文件夹路径（取第一个文件的路径去掉文件名）
      const firstFile = files[0]
      const fullPath = firstFile.webkitRelativePath
      const folderPath = fullPath.substring(0, fullPath.lastIndexOf('/'))

      // 设置数据集路径
      datasetPathForm.value.datasetPath = folderPath || firstFile.name.split('/')[0]

      ElMessage.success(`已选择文件夹：${datasetPathForm.value.datasetPath}`)
    }
  }

  input.click()
}

// 校验数据集文件是否存在
const handleValidateDatasetPath = async () => {
  if (!currentAnnotationRow.value?.datasetPath) {
    ElMessage.warning('当前项目没有设置数据集路径')
    return
  }

  const fileName = extractDatasetFileName(currentAnnotationRow.value.datasetPath)
  console.log('校验数据集文件:', fileName)

  validatingPath.value = true
  try {
    // 模拟文件校验（实际项目中应该调用后端API校验文件是否存在）
    await new Promise(resolve => setTimeout(resolve, 1000))

    // 模拟校验结果 - 这里应该调用真实的API
    const isValid = Math.random() > 0.3 // 模拟70%的成功率
    pathValidationResult.value = {
      valid: isValid,
      message: isValid
        ? `数据集文件 ${fileName} 在服务器上存在`
        : `数据集文件 ${fileName} 在服务器上不存在或无法访问`
    }

    ElMessage.success(isValid ? '文件校验成功' : '文件校验失败')
  } catch (error) {
    console.error('校验文件失败:', error)
    ElMessage.error('校验文件失败: ' + error.message)
    pathValidationResult.value = {
      valid: false,
      message: '校验过程中发生错误'
    }
  } finally {
    validatingPath.value = false
  }
}

// 关闭数据集文件校验弹窗
const handleDatasetDialogClose = () => {
  showDatasetDialog.value = false
  currentAnnotationRow.value = null
  pathValidationResult.value = null
}

const handleSizeChange = (val) => {
  pageSize.value = val
  loadData()
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  loadData()
}

// 导入弹窗相关方法
const handleSelectDirectory = () => {
  const isZipImport = importForm.value.annotationStatus === 'exist'
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
        ElMessage.warning('请选择zip文件.')
        pendingImportZipFile.value = null
        return
      }
      pendingImportZipFile.value = zipFile
      importForm.value.directoryPath = zipFile.name
      ElMessage.info(`选择zip文件: ${zipFile.name}`)
      return
    }

    const imageFiles = files.filter(file => file.type.startsWith('image/'))

    if (imageFiles.length === 0) {
      ElMessage.warning('所选文件夹中没有找到图片文件')
      return
    }

    pendingImportZipFile.value = null

    if (files.length > 0) {
      const firstFile = files[0]
      const fullPath = firstFile.webkitRelativePath
      const folderPath = fullPath.substring(0, fullPath.lastIndexOf('/'))
      importForm.value.directoryPath = folderPath || firstFile.name.split('/')[0]
    }

    ElMessage.info(`找到 ${imageFiles.length} 个图片文件，已选择目录：${importForm.value.directoryPath}`)

    const annotationId = importTargetId.value || currentAnnotationData.value?.id
    if (!annotationId) {
      ElMessage.warning('Please select a target annotation first.')
      return
    }
    await batchUploadImages(imageFiles, annotationId)
  }

  input.click()
}
const batchUploadImages = async (files, annotationId) => {
  try {
    if (!annotationId) {
      ElMessage.warning('Please select a target annotation first.')
      return
    }
    console.log('批量上传图片到标注项目:', annotationId, '文件数量:', files.length)

    const uploadedImagesList = []
    const batchSize = 15 // send 5 files per request

    for (let i = 0; i < files.length; i += batchSize) {
      const batch = files.slice(i, i + batchSize)

      // 生成唯一文件名，只保留文件名部分，去掉文件夹路径
      const fileNamePairs = batch.map((file) => {
        const timestamp = Date.now()
        const randomStr = Math.random().toString(36).substr(2, 9)
        const originalFileName = file.name.split('/').pop().split('\\').pop()
        const uniqueFileName = `${timestamp}_${randomStr}_${originalFileName}`
        return { originalFileName, uniqueFileName }
      })

      const formData = new FormData()
      batch.forEach((file, idx) => {
        formData.append('file', file)
        formData.append('fileName', fileNamePairs[idx].uniqueFileName)
      })

      const uploadResponse = await imageUploadRequest({
        url: '/image/upload',
        method: 'post',
        data: formData,
        headers: { 'Content-Type': 'multipart/form-data' }
      })

      if (!uploadResponse?.success && uploadResponse?.code !== 200) {
        throw new Error(uploadResponse.message || '图片上传失败')
      }

      const respList = Array.isArray(uploadResponse?.data)
        ? uploadResponse.data
        : Array.isArray(uploadResponse?.data?.files)
          ? uploadResponse.data.files
          : []

      const mapped = fileNamePairs.map((pair, idx) => {
        const respItem = respList[idx] || uploadResponse?.data || {}
        const finalName = respItem.fileName || pair.uniqueFileName
        return {
          id: Date.now() + Math.random(),
          name: finalName,
          originalName: pair.originalFileName,
          url: `/image/${finalName}`,
          annotations: [],
          isFromUpload: true,
          uploadData: respItem
        }
      })

      uploadedImagesList.push(...mapped)
      ElMessage.info(`已处理 ${Math.min(i + batchSize, files.length)}/${files.length} 个文件`)
    }

    uploadedImages.value.push(...uploadedImagesList)

    ElMessage.success(`批量导入完成！成功导入 ${uploadedImagesList.length} 张图片`)

    // 如果当前没有选中图片，选中第一个
    if (currentImageIndex.value === -1 && uploadedImages.value.length > 0) {
      currentImageIndex.value = 0
    }

  } catch (error) {
    console.error('批量上传图片失败:', error)
    ElMessage.error(`批量导入失败: ${error.message}`)
  }
}

const toggleHelpSection = (section) => {
  expandedSections.value[section] = !expandedSections.value[section]
}

const handleCloseImportDialog = () => {
  showImportDialog.value = false
  importTargetId.value = null
  pendingImportZipFile.value = null
}
const handleConfirmImport = async () => {
  try {
    if (importForm.value.annotationStatus === 'exist') {
      if (!pendingImportZipFile.value) {
        ElMessage.warning('Please select a .zip file.')
        return
      }

      const annotationId = importTargetId.value || currentAnnotationData.value?.id
      if (!annotationId) {
        ElMessage.warning('Please select a target annotation first.')
        return
      }
      const response = await importAnnotationData(annotationId, pendingImportZipFile.value)
      const isSuccess = response?.success || response?.code === 200
      if (!isSuccess) {
        throw new Error(response?.message || 'Zip import failed')
      }

      pendingImportZipFile.value = null
      showImportDialog.value = false
      importTargetId.value = null
      await loadData()
      if (showAnnotationView.value) {
        await loadAllAnnotationData()
      }
      ElMessage.success('Import succeeded.')
      return
    }

    const imagesToSave = uploadedImages.value.filter(img => img.isFromUpload && !img.savedToDb)

    if (imagesToSave.length > 0) {
      const annotationId = importTargetId.value || currentAnnotationData.value?.id
      if (!annotationId) {
        ElMessage.warning('Please select a target annotation first.')
        return
      }
      const savedCount = await saveImagesToDatabase(imagesToSave, annotationId)
      ElMessage.success(`导入成功！已保存 ${savedCount} 张图片信息到数据库`)
    } else {
      ElMessage.success('导入成功')
    }

    showImportDialog.value = false
    importTargetId.value = null
  } catch (error) {
    console.error('Import failed:', error)
    ElMessage.error(`Import failed: ${error.message}`)
  }
}
const saveImagesToDatabase = async (images, annotationId) => {
  console.log('标注项目ID:', annotationId)

  if (!images || images.length === 0) {
    console.log('没有需要保存的图片')
    return 0
  }

  try {
    const payload = images.map((image) => ({
      annotationId: annotationId,
      imageName: image.name, // align with backend DTO
      originalName: image.originalName,
      localPath: image.url,  // align with backend DTO
      fileSize: image.uploadData?.fileSize || 0,
      status: 'PENDING'
    }))

    const response = await batchSaveAnnotationImages(payload)
    const isSuccess = response?.success || response?.code === 200

    if (!isSuccess) {
      throw new Error(response?.message || '批量保存图片失败')
    }

    const respList = Array.isArray(response?.data) ? response.data : []
    const savedCount = respList.length > 0 ? respList.length : images.length

    images.forEach((image, idx) => {
      image.savedToDb = true
      if (respList[idx]?.id) {
        image.dbId = respList[idx].id
      }
    })

    console.log('=== saveImagesToDatabase 结束，成功保存数量:', savedCount, '===')
    return savedCount
  } catch (error) {
    console.error('批量保存图片到数据库失败:', error)
    return 0
  }
}

// 视图切换方法
const showTableView = () => {
  showAnnotationView.value = false
  showDetailAnnotation.value = false  // 确保详细标注页面也被隐藏
  currentAnnotationData.value = null
  currentAnnotationImage.value = null  // 清空当前标注图片
}

const handleBackToList = () => {
  showAnnotationView.value = false
  showDetailAnnotation.value = false
  currentAnnotationData.value = null
  currentAnnotationImage.value = null
}

const handleStartAnnotation = (data) => {
  if (data.type === 'batch') {
    // 批量标注模式
    console.log('开始批量标注:', data.images)

    // 将所有图片添加到标注列表
    uploadedImages.value = [...uploadedImages.value, ...data.images]

    // 设置第一张图片为当前图片
    if (data.images.length > 0) {
      const firstImageIndex = uploadedImages.value.findIndex(img => img.id === data.images[0].id)
      currentImageIndex.value = firstImageIndex >= 0 ? firstImageIndex : 0
      currentAnnotationImage.value = data.images[0]
    }

    ElMessage.success(`已加载 ${data.images.length} 张图片，可以开始批量标注`)
  } else {
    // 单张图片标注模式
    console.log('开始标注图片:', data)
    currentAnnotationImage.value = data

    // 确保图片在uploadedImages中
    const imageIndex = uploadedImages.value.findIndex(img => img.id === data.id)
    if (imageIndex >= 0) {
      currentImageIndex.value = imageIndex
    }
  }

  showDetailAnnotation.value = true
}

// 处理从网格视图导入的图片
const handleImagesImported = (importedImages) => {
  console.log('接收到导入的图片:', importedImages)

  // 将导入的图片添加到当前图片列表
  uploadedImages.value.push(...importedImages)

  // 如果当前没有选中图片，选中第一张导入的图片
  if (currentImageIndex.value === -1 && importedImages.length > 0) {
    currentImageIndex.value = uploadedImages.value.length - importedImages.length
  }

  ElMessage.success(`成功导入 ${importedImages.length} 张图片到标注项目`)
}

// 处理从网格视图添加标签
const handleAddLabelFromGrid = async (labelData) => {
  try {
    await createLabelAPI(labelData)
  } catch (error) {
    console.error('从网格视图添加标签失败:', error)
  }
}

// 处理从网格视图更新标签
const handleUpdateLabelFromGrid = async (labelData) => {
  try {
    await updateLabelAPI(labelData.id, labelData)
  } catch (error) {
    console.error('从网格视图更新标签失败:', error)
  }
}

// 处理从网格视图删除标签
const handleDeleteLabelFromGrid = async (labelId) => {
  try {
    await deleteLabelAPI(labelId)
  } catch (error) {
    console.error('从网格视图删除标签失败:', error)
  }
}

// 处理从网格视图删除图片及相关数据
const handleDeleteImageAndDataFromGrid = async ({ annotationId, imageIds }) => {
  console.log('=== 父组件收到删除图片及相关数据的请求 ===')

  try {
    console.log('准备调用 deleteAnnotationInstancesByImage API')

    const response = await deleteAnnotationInstancesByImage(annotationId, imageIds)

    console.log('API响应:', response)

    if (response && response.code === 200) {
      console.log('删除图片及相关数据成功:', imageIds)
      // 不在这里显示成功消息，让子组件处理
      // ElMessage.success('图片及相关标注数据删除成功')
    } else {
      console.error('API返回错误:', response)
      throw new Error(response?.message || '删除失败')
    }
  } catch (error) {
    console.error('从网格视图删除图片及相关数据失败:', error)
    ElMessage.error('删除图片及相关数据失败: ' + error.message)
    throw error
  }
}

const handleBatchUploadFromGrid = async (files, uploadResp) => {
  try {
    const annotationId = getCurrentAnnotationId()
    if (!annotationId) {
      ElMessage.warning('Missing annotation ID, cannot batch upload')
      return
    }

    const response = uploadResp
    const isSuccess = response?.success || response?.code === 200
    if (!isSuccess) {
      throw new Error(response?.message || 'Batch upload images failed')
    }

    await loadAllAnnotationData()
    ElMessage.success('Batch upload succeeded. Refreshed image list.')
  } catch (error) {
    console.error('Grid batch upload failed:', error)
    ElMessage.error(error?.message || 'Batch upload images failed')
  }
}
const handleImportAnnotationZipFromGrid = async () => {
  try {
    await loadAllAnnotationData()
  } catch (error) {
    console.error('Refresh after zip import failed:', error)
  }
}
const handleAutoSaveToDatabase = async () => {
  try {
    console.log('=== 网格视图自动保存到数据库 ===')

    // 检查是否有已上传但未保存到数据库的图片
    const imagesToSave = uploadedImages.value.filter(img => img.isFromUpload && !img.savedToDb)
    console.log('需要自动保存到数据库的图片:', imagesToSave)

    if (imagesToSave.length > 0) {
      const annotationId = getCurrentAnnotationId()
      console.log('自动保存，当前标注项目ID:', annotationId)

      const savedCount = await saveImagesToDatabase(imagesToSave, annotationId)
      console.log('自动保存完成，成功保存数量:', savedCount)

      ElMessage.success(`自动保存完成！已保存 ${savedCount} 张图片信息到数据库`)
    } else {
      console.log('没有需要自动保存到数据库的图片')
    }
  } catch (error) {
    console.error('自动保存到数据库失败:', error)
    ElMessage.error(`自动保存失败: ${error.message}`)
  }
}

const handleBackToGrid = () => {
  showDetailAnnotation.value = false
  currentAnnotationImage.value = null
}

// 标注视图相关方法
const selectAnnotationLabel = (labelId) => {
  selectedAnnotationLabel.value = labelId
}

const handleAddAnnotationLabel = (labelData) => {
  // 新组件传递的是标签数据对象
  if (labelData && labelData.name) {
    // 处理新增标签
    addAnnotationLabel(labelData)
  } else {
    // 兼容原有调用方式
    labelDialogData.value = {
      id: null,
      name: '',
      color: '#409EFF'
    }
    showLabelDialog.value = true
  }
}

const handleEditAnnotationLabel = (labelData) => {
  // 新组件传递的是标签数据对象
  if (labelData && labelData.id) {
    // 处理编辑标签
    updateAnnotationLabel(labelData)
  } else {
    // 兼容原有调用方式
    labelDialogData.value = {
      id: labelData?.id ?? null,
      name: labelData?.name || '',
      color: labelData?.color || '#409EFF'
    }
    showLabelDialog.value = true
  }
}

const handleDeleteAnnotationLabel = async (labelId) => {
  try {
    await deleteLabelAPI(labelId)
    // 重新加载标签列表
    await loadAnnotationLabels()
  } catch (error) {
    console.error('删除标签失败:', error)
  }
}

// API相关函数

// 获取当前标注项目ID
const getCurrentAnnotationId = () => {
  const id = currentAnnotationData.value?.id || 1
  console.log('getCurrentAnnotationId 调用:')
  console.log('- currentAnnotationData.value:', currentAnnotationData.value)
  console.log('- 返回的ID:', id)
  return id
}

// 加载标签数据
const loadAnnotationLabels = async (keyword = '') => {
  try {
    labelsLoading.value = true
    const annotationId = getCurrentAnnotationId()
    const response = await getAnnotationLabels(annotationId, keyword)
    
    if (response.code === 200) {
      annotationLabels.value = response.data || []
      // 如果有第一个标签，默认选中
      if (annotationLabels.value.length > 0 && !selectedAnnotationLabel.value) {
        selectedAnnotationLabel.value = annotationLabels.value[0].id
      }
    } else {
      ElMessage.error('加载标签失败: ' + response.message)
    }
  } catch (error) {
    console.error('加载标签失败:', error)
    ElMessage.error('加载标签失败')
  } finally {
    labelsLoading.value = false
  }
}

// 创建标签
const createLabelAPI = async (labelData) => {
  try {
    const annotationId = getCurrentAnnotationId()
    const response = await createAnnotationLabel(annotationId, labelData)
    
    if (response.code === 200) {
      ElMessage.success('标签创建成功')
      // 重新加载标签列表
      await loadAnnotationLabels(labelSearchKeyword.value)
      return response.data
    } else {
      ElMessage.error('创建标签失败: ' + response.message)
      throw new Error(response.message)
    }
  } catch (error) {
    console.error('创建标签失败:', error)
    ElMessage.error('创建标签失败')
    throw error
  }
}

// 新增标签（调用 createAnnotationLabel API）
const addAnnotationLabel = async (labelData) => {
  if (!labelData?.name) {
    ElMessage.warning('标签名称不能为空')
    return null
  }

  try {
    const annotationId = getCurrentAnnotationId()
    const response = await createAnnotationLabel(annotationId, labelData)

    if (response?.code === 200) {
      const created = response.data
      await loadAnnotationLabels(labelSearchKeyword.value)
      if (created?.id) {
        selectedAnnotationLabel.value = created.id
      }
      ElMessage.success('新增标签成功')
      return created
    }

    throw new Error(response?.message || '新增标签失败')
  } catch (error) {
    console.error('新增标签失败:', error)
    ElMessage.error('新增标签失败')
    return null
  }
}

// 更新标签
const updateLabelAPI = async (labelId, labelData) => {
  try {
    const response = await updateAnnotationLabel(labelId, labelData)
    
    if (response.code === 200) {
      ElMessage.success('标签更新成功')
      // 重新加载标签列表
      await loadAnnotationLabels(labelSearchKeyword.value)
      return response.data
    } else {
      ElMessage.error('更新标签失败: ' + response.message)
      throw new Error(response.message)
    }
  } catch (error) {
    console.error('更新标签失败:', error)
    ElMessage.error('更新标签失败')
    throw error
  }
}

// 删除标签
const deleteLabelAPI = async (labelId) => {
  try {
    const response = await deleteAnnotationLabel(labelId)
    
    if (response.code === 200) {
      ElMessage.success('标签删除成功')
      // 重新加载标签列表
      await loadAnnotationLabels(labelSearchKeyword.value)
      return true
    } else {
      ElMessage.error('删除标签失败: ' + response.message)
      throw new Error(response.message)
    }
  } catch (error) {
    console.error('删除标签失败:', error)
    ElMessage.error('删除标签失败')
    throw error
  }
}

// 优化：获取真实的标签信息和标注数据
const loadAllAnnotationData = async () => {
  try {
    const annotationId = getCurrentAnnotationId()
    console.log('=== 加载所有标注数据（带真实标签名称） ===')
    console.log('标注项目ID:', annotationId)
    
    // 并行调用三个API：获取标签信息、图片列表和标注实例
    const [labelsResponse, imagesResponse, instancesResponse] = await Promise.all([
      getAnnotationLabels(annotationId),
      getAnnotationImages(annotationId),
      getAllAnnotationInstances(annotationId)
    ])
    // 记录图片元信息，便于按 imageId 反查名称/URL
    const imageMetaMap = new Map()
    
    console.log('标签API响应:', labelsResponse)
    console.log('图片列表API响应:', imagesResponse)
    console.log('标注实例API响应:', instancesResponse)
    
    // 详细分析图片列表API响应
    if (imagesResponse.success && imagesResponse.data) {
      console.log('图片列表数据结构分析:')
      console.log('- 响应状态:', imagesResponse.success)
      console.log('- 数据长度:', imagesResponse.data.length)
      console.log('- 第一条数据示例:', imagesResponse.data[0])
      console.log('- 所有图片名称:', imagesResponse.data.map(img => img.originalName))
    }

    // 处理标签数据（即使实例API失败也要显示标签）
    if (labelsResponse.code === 200) {
      const labelsList = labelsResponse.data || []
      const instances = instancesResponse.code === 200 ? instancesResponse.data || [] : []
      
      console.log('标签数量:', labelsList.length)
      console.log('标注实例数量:', instances.length)
      
      // 1. 显示所有标签，并重新计算使用次数
      labelsList.forEach(label => {
        label.usageCount = instances.filter(instance => instance.labelId === label.id).length
      })
      
      annotationLabels.value = labelsList.sort((a, b) => a.sortOrder - b.sortOrder)
      console.log('所有标签列表:', annotationLabels.value)

      // 如果有第一个标签，默认选中
      if (annotationLabels.value.length > 0 && !selectedAnnotationLabel.value) {
        selectedAnnotationLabel.value = annotationLabels.value[0].id
      }
    } else {
      console.error('加载标签失败:', labelsResponse.message)
      ElMessage.error('加载标签失败: ' + labelsResponse.message)
    }

    // 处理图片列表数据（从annotation_image表）
    if (imagesResponse.success && imagesResponse.data) {
      const allImages = imagesResponse.data
      console.log('从数据库加载的图片数量:', allImages.length)
      console.log('图片数据详情:', allImages)

      // 为每个图片创建前端显示对象
      allImages.forEach((dbImage, index) => {
        console.log(`处理第${index + 1}张图片:`, dbImage)
        
        // 检查图片名称是否存在且有效 - 使用imageName字段（完整文件名）
        if (!dbImage.imageName || typeof dbImage.imageName !== 'string') {
          console.warn('跳过无效的图片数据:', dbImage)
          return
        }
        const metaName = dbImage.imageName || dbImage.originalName
        const metaUrl = dbImage.localPath
        imageMetaMap.set(dbImage.id, { name: metaName, originalName: dbImage.originalName, url: metaUrl })


        // 检查是否已经存在该图片
        const existingImageIndex = uploadedImages.value.findIndex(
          img => img.id === dbImage.id || img.name === dbImage.imageName || img.originalName === dbImage.imageName
        )

        if (existingImageIndex === -1) {
          // 创建新的图片对象
          const imageObj = {
            id: dbImage.id,
            name: dbImage.imageName,  // 使用完整文件名（包含时间戳前缀）
            originalName: dbImage.originalName,  // 保留原始文件名
            url: metaUrl,
            annotations: [],
            isFromDatabase: true,
            dbData: dbImage,
            // 简化状态标记
            needsImageCheck: false,
            imageExists: true
          }
          uploadedImages.value.push(imageObj)
          console.log('添加数据库图片:', imageObj.name, 'URL:', imageObj.url)
        } else {
          console.log('图片已存在，跳过:', dbImage.imageName)
        }
      })
      
      // 过滤掉无效的图片
      uploadedImages.value = uploadedImages.value.filter(isValidImage)
      console.log('图片列表过滤后的有效图片数量:', uploadedImages.value.length)
      
      console.log('=== 图片URL设置完成 ===')
      uploadedImages.value.forEach((img, index) => {
        console.log(`图片${index + 1}:`, {
          name: img.name,
          originalName: img.originalName,
          url: img.url,
          isValid: isValidImage(img),
          imageExists: img.imageExists
        })
      })
      console.log('=== 图片列表处理完成 ===')
    } else {
      console.warn('图片列表API响应无效:', imagesResponse)
    }

    // 处理标注实例数据
    if (instancesResponse.code === 200) {
      const instances = instancesResponse.data || []
      
      // 2. 按图片名称分组标注数据
      const imageGroups = {}
      instances.forEach((instance, index) => {
        console.log(`处理标注实例 ${index + 1}:`, instance)
        
        // 检查标注实例的图片名称字段 - 可能是imageName或originalName
        const meta = imageMetaMap.get(instance.imageId) || {}
        const instanceImageName = meta.name || instance.imageName || instance.originalName
        if (!instanceImageName || typeof instanceImageName !== 'string') {
          console.warn('跳过无效的标注实例（缺少图片名称）:', instance)
          return
        }
        
        if (!imageGroups[instanceImageName]) {
          imageGroups[instanceImageName] = []
        }
        imageGroups[instanceImageName].push(instance)
      })
      
      console.log('按图片分组的标注数据:', imageGroups)
      
      // 3. 为每个图片创建图片对象
      Object.keys(imageGroups).forEach(imageName => {
        // 检查图片名称是否有效
        if (!imageName || typeof imageName !== 'string') {
          console.warn('跳过无效的图片名称:', imageName)
          return
        }

        const imageInstances = imageGroups[imageName]
        
        // 检查是否已经存在该图片（兼容使用 originalName 保存的老数据）
        const existingImageIndex = uploadedImages.value.findIndex(
          img => img.id === imageInstances?.[0]?.imageId || img.name === imageName || img.originalName === imageName
        )
        
        // 转换标注数据格式
        const annotations = imageInstances.map(instance => {
          let data
          try {
            data = JSON.parse(instance.annotationData)
          } catch (parseError) {
            console.error('解析标注数据失败:', parseError)
            return null
          }
          
          const label = annotationLabels.value.find(l => l.id === instance.labelId)
          
          return {
            id: instance.id,
            labelId: instance.labelId,
            labelName: label?.name || `标签${instance.labelId}`,
            labelColor: label?.color || '#999999',
            type: instance.annotationType,
            ...data
          }
        }).filter(Boolean)
        
        if (existingImageIndex >= 0) {
          // 如果图片已存在，更新其标注数据
          uploadedImages.value[existingImageIndex].annotations = annotations
          console.log(`更新现有图片 ${imageName} 的标注数据，数量:`, annotations.length)
        } else {
          // 如果图片不存在，创建新的图片对象（根据 imageId 反查元数据）
          const meta = imageMetaMap.get(imageInstances?.[0]?.imageId) || {}
          const imageUrl = meta.url
          const newImage = {
            id: imageInstances?.[0]?.imageId || Date.now() + Math.random(),
            name: imageName,
            originalName: meta.originalName || imageName,
            url: imageUrl,
            annotations: annotations,
            isFromDatabase: true, // 标记这是从数据库加载的
            needsImageCheck: true // 标记需要检查图片是否存在
          }
          uploadedImages.value.push(newImage)
          console.log(`添加数据库图片 ${imageName}，URL: ${imageUrl}，标注数量:`, annotations.length)
        }
      })
      
      // 4. 过滤掉无效的图片，只保留有效的图片
      uploadedImages.value = uploadedImages.value.filter(isValidImage)
      console.log('过滤后的有效图片数量:', uploadedImages.value.length)
      
      // 如果当前没有选中图片且有图片数据，选中第一张
      if (currentImageIndex.value < 0 && uploadedImages.value.length > 0) {
        currentImageIndex.value = 0
        imageAnnotations.value = uploadedImages.value[0].annotations || []
      }
      
      console.log('标注实例数据加载完成')
      console.log('- 图片数量:', uploadedImages.value.length)
      console.log('- 当前图片索引:', currentImageIndex.value)
    } else {
      console.error('加载标注实例失败:', instancesResponse.message)
      // 标注实例加载失败不影响标签显示
    }
    
    console.log('所有标注数据加载完成（含真实标签名称）')
    console.log('- 标签数量:', annotationLabels.value.length)
    console.log('- 图片数量:', uploadedImages.value.length)
    console.log('- 当前图片索引:', currentImageIndex.value)
    console.log('- 图片列表详情:', uploadedImages.value.map(img => ({ name: img.name, url: img.url, isFromDatabase: img.isFromDatabase })))
  } catch (error) {
    console.error('加载所有标注数据失败:', error)
    // 如果加载失败，至少尝试单独加载标签
    try {
      await loadAnnotationLabels()
    } catch (labelError) {
      console.error('单独加载标签也失败:', labelError)
      ElMessage.error('加载标签失败')
    }
  }
}

// 保存图片标注实例
const saveImageAnnotations = async (imageId, annotations) => {
  try {
    const annotationId = getCurrentAnnotationId()
    console.log('=== 保存图片标注 ===')
    console.log('标注项目ID:', annotationId)
    console.log('图片ID:', imageId)
    console.log('标注数量:', annotations.length)
    console.log('标注详情:', annotations)
    
    // 转换标注数据格式
    const instances = annotations.map(annotation => ({
      labelId: annotation.labelId,
      imageId: imageId,
      annotationType: annotation.type,
      annotationData: JSON.stringify({
        x: annotation.x,
        y: annotation.y,
        width: annotation.width,
        height: annotation.height,
        cx: annotation.cx,
        cy: annotation.cy,
        r: annotation.r
      })
    }))
    
    console.log('转换后的实例数据:', instances)
    
    const response = await batchSaveAnnotationInstances(annotationId, imageId, instances)
    console.log('保存响应:', response)
    
    if (response.code === 200) {
      ElMessage.success('标注保存成功')
      // 重新加载标签列表以更新使用次数
      await loadAnnotationLabels(labelSearchKeyword.value)
      return true
    } else {
      ElMessage.error('保存标注失败: ' + response.message)
      return false
    }
  } catch (error) {
    console.error('保存标注失败:', error)
    ElMessage.error('保存标注失败')
    return false
  }
}



// 弹窗相关方法
const handleAnnotationDialogSubmit = async (formData) => {
  try {
    const data = {
      annotationName: formData.name,
      annotationType: formData.type,
      remark: formData.remark || null,
      totalCount: 0,
      annotatedCount: 0,
      annotationStatus: ANNOTATION_STATUS.NONE,
      progress: 0,
      annotationRules: null,
      createdBy: 1
    }

    if (formData.id) {
      await updateAlgorithmAnnotation(formData.id, data)
      ElMessage.success('编辑标注成功')
    } else {
      await createAlgorithmAnnotation(data)
      ElMessage.success('新增标注成功')
    }

    showAddDialog.value = false
    selectedRows.value = []
    await loadData()
  } catch (error) {
    console.error('操作失败:', error)
    ElMessage.error('操作失败')
  }
}

// 图像标注编辑相关方法
const selectTool = (tool) => {
  selectedTool.value = tool
}

const handleSaveAnnotation = async () => {
  if (!currentImage.value) {
    ElMessage.warning('请先选择图片')
    return
  }
  
  if (!currentAnnotationData.value || !currentAnnotationData.value.id) {
    ElMessage.warning('请先选择标注项目')
    return
  }

  await saveImageAnnotations(currentImage.value.id, currentImage.value.annotations || [])
}

const handleZoomIn = () => {
  ElMessage.info('放大图像')
}

const handleZoomOut = () => {
  ElMessage.info('缩小图像')
}

const handleResetZoom = () => {
  // 重置旋转角度
  imageRotation.value = 0
  console.log('重置缩放和旋转')
  ElMessage.success('已重置缩放和旋转')
}

const handleRotateLeft = () => {
  // 逆时针旋转90度
  imageRotation.value = (imageRotation.value - 90 + 360) % 360
  
  // 如果有当前图片和标注，需要变换标注坐标
  if (currentImage.value && currentImage.value.annotations.length > 0) {
    transformAnnotationsForRotation(-90)
  }
  
  console.log('逆时针旋转90°，当前角度:', imageRotation.value)
  ElMessage.success('逆时针旋转90°')
}

const handleRotateRight = () => {
  // 顺时针旋转90度
  imageRotation.value = (imageRotation.value + 90) % 360
  
  // 如果有当前图片和标注，需要变换标注坐标
  if (currentImage.value && currentImage.value.annotations.length > 0) {
    transformAnnotationsForRotation(90)
  }
  
  console.log('顺时针旋转90°，当前角度:', imageRotation.value)
  ElMessage.success('顺时针旋转90°')
}

// 旋转时变换标注坐标
const transformAnnotationsForRotation = (angleDelta) => {
  const { width, height } = getAnnotationCanvasSize()
  if (!currentImage.value || !width || !height) return
  
  const centerX = width / 2
  const centerY = height / 2
  const angleRad = (angleDelta * Math.PI) / 180
  
  currentImage.value.annotations.forEach(annotation => {
    if (annotation.type === 'rect') {
      // 矩形标注的四个关键点
      const points = [
        { x: annotation.x, y: annotation.y },
        { x: annotation.x + annotation.width, y: annotation.y },
        { x: annotation.x + annotation.width, y: annotation.y + annotation.height },
        { x: annotation.x, y: annotation.y + annotation.height }
      ]
      
      // 旋转所有点
      const rotatedPoints = points.map(point => {
        const dx = point.x - centerX
        const dy = point.y - centerY
        
        return {
          x: centerX + dx * Math.cos(angleRad) - dy * Math.sin(angleRad),
          y: centerY + dx * Math.sin(angleRad) + dy * Math.cos(angleRad)
        }
      })
      
      // 计算新的边界框
      const minX = Math.min(...rotatedPoints.map(p => p.x))
      const maxX = Math.max(...rotatedPoints.map(p => p.x))
      const minY = Math.min(...rotatedPoints.map(p => p.y))
      const maxY = Math.max(...rotatedPoints.map(p => p.y))
      
      annotation.x = minX
      annotation.y = minY
      annotation.width = maxX - minX
      annotation.height = maxY - minY
      
    } else if (annotation.type === 'circle') {
      // 圆形标注的中心点旋转
      const dx = annotation.cx - centerX
      const dy = annotation.cy - centerY
      
      annotation.cx = centerX + dx * Math.cos(angleRad) - dy * Math.sin(angleRad)
      annotation.cy = centerY + dx * Math.sin(angleRad) + dy * Math.cos(angleRad)
    }
  })
  
  // 刷新显示的标注
  imageAnnotations.value = [...currentImage.value.annotations]
  console.log('标注坐标已旋转变换')
}

// 标签管理方法
const handleLabelDialogSubmit = async (labelData) => {
  try {
    const payload = {
      name: labelData.name,
      color: labelData.color,
      description: ''
    }

    if (labelData.id) {
      await updateLabelAPI(labelData.id, payload)

      // 更新所有图片中使用该标签的标注
      uploadedImages.value.forEach(image => {
        if (image.annotations) {
          image.annotations.forEach(annotation => {
            if (annotation.labelId === labelData.id) {
              annotation.labelName = labelData.name
              annotation.labelColor = labelData.color
            }
          })
        }
      })

      imageAnnotations.value.forEach(annotation => {
        if (annotation.labelId === labelData.id) {
          annotation.labelName = labelData.name
          annotation.labelColor = labelData.color
        }
      })
    } else {
      const newLabel = await createLabelAPI(payload)
      if (newLabel && annotationLabels.value.length === 1) {
        selectedAnnotationLabel.value = newLabel.id
      }
    }

    showLabelDialog.value = false
  } catch (error) {
    console.error('标签操作失败:', error)
  }
}

// 保存图片到本地目录的函数
const saveImageToLocal = async (file, fileName) => {
  try {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('fileName', fileName)
    
    console.log('保存图片到本地:', fileName)
    
    // 调用后端API保存图片 (使用8080端口的图片上传服务)
    const response = await imageUploadRequest({
      url: '/image/upload',
      method: 'POST',
      data: formData,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    
    if (response.success) {
      console.log('图片上传成功:', response.data)
      return response.data.localPath
    } else {
      throw new Error(response.message || '图片上传失败')
    }
  } catch (error) {
    console.error('保存图片到本地失败:', error)
    throw error
  }
}

// 图片上传方法
const handleImageUpload = async (uploadFile) => {
  console.log('上传文件对象:', uploadFile)
  
  // Element Plus 上传组件传递的是包装后的文件对象
  const file = uploadFile.raw || uploadFile
  
  if (!file) {
    ElMessage.error('文件上传失败，请重试')
    return
  }
  
  // 检查文件类型
  if (!file.type.startsWith('image/')) {
    ElMessage.error('请选择图片文件')
    return
  }
  
  // 检查文件大小（10MB限制）
  const maxSize = 10 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error('图片大小不能超过10MB')
    return
  }
  
  try {
    // 生成唯一文件名（避免冲突）
    const timestamp = Date.now()
    const fileExtension = file.name.split('.').pop()
    const uniqueFileName = `${timestamp}_${file.name}`
    
    // 保存图片到本地目录
    const localPath = await saveImageToLocal(file, uniqueFileName)
    
    // 读取图片作为base64用于显示
    const reader = new FileReader()
    reader.onload = (e) => {
      const newImage = {
        id: timestamp,
        name: uniqueFileName,
        originalName: file.name,
        url: e.target.result, // 用于显示的base64
        localPath: localPath, // 本地存储路径
        annotations: [], // 确保每个图片都有annotations数组
        isUploaded: true // 标记为已上传
      }
      console.log('创建新图片对象:', newImage)
      
      uploadedImages.value.push(newImage)
      currentImageIndex.value = uploadedImages.value.length - 1
      
      // 切换到新图片时清空当前标注显示（新图片还没有标注）
      imageAnnotations.value = []
      selectedAnnotation.value = null
      
      // 重置旋转角度
      imageRotation.value = 0
      
      console.log('图片上传成功，当前图片索引:', currentImageIndex.value)
      console.log('当前图片列表长度:', uploadedImages.value.length)
      console.log('当前选中图片:', currentImage.value)
      console.log('图片本地路径:', localPath)
      ElMessage.success('图片上传并保存成功')
    }
    reader.onerror = () => {
      ElMessage.error('图片读取失败')
    }
    reader.readAsDataURL(file)
  } catch (error) {
    console.error('图片上传失败:', error)
    ElMessage.error('图片上传失败，请重试')
  }
}

const switchImage = async (index) => {
  console.log('=== 切换图片（优化版本） ===')
  console.log('切换到索引:', index)
  console.log('原索引:', currentImageIndex.value)

  // 检查索引是否有效
  if (index < 0 || index >= uploadedImages.value.length) {
    console.warn('无效的图片索引:', index)
    return
  }

  // 检查图片是否有效
  const targetImage = uploadedImages.value[index]
  if (!isValidImage(targetImage)) {
    console.warn('目标图片无效:', targetImage)
    return
  }

  currentImageIndex.value = index
  selectedAnnotation.value = null

  // 重置旋转角度
  imageRotation.value = 0
  imageWidth.value = 0
  imageHeight.value = 0
  imageNaturalWidth.value = 0
  imageNaturalHeight.value = 0

  // 优化：直接从已加载的数据中获取标注信息，无需再调用API
  const imageName = targetImage.name
  console.log('切换到图片:', imageName)

  // 直接使用已加载的标注数据
  const annotations = targetImage.annotations || []
  imageAnnotations.value = [...annotations]

  const annotationSize = annotations.find((item) =>
    Number.isFinite(item?.imageWidth) && Number.isFinite(item?.imageHeight)
  )
  if (annotationSize) {
    imageNaturalWidth.value = annotationSize.imageWidth
    imageNaturalHeight.value = annotationSize.imageHeight
  }

  console.log('从缓存加载的标注数量:', annotations.length)
  console.log('imageAnnotations.value:', imageAnnotations.value)
  console.log('=== 切换完成（无API调用） ===')
}

// 上一张图片
const previousImage = () => {
  if (currentImageIndex.value > 0) {
    switchImage(currentImageIndex.value - 1)
  }
}

// 下一张图片
const nextImage = () => {
  if (currentImageIndex.value < uploadedImages.value.length - 1) {
    switchImage(currentImageIndex.value + 1)
  }
}

// 保存所有标注
const handleSaveAllAnnotations = async () => {
  if (uploadedImages.value.length === 0) {
    ElMessage.warning('没有图片需要保存')
    return
  }

  try {
    ElMessage.info('开始保存所有标注...')
    let savedCount = 0
    let errorCount = 0

    for (let i = 0; i < uploadedImages.value.length; i++) {
      const image = uploadedImages.value[i]
      if (image.annotations && image.annotations.length > 0) {
        try {
          // 这里应该调用保存单个图片标注的API
          // await saveImageAnnotations(image.name, image.annotations)
          savedCount++
        } catch (error) {
          console.error(`保存图片 ${image.name} 的标注失败:`, error)
          errorCount++
        }
      }
    }

    if (errorCount === 0) {
      ElMessage.success(`成功保存 ${savedCount} 个图片的标注`)
    } else {
      ElMessage.warning(`保存完成：成功 ${savedCount} 个，失败 ${errorCount} 个`)
    }
  } catch (error) {
    console.error('批量保存标注失败:', error)
    ElMessage.error('批量保存失败: ' + error.message)
  }
}

// 清空所有图片
const handleClearAllImages = () => {
  ElMessageBox.confirm(
    '确定要清空所有图片吗？此操作不可恢复！',
    '确认清空',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(() => {
    uploadedImages.value = []
    currentImageIndex.value = -1
    imageAnnotations.value = []
    selectedAnnotation.value = null
    ElMessage.success('已清空所有图片')
  }).catch(() => {
    // 用户取消操作
  })
}

// 全屏预览功能
const handleFullScreenPreview = () => {
  if (!currentImage.value) {
    ElMessage.warning('请先选择要预览的图片')
    return
  }

  showFullScreenPreview.value = true
}

// 关闭全屏预览
const handleCloseFullScreenPreview = () => {
  showFullScreenPreview.value = false
  previewImageScale.value = 1
}

// 预览中的上一张图片
const previousImageInPreview = () => {
  if (currentImageIndex.value > 0) {
    switchImage(currentImageIndex.value - 1)
  }
}

// 预览中的下一张图片
const nextImageInPreview = () => {
  if (currentImageIndex.value < uploadedImages.value.length - 1) {
    switchImage(currentImageIndex.value + 1)
  }
}

// 预览图片加载完成
const handlePreviewImageLoad = (event) => {
  const img = event.target
  const container = img.parentElement

  // 计算缩放比例以适应容器
  const containerWidth = container.clientWidth
  const containerHeight = container.clientHeight
  const imgWidth = img.naturalWidth
  const imgHeight = img.naturalHeight

  const scaleX = containerWidth / imgWidth
  const scaleY = containerHeight / imgHeight
  previewImageScale.value = Math.min(scaleX, scaleY, 1) // 不超过原始大小

  console.log('预览图片缩放比例:', previewImageScale.value)
}

// 图片加载完成时获取显示尺寸
const handleImageLoad = (event) => {
  const img = event.target
  // 获取图片的实际显示尺寸，而不是原始尺寸
  imageWidth.value = img.clientWidth
  imageHeight.value = img.clientHeight
  imageNaturalWidth.value = img.naturalWidth
  imageNaturalHeight.value = img.naturalHeight
  console.log('图片显示尺寸:', imageWidth.value, imageHeight.value)
  console.log('图片原始尺寸:', img.naturalWidth, img.naturalHeight)
}

// 图片加载错误处理
const handleImageError = (event) => {
  const failedImage = currentImage.value
  console.error('图片加载失败:', failedImage?.url)
  
  if (failedImage) {
    ElMessage.error(`图片加载失败: ${failedImage.name}`)
    
    // 标记当前图片为无效
    failedImage.url = ''
    failedImage.isInvalid = true
    
    // 尝试切换到下一个有效的图片
    const validImages = uploadedImages.value.filter(isValidImage)
    if (validImages.length > 0) {
      const nextImageIndex = uploadedImages.value.findIndex(img => img.id === validImages[0].id)
      if (nextImageIndex >= 0) {
        console.log('切换到下一个有效图片:', validImages[0].name)
        currentImageIndex.value = nextImageIndex
      }
    } else {
      // 如果没有有效图片，清空显示
      currentImageIndex.value = -1
      imageAnnotations.value = []
      ElMessage.warning('没有可用的图片')
    }
  }
}

// 缩略图加载错误处理
const handleThumbnailError = (event) => {
  console.error('缩略图加载失败:', event.target.src)
  // 设置默认占位图片
  event.target.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgZmlsbD0iI2Y1ZjVmNSIvPjx0ZXh0IHg9IjUwIiB5PSI1MCIgZm9udC1mYW1pbHk9IkFyaWFsIiBmb250LXNpemU9IjEyIiBmaWxsPSIjOTk5IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBkeT0iLjNlbSI+5Zu+54mH5LiN5a2Y5ZyoPC90ZXh0Pjwvc3ZnPg=='
}

// 画布事件处理方法
const handleCanvasClick = (event) => {
  if (selectedTool.value === 'select') {
    // 取消选中
    selectedAnnotation.value = null
  }
  hideContextMenu()
}

const handleCanvasRightClick = (event) => {
  if (pendingAnnotation.value) {
    contextMenuX.value = event.clientX
    contextMenuY.value = event.clientY
    showContextMenu.value = true
  }
}

const handleMouseDown = (event) => {
  if (selectedTool.value === 'brush' || selectedTool.value === 'rect' || selectedTool.value === 'circle') {
    // 获取图片元素的位置和尺寸
    const imageEl = document.querySelector('.main-image')
    if (!imageEl) return
    
    const imageRect = imageEl.getBoundingClientRect()
    const xDisplay = event.clientX - imageRect.left
    const yDisplay = event.clientY - imageRect.top
    const { width, height } = getAnnotationCanvasSize()
    if (!width || !height) return
    const scaleX = width ? width / imageRect.width : 1
    const scaleY = height ? height / imageRect.height : 1
    const x = xDisplay * scaleX
    const y = yDisplay * scaleY
    
    // 确保坐标在图片范围内
    if (xDisplay >= 0 && xDisplay <= imageRect.width && yDisplay >= 0 && yDisplay <= imageRect.height) {
      isDrawing.value = true
      currentDrawing.value = {
        startX: x,
        startY: y,
        endX: x,
        endY: y
      }
      console.log('开始绘制:', currentDrawing.value)
    }
  }
}

const handleMouseMove = (event) => {
  if (isDrawing.value && currentDrawing.value) {
    const imageEl = document.querySelector('.main-image')
    if (!imageEl) return
    
    const imageRect = imageEl.getBoundingClientRect()
    const xDisplay = event.clientX - imageRect.left
    const yDisplay = event.clientY - imageRect.top
    const { width, height } = getAnnotationCanvasSize()
    if (!width || !height) return
    const scaleX = width ? width / imageRect.width : 1
    const scaleY = height ? height / imageRect.height : 1
    const x = xDisplay * scaleX
    const y = yDisplay * scaleY
    
    currentDrawing.value.endX = x
    currentDrawing.value.endY = y
    // console.log('绘制中:', currentDrawing.value)
  }
}

const handleMouseUp = (event) => {
  if (isDrawing.value && currentDrawing.value) {
    const drawing = currentDrawing.value
    
    let newAnnotation = null
    
    if (selectedTool.value === 'rect') {
      newAnnotation = {
        id: Date.now(),
        type: 'rect',
        x: Math.min(drawing.startX, drawing.endX),
        y: Math.min(drawing.startY, drawing.endY),
        width: Math.abs(drawing.endX - drawing.startX),
        height: Math.abs(drawing.endY - drawing.startY),
        labelId: null,
        labelName: '',
        labelColor: '#409EFF'
      }
    } else if (selectedTool.value === 'circle') {
      const radius = Math.sqrt(Math.pow(drawing.endX - drawing.startX, 2) + Math.pow(drawing.endY - drawing.startY, 2))
      newAnnotation = {
        id: Date.now(),
        type: 'circle',
        cx: drawing.startX,
        cy: drawing.startY,
        r: radius,
        labelId: null,
        labelName: '',
        labelColor: '#409EFF'
      }
    }
    
    if (newAnnotation && (newAnnotation.width > 10 || newAnnotation.r > 10)) {
      // 阻止事件冒泡，防止立即触发全局click事件隐藏菜单
      event.stopPropagation()
      event.preventDefault()
      
      // 设置防护标志，防止立即的click事件干扰
      justFinishedDrawing.value = true
      setTimeout(() => {
        justFinishedDrawing.value = false
      }, 200) // 200ms后清除标志
      
      pendingAnnotation.value = newAnnotation
      console.log('创建标注:', newAnnotation)
      
      // 延迟显示右键菜单，避免与click事件冲突
      setTimeout(() => {
        // 显示右键菜单选择标签 - 使用页面坐标
        contextMenuX.value = event.clientX
        contextMenuY.value = event.clientY
        showContextMenu.value = true
        console.log('延迟显示右键菜单')
        console.log('右键菜单位置:', { x: contextMenuX.value, y: contextMenuY.value })
        console.log('右键菜单显示状态:', showContextMenu.value)
        console.log('可用标签数量:', annotationLabels.value.length)
        console.log('标签列表:', annotationLabels.value)
      }, 100) // 延迟100ms显示
      
      // 正常流程：显示右键菜单让用户选择标签
    }
    
    isDrawing.value = false
    currentDrawing.value = null
  }
}

// 标注选择和标签分配
const selectAnnotation = (annotationId) => {
  selectedAnnotation.value = annotationId
}

// 删除选中的标注或整个图片
const handleDeleteAnnotation = async () => {
  if (!currentImage.value) {
    ElMessage.warning('没有可删除的图片')
    return
  }

  // 如果有标注，删除标注；如果没有标注，删除整个图片
  if (currentImage.value.annotations && currentImage.value.annotations.length > 0) {
    // 删除标注逻辑
    let annotationToDelete = null
    let annotationIndex = -1

    if (selectedAnnotation.value) {
      // 删除选中的标注
      annotationIndex = currentImage.value.annotations.findIndex(
        annotation => annotation.id === selectedAnnotation.value
      )
      if (annotationIndex > -1) {
        annotationToDelete = currentImage.value.annotations[annotationIndex]
      }
    } else {
      // 删除最后一个标注
      annotationIndex = currentImage.value.annotations.length - 1
      annotationToDelete = currentImage.value.annotations[annotationIndex]
    }

    if (annotationToDelete) {
      try {
        // 确认删除标注
        await ElMessageBox.confirm(
          `确定要删除这个标注吗？\n注意：删除后将同时删除标注数据和相关图片文件！`,
          '提示',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )

        // 调用后端API删除标注实例
        await deleteAnnotationInstance(annotationToDelete.id)
        
        // 从当前图片的标注数组中删除
        currentImage.value.annotations.splice(annotationIndex, 1)
        
        // 刷新当前显示的标注 - 重新从当前图片加载
        imageAnnotations.value = [...currentImage.value.annotations]
        
        // 如果删除的是当前选中的标注，清除选中状态
        if (selectedAnnotation.value === annotationToDelete.id) {
          selectedAnnotation.value = null
        }
        
        ElMessage.success('标注已删除')
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除标注失败:', error)
          ElMessage.error('删除标注失败')
        }
      }
    } else {
      ElMessage.warning('未找到要删除的标注')
    }
  } else {
    // 删除整个图片
    try {
      // 确认删除图片
      await ElMessageBox.confirm(
        `确定要删除这张图片吗？\n注意：删除后将同时删除图片文件和所有相关数据！`,
        '提示',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )

      // 先删除该图片的所有标注实例
      if (currentImage.value.annotations && currentImage.value.annotations.length > 0) {
        console.log(`删除图片 ${currentImage.value.name} 的 ${currentImage.value.annotations.length} 个标注实例`)

        try {
          // 方法1：使用根据图片名称删除的API（更高效）
          if (currentAnnotationData.value && currentAnnotationData.value.id) {
            await deleteAnnotationInstancesByImage(currentAnnotationData.value.id, [currentImage.value.name])
            console.log(`已批量删除图片 ${currentImage.value.name} 的所有标注实例`)
          } else {
            // 方法2：批量删除标注实例
            const instanceIds = currentImage.value.annotations.map(annotation => annotation.id)
            await batchDeleteAnnotationInstances(instanceIds)
            console.log(`已批量删除 ${instanceIds.length} 个标注实例`)
          }
        } catch (error) {
          console.error('批量删除标注实例失败，尝试逐个删除:', error)

          // 方法3：逐个删除（备用方案）
          for (const annotation of currentImage.value.annotations) {
            try {
              await deleteAnnotationInstance(annotation.id)
              console.log(`已删除标注实例: ${annotation.id}`)
            } catch (error) {
              console.error(`删除标注实例 ${annotation.id} 失败:`, error)
            }
          }
        }
      }

      // 然后删除图片文件
      if (currentImage.value.name) {
        await deleteImage(currentImage.value.name)
        console.log(`已删除图片文件: ${currentImage.value.name}`)
      }
      
      // 从图片列表中删除
      const imageIndex = uploadedImages.value.findIndex(img => img.id === currentImage.value.id)
      if (imageIndex > -1) {
        uploadedImages.value.splice(imageIndex, 1)
        
        // 如果删除的是当前图片，切换到上一张图片
        if (currentImageIndex.value >= uploadedImages.value.length) {
          currentImageIndex.value = Math.max(0, uploadedImages.value.length - 1)
        }
        
        // 更新当前图片和标注显示
        if (uploadedImages.value.length > 0) {
          currentImage.value = uploadedImages.value[currentImageIndex.value]
          imageAnnotations.value = [...currentImage.value.annotations]
        } else {
          currentImage.value = null
          imageAnnotations.value = []
        }
        
        selectedAnnotation.value = null
        ElMessage.success('图片已删除')
      } else {
        ElMessage.warning('未找到要删除的图片')
      }
    } catch (error) {
      if (error !== 'cancel') {
        console.error('删除图片失败:', error)
        ElMessage.error('删除图片失败')
      }
    }
  }
}

const selectLabelForAnnotation = (label) => {
  console.log('=== 开始选择标签 ===')
  console.log('pendingAnnotation:', pendingAnnotation.value)
  console.log('currentImage:', currentImage.value)
  console.log('currentImageIndex:', currentImageIndex.value)
  
  if (pendingAnnotation.value && currentImage.value) {
    pendingAnnotation.value.labelId = label.id
    pendingAnnotation.value.labelName = label.name
    pendingAnnotation.value.labelColor = label.color
    
    console.log('标注信息设置完成:', pendingAnnotation.value)
    
    // 确保当前图片有annotations数组
    if (!currentImage.value.annotations) {
      currentImage.value.annotations = []
      console.log('创建了annotations数组')
    }
    
    // 将标注保存到当前图片的annotations数组中
    currentImage.value.annotations.push(pendingAnnotation.value)
    console.log('标注已添加到图片:', currentImage.value.annotations.length)
    
    // 刷新当前显示的标注 - 重新从当前图片加载
    imageAnnotations.value = [...currentImage.value.annotations]
    console.log('刷新显示标注数量:', imageAnnotations.value.length)
    console.log('所有显示标注:', imageAnnotations.value)
    
    pendingAnnotation.value = null
    console.log('=== 标签选择完成 ===')
    
    ElMessage.success(`已添加${label.name}标注`)
  } else {
    console.log('标注保存失败 - 缺少必要条件')
    if (!pendingAnnotation.value) console.log('缺少pendingAnnotation')
    if (!currentImage.value) console.log('缺少currentImage')
  }
  hideContextMenu()
}

const hideContextMenu = () => {
  console.log('hideContextMenu调用，当前菜单状态:', showContextMenu.value)
  
  // 只有菜单真正显示时才隐藏
  if (showContextMenu.value) {
    showContextMenu.value = false
    contextMenuX.value = 0
    contextMenuY.value = 0
    
    // 如果有未完成的标注，清除它
    if (pendingAnnotation.value) {
      console.log('清除未完成的标注')
      pendingAnnotation.value = null
    }
  } else {
    console.log('菜单未显示，不执行清除操作')
  }
}

// 智能的全局点击监听器 - 只在点击菜单外部时隐藏
const handleGlobalClick = (event) => {
  console.log('=== 全局点击事件触发 ===')
  console.log('点击目标:', event.target)
  console.log('右键菜单显示状态:', showContextMenu.value)
  console.log('刚完成绘制标志:', justFinishedDrawing.value)
  console.log('事件类型:', event.type)
  console.log('事件时间戳:', event.timeStamp)
  
  // 如果刚完成绘制，忽略这次点击事件
  if (justFinishedDrawing.value) {
    console.log('刚完成绘制，忽略此次点击事件')
    return
  }
  
  // 如果右键菜单显示中
  if (showContextMenu.value) {
    // 检查点击是否在右键菜单内部
    const contextMenu = document.querySelector('.context-menu')
    console.log('找到右键菜单元素:', contextMenu)
    
    if (contextMenu && !contextMenu.contains(event.target)) {
      // 点击在菜单外部，隐藏菜单
      console.log('点击在菜单外部，隐藏菜单')
      hideContextMenu()
    } else {
      console.log('点击在菜单内部或菜单不存在')
    }
  }
  console.log('=== 全局点击事件处理完成 ===')
}

// 高级搜索相关方法
const handleAdvancedSearch = (searchData) => {
  console.log('高级搜索:', searchData)
  
  // 更新搜索表单
  if (searchData.keyword) {
    searchForm.keyword = searchData.keyword
  }
  if (searchData.annotationName) {
    searchForm.keyword = searchData.annotationName
  }
  if (searchData.annotationType) {
    searchForm.type = searchData.annotationType
  }
  if (searchData.dateRange && searchData.dateRange.length > 0) {
    searchForm.dateRange = searchData.dateRange
  }
  
  handleSearch()
}

const handleAdvancedSearchReset = () => {
  console.log('重置高级搜索')
  searchForm.keyword = ''
  searchForm.type = ''
  searchForm.dateRange = []
  handleReset()
}

const handleDownloadTemplate = () => {
  console.log('下载模板')
  ElMessage.success('下载模板')
}

const handleBatchOperation = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择要删除的标注')
    return
  }

  const ids = selectedRows.value.map(row => row.id)

  try {
    await ElMessageBox.confirm(
      `确认批量删除选中的 ${ids.length} 条标注吗？\n删除后不可恢复，关联的数据也会被移除！`,
      '批量删除',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    if (ids.length === 1) {
      await deleteAlgorithmAnnotation(ids[0])
    } else {
      await batchDeleteAlgorithmAnnotation(ids)
    }

    ElMessage.success('批量删除成功')
    selectedRows.value = []
    await loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除失败:', error)
      ElMessage.error('批量删除失败')
    }
  }
}

// 监听当前图片变化，自动加载标注数据（优化版本：无需API调用）
watch(currentImage, async (newImage, oldImage) => {
  if (newImage && newImage.name && showAnnotationView.value) {
    // 只有在标注视图中且图片有名称时才更新显示
    console.log('currentImage变化，从缓存加载标注数据:', newImage.name)
    
    // 检查是否是同一张图片，避免重复处理
    if (oldImage && newImage.name === oldImage.name) {
      console.log('同一张图片，跳过处理')
      return
    }
    
    // 优化：直接从已加载的数据中获取标注信息
    const annotations = newImage.annotations || []
    imageAnnotations.value = [...annotations]
    
    console.log('从缓存加载的标注数量:', annotations.length)
  }
})

// 键盘事件处理
const handleKeyDown = (event) => {
  // 全屏预览中的键盘事件
  if (showFullScreenPreview.value) {
    switch (event.key) {
      case 'Escape':
        event.preventDefault()
        handleCloseFullScreenPreview()
        break
      case 'ArrowLeft':
        event.preventDefault()
        previousImageInPreview()
        break
      case 'ArrowRight':
        event.preventDefault()
        nextImageInPreview()
        break
    }
    return
  }

  // 只在标注视图中响应键盘事件
  if (!showAnnotationView.value) return

  switch (event.key) {
    case 'ArrowLeft':
      event.preventDefault()
      previousImage()
      break
    case 'ArrowRight':
      event.preventDefault()
      nextImage()
      break
    case 'Delete':
      event.preventDefault()
      if (selectedAnnotation.value) {
        handleDeleteAnnotation()
      }
      break
    case 'F11':
    case 'f':
      event.preventDefault()
      handleFullScreenPreview()
      break
  }
}

// 组件挂载时加载数据
onMounted(() => {
  loadData()
  // 添加全局点击监听器
  document.addEventListener('click', handleGlobalClick)
  // 添加键盘事件监听器
  document.addEventListener('keydown', handleKeyDown)
})

// 组件卸载时清理事件监听器
onUnmounted(() => {
  document.removeEventListener('click', handleGlobalClick)
  document.removeEventListener('keydown', handleKeyDown)
})

// 组件卸载时清理事件监听器
onUnmounted(() => {
  document.removeEventListener('click', handleGlobalClick)
})

const handleCopyLabel = (label) => {
  // 复制标签功能
  const newLabel = {
    id: Date.now(),
    name: `${label.name}_副本`,
    color: label.color,
    usageCount: 0
  }
  annotationLabels.value.push(newLabel)
  console.log('复制标签:', newLabel)
  ElMessage.success(`已复制标签: ${label.name}`)
}

const handleMoreActions = (label) => {
  // 更多操作功能（可以展开菜单等）
  ElMessageBox.confirm(
    `确定要删除标签 "${label.name}" 吗？此操作不可恢复。`,
    '删除标签',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(async () => {
    try {
      // 调用API删除标签
      await deleteLabelAPI(label.id)
      
      // 删除成功后，从本地数组中移除
      const index = annotationLabels.value.findIndex(l => l.id === label.id)
      if (index > -1) {
        annotationLabels.value.splice(index, 1)
      }
      
      // 重新加载标签列表以确保数据同步
      await loadAnnotationLabels()
      
      ElMessage.success('标签已删除')
    } catch (error) {
      console.error('删除标签失败:', error)
      ElMessage.error('删除标签失败')
    }
  }).catch(() => {
    ElMessage.info('已取消删除')
  })
}

// 测试删除标注实例的方法
const testDeleteAnnotationInstance = async (instanceId) => {
  try {
    console.log('开始删除标注实例:', instanceId)
    const response = await deleteAnnotationInstance(instanceId)
    console.log('删除响应:', response)
    ElMessage.success('删除成功')
    return response
  } catch (error) {
    console.error('删除失败:', error)
    ElMessage.error('删除失败: ' + error.message)
    return null
  }
}

// 测试删除图片及相关数据的方法
const testDeleteImageAndRelatedData = async (annotationId, imageName) => {
  try {
    console.log('开始删除图片及相关数据:', annotationId, imageName)
    const response = await deleteAnnotationInstancesByImage(annotationId, [imageName])
    console.log('删除响应:', response)
    ElMessage.success('删除成功')
    return response
  } catch (error) {
    console.error('删除失败:', error)
    ElMessage.error('删除失败: ' + error.message)
    return null
  }
}

// 暴露测试方法到全局
window.testDeleteAnnotation = testDeleteAnnotationInstance
window.deleteAnnotationInstancesByImage = testDeleteImageAndRelatedData
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

.list-view {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0;
}

/* 页面标题 */
.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  font-size: 24px;
  font-weight: 600;
  color: #262626;
  margin: 0 0 8px 0;
}

.page-header p {
  color: #8c8c8c;
  margin: 0;
  font-size: 14px;
}

/* 主内容区域 */
.main-content {
  background: white;
  border-radius: 0 0 8px 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow: hidden;
}

/* 工具栏 */
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

/* 表格内容 */
.table-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
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

/* 操作按钮强制一行展示 */
.action-buttons {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
  flex-wrap: nowrap;
}

/* 分页 */
.table-pagination {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.type-option.active .type-icon {
  background: #409eff;
}

.type-option.active .type-label {
  color: #409eff;
  font-weight: 600;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 导入弹窗样式 */
.import-dialog-content {
  display: flex;
  gap: 24px;
}

.import-form {
  flex: 1;
  min-width: 350px;
}

.form-item {
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
}

.radio-group {
  display: flex;
  gap: 24px;
}

.path-selector {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.select-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 6px;
}

.selected-path {
  font-size: 13px;
  color: #52c41a;
  background: #f6ffed;
  padding: 4px 8px;
  border-radius: 4px;
  border: 1px solid #b7eb8f;
}

.import-tips {
  display: flex;
  gap: 8px;
  padding: 12px;
  background: #f0f9ff;
  border: 1px solid #91d5ff;
  border-radius: 6px;
  margin-top: 12px;
}

.tip-icon {
  color: #1890ff;
  font-size: 16px;
  margin-top: 2px;
  flex-shrink: 0;
}

.tip-content {
  flex: 1;
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

.select-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border: 1px dashed #d9d9d9;
  background: #fafafa;
  color: #666;
  border-radius: 6px;
  transition: all 0.3s;
}

.select-btn:hover {
  border-color: #409eff;
  color: #409eff;
  background: #f0f9ff;
}

.import-tips {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 12px;
  background: #f6f8fa;
  border-radius: 6px;
  border-left: 4px solid #409eff;
}

.tip-icon {
  color: #409eff;
  margin-top: 2px;
  flex-shrink: 0;
}

.tip-content {
  font-size: 13px;
  line-height: 1.5;
  color: #666;
}

.tip-content p {
  margin: 0 0 8px 0;
}

.tip-content p:last-of-type {
  margin-bottom: 8px;
}

.link-wrapper {
  margin-top: 8px;
}

.help-link {
  color: #409eff;
  text-decoration: none;
  font-size: 13px;
}

.help-link:hover {
  text-decoration: underline;
}

.help-content p {
  margin: 0 0 12px 0;
}

.help-content p:last-child {
  margin-bottom: 0;
}

/* 导航栏样式 */
.content-header {
  padding: 16px 20px;
  background: white;
  border-bottom: 1px solid #e8e8e8;
  margin-bottom: 20px;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.breadcrumb-item {
  color: #409eff;
  cursor: pointer;
  transition: color 0.3s;
}

.breadcrumb-item:hover {
  color: #337ecc;
}

.breadcrumb-item.active {
  color: #262626;
  cursor: default;
}

.breadcrumb-separator {
  color: #999;
}

/* 标注视图样式 */
.annotation-view-grid {
  height: 1200px;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
  overflow: hidden;
}

.grid-container {
  height: 1200px;
  display: flex;
  gap: 0;
  overflow: hidden;
}

/* 左侧标签面板 */
.left-sidebar {
  width: 280px;
  background: white;
  border-right: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.label-name {
  font-size: 14px;
  color: #262626;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.label-item:hover .label-action-buttons {
  opacity: 1;
}

.action-btn .el-icon {
  font-size: 12px;
}

/* 保存按钮区域 */
.save-annotation-section {
  padding: 16px 20px;
  background: white;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.save-buttons-left,
.save-buttons-right {
  display: flex;
  gap: 12px;
}

.save-annotation-btn,
.save-all-btn,
.clear-all-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  font-size: 13px;
  border-radius: 6px;
  height: 36px;
  font-weight: 500;
}

/* 主要编辑区域 */
.image-edit-main {
  height: 920px;
  display: flex;
  position: relative;
  overflow: hidden;
}

/* 左侧工具栏样式 */
.annotation-toolbar-left {
  width: 70px;
  background: white;
  border-right: 1px solid #e8e8e8;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 20px 15px;
}

.tool-group-vertical {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tool-btn-left {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 1px solid #e8e8e8;
  background: white;
  color: #666;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

.tool-btn-left:hover {
  border-color: #409eff;
  color: #409eff;
  background: #f0f9ff;
}

.tool-btn-left.active {
  border-color: #409eff;
  color: white;
  background: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
}

.tool-btn-left .el-icon {
  font-size: 18px;
}

/* 图片内容区域 */
.image-content-area {
  height: 800px;
  display: flex;
  flex-direction: column;
  position: relative;
  align-items: flex-start;
  flex-shrink: 0;
}

/* 图片画布包装器 */
.image-canvas-wrapper {
  position: relative;
  flex: 1;
  display: flex;
  flex-direction: column;
}

/* 图片切换按钮 */
.image-nav-button {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 48px;
  height: 48px;
  background: rgba(0, 0, 0, 0.6);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: white;
  font-size: 20px;
  z-index: 10;
  transition: all 0.3s ease;
  backdrop-filter: blur(4px);
}

.image-nav-button:hover {
  background: rgba(0, 0, 0, 0.8);
  transform: translateY(-50%) scale(1.1);
}

.image-nav-button.disabled {
  background: rgba(0, 0, 0, 0.3);
  cursor: not-allowed;
  color: rgba(255, 255, 255, 0.5);
}

.image-nav-button.disabled:hover {
  background: rgba(0, 0, 0, 0.3);
  transform: translateY(-50%);
}

/* 左侧切换按钮 */
.image-nav-left {
  left: 20px;
}

/* 右侧切换按钮 */
.image-nav-right {
  right: 20px;
}

/* 图片画布 */
.image-canvas {
  width: 1040px;
  height: 590px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  padding: 20px;
  position: relative;
  overflow: auto;
  background: #f8f9fa;
  margin: 0;
  flex-shrink: 0;
  box-sizing: border-box;
}

.image-container {
  position: relative;
  display: inline-block;
  max-width: 100%;
  max-height: 100%;
}

.main-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  display: block;
}

/* SVG标注层 */
.annotation-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.annotation-overlay .annotation-group {
  pointer-events: auto;
  cursor: pointer;
}

.annotation-overlay rect.selected,
.annotation-overlay circle.selected {
  stroke-width: 3;
  filter: drop-shadow(0 0 6px rgba(64, 158, 255, 0.6));
}

.drawing-annotation {
  pointer-events: none;
}

/* 无图片占位符 */
.no-image-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #999;
  min-height: 300px;
}

.placeholder-icon {
  font-size: 48px;
  margin-bottom: 16px;
  color: #ddd;
}

.placeholder-text {
  font-size: 16px;
  color: #999;
  margin-bottom: 20px;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.suggestion-list li {
  margin-bottom: 4px;
}

.thumbnail-loading-placeholder .loading-icon {
  font-size: 24px;
  animation: rotate 2s linear infinite;
}

.thumbnail-error-placeholder .error-icon {
  font-size: 24px;
}

/* 缩略图导航 - 位于图片区域下方 */
.thumbnail-navigation {
  height: 120px;
  background: white;
  border-top: 1px solid #f0f0f0;
  padding: 12px 0;
  overflow: hidden;
  width: 1040px;
  margin: 0;
  flex-shrink: 0;
}

.thumbnail-container {
  display: flex;
  gap: 8px;
  height: 100%;
  overflow-x: auto;
  padding: 8px 20px;
}

.thumbnail-item {
  position: relative;
  width: 96px;
  height: 96px;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.3s;
  flex-shrink: 0;
}

.thumbnail-item:hover {
  border-color: #91d5ff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.thumbnail-item.active {
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.thumbnail-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumbnail-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(to bottom, rgba(0,0,0,0.3) 0%, transparent 30%, transparent 70%, rgba(0,0,0,0.3) 100%);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 6px;
}

.thumbnail-index {
  font-size: 12px;
  color: white;
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(0,0,0,0.5);
  align-self: flex-start;
}

.annotation-indicator {
  font-size: 11px;
  color: white;
  background: #409eff;
  padding: 2px 6px;
  border-radius: 10px;
  align-self: flex-end;
  font-weight: 500;
  min-width: 16px;
  text-align: center;
}

/* 右键菜单样式 */
.context-menu {
  position: fixed;
  background: white;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 9999;
  min-width: 180px;
  max-width: 250px;
  overflow: hidden;
}

.context-menu-header {
  padding: 12px 16px;
  background: #f5f7fa;
  border-bottom: 1px solid #e8e8e8;
  font-size: 13px;
  font-weight: 600;
  color: #666;
}

.context-menu-items {
  max-height: 200px;
  overflow-y: auto;
}

.context-menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  cursor: pointer;
  transition: background-color 0.2s;
  border-bottom: 1px solid #f5f5f5;
}

.context-menu-item:last-child {
  border-bottom: none;
}

.context-menu-item:hover {
  background: #f5f7fa;
}

.label-color-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  flex-shrink: 0;
  border: 1px solid rgba(0, 0, 0, 0.1);
}

.context-menu-item .label-name {
  font-size: 14px;
  color: #262626;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.context-menu-empty {
  padding: 20px 16px;
  text-align: center;
  color: #999;
  font-size: 13px;
}

/* 画布交互样式 */
.image-canvas {
  cursor: crosshair;
}

.image-canvas[data-tool="select"] {
  cursor: default;
}

.image-canvas[data-tool="rect"] {
  cursor: crosshair;
}

.image-canvas[data-tool="circle"] {
  cursor: crosshair;
}

.label-search-input :deep(.el-input__wrapper) {
  border-radius: 6px;
  border: 1px solid #e8e8e8;
  box-shadow: none;
  transition: all 0.3s;
}

.label-search-input :deep(.el-input__wrapper:hover) {
  border-color: #409eff;
}

.label-search-input :deep(.el-input__wrapper.is-focus) {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
}

.label-search-input :deep(.el-input__inner) {
  color: #262626;
  padding: 0 12px;
  height: 36px;
}

.label-search-input :deep(.el-input__inner::placeholder) {
  color: #999;
  font-size: 14px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  font-weight: 600;
  color: #262626;
  font-size: 15px;
}

.label-name {
  flex: 1;
  font-size: 14px;
  color: #262626;
  font-weight: 500;
}

.label-item.active .label-count {
  background: #409eff;
  color: white;
}

.label-item:hover .label-action-buttons {
  opacity: 1;
}

.action-button .el-icon {
  font-size: 16px;
}

.annotation-image-container img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.annotation-image-card:hover .annotation-image-container img {
  transform: scale(1.05);
}

.annotation-image-card:hover .annotation-image-overlay {
  opacity: 1;
}

/* 右侧图像标注区域 */
.image-annotation-area {
  width: calc(100% - 350px);
  display: flex;
  flex-direction: column;
  background: white;
  position: relative;
}

/* 保存按钮区域 - 单独一行 */
.save-annotation-section {
  padding: 12px 20px;
  background: #f8f9fa;
  border-bottom: 1px solid #e8e8e8;
  border-radius: 6px;
  margin-bottom: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.save-buttons-left {
  display: flex;
  gap: 12px;
}

.save-buttons-right {
  display: flex;
  gap: 12px;
}

.save-annotation-btn,
.save-all-btn,
.clear-all-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  font-size: 13px;
  border-radius: 6px;
  height: 36px;
  font-weight: 500;
}

/* 主要编辑区域 */
.image-edit-main {
  flex: 1;
  display: flex;
  position: relative;
  overflow: hidden;
}

/* 图片内容区域 */
.image-content-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
}

/* 图片左上角保存按钮 */
.image-annotation-area .save-annotation-overlay {
  position: absolute;
  top: 20px;
  left: 20px;
  z-index: 10;
}

.image-annotation-area .save-annotation-btn-overlay {
  padding: 8px 16px;
  height: 36px;
  border-radius: 18px;
  font-weight: 500;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  border: none;
  background: #409eff;
  color: white;
  transition: all 0.3s ease;
}

.image-annotation-area .save-annotation-btn-overlay:hover {
  background: #337ecc;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  transform: translateY(-1px);
}

/* 图像标注区域的文件名显示 */
.image-annotation-area .image-filename {
  padding: 16px 20px;
  background: white;
  border-bottom: 1px solid #f0f0f0;
  font-size: 14px;
  color: #666;
  text-align: right;
}

/* 图像标注区域的主图像容器 */
.image-annotation-area .main-image-container {
  flex: 1;
  display: flex;
  position: relative;
  overflow: hidden;
  background: #f8f9fa;
}

.image-annotation-area .image-canvas {
  flex: 1;
  display: flex;
  align-items: flex-start;
  justify-content: flex-start;
  padding: 0;
  position: relative;
}

/* 图片容器 */
.image-annotation-area .image-container {
  position: relative;
  display: inline-block;
  max-width: 100%;
  max-height: 100%;
}

.image-annotation-area .main-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  display: block;
}

/* SVG标注层 */
.image-annotation-area .annotation-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.image-annotation-area .annotation-overlay .annotation-group {
  pointer-events: auto;
  cursor: pointer;
}

/* 无图片占位符 */
.image-annotation-area .no-image-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #999;
  min-height: 300px;
}

.image-annotation-area .placeholder-icon {
  font-size: 48px;
  margin-bottom: 16px;
  color: #ddd;
}

.image-annotation-area .placeholder-text {
  font-size: 16px;
  color: #999;
}

.image-annotation-area .main-image-svg {
  max-width: 100%;
  max-height: 100%;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  background: white;
}

/* SVG交互样式 */
.image-annotation-area .main-image-svg .screw-item:hover,
.image-annotation-area .main-image-svg .nut-item:hover {
  cursor: pointer;
  opacity: 0.8;
  transform: scale(1.05);
  transform-origin: center;
  transition: all 0.3s ease;
}

.image-annotation-area .main-image-svg .annotation-box,
.image-annotation-area .main-image-svg .annotation-circle {
  animation: annotationPulse 2s infinite;
}

.image-annotation-area .main-image-svg .label-selection-popup {
  animation: popupFadeIn 0.3s ease-out;
}

@keyframes annotationPulse {
  0%, 100% { opacity: 0.8; }
  50% { opacity: 1; }
}

@keyframes popupFadeIn {
  0% {
    opacity: 0;
    transform: translateY(-10px) scale(0.9);
  }
  100% {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* 工具栏按钮活跃状态优化 */
.image-annotation-area .tool-btn-right.active {
  border-color: #409eff;
  color: white;
  background: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
}

/* 图像标注的右侧工具栏 */
.image-annotation-area .annotation-toolbar-right {
  width: 70px;
  background: white;
  border-left: 1px solid #e8e8e8;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 30px 15px;
}

.image-annotation-area .tool-group-vertical {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.image-annotation-area .tool-btn-right {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 1px solid #e8e8e8;
  background: white;
  color: #666;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

.image-annotation-area .tool-btn-right:hover {
  border-color: #409eff;
  color: #409eff;
  background: #f0f9ff;
}

.image-annotation-area .tool-btn-right.active {
  border-color: #409eff;
  color: white;
  background: #409eff;
}

.image-annotation-area .tool-btn-right .el-icon {
  font-size: 18px;
}

/* 左侧工具栏样式 */
.annotation-toolbar-left {
  width: 70px;
  background: white;
  border-right: 1px solid #e8e8e8;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 30px 15px;
}

.tool-group-vertical {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tool-btn-left {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 1px solid #e8e8e8;
  background: white;
  color: #666;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

.tool-btn-left:hover {
  border-color: #409eff;
  color: #409eff;
  background: #f0f9ff;
}

.tool-btn-left.active {
  border-color: #409eff;
  color: white;
  background: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
}

.tool-btn-left .el-icon {
  font-size: 18px;
}

:deep(.image-uploader .el-upload) {
  width: 100%;
  height: 100%;
}

:deep(.image-uploader .el-upload-dragger) {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 2px dashed #dcdfe6;
  border-radius: 6px;
  background: #fafafa;
  transition: all 0.3s;
}

:deep(.image-uploader .el-upload-dragger:hover) {
  border-color: #409eff;
  background: #f0f9ff;
}

/* 底部图片缩略图 */
.image-thumbnails {
  display: flex;
  gap: 12px;
  padding: 16px 20px;
  background: white;
  border-top: 1px solid #f0f0f0;
  overflow-x: auto;
}

.thumbnail-item {
  flex-shrink: 0;
  width: 80px;
  height: 60px;
  border-radius: 6px;
  overflow: hidden;
  border: 2px solid transparent;
  cursor: pointer;
  transition: all 0.3s;
}

.thumbnail-item:hover {
  border-color: #409eff;
  transform: scale(1.05);
}

.thumbnail-item.active {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
}

.thumbnail-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 右键菜单样式 */
.context-menu {
  position: fixed;
  background: white;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  padding: 8px 0;
  z-index: 9999;
  min-width: 150px;
}

.context-menu-header {
  padding: 8px 16px;
  font-size: 12px;
  color: #8c939d;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 4px;
}

.context-menu-item {
  display: flex;
  align-items: center;
  padding: 8px 16px;
  cursor: pointer;
  transition: background-color 0.3s;
  gap: 8px;
}

.context-menu-item:hover {
  background-color: #f5f7fa;
}

.label-color-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  flex-shrink: 0;
}

.label-name {
  font-size: 14px;
  color: #262626;
}

/* 标签管理弹窗样式 */
.color-picker-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.color-preview {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100px;
  height: 32px;
  border-radius: 16px;
  color: white;
  font-size: 12px;
  font-weight: 500;
}

.preview-text {
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

/* SVG 标注相关样式 */
.annotation-group {
  cursor: pointer;
}

.annotation-group rect,
.annotation-group circle {
  transition: all 0.3s ease;
}

.annotation-group rect:hover,
.annotation-group circle:hover {
  stroke-width: 3;
  filter: brightness(1.1);
}

.drawing-annotation {
  pointer-events: none;
}

/* 画布交互样式 */
.image-canvas {
  cursor: crosshair;
}

.image-canvas[data-tool="select"] {
  cursor: default;
}

.image-canvas[data-tool="brush"] {
  cursor: crosshair;
}

.image-canvas[data-tool="rect"] {
  cursor: crosshair;
}

.image-canvas[data-tool="circle"] {
  cursor: crosshair;
}

.navigation-buttons .el-button {
  padding: 4px 8px;
  font-size: 12px;
}

/* 图片容器样式 */
.image-container {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  transition: transform 0.3s ease; /* 平滑的旋转过渡 */
  transform-origin: center center; /* 设置旋转中心点 */
}

/* 数据集文件校验弹窗样式 */
.dataset-validation-dialog {
  .el-dialog__body {
    padding: 20px;
  }
}

.dataset-validation-content {
  .annotation-info {
    margin-bottom: 24px;
    padding: 16px;
    background: #f8f9fa;
    border-radius: 8px;

    h4 {
      margin: 0 0 16px 0;
      font-size: 16px;
      font-weight: 600;
      color: #262626;
      text-align: center;
    }

    .path-info {
      .path-item {
        display: flex;
        margin-bottom: 8px;
        align-items: flex-start;

        .label {
          min-width: 80px;
          font-size: 14px;
          color: #666;
          font-weight: 500;
        }

        .path-value {
          flex: 1;
          font-size: 14px;
          color: #409eff;
          font-family: 'Courier New', monospace;
          word-break: break-all;
          line-height: 1.4;
        }

        .file-name {
          flex: 1;
          font-size: 14px;
          color: #e6a23c;
          font-family: 'Courier New', monospace;
          font-weight: 600;
        }
      }

      .path-item:last-child {
        margin-bottom: 0;
      }
    }
  }

  .validation-section {
    min-height: 120px;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 20px;
    border: 2px dashed #e4e7ed;
    border-radius: 8px;
    background: #fafafa;

    .validation-status {
      text-align: center;

      .status-success {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 8px;
        color: #67c23a;
        font-size: 16px;
        font-weight: 500;
        margin-bottom: 8px;

        .el-icon {
          font-size: 20px;
        }
      }

      .status-error {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 8px;
        color: #f56c6c;
        font-size: 16px;
        font-weight: 500;
        margin-bottom: 8px;

        .el-icon {
          font-size: 20px;
        }
      }

      .validation-message {
        margin: 0;
        font-size: 14px;
        color: #666;
      }
    }

    .validation-loading {
      text-align: center;
      color: #409eff;

      .el-icon {
        font-size: 24px;
        margin-bottom: 8px;
      }

      p {
        margin: 0;
        font-size: 14px;
      }
    }
  }

  .path-validation-section {
    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;

      h3 {
        margin: 0;
        font-size: 16px;
        font-weight: 600;
        color: #262626;
      }
    }

    .validation-details {
      background: #f8f9fa;
      border: 1px solid #e9ecef;
      border-radius: 8px;
      padding: 16px;

      .validation-item {
        display: flex;
        margin-bottom: 8px;

        &:last-child {
          margin-bottom: 0;
        }

        .label {
          font-weight: 500;
          color: #606266;
          min-width: 120px;
        }

        .value {
          color: #303133;
        }
      }
    }
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 数据集路径样式 */
.dataset-path {
  color: #409eff;
  font-size: 12px;
  max-width: 180px;
  display: inline-block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}

.dataset-path.clickable {
  cursor: pointer;
  text-decoration: underline;
  transition: color 0.3s ease;
}

.dataset-path.clickable:hover {
  color: #66b1ff;
  text-decoration: underline;
}

.no-dataset {
  color: #909399;
  font-size: 12px;
  font-style: italic;
}
</style>
