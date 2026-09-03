<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
  Created by: ChaoQun Lei
  Updated by: ChaoQun Lei
-->

<template>
  <div class="algorithm-management tenant_Page draHeaPB">
    <!-- Tab + ModelHub -->
    <div class="top-tabs-header" v-loading="repositoriesLoading">
      <el-tabs
        v-model="activeTopMenu"
        class="tenanat-tabs"
        @tab-change="setActiveTopMenu"
      >
        <el-tab-pane
          v-for="menu in topMenus"
          :key="menu.key"
          :label="menu.label"
          :name="menu.key"
        />
      </el-tabs>
      <a
        class="modelhub-button"
        href="https://vls.oortcloudsmart.com/zh/ModelHub/ModelHub"
        target="_blank"
        rel="noopener noreferrer"
      >
        Model_Hub
      </a>
    </div>

    <div class="tenant_content">
      <!-- : tab (tenanat-tabs_act) -->
      <div
        v-if="showAddButton && typeOptions.length > 0"
        class="category-tabs-wrap"
      >
        <el-tabs
          v-model="activeCategory"
          class="tenanat-tabs_act"
          @tab-change="setActiveCategory"
        >
          <el-tab-pane
            v-for="category in typeOptions"
            :key="category.value"
            :label="category.label"
            :name="category.value"
          />
        </el-tabs>
      </div>

      <!-- category-tabs : button -->
      <div v-if="showAddButton" class="add-toolbar">
        <div class="exportBtnBox flexRowAC">
          <button type="button" class="exportBtn newBtn flexRowAC" @click="addAlgorithm">
            <el-icon class="BtnImg">
              <Plus />
            </el-icon>
            添加
          </button>
        </div>
      </div>

      <!-- algorithm -->
      <div v-if="activeTopMenu !== 'management'" class="algorithm-grid" v-loading="algorithmsLoading">
        <div
          v-for="algorithm in currentPageAlgorithms"
          :key="algorithm.id"
          class="algorithm-card"
        >
          <div class="card-image">
            <img
              :src="getAlgorithmCardBackground(algorithm, currentRepositoryId)"
              :alt="algorithm.name"
              @click="editAlgorithm(algorithm)"
            />
            <div class="card-menu">
              <el-dropdown trigger="click" placement="bottom-end">
                <div class="menu-trigger">
                  <el-icon><MoreFilled /></el-icon>
                </div>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="editAlgorithm(algorithm)">
                      <el-icon><Edit /></el-icon>
                      编辑
                    </el-dropdown-item>
                    <el-dropdown-item @click="evaluateAlgorithm(algorithm)">
                      <el-icon><DataAnalysis /></el-icon>
                      算法评估
                    </el-dropdown-item>
                    <el-dropdown-item @click="deployAlgorithm(algorithm)">
                      <el-icon><Download /></el-icon>
                      下发到摄像机
                    </el-dropdown-item>
                    <el-dropdown-item @click="publishToModelHub(algorithm)">
                      <el-icon><Upload /></el-icon>
                      发布到 Model Hub
                    </el-dropdown-item>
                    <el-dropdown-item divided @click="handleDeleteAlgorithm(algorithm)">
                      <el-icon><Delete /></el-icon>
                      删除
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
          <div class="card-content">
            <div class="card-title">{{ algorithm.name }}</div>
            <div class="card-subtitle">{{ algorithm.categoryName }}</div>
            <div class="card-description">{{ algorithm.description }}</div>
          </div>
        </div>
      </div>

      <!-- algorithm table -->
      <div v-if="activeTopMenu === 'management'" class="algorithm-management-container">
        <div class="depNameBox_out flexRowAC">
          <div class="depNameBox flexRowAC">
            <div class="exportBtnBox flexRowAC">
              <button type="button" class="exportBtn newBtn flexRowAC" @click="addAlgorithmLibrary">
                <el-icon class="BtnImg">
                  <Plus />
                </el-icon>
                新增
              </button>
              <button-group :button-list="managementToolbarButtonList" />
            </div>
          </div>
        </div>

        <TableSelf
            class="new_table"
            header-cell-class-name="header_tenant_cell"
            stripe
            v-loading="repositoriesLoading"
            :data="currentPageRepositories"
            @selection-change="handleLibrarySelectionChange"
        >
          <el-table-column type="selection" :width="clacPXToVW(55)" />
          <el-table-column label="序号" :width="clacPXToVW(80)" align="center">
            <template #default="scope">
              {{ scope.$index + (repositoryCurrentPage - 1) * repositoryPageSize + 1 }}
            </template>
          </el-table-column>
          <el-table-column prop="name" label="名称" show-overflow-tooltip />
          <el-table-column prop="remark" label="备注" show-overflow-tooltip />
          <el-table-column prop="algorithmCount" label="拥有算法" :width="clacPXToVW(120)" align="center" />
          <el-table-column prop="repositoryType" label="类型" :width="clacPXToVW(120)" align="center">
            <template #default="scope">
              <el-tag :type="getRepositoryTypeTagType(scope.row.repositoryType)">
                {{ getRepositoryTypeText(scope.row.repositoryType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" :width="clacPXToVW(100)" align="center">
            <template #default="scope">
              <el-tag
                  :type="scope.row.status === 1 ? 'success' : 'danger'"
                  size="small"
              >
                {{ scope.row.status === 1 ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" :width="clacPXToVW(180)" />
          <el-table-column label="操作" :width="clacPXToVW(220)" fixed="right" align="right">
            <template #default="scope">
              <div class="operateAppBox flexRowAC" @click.stop>
                <div class="new_table_svg_group" @click="editLibraryItem(scope.row)">
                  <oort-svg-icon width="14" height="14" name="edit_icon" class="new_table_svg_group_svg" />
                  <span>编辑</span>
                </div>
                <div class="new_table_svg_group" @click="toggleRepositoryStatus(scope.row)">
                  <oort-svg-icon width="14" height="14" name="enable" class="new_table_svg_group_svg" />
                  <span>{{ scope.row.status === 1 ? '禁用' : '启用' }}</span>
                </div>
                <div
                    class="new_table_svg_group"
                    :class="{ 'is-disabled': scope.row.repositoryType === 'basic' }"
                    @click="scope.row.repositoryType !== 'basic' && deleteLibraryItem(scope.row)"
                >
                  <oort-svg-icon
                      color="red"
                      width="14"
                      height="14"
                      name="delete_icon"
                      class="new_table_svg_group_svg"
                  />
                  <span>删除</span>
                </div>
              </div>
            </template>
          </el-table-column>
        </TableSelf>

        <div class="paginationBox flexRowAC">
          <el-pagination
              background
              :current-page="repositoryCurrentPage"
              :page-size="repositoryPageSize"
              :page-sizes="[10, 20, 50, 100]"
              :total="repositoryTotal"
              layout="total, prev, pager, next, sizes"
              class="justifyAlign"
              @size-change="handleRepositorySizeChange"
              @current-change="handleRepositoryCurrentChange"
          />
        </div>
      </div>
    </div>

    <!-- Add / algorithm -->
    <el-dialog
        v-model="showAddDialog"
        :title="editingRepository ? '编辑算法库' : '新增算法库'"
        width="30%"
        :close-on-click-modal="false"
    >
      <el-form :model="addForm" :rules="addFormRules" ref="addFormRef" label-width="80px" class="add-form">
        <el-form-item label="名称" prop="name" required>
          <el-input
              v-model="addForm.name"
              placeholder="请输入算法库名称"
              clearable
          />
        </el-form-item>
        <el-form-item label="类型" prop="repositoryType" required>
          <el-select
              v-model="addForm.repositoryType"
              placeholder="请选择仓库类型"
              style="width: 100%"
              :disabled="editingRepository && editingRepository.repositoryType === 'basic'"
          >
            <el-option label="基础算法库" value="basic" />
            <el-option label="扩展算法库" value="extended" />
            <el-option label="测试算法库" value="test" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select
              v-model="addForm.status"
              placeholder="请选择状态"
              style="width: 100%"
          >
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
              v-model="addForm.remark"
              type="textarea"
              placeholder="请输入备注"
              :rows="4"
              resize="none"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleAddCancel" class="common_btn">取消</el-button>
          <el-button type="primary" @click="handleAddConfirm" :loading="submitting" class="common_btn">
            {{ editingRepository ? '更新' : '创建' }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- algorithm -->
    <el-dialog
        v-model="showAlgorithmAddDialog"
        title="添加算法"
        width="40%"
        :close-on-click-modal="false"
    >
      <el-form :model="algorithmAddForm" :rules="algorithmAddFormRules" ref="algorithmAddFormRef" label-width="80px" class="add-form">
        <el-form-item label="算法名称" prop="name" required>
          <el-input
              v-model="algorithmAddForm.name"
              placeholder="请输入算法名称"
              clearable
          />
        </el-form-item>
        <el-form-item label="算法类型" prop="category" required>
          <el-select
              v-model="algorithmAddForm.category"
              placeholder="请选择算法类型"
              style="width: 100%">
            <el-option label="目标检测算法" value="detect" />
            <el-option label="实例分割算法" value="segment" />
            <el-option label="图像分类算法" value="classify" />
            <el-option label="关键点检测算法" value="pose" />
            <el-option label="旋转目标检测算法" value="obb" />
          </el-select>
        </el-form-item>
        <el-form-item label="pt算法模型" prop="ptModelFilePath" label-width="100px">
          <el-input
              v-model="algorithmAddForm.ptModelFilePath"
              placeholder="请输入pt算法模型"
              clearable/>
        </el-form-item>
        <el-form-item label="onnx算法模型" prop="onnxModelFilePath" label-width="110px">
          <el-input
              v-model="algorithmAddForm.onnxModelFilePath"
              placeholder="请输入onnx算法模型"
              clearable/>
        </el-form-item>
        <el-form-item label="是否为系统预置算法" label-width="140px">
          <el-radio-group v-model="algorithmAddForm.isSystem">
            <el-radio value="YES">是</el-radio>
            <el-radio value="NO">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="算法描述" prop="description">
          <el-input
              v-model="algorithmAddForm.description"
              type="textarea"
              placeholder="请输入算法描述"
              :rows="4"
              resize="none"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleAlgorithmAddCancel" class="common_btn">取消</el-button>
          <el-button type="primary" @click="handleAlgorithmAddConfirm" :loading="submitting" class="common_btn">
            添加
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- algorithm -->
    <el-dialog
        v-model="showAlgorithmEditDialog"
        title="编辑算法"
        width="40%"
        :close-on-click-modal="false"
    >
      <el-form :model="algorithmEditForm" :rules="algorithmEditFormRules" ref="algorithmEditFormRef" label-width="80px" class="add-form">
        <el-form-item label="算法名称" prop="name" required>
          <el-input
              v-model="algorithmEditForm.name"
              placeholder="请输入算法名称"
              clearable
          />
        </el-form-item>
        <el-form-item label="算法分类" prop="category" required>
          <el-select
              v-model="algorithmEditForm.category"
              placeholder="请选择算法类型"
              style="width: 100%">
            <el-option label="目标检测算法" value="detect" />
            <el-option label="实例分割算法" value="segment" />
            <el-option label="图像分类算法" value="classify" />
            <el-option label="关键点检测算法" value="pose" />
            <el-option label="旋转目标检测算法" value="obb" />
          </el-select>
        </el-form-item>
        <el-form-item label="pt算法模型" prop="ptModelFilePath" label-width="100px">
          <el-input
              v-model="algorithmEditForm.ptModelFilePath"
              placeholder="请输入算法模型"
              clearable/>
        </el-form-item>
        <el-form-item label="onnx算法模型" prop="onnxModelFilePath" label-width="110px">
          <el-input
              v-model="algorithmEditForm.onnxModelFilePath"
              placeholder="请输入onnx算法模型"
              clearable/>
        </el-form-item>
        <el-form-item label="是否为系统预置算法" label-width="140px">
          <el-radio-group v-model="algorithmEditForm.isSystem">
            <el-radio value="YES">是</el-radio>
            <el-radio value="NO">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="算法图片">
          <div class="algorithm-image-editor">
            <el-upload
                class="algorithm-image-uploader"
                :action="algorithmImageUploadUrl"
                :headers="algorithmImageUploadHeaders"
                accept="image/jpeg,image/png,image/webp"
                :show-file-list="false"
                :multiple="false"
                :disabled="imageUploading"
                :on-success="handleAlgorithmImageUploadSuccess"
                :on-error="handleAlgorithmImageUploadError"
                :before-upload="beforeAlgorithmImageUpload"
            >
              <div class="algorithm-image-preview">
                <img
                    :src="algorithmEditForm.imageUrl || getAlgorithmCardBackground(editingAlgorithm || {}, algorithmEditForm.repositoryId || currentRepositoryId)"
                    alt="算法图片"
                />
                <div class="algorithm-image-overlay">
                  {{ imageUploading ? '上传中...' : '点击更换图片' }}
                </div>
              </div>
            </el-upload>
            <el-button
                v-if="algorithmEditForm.imageUrl"
                type="danger"
                link
                size="small"
                @click="clearAlgorithmImage"
            >
              清除图片
            </el-button>
            <span class="algorithm-image-tip">支持 JPG、PNG、WEBP，大小不超过 5MB</span>
          </div>
        </el-form-item>
        <el-form-item label="算法描述" prop="description">
          <el-input
              v-model="algorithmEditForm.description"
              type="textarea"
              placeholder="请输入算法描述"
              :rows="4"
              resize="none"
          />
        </el-form-item>
        <el-divider content-position="left">大模型复核（可选）</el-divider>
        <el-form-item label="开启复核" label-width="100px">
          <el-switch v-model="llmReviewForm.enabled" />
          <span class="llm-review-tip">关闭时保持原有 YOLO 事件流程</span>
        </el-form-item>
        <template v-if="llmReviewForm.enabled">
          <el-form-item label="视觉大模型" label-width="100px" required>
            <el-select v-model="llmReviewForm.providerId" placeholder="请选择大模型" style="width: 100%">
              <el-option v-for="provider in llmProviders" :key="provider.id"
                         :label="`${provider.name} / ${provider.modelName}`" :value="provider.id"
                         :disabled="!provider.enabled" />
            </el-select>
            <el-button link type="primary" @click="router.push('/llm-provider-management')">管理大模型</el-button>
          </el-form-item>
          <el-form-item label="提示词" label-width="100px" required>
            <el-input v-model="llmReviewForm.promptTemplate" type="textarea" :rows="5" />
          </el-form-item>
          <el-form-item label="确认阈值" label-width="100px">
            <el-input-number v-model="llmReviewForm.decisionThreshold" :min="0" :max="1" :step="0.05" :precision="2" />
            <span class="llm-review-tip">模型结论为 confirmed 且置信度达到阈值才进入正式事件流程</span>
          </el-form-item>
          <el-form-item label="失败重试" label-width="100px">
            <el-input-number v-model="llmReviewForm.maxRetries" :min="0" :max="5" />
          </el-form-item>
          <el-form-item label="图片范围" label-width="100px">
            <el-radio-group v-model="llmReviewForm.imageMode">
              <el-radio value="FULL_AND_CROP">完整图 + 目标框</el-radio>
              <el-radio value="FULL">仅完整图</el-radio>
              <el-radio value="CROP">仅目标框</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-alert title="调用超时或重试耗尽后进入人工复核，不会直接生成主动安全事件。" type="info" :closable="false" />
        </template>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleAlgorithmEditCancel" class="common_btn">取消</el-button>
          <el-button type="primary" @click="handleAlgorithmEditConfirm" :loading="submitting" class="common_btn">
            更新
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!--  -->
    <el-drawer
        v-model="showDeviceDrawer"
        title="下发到摄像机"
        direction="rtl"
        size="55%"
        destroy-on-close
        :before-close="handleDrawerClose"
        class="device-deploy-drawer"
    >
      <DeviceClassificationLayout
          class="device-drawer-content"
          protocol-type="VLSTREAM"
          :show-assignment="false"
          readonly
          @filter-change="handleDeviceFilterChange"
      >
        <div class="device-table-panel">
          <div class="depNameBox_out flexRowAC">
            <div class="depNameBox flexRowAC">
              <div class="exportBtnBox flexRowAC">
                <el-select v-model="dispatchModelType" style="width: 170px" placeholder="选择模型格式">
                  <el-option
                      v-for="item in modelTypeOptions"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                  />
                </el-select>
                <button type="button" class="exportBtn newBtn flexRowAC" @click="handleDeployToDevice">
                  <el-icon class="BtnImg">
                    <Plus />
                  </el-icon>
                  下发
                </button>
              </div>
            </div>
          </div>

          <TableSelf
              class="new_table"
              header-cell-class-name="header_tenant_cell"
              stripe
              v-loading="deviceLoading"
              :data="deviceTableData"
              height="450"
              @selection-change="handleDeviceSelectionChange"
          >
            <el-table-column type="selection" :width="clacPXToVW(55)" />
            <el-table-column label="序号" :width="clacPXToVW(65)" align="center">
              <template #default="scope">
                {{ scope.row.index || (scope.$index + (currentPage - 1) * pageSize + 1) }}
              </template>
            </el-table-column>
            <el-table-column prop="deviceName" label="设备名称" show-overflow-tooltip />
            <el-table-column prop="deviceId" label="设备ID" show-overflow-tooltip />
            <el-table-column prop="deviceModel" label="设备型号" show-overflow-tooltip />
            <el-table-column prop="deviceSerial" label="序列号" show-overflow-tooltip />
            <el-table-column label="状态" :width="clacPXToVW(90)">
              <template #default="scope">
                <el-tag :type="scope.row.online ? 'success' : 'info'">
                  {{ scope.row.online ? '在线' : '离线' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="ipAddr" label="IP" show-overflow-tooltip />
            <el-table-column prop="firmwareVersion" label="RootFS版本" show-overflow-tooltip />
            <el-table-column prop="lastHeartbeatTime" label="最后心跳" show-overflow-tooltip />
          </TableSelf>

          <div class="paginationBox flexRowAC">
            <el-pagination
                background
                :current-page="currentPage"
                :page-size="pageSize"
                :page-sizes="[10, 20, 50, 100]"
                :total="totalDevices"
                layout="total, sizes, prev, pager, next, jumper"
                class="justifyAlign"
                @size-change="handleSizeChange"
                @current-change="handleCurrentChange"
            />
          </div>
        </div>
      </DeviceClassificationLayout>
    </el-drawer>
  </div>
</template>

<script setup>
import {computed, onMounted, ref} from 'vue'
import { useRouter } from 'vue-router'
import {ElMessage, ElMessageBox} from 'element-plus'
import {DataAnalysis, Delete, Download, Edit, MoreFilled, Plus, Upload} from '@element-plus/icons-vue'
import { clacPXToVW } from '@/utils/index'
import DeviceClassificationLayout from '@/components/DeviceClassificationLayout/index.vue'
import Config from '@/config'
import {
  getCloudPlatformUserPath,
  getModelHubAccessToken,
  startModelHubLogin
} from '@/utils/modelHubAuth'
import {
  batchDeleteAlgorithmRepositories,
  createAlgorithm,
  createAlgorithmRepository,
  deleteAlgorithm,
  deleteAlgorithmRepository,
  evaluateAlgorithm as apiEvaluateAlgorithm,
  getAlgorithmPage,
  getAlgorithmRepositoryPage,
  updateAlgorithm,
  updateAlgorithmRepository,
  updateAlgorithmRepositoryStatus
} from '@/api/algorithmManagement'
import {dispatchAlgorithmToDevices} from '@/api/device'
import {getMqttDevicePage} from '@/api/vlstreamMqttDevice'
import {
  getAlgorithmLlmReviewConfig,
  getLlmProviders,
  saveAlgorithmLlmReviewConfig
} from '@/api/llmReview'

const router = useRouter()

// Load
const repositoriesLoading = ref(false)
const algorithmsLoading = ref(false)
const submitting = ref(false)
const imageUploading = ref(false)

const algorithmImageUploadUrl = `${Config.URL}${Config.gateWay}apaas-fastdfsservice/fastdfs/v1/uploadFile`
const algorithmImageUploadHeaders = Config.headers

// current menu
const activeTopMenu = ref('management')

// current
const activeCategory = ref('all')

// device relateddata
const showDeviceDrawer = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const totalDevices = ref(0)
const deviceLoading = ref(false)
const deviceFilter = ref({})

// algorithm data
const algorithmRepositories = ref([])
const repositoryCurrentPage = ref(1)
const repositoryPageSize = ref(10)
const repositoryTotal = ref(0)

// algorithmdata
const algorithms = ref([])
const algorithmTotal = ref(0)
const currentRepositoryId = ref(null)
const algorithmRequestId = ref(0)

// from current algorithm in all
const typeOptions = ref([
  { label: '全部', value: 'all' },
  { label: '目标检测算法', value: 'detect' },
  { label: '实例分割算法', value: 'segment' },
  { label: '图像分类算法', value: 'classify' },
  { label: '关键点检测算法', value: 'pose' },
  { label: '旋转目标检测算法', value: 'obb' }
])

// devicetabledata
const deviceTableData = ref([])
const selectedDeviceRows = ref([])
const dispatchModelType = ref('om')
const modelTypeOptions = [
  { label: 'OM（昇腾/海思）', value: 'om' },
  { label: 'RKNN（瑞芯微）', value: 'rknn' },
  { label: 'INT8 RKNN', value: 'int8-rknn' },
  { label: 'ONNX（通用）', value: 'onnx' },
  { label: 'PT（PyTorch）', value: 'pt' }
]

// algorithm related
const selectedRepositories = ref([])
const showAddDialog = ref(false)
const editingRepository = ref(null)
const addForm = ref({
  name: '',
  repositoryType: 'extended',
  status: 1,
  remark: ''
})

// algorithm related
const showAlgorithmAddDialog = ref(false)
const algorithmAddForm = ref({
  name: '',
  category: '',
  type: 'detect',
  version: '1.0.0',
  description: '',
  repositoryId: null,
  ptModelFilePath: '',
  onnxModelFilePath: ''
})

// algorithm related
const showAlgorithmEditDialog = ref(false)
const editingAlgorithm = ref(null)
const algorithmEditForm = ref({
  name: '',
  category: '',
  type: 'detect',
  version: '1.0.0',
  description: '',
  repositoryId: null,
  ptModelFilePath: '',
  onnxModelFilePath: '',
  isSystem: 'YES',
  imageUrl: ''
})
const llmProviders = ref([])
const defaultLlmReviewForm = () => ({
  enabled: false,
  providerId: null,
  promptTemplate: '',
  decisionThreshold: 0.8,
  maxRetries: 2,
  failureStrategy: 'MANUAL_REVIEW',
  imageMode: 'FULL_AND_CROP'
})
const llmReviewForm = ref(defaultLlmReviewForm())

// form
const addFormRules = ref({
  name: [
    { required: true, message: '请输入算法库名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  repositoryType: [
    { required: true, message: '请选择仓库类型', trigger: 'change' }
  ]
})

const algorithmAddFormRules = ref({
  name: [
    { required: true, message: '请输入算法名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  category: [
    { required: true, message: '请输入算法类型', trigger: 'blur' }
  ]
})

const algorithmEditFormRules = ref({
  name: [
    { required: true, message: '请输入算法名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  category: [
    { required: true, message: '请输入算法类型', trigger: 'blur' }
  ]
})

const addFormRef = ref(null)
const algorithmAddFormRef = ref(null)
const algorithmEditFormRef = ref(null)

// algorithmrelated
const selectedAlgorithm = ref(null)

// - 300x200
const cardBackgrounds = [
  // main
  'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48ZGVmcz48bGluZWFyR3JhZGllbnQgaWQ9ImJsdWVHcmFkaWVudCIgeDE9IjAlIiB5MT0iMCUiIHgyPSIxMDAlIiB5Mj0iMTAwJSI+PHN0b3Agb2Zmc2V0PSIwJSIgc3R5bGU9InN0b3AtY29sb3I6IzQwOTZmZjtzdG9wLW9wYWNpdHk6MSIgLz48c3RvcCBvZmZzZXQ9IjEwMCUiIHN0eWxlPSJzdG9wLWNvbG9yOiMxODY3YzA7c3RvcC1vcGFjaXR5OjEiIC8+PC9saW5lYXJHcmFkaWVudD48L2RlZnM+PHJlY3Qgd2lkdGg9IjEwMCUiIGhlaWdodD0iMTAwJSIgZmlsbD0idXJsKCNibHVlR3JhZGllbnQpIi8+PGNpcmNsZSBjeD0iMjUwIiBjeT0iNTAiIHI9IjMwIiBmaWxsPSJyZ2JhKDI1NSwyNTUsMjU1LDAuMSkiLz48Y2lyY2xlIGN4PSI1MCIgY3k9IjE1MCIgcj0iMjAiIGZpbGw9InJnYmEoMjU1LDI1NSwyNTUsMC4xNSkiLz48cmVjdCB4PSIyMDAiIHk9IjEyMCIgd2lkdGg9IjgwIiBoZWlnaHQ9IjgwIiByeD0iMTAiIGZpbGw9InJnYmEoMjU1LDI1NSwyNTUsMC4wOCkiLz48L3N2Zz4=',

  // main
  'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48ZGVmcz48bGluZWFyR3JhZGllbnQgaWQ9ImdyZWVuR3JhZGllbnQiIHgxPSIwJSIgeTE9IjAlIiB4Mj0iMTAwJSIgeTI9IjEwMCUiPjxzdG9wIG9mZnNldD0iMCUiIHN0eWxlPSJzdG9wLWNvbG9yOiM2N0M5MkE7c3RvcC1vcGFjaXR5OjEiIC8+PHN0b3Agb2Zmc2V0PSIxMDAlIiBzdHlsZT0ic3RvcC1jb2xvcjojNDE4MDE5O3N0b3Atb3BhY2l0eToxIiAvPjwvbGluZWFyR3JhZGllbnQ+PC9kZWZzPjxyZWN0IHdpZHRoPSIxMDAlIiBoZWlnaHQ9IjEwMCUiIGZpbGw9InVybCgjZ3JlZW5HcmFkaWVudCkiLz48cG9seWdvbiBwb2ludHM9IjAsMCAxMDAsMCA1MCw1MCIgZmlsbD0icmdiYSgyNTUsMjU1LDI1NSwwLjEpIi8+PGNpcmNsZSBjeD0iMjMwIiBjeT0iMTcwIiByPSIyNSIgZmlsbD0icmdiYSgyNTUsMjU1LDI1NSwwLjEyKSIvPjxwYXRoIGQ9Ik0yMDAgNTBMMjUwIDUwTDIyNSAxMDBaIiBmaWxsPSJyZ2JhKDI1NSwyNTUsMjU1LDAuMDgpIi8+PC9zdmc+',

  // main
  'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48ZGVmcz48bGluZWFyR3JhZGllbnQgaWQ9Im9yYW5nZUdyYWRpZW50IiB4MT0iMCUiIHkxPSIwJSIgeDI9IjEwMCUiIHkyPSIxMDAlIj48c3RvcCBvZmZzZXQ9IjAlIiBzdHlsZT0ic3RvcC1jb2xvcjojRkY5ODAwO3N0b3Atb3BhY2l0eToxIiAvPjxzdG9wIG9mZnNldD0iMTAwJSIgc3R5bGU9InN0b3AtY29sb3I6I0VGNkMwMDtzdG9wLW9wYWNpdHk6MSIgLz48L2xpbmVhckdyYWRpZW50PjwvZGVmcz48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSJ1cmwoI29yYW5nZUdyYWRpZW50KSIvPjxlbGxpcHNlIGN4PSI4MCIgY3k9IjYwIiByeD0iNDAiIHJ5PSIyNSIgZmlsbD0icmdiYSgyNTUsMjU1LDI1NSwwLjEpIi8+PHJlY3QgeD0iMjAwIiB5PSIzMCIgd2lkdGg9IjYwIiBoZWlnaHQ9IjYwIiByeD0iMzAiIGZpbGw9InJnYmEoMjU1LDI1NSwyNTUsMC4xMikiLz48cG9seWdvbiBwb2ludHM9IjUwLDE1MCA5MCwxNTAgNzAsMTkwIiBmaWxsPSJyZ2JhKDI1NSwyNTUsMjU1LDAuMDgpIi8+PC9zdmc+',

  // main
  'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48ZGVmcz48bGluZWFyR3JhZGllbnQgaWQ9InB1cnBsZUdyYWRpZW50IiB4MT0iMCUiIHkxPSIwJSIgeDI9IjEwMCUiIHkyPSIxMDAlIj48c3RvcCBvZmZzZXQ9IjAlIiBzdHlsZT0ic3RvcC1jb2xvcjojOWM0ZGNjO3N0b3Atb3BhY2l0eToxIiAvPjxzdG9wIG9mZnNldD0iMTAwJSIgc3R5bGU9InN0b3AtY29sb3I6IzVhNjc5ODtzdG9wLW9wYWNpdHk6MSIgLz48L2xpbmVhckdyYWRpZW50PjwvZGVmcz48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSJ1cmwoI3B1cnBsZUdyYWRpZW50KSIvPjxjaXJjbGUgY3g9IjYwIiBjeT0iNDAiIHI9IjE4IiBmaWxsPSJyZ2JhKDI1NSwyNTUsMjU1LDAuMTUpIi8+PHJlY3QgeD0iMTgwIiB5PSIxMjAiIHdpZHRoPSI5MCIgaGVpZ2h0PSI0MCIgcng9IjIwIiBmaWxsPSJyZ2JhKDI1NSwyNTUsMjU1LDAuMSkiLz48Y2lyY2xlIGN4PSIyNDAiIGN5PSI3MCIgcj0iMjIiIGZpbGw9InJnYmEoMjU1LDI1NSwyNTUsMC4wOCkiLz48L3N2Zz4=',

  // main
  'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48ZGVmcz48bGluZWFyR3JhZGllbnQgaWQ9InJlZEdyYWRpZW50IiB4MT0iMCUiIHkxPSIwJSIgeDI9IjEwMCUiIHkyPSIxMDAlIj48c3RvcCBvZmZzZXQ9IjAlIiBzdHlsZT0ic3RvcC1jb2xvcjojZjU2YzZjO3N0b3Atb3BhY2l0eToxIiAvPjxzdG9wIG9mZnNldD0iMTAwJSIgc3R5bGU9InN0b3AtY29sb3I6I2UzMzY0NTtzdG9wLW9wYWNpdHk6MSIgLz48L2xpbmVhckdyYWRpZW50PjwvZGVmcz48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSJ1cmwoI3JlZEdyYWRpZW50KSIvPjxyZWN0IHg9IjIwIiB5PSIyMCIgd2lkdGg9IjUwIiBoZWlnaHQ9IjUwIiByeD0iOCIgZmlsbD0icmdiYSgyNTUsMjU1LDI1NSwwLjEyKSIvPjxjaXJjbGUgY3g9IjIyMCIgY3k9IjE0MCIgcj0iMzUiIGZpbGw9InJnYmEoMjU1LDI1NSwyNTUsMC4wOCkiLz48cG9seWdvbiBwb2ludHM9IjEyMCwzMCAxNzAsMzAgMTQ1LDgwIiBmaWxsPSJyZ2JhKDI1NSwyNTUsMjU1LDAuMSkiLz48L3N2Zz4=',

  // main
  'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48ZGVmcz48bGluZWFyR3JhZGllbnQgaWQ9ImN5YW5HcmFkaWVudCIgeDE9IjAlIiB5MT0iMCUiIHgyPSIxMDAlIiB5Mj0iMTAwJSI+PHN0b3Agb2Zmc2V0PSIwJSIgc3R5bGU9InN0b3AtY29sb3I6IzE3YTJiODtzdG9wLW9wYWNpdHk6MSIgLz48c3RvcCBvZmZzZXQ9IjEwMCUiIHN0eWxlPSJzdG9wLWNvbG9yOiMxMzc5OGU7c3RvcC1vcGFjaXR5OjEiIC8+PC9saW5lYXJHcmFkaWVudD48L2RlZnM+PHJlY3Qgd2lkdGg9IjEwMCUiIGhlaWdodD0iMTAwJSIgZmlsbD0idXJsKCNjeWFuR3JhZGllbnQpIi8+PGVsbGlwc2UgY3g9IjE1MCIgY3k9IjQ0IiByeD0iNjAiIHJ5PSIyMCIgZmlsbD0icmdiYSgyNTUsMjU1LDI1NSwwLjEpIi8+PGNpcmNsZSBjeD0iNzAiIGN5PSIxMzAiIHI9IjI4IiBmaWxsPSJyZ2JhKDI1NSwyNTUsMjU1LDAuMTIpIi8+PHJlY3QgeD0iMjIwIiB5PSIxMzAiIHdpZHRoPSI1MCIgaGVpZ2h0PSI1MCIgcng9IjI1IiBmaWxsPSJyZ2JhKDI1NSwyNTUsMjU1LDAuMDgpIi8+PC9zdmc+',

  // main - to 7
  'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjZjVmNWY1Ii8+PHBhdGggZD0iTTEwMCA5MGMwIDEyLjQxOCAxMC4wODIgMjIuNSAyMi41IDIyLjVzMjIuNS0xMC4wODIgMjIuNS0yMi41Uzg3LjQxOCA2Ny41IDc1IDY3LjUgMTAwIDc3LjU4MiAxMDAgOTB6bTc4IDkwTDE1MCAyMDBsLTMwLTMwLTQwIDQweiIgZmlsbD0iI2RkZCIvPjx0ZXh0IHg9IjE1MCIgeT0iMTgwIiBmb250LWZhbWlseT0iQXJpYWwiIGZvbnQtc2l6ZT0iMTQiIGZpbGw9IiM5OTkiIHRleHQtYW5jaG9yPSJtaWRkbGUiPjMwMCB4IDIwMDwvdGV4dD48L3N2Zz4='
]

// algorithm ID after Long, JavaScript .
const getNumericModulo = (value, divisor) => {
  const digits = String(value ?? '')
  if (!/^\d+$/.test(digits)) return 0

  let remainder = 0
  for (const digit of digits) {
    remainder = (remainder * 10 + Number(digit)) % divisor
  }
  return remainder
}

// Get algorithm
const getAlgorithmCardBackground = (algorithm, repositoryId) => {
  // if algorithm already ,
  if (algorithm.imageUrl && algorithm.imageUrl.trim() !== '') {
    return algorithm.imageUrl
  }

  // algorithmID and ID
  const algorithmIndex = getNumericModulo(algorithm.id, cardBackgrounds.length)
  const repoIndex = getNumericModulo(repositoryId, cardBackgrounds.length)
  const backgroundIndex = (algorithmIndex + repoIndex) % cardBackgrounds.length

  return cardBackgrounds[backgroundIndex]
}

// property

// menu ( Generate )
const topMenus = computed(() => {
  const menus = []

  // all algorithm
  algorithmRepositories.value.forEach(repo => {
    if (repo.status === 1) {
      menus.push({
        key: repo.id.toString(),
        label: repo.name
      })
    }
  })

  // algorithm
  menus.push({
    key: 'management',
    label: '算法库管理'
  })

  return menus
})

// current
const currentPageRepositories = computed(() => {
  return algorithmRepositories.value
})

// current algorithm
const currentPageAlgorithms = computed(() => {
  if (activeCategory.value === 'all') {
    return algorithms.value
  }
  return algorithms.value.filter(alg => alg.category === activeCategory.value)
})

// whether button
const showAddButton = computed(() => {
  // algorithm page button
  if (activeTopMenu.value === 'management') {
    return false
  }

  // current
  const currentRepo = algorithmRepositories.value.find(repo =>
      repo.id.toString() === activeTopMenu.value
  )

  // only algorithm button
  return currentRepo
})

// method
const formatDateTime = (dateStr) => {
  if (!dateStr) return '-'
  try {
    return new Date(dateStr).toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    }).replace(/\//g, '-')
  } catch (e) {
    return dateStr
  }
}

const getRepositoryTypeText = (type) => {
  const typeMap = {
    'basic': '基础算法库',
    'extended': '扩展算法库',
    'test': '测试算法库'
  }
  return typeMap[type] || type
}

const getRepositoryTypeTagType = (type) => {
  const typeMap = {
    'basic': 'primary',
    'extended': 'success',
    'test': 'warning'
  }
  return typeMap[type] || ''
}

// API method

const normalizeRepositoryId = (repositoryId) => {
  const normalizedRepositoryId = String(repositoryId ?? '').trim()
  if (!/^\d+$/.test(normalizedRepositoryId) || /^0+$/.test(normalizedRepositoryId)) {
    return null
  }
  return normalizedRepositoryId
}

// Load algorithm
const loadAlgorithmRepositories = async () => {
  try {
    repositoriesLoading.value = true
    const response = await getAlgorithmRepositoryPage({
      current: repositoryCurrentPage.value,
      size: repositoryPageSize.value
    })

    if (response.code === 200) {
      algorithmRepositories.value = response.data.records || []
      repositoryTotal.value = response.data.total || 0

      // Format create time
      algorithmRepositories.value.forEach(repo => {
        repo.createTime = formatDateTime(repo.createTime)
      })
    } else {
      ElMessage.error(response.message || '加载算法仓库失败')
    }
  } catch (error) {
    console.error('加载算法仓库失败:', error)
    ElMessage.error('加载算法仓库失败')
  } finally {
    repositoriesLoading.value = false
  }
}

// Load algorithm
const loadAlgorithmsByRepository = async (repositoryId) => {
  const requestId = ++algorithmRequestId.value
  const normalizedRepositoryId = normalizeRepositoryId(repositoryId)
  if (!normalizedRepositoryId) {
    algorithms.value = []
    algorithmTotal.value = 0
    currentRepositoryId.value = null
    algorithmsLoading.value = false
    return
  }

  // algorithm after null / empty old data, before algorithm.
  algorithms.value = []
  algorithmTotal.value = 0
  currentRepositoryId.value = normalizedRepositoryId

  try {
    algorithmsLoading.value = true
    const response = await getAlgorithmPage({
      current: 1,
      size: 1000, // page Get all algorithm
      repositoryId: normalizedRepositoryId
    })

    // algorithm , , data page.
    if (requestId !== algorithmRequestId.value) {
      return
    }

    if (response.code === 200) {
      algorithms.value = response.data.records || []
      algorithmTotal.value = response.data.total || 0
    } else {
      ElMessage.error(response.message || '加载算法列表失败')
    }
  } catch (error) {
    console.error('加载算法列表失败:', error)
    ElMessage.error('加载算法列表失败')
  } finally {
    if (requestId === algorithmRequestId.value) {
      algorithmsLoading.value = false
    }
  }
}

const loadDeviceList = async () => {
  deviceLoading.value = true
  try {
    const response = await getMqttDevicePage({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      ...deviceFilter.value
    })
    const devices = Array.isArray(response?.rows) ? response.rows : []
    const startIndex = (currentPage.value - 1) * pageSize.value
    deviceTableData.value = devices.map((device, idx) => ({
      ...device,
      index: startIndex + idx + 1
    }))
    totalDevices.value = Number(response?.total || 0)
  } catch (error) {
    console.error('load device list failed:', error)
    ElMessage.error('加载设备列表失败')
  } finally {
    deviceLoading.value = false
  }
}

// Initialize data
const initData = async () => {
  await loadAlgorithmRepositories()
  // if , Load algorithm
  if (algorithmRepositories.value.length > 0) {
    const firstEnabledRepo = algorithmRepositories.value.find(repo => repo.status === 1)
    if (firstEnabledRepo) {
      activeTopMenu.value = firstEnabledRepo.id.toString()
      await loadAlgorithmsByRepository(firstEnabledRepo.id)
    }
  }
}

// method
const setActiveTopMenu = async (menu) => {
  console.log('切换顶部菜单到:', menu)
  activeTopMenu.value = menu
  // menu , to " full "
  activeCategory.value = 'all'

  if (menu === 'management') {
    // algorithm page
    console.log('切换到算法库管理页面')
  } else {
    // algorithm , Load algorithm
    await loadAlgorithmsByRepository(menu)
  }
}

const setActiveCategory = (category) => {
  activeCategory.value = category
}

// algorithmrelatedoperation
const addAlgorithm = () => {
  // Get current algorithm ID
  const repositoryId = normalizeRepositoryId(activeTopMenu.value)
  if (!repositoryId) {
    ElMessage.error('无法获取算法库信息')
    return
  }

  // form
  algorithmAddForm.value = {
    name: '',
    category: '',
    type: 'detect',
    version: '1.0.0',
    modelFilePath: '',
    description: '',
    repositoryId: repositoryId
  }

  showAlgorithmAddDialog.value = true
}

const editAlgorithm = async (algorithm) => {
  // Set algorithm
  editingAlgorithm.value = algorithm

  // Load algorithmdata form
  algorithmEditForm.value = {
    name: algorithm.name || '',
    category: algorithm.category || '',
    type: algorithm.type || 'detect',
    ptModelFilePath: algorithm.ptModelFilePath,
    onnxModelFilePath: algorithm.onnxModelFilePath,
    isSystem: algorithm.isSystem,
    imageUrl: algorithm.imageUrl || '',
    description: algorithm.description || '',
    repositoryId: algorithm.repositoryId || currentRepositoryId.value
  }

  llmReviewForm.value = defaultLlmReviewForm()
  try {
    const [configResponse, providersResponse] = await Promise.all([
      getAlgorithmLlmReviewConfig(algorithm.id),
      getLlmProviders()
    ])
    llmReviewForm.value = { ...defaultLlmReviewForm(), ...(configResponse.data || {}) }
    llmProviders.value = providersResponse.data || []
  } catch (error) {
    ElMessage.warning('大模型复核配置加载失败，可稍后重试')
  }

  // dialog
  showAlgorithmEditDialog.value = true
}

const evaluateAlgorithm = async (algorithm) => {
  try {
    ElMessage.info('正在进行算法评估...')
    const response = await apiEvaluateAlgorithm(algorithm.id)

    if (response.code === 200) {
      ElMessage.success('算法评估完成')
      console.log('评估结果:', response.data)
    } else {
      ElMessage.error(response.message || '算法评估失败')
    }
  } catch (error) {
    console.error('算法评估失败:', error)
    ElMessage.error('算法评估失败')
  }
}

const beforeAlgorithmImageUpload = (file) => {
  const acceptedTypes = ['image/jpeg', 'image/png', 'image/webp']
  const isImage = acceptedTypes.includes(file.type)
  const isWithinLimit = file.size / 1024 / 1024 <= 5

  if (!isImage) {
    ElMessage.error('图片格式仅支持 JPG、PNG 或 WEBP')
  }
  if (!isWithinLimit) {
    ElMessage.error('图片大小不能超过 5MB')
  }

  if (isImage && isWithinLimit) {
    imageUploading.value = true
  }
  return isImage && isWithinLimit
}

const handleAlgorithmImageUploadSuccess = (response) => {
  imageUploading.value = false
  const imageUrl = response?.data?.url
  if (response?.code === 200 && imageUrl) {
    algorithmEditForm.value.imageUrl = imageUrl
    ElMessage.success('图片上传成功')
  } else {
    ElMessage.error(response?.message || response?.msg || '图片上传失败')
  }
}

const handleAlgorithmImageUploadError = () => {
  imageUploading.value = false
  ElMessage.error('图片上传失败')
}

const clearAlgorithmImage = () => {
  algorithmEditForm.value.imageUrl = ''
}

const deployAlgorithm = async (algorithm) => {
  selectedAlgorithm.value = algorithm
  showDeviceDrawer.value = true
  selectedDeviceRows.value = []
  currentPage.value = 1
  deviceFilter.value = {}
  await loadDeviceList()
}

/* * Model Hub: token , token userinfo */
const publishToModelHub = (algorithm) => {
  const pending = algorithm
      ? {
        algorithmId: algorithm.id,
        algorithmName: algorithm.name,
        from: 'algorithm-management'
      }
      : { from: 'algorithm-management' }

  if (!getModelHubAccessToken()) {
    ElMessage.info('请先登录后继续发布到 Model Hub')
    startModelHubLogin(pending)
    return
  }

  router.push(getCloudPlatformUserPath())
}

const handleDeleteAlgorithm = async (algorithm) => {
  try {
    await ElMessageBox.confirm(
        `确定要删除算法"${algorithm.name}"吗？`,
        '确认删除',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
    )

    const response = await deleteAlgorithm(algorithm.id)
    if (response.code === 200) {
      ElMessage.success('删除成功')
      // new Load current algorithm
      if (currentRepositoryId.value) {
        await loadAlgorithmsByRepository(currentRepositoryId.value)
      }
    } else {
      ElMessage.error(response.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除算法失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// device related method
const handleDrawerClose = (done) => {
  done()
}

const handleDeviceFilterChange = async (filter) => {
  deviceFilter.value = filter || {}
  currentPage.value = 1
  selectedDeviceRows.value = []
  await loadDeviceList()
}

const handleDeviceSelectionChange = (selection) => {
  selectedDeviceRows.value = selection
}

const handleDeployToDevice = async () => {
  if (!selectedAlgorithm.value) {
    ElMessage.warning('请先选择要下发的算法')
    return
  }

  const deviceIds = selectedDeviceRows.value
      .map(item => item.deviceId)
      .filter(Boolean)

  if (deviceIds.length === 0) {
    ElMessage.warning('请选择要下发的设备')
    return
  }

  const deviceIdsStr = deviceIds.join(',')

  try {
    const response = await dispatchAlgorithmToDevices(
        selectedAlgorithm.value.id,
        deviceIdsStr,
        dispatchModelType.value
    )
    if (response.code === 200) {
      ElMessage.success('下发成功')
      showDeviceDrawer.value = false
    } else {
      ElMessage.error(response.msg || response.message || '下发失败')
    }
  } catch (error) {
    console.error('下发失败:', error)
    // already after , prompt / tip .
  }
}

// algorithm
const handleRepositorySizeChange = async (val) => {
  repositoryPageSize.value = val
  repositoryCurrentPage.value = 1
  await loadAlgorithmRepositories()
}

const handleRepositoryCurrentChange = async (val) => {
  repositoryCurrentPage.value = val
  await loadAlgorithmRepositories()
}



// devicerelated method
const handleSizeChange = async (val) => {
  pageSize.value = val
  currentPage.value = 1
  await loadDeviceList()
}

const handleCurrentChange = async (val) => {
  currentPage.value = val
  await loadDeviceList()
}

// algorithm related method
const handleLibrarySelectionChange = (selection) => {
  selectedRepositories.value = selection
}

const addAlgorithmLibrary = () => {
  editingRepository.value = null
  addForm.value = {
    name: '',
    repositoryType: 'extended',
    status: 1,
    remark: ''
  }
  showAddDialog.value = true
}

const editAlgorithmLibrary = () => {
  if (selectedRepositories.value.length === 0) {
    ElMessage.warning('请先选择要编辑的算法库')
    return
  }

  const repository = selectedRepositories.value[0]
  editingRepository.value = repository
  addForm.value = {
    name: repository.name,
    repositoryType: repository.repositoryType,
    status: repository.status,
    remark: repository.remark || ''
  }
  showAddDialog.value = true
}

const batchDeleteAlgorithmLibrary = async () => {
  if (selectedRepositories.value.length === 0) {
    ElMessage.warning('请先选择要删除的算法库')
    return
  }

  try {
    const basicRepos = selectedRepositories.value.filter(repo => repo.repositoryType === 'basic')
    if (basicRepos.length > 0) {
      ElMessage.warning('基础算法库不能删除')
      return
    }

    await ElMessageBox.confirm(
        `确定要删除选中的 ${selectedRepositories.value.length} 个算法库吗？`,
        '确认删除',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
    )

    const ids = selectedRepositories.value.map(repo => repo.id)
    const response = await batchDeleteAlgorithmRepositories(ids)

    if (response.code === 200) {
      ElMessage.success('删除成功')
      selectedRepositories.value = []
      await loadAlgorithmRepositories()
    } else {
      ElMessage.error(response.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const managementToolbarButtonList = computed(() => [
  { name: '编辑', svg: 'table_edit', clickFn: editAlgorithmLibrary },
  { name: '删除', svg: 'table_del', clickFn: batchDeleteAlgorithmLibrary }
])

const editLibraryItem = (row) => {
  editingRepository.value = row
  addForm.value = {
    name: row.name,
    repositoryType: row.repositoryType,
    status: row.status,
    remark: row.remark || ''
  }
  showAddDialog.value = true
}

const deleteLibraryItem = async (row) => {
  if (row.repositoryType === 'basic') {
    ElMessage.warning('基础算法库不能删除')
    return
  }

  try {
    await ElMessageBox.confirm(
        `确定要删除算法库"${row.name}"吗？`,
        '确认删除',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
    )

    const response = await deleteAlgorithmRepository(row.id)
    if (response.code === 200) {
      ElMessage.success('删除成功')
      await loadAlgorithmRepositories()
    } else {
      ElMessage.error(response.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const toggleRepositoryStatus = async (row) => {
  try {
    const newStatus = row.status === 1 ? 0 : 1
    const response = await updateAlgorithmRepositoryStatus(row.id, newStatus)

    if (response.code === 200) {
      ElMessage.success(`${newStatus === 1 ? '启用' : '禁用'}成功`)
      await loadAlgorithmRepositories()
    } else {
      ElMessage.error(response.message || '状态更新失败')
    }
  } catch (error) {
    console.error('状态更新失败:', error)
    ElMessage.error('状态更新失败')
  }
}

const handleAddConfirm = async () => {
  if (!addFormRef.value) return

  try {
    await addFormRef.value.validate()
    submitting.value = true

    const formData = {
      name: addForm.value.name.trim(),
      repositoryType: addForm.value.repositoryType,
      status: addForm.value.status,
      remark: addForm.value.remark.trim()
    }

    let response
    if (editingRepository.value) {
      //
      response = await updateAlgorithmRepository(editingRepository.value.id, formData)
    } else {
      // Add
      response = await createAlgorithmRepository(formData)
    }

    if (response.code === 200) {
      ElMessage.success(`${editingRepository.value ? '更新' : '创建'}成功`)
      showAddDialog.value = false
      await loadAlgorithmRepositories()

      // form
      addForm.value = {
        name: '',
        repositoryType: 'extended',
        status: 1,
        remark: ''
      }
      editingRepository.value = null
    } else {
      ElMessage.error(response.message || `${editingRepository.value ? '更新' : '创建'}失败`)
    }
  } catch (error) {
    if (typeof error === 'object' && error.message) {
      // form
      return
    }
    console.error('操作失败:', error)
    ElMessage.error('操作失败')
  } finally {
    submitting.value = false
  }
}

const handleAddCancel = () => {
  showAddDialog.value = false
  editingRepository.value = null
  addForm.value = {
    name: '',
    repositoryType: 'extended',
    status: 1,
    remark: ''
  }
  if (addFormRef.value) {
    addFormRef.value.resetFields()
  }
}

// algorithm dialogProcess method
const handleAlgorithmAddConfirm = async () => {
  try {
    // form
    await algorithmAddFormRef.value.validate()

    submitting.value = true

    // to new algorithm
    const randomBackgroundIndex = Math.floor(Math.random() * cardBackgrounds.length)
    const assignedBackground = cardBackgrounds[randomBackgroundIndex]

    // algorithmdata
    const algorithmData = {
      repositoryId: algorithmAddForm.value.repositoryId,
      name: algorithmAddForm.value.name,
      category: algorithmAddForm.value.category,
      description: algorithmAddForm.value.description,
      ptModelFilePath: algorithmEditForm.value.ptModelFilePath,
      onnxModelFilePath: algorithmEditForm.value.onnxModelFilePath,
      isSystem: algorithmEditForm.value.isSystem,
      inputFormat: 'image',
      outputFormat: 'json',
      gpuRequired: 0,
      imageUrl: assignedBackground //
    }

    // algorithm API
    const response = await createAlgorithm(algorithmData)

    if (response.code === 200) {
      ElMessage.success('算法添加成功')
      showAlgorithmAddDialog.value = false

      // new Load current algorithm algorithm
      if (algorithmAddForm.value.repositoryId) {
        await loadAlgorithmsByRepository(algorithmAddForm.value.repositoryId)
      }
    } else {
      ElMessage.error(response.message || '算法添加失败')
    }

  } catch (error) {
    if (error.message) {
      console.error('算法添加失败:', error)
      ElMessage.error('算法添加失败')
    }
  } finally {
    submitting.value = false
  }
}

const handleAlgorithmAddCancel = () => {
  showAlgorithmAddDialog.value = false
  algorithmAddForm.value = {
    name: '',
    category: '',
    type: 'detect',
    description: '',
    repositoryId: null
  }
  if (algorithmAddFormRef.value) {
    algorithmAddFormRef.value.resetFields()
  }
}

// algorithm dialogProcess method
const handleAlgorithmEditConfirm = async () => {
  let algorithmSaved = false
  try {
    // form
    await algorithmEditFormRef.value.validate()

    submitting.value = true

    // new data
    const updateData = {
      name: algorithmEditForm.value.name,
      category: algorithmEditForm.value.category,
      ptModelFilePath: algorithmEditForm.value.ptModelFilePath,
      onnxModelFilePath: algorithmEditForm.value.onnxModelFilePath,
      isSystem: algorithmEditForm.value.isSystem,
      description: algorithmEditForm.value.description,
      inputFormat: 'image',
      outputFormat: 'json',
      gpuRequired: 0,
      imageUrl: algorithmEditForm.value.imageUrl.trim()
    }

    // algorithm new API
    const response = await updateAlgorithm(editingAlgorithm.value.id, updateData)

    if (response.code === 200) {
      algorithmSaved = true
      if (llmReviewForm.value.enabled && !llmReviewForm.value.providerId) {
        throw new Error('开启大模型复核时必须选择视觉大模型')
      }
      const reviewResponse = await saveAlgorithmLlmReviewConfig(
        editingAlgorithm.value.id,
        llmReviewForm.value
      )
      if (reviewResponse.code !== 200) {
        throw new Error(reviewResponse.message || '大模型复核配置保存失败')
      }
      ElMessage.success('算法及大模型复核配置更新成功')
      showAlgorithmEditDialog.value = false

      // new Load current algorithm algorithm
      if (algorithmEditForm.value.repositoryId) {
        await loadAlgorithmsByRepository(algorithmEditForm.value.repositoryId)
      }
    } else {
      ElMessage.error(response.message || '算法更新失败')
    }

  } catch (error) {
    if (error.message) {
      console.error('算法更新失败:', error)
      ElMessage.error(algorithmSaved ? `算法已保存，但${error.message}` : error.message)
    }
  } finally {
    submitting.value = false
  }
}

const handleAlgorithmEditCancel = () => {
  showAlgorithmEditDialog.value = false
  imageUploading.value = false
  algorithmEditForm.value = {
    name: '',
    category: '',
    type: 'detect',
    version: '1.0.0',
    description: '',
    repositoryId: null,
    imageUrl: ''
  }
  llmReviewForm.value = defaultLlmReviewForm()
  if (algorithmEditFormRef.value) {
    algorithmEditFormRef.value.resetFields()
  }
}



// pageInitialize
onMounted(() => {
  initData()
})
</script>

<style scoped>
.llm-review-tip {
  margin-left: 10px;
  color: #8c8c8c;
  font-size: 12px;
}
.algorithm-management {
  height: 100%;
  margin: 0;
  padding: 0;
  background: #fff;
  border-radius: var(--common-border-radius) var(--common-border-radius) 0 0;
  overflow: hidden;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.tenant_content {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 0 20px 20px;
  box-sizing: border-box;
}

/* Tab + ModelHub ( tenanat-tabs) */
.top-tabs-header {
  display: flex;
  align-items: flex-end;
  background: #fff;
  flex-shrink: 0;
  padding-right: 16px;
  border-radius: var(--common-border-radius) var(--common-border-radius) 0 0;
}

.top-tabs-header :deep(.tenanat-tabs) {
  flex: 1;
  min-width: 0;
  padding-right: 0;
}

.modelhub-button {
  flex-shrink: 0;
  align-self: center;
  margin: 0 0 8px 12px;
  color: #409eff;
  font-size: 14px;
  line-height: 20px;
  text-decoration: underline;
  text-underline-offset: 2px;
  transition: color 0.2s;
}

.modelhub-button:hover {
  color: #3d70ff;
}

/* category-tabs button */
.add-toolbar {
  padding: 8px 0 16px;
}

/* : in ( ) */
.category-tabs-wrap {
  margin: 0 -20px;
  background: #fff;
}

:deep(.tenanat-tabs_act) {
  .el-tabs__header {
    padding: 0 20px;
    margin-bottom: 0;
  }

  .el-tabs__nav-wrap::after {
    display: none;
  }

  .el-tabs__active-bar {
    display: none;
  }

  .el-tabs__item {
    position: relative;
    color: #999;
    font-size: 16px;
    font-weight: 400;
    height: 44px;
    line-height: 44px;
    padding: 0 20px !important;
  }

  /* Element Plus tab padding-left:0, will , */
  .el-tabs__item:nth-child(2) {
    padding-left: 20px !important;
  }

  .el-tabs__item:hover {
    color: var(--el-color-primary);
  }

  .el-tabs__item.is-active {
    color: var(--el-color-primary);
    font-weight: 500;
  }

  .el-tabs__item.is-active::after {
    content: '';
    position: absolute;
    left: 50%;
    bottom: 6px;
    transform: translateX(-50%);
    width: 20px;
    height: 3px;
    border-radius: 2px;
    background: var(--el-color-primary);
  }
}

/* algorithm */
.algorithm-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  padding: 0;
  width: 100%;
}

.algorithm-card {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  cursor: pointer;
}

.algorithm-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.card-image {
  width: 100%;
  height: 200px;
  overflow: hidden;
  position: relative;
}

.card-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  cursor: pointer;
  transition: transform 0.3s ease;
}

.algorithm-card:hover .card-image img {
  transform: scale(1.05);
}

.algorithm-image-editor {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
}

.algorithm-image-uploader :deep(.el-upload) {
  display: block;
}

.algorithm-image-preview {
  position: relative;
  width: 220px;
  height: 146px;
  overflow: hidden;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
  background: #f5f7fa;
  cursor: pointer;
}

.algorithm-image-preview img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.algorithm-image-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 14px;
  background: rgba(0, 0, 0, 0.45);
  opacity: 0;
  transition: opacity 0.2s;
}

.algorithm-image-preview:hover .algorithm-image-overlay {
  opacity: 1;
}

.algorithm-image-tip {
  color: #909399;
  font-size: 12px;
  line-height: 18px;
}

.card-menu {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 10;
}

.menu-trigger {
  width: 32px;
  height: 32px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.menu-trigger:hover {
  background: rgba(255, 255, 255, 1);
  transform: scale(1.1);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.menu-trigger .el-icon {
  color: #606266;
  font-size: 16px;
}

/* menu */
:deep(.el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
}

:deep(.el-dropdown-menu__item .el-icon) {
  font-size: 14px;
  color: #606266;
}

:deep(.el-dropdown-menu__item:hover .el-icon) {
  color: #409eff;
}

.card-content {
  padding: 16px 20px 20px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
  line-height: 1.4;
}

.card-subtitle {
  font-size: 14px;
  color: #909399;
  line-height: 1.4;
  margin-bottom: 8px;
}

.card-description {
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-meta {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.version {
  font-size: 12px;
  color: #909399;
}



/* tableoperationbutton */
.table-action-buttons {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

/* operationbutton to */
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

/* table */
.table-pagination {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

/*  */
@media (max-width: 1200px) {
  .algorithm-grid {
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 20px;
  }
}

@media (max-width: 768px) {
  .algorithm-management {
    padding: 16px;
  }

  .top-tabs-header {
    overflow-x: auto;
    padding-right: 12px;
  }

  .modelhub-button {
    flex-shrink: 0;
    margin-left: 8px;
  }

  .category-tabs-wrap {
    margin: 0 -16px;
  }

  .algorithm-grid {
    grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
    gap: 16px;
  }
}

@media (max-width: 480px) {
  .algorithm-grid {
    grid-template-columns: 1fr;
  }
}

/* Load */
.algorithm-card {
  animation: fadeInUp 0.5s ease forwards;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* to */
.algorithm-card:nth-child(1) { animation-delay: 0.1s; }
.algorithm-card:nth-child(2) { animation-delay: 0.2s; }
.algorithm-card:nth-child(3) { animation-delay: 0.3s; }
.algorithm-card:nth-child(4) { animation-delay: 0.4s; }
.algorithm-card:nth-child(5) { animation-delay: 0.5s; }
.algorithm-card:nth-child(6) { animation-delay: 0.6s; }

/*  */
.algorithm-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.1) 0%, rgba(64, 158, 255, 0.05) 100%);
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
}

.algorithm-card:hover::before {
  opacity: 1;
}

/* device */
.device-drawer-content {
  height: 100%;
  padding: 0;
  background: #fff;

  :deep(.tenant_content),
  :deep(.tableTenBox) {
    height: 100%;
    padding: 0;
  }
}

.device-table-panel {
  flex: 1;
  min-width: 0;
  height: 100%;
  overflow: auto;
  display: flex;
  flex-direction: column;
}

/* algorithm */
.algorithm-management-container {
  margin-top: 0;
  padding: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  margin-top: 20px;
}

.algorithm-management-container :deep(.header_tenant_cell) {
  background: #F8F8F9;
}

.paginationBox {
  justify-content: center;
  height: 100px;
}

.operateAppBox {
  justify-content: flex-end;
  gap: 2px;
}

.operateAppBox .new_table_svg_group.is-disabled {
  opacity: 0.4;
  cursor: not-allowed;
  pointer-events: none;
}

/* main new */
:deep(.el-button--primary) {
  background-color: #1A53FF;
  border-color: #1A53FF;
}

:deep(.el-button--primary:hover) {
  background-color: #3d70ff;
  border-color: #3d70ff;
}

/* table */
:deep(.el-table) {
  border: 1px solid #ebeef5;
  border-radius: 4px;
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

/* Drawer */
:deep(.el-drawer__header) {
  margin-bottom: 0;
  padding: 20px 24px 16px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}

/* tableoperation button */
.table-action-buttons {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

/* operationbutton to */
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

/* Add */
.add-form {
  padding: 20px 0;
}

.add-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #262626;
}

.add-form :deep(.el-form-item__label::before) {
  color: #ff4d4f;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

:deep(.el-drawer__title) {
  font-size: 16px;
  font-weight: 400;
  color: #191919;
  letter-spacing: 0.4px;
}

:deep(.el-drawer__body) {
  padding: 24px;
  height: calc(100% - 60px);
  box-sizing: border-box;
}

</style>

