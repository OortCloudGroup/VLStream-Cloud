<template>
  <div class="algorithm-management">
    <!-- 顶部菜单栏 -->
    <div class="top-menu-bar" v-loading="repositoriesLoading">
      <div 
        v-for="menu in topMenus"
        :key="menu.key"
        class="top-menu-item"
        :class="{ active: activeTopMenu === menu.key }"
        @click="setActiveTopMenu(menu.key)"
      >
        {{ menu.label }}
      </div>
    </div>

    <!-- 分类标签栏和添加按钮 -->
    <div class="category-tabs"  v-if="showAddButton">
      <div class="add-button-section">
        <el-button type="primary" @click="addAlgorithm">
          <el-icon><Plus /></el-icon>
          添加
        </el-button>
      </div>
      <div v-if="typeOptions.length > 0" class="category-section">
        <div 
          v-for="category in typeOptions"
          :key="category.value"
          class="category-tab"
          :class="{ active: activeCategory === category.value }"
          @click="setActiveCategory(category.value)"
        >
          {{ category.label }}
        </div>
      </div>
    </div>

    <!-- 算法网格 -->
    <div v-if="activeTopMenu !== 'management'" class="algorithm-grid" v-loading="algorithmsLoading">
      <div 
        v-for="algorithm in currentPageAlgorithms"
        :key="algorithm.id"
        class="algorithm-card"
      >
        <div class="card-image">
          <img :src="getAlgorithmCardBackground(algorithm, currentRepositoryId)" :alt="algorithm.name" />
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

    <!-- 算法库管理表格 -->
    <div v-if="activeTopMenu === 'management'" class="algorithm-management-container">
      <!-- 工具栏 -->
      <div class="management-toolbar">
        <ActionButtonGroup 
          :selected-count="selectedRepositories.length"
          @add="addAlgorithmLibrary"
          @edit="editAlgorithmLibrary"
          @delete="batchDeleteAlgorithmLibrary"
        />
      </div>

      <!-- 表格内容 -->
      <div class="management-table-content">
        <el-table 
          :data="currentPageRepositories" 
          stripe 
          v-loading="repositoriesLoading"
          @selection-change="handleLibrarySelectionChange"
        >
          <el-table-column type="selection" width="55" />
          <el-table-column type="index" label="序号" width="80" align="center" 
            :index="(index) => (repositoryCurrentPage - 1) * repositoryPageSize + index + 1" />
          <el-table-column prop="name" label="名称" min-width="200" />
          <el-table-column prop="remark" label="备注" min-width="300" />
          <el-table-column prop="algorithmCount" label="拥有算法" width="120" align="center" />
          <el-table-column prop="repositoryType" label="类型" width="120" align="center">
            <template #default="scope">
              <el-tag :type="getRepositoryTypeTagType(scope.row.repositoryType)">
                {{ getRepositoryTypeText(scope.row.repositoryType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="scope">
              <el-tag
                  :type="scope.row.status === 1 ? 'success' : 'danger'"
                  size="small">
                {{ scope.row.status === 1 ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="180" />
          <el-table-column label="操作" width="240" fixed="right" align="right">
            <template #default="scope">
              <div class="table-action-buttons">
                <el-button 
                  type="primary" 
                  text
                  size="small" 
                  @click="editLibraryItem(scope.row)"
                >
                  编辑
                </el-button>
                <el-button 
                  type="primary" 
                  text
                  size="small" 
                  @click="toggleRepositoryStatus(scope.row)"
                >
                  {{ scope.row.status === 'enabled' ? '禁用' : '启用' }}
                </el-button>
                <el-button 
                  type="danger" 
                  text
                  size="small" 
                  @click="deleteLibraryItem(scope.row)"
                  :disabled="scope.row.repositoryType === 'basic'"
                >
                  删除
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
        
        <!-- 分页 -->
        <div class="management-table-pagination">
          <el-pagination
            v-model:current-page="repositoryCurrentPage"
            v-model:page-size="repositoryPageSize"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="repositoryTotal"
            @size-change="handleRepositorySizeChange"
            @current-change="handleRepositoryCurrentChange"
          />
        </div>
      </div>
    </div>

    <!-- 新增/编辑算法库弹框 -->
    <el-dialog
      v-model="showAddDialog"
      :title="editingRepository ? '编辑算法库' : '新增算法库'"
      width="500px"
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
            <el-option label="启用" value="enabled" />
            <el-option label="禁用" value="disabled" />
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
          <el-button @click="handleAddCancel">取消</el-button>
          <el-button type="primary" @click="handleAddConfirm" :loading="submitting">
            {{ editingRepository ? '更新' : '创建' }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 添加算法弹框 -->
    <el-dialog
      v-model="showAlgorithmAddDialog"
      title="添加算法"
      width="700px"
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
          <el-button @click="handleAlgorithmAddCancel">取消</el-button>
          <el-button type="primary" @click="handleAlgorithmAddConfirm" :loading="submitting">
            添加
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 算法编辑弹框 -->
    <el-dialog
      v-model="showAlgorithmEditDialog"
      title="编辑算法"
      width="700px"
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
        <el-form-item label="算法描述" prop="description">
          <el-input
            v-model="algorithmEditForm.description"
            type="textarea"
            placeholder="请输入算法描述"
            :rows="4"
            resize="none"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleAlgorithmEditCancel">取消</el-button>
          <el-button type="primary" @click="handleAlgorithmEditConfirm" :loading="submitting">
            更新
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 下发到摄像机侧边栏 -->
    <el-drawer
      v-model="showDeviceDrawer"
      title="下发到摄像机"
      direction="rtl"
      size="1000px"
      :before-close="handleDrawerClose"
    >
      <div class="device-drawer-content">
        <!-- 主要内容区域 -->
        <div class="main-content">
          <!-- 左侧设备树 -->
          <div class="device-tree-container" :class="{ collapsed: deviceTreeCollapsed }">
            <DeviceTree
              :tree-data="deviceTreeData"
              title="设备树"
              :show-search="true"
              :show-collapse-btn="true"
              :show-add-actions="false"
              :show-delete-actions="false"
              :show-bottom-actions="false"
              :collapsed="deviceTreeCollapsed"
              @node-click="handleDeviceNodeClick"
              @search="handleDeviceTreeSearch"
              @toggle-collapse="toggleDeviceTree"
            />
          </div>

          <!-- 右侧设备列表 -->
          <div class="table-container">
            <div class="table-content">
              <!-- 上方操作区 -->
              <div class="table-actions">
                <!-- 设备树折叠时显示的展开按钮 -->
                <CollapseToggle 
                  v-if="deviceTreeCollapsed"
                  class="expand-device-tree-btn"
                  :is-expanded="false"
                  @toggle="toggleDeviceTree"
                />
                <el-button type="primary" @click="handleDeployToDevice">
                  <el-icon><Plus /></el-icon>
                  下发
                </el-button>
              </div>
              
              <el-table :data="deviceTableData" height="450" stripe v-loading="deviceLoading" @selection-change="handleDeviceSelectionChange">
                <el-table-column type="selection" width="55" />
                <el-table-column prop="index" label="序号" width="60" align="center" />
                <el-table-column prop="name" label="设备名称" width="120" />
                <el-table-column prop="tag" label="标签" width="100">
                  <template #default="scope">
                    <el-tag size="small" type="primary">{{ scope.row.tag }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="deviceId" label="设备ID" width="120" />
                <el-table-column prop="location" label="设备位置" />
              </el-table>
            </div>
            
            <!-- 分页 -->
            <div class="pagination-container">
              <el-pagination
                v-model:current-page="currentPage"
                v-model:page-size="pageSize"
                :page-sizes="[10, 20, 50, 100]"
                layout="total, sizes, prev, pager, next, jumper"
                :total="totalDevices"
                @size-change="handleSizeChange"
                @current-change="handleCurrentChange"
              />
            </div>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import {computed, onMounted, ref} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {DataAnalysis, Delete, Download, Edit, MoreFilled, Plus} from '@element-plus/icons-vue'
import DeviceTree from '@/components/DeviceTree.vue'
import CollapseToggle from '@/components/CollapseToggle.vue'
import ActionButtonGroup from '@/components/ActionButtonGroup.vue' // Added import for ActionButtonGroup
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
import {getDeviceById, getDeviceList, getDeviceTree, dispatchAlgorithmToDevices} from '@/api/device'
import {getTagTree} from '@/api/tagManagement'
import {defaultDeviceTreeData} from '@/views/VideoAggregation/constants'

// 加载状态
const repositoriesLoading = ref(false)
const algorithmsLoading = ref(false)
const submitting = ref(false)

// 当前激活的顶部菜单
const activeTopMenu = ref('management')

// 当前激活的分类
const activeCategory = ref('all')

// 设备侧边栏相关数据
const showDeviceDrawer = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const totalDevices = ref(0)
const deviceLoading = ref(false)
const deviceTreeCollapsed = ref(false)

// 算法仓库数据
const algorithmRepositories = ref([])
const repositoryCurrentPage = ref(1)
const repositoryPageSize = ref(10)
const repositoryTotal = ref(0)

// 算法数据
const algorithms = ref([])
const algorithmTotal = ref(0)
const currentRepositoryId = ref(null)

// 设备树数据
const deviceTreeData = ref([...defaultDeviceTreeData])

// 从当前算法列表中提取所有分类
const typeOptions = ref([
  { label: '全部', value: 'all' },
  { label: '目标检测算法', value: 'detect' },
  { label: '实例分割算法', value: 'segment' },
  { label: '图像分类算法', value: 'classify' },
  { label: '关键点检测算法', value: 'pose' },
  { label: '旋转目标检测算法', value: 'obb' }
])

// 设备表格数据
const deviceTableData = ref([])
const tagNameMap = ref(new Map())
const selectedDeviceRows = ref([])

// 算法库管理相关
const selectedRepositories = ref([])
const showAddDialog = ref(false)
const editingRepository = ref(null)
const addForm = ref({
  name: '',
  repositoryType: 'extended',
  status: 'enabled',
  remark: ''
})

// 算法添加相关
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

// 算法编辑相关
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
  isSystem: 'YES'
})

// 表单验证规则
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

// 算法相关
const selectedAlgorithm = ref(null)

// 多种卡片背景图片 - 300x200尺寸
const cardBackgrounds = [
  // 蓝色渐变主题
  'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48ZGVmcz48bGluZWFyR3JhZGllbnQgaWQ9ImJsdWVHcmFkaWVudCIgeDE9IjAlIiB5MT0iMCUiIHgyPSIxMDAlIiB5Mj0iMTAwJSI+PHN0b3Agb2Zmc2V0PSIwJSIgc3R5bGU9InN0b3AtY29sb3I6IzQwOTZmZjtzdG9wLW9wYWNpdHk6MSIgLz48c3RvcCBvZmZzZXQ9IjEwMCUiIHN0eWxlPSJzdG9wLWNvbG9yOiMxODY3YzA7c3RvcC1vcGFjaXR5OjEiIC8+PC9saW5lYXJHcmFkaWVudD48L2RlZnM+PHJlY3Qgd2lkdGg9IjEwMCUiIGhlaWdodD0iMTAwJSIgZmlsbD0idXJsKCNibHVlR3JhZGllbnQpIi8+PGNpcmNsZSBjeD0iMjUwIiBjeT0iNTAiIHI9IjMwIiBmaWxsPSJyZ2JhKDI1NSwyNTUsMjU1LDAuMSkiLz48Y2lyY2xlIGN4PSI1MCIgY3k9IjE1MCIgcj0iMjAiIGZpbGw9InJnYmEoMjU1LDI1NSwyNTUsMC4xNSkiLz48cmVjdCB4PSIyMDAiIHk9IjEyMCIgd2lkdGg9IjgwIiBoZWlnaHQ9IjgwIiByeD0iMTAiIGZpbGw9InJnYmEoMjU1LDI1NSwyNTUsMC4wOCkiLz48L3N2Zz4=',
  
  // 绿色渐变主题
  'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48ZGVmcz48bGluZWFyR3JhZGllbnQgaWQ9ImdyZWVuR3JhZGllbnQiIHgxPSIwJSIgeTE9IjAlIiB4Mj0iMTAwJSIgeTI9IjEwMCUiPjxzdG9wIG9mZnNldD0iMCUiIHN0eWxlPSJzdG9wLWNvbG9yOiM2N0M5MkE7c3RvcC1vcGFjaXR5OjEiIC8+PHN0b3Agb2Zmc2V0PSIxMDAlIiBzdHlsZT0ic3RvcC1jb2xvcjojNDE4MDE5O3N0b3Atb3BhY2l0eToxIiAvPjwvbGluZWFyR3JhZGllbnQ+PC9kZWZzPjxyZWN0IHdpZHRoPSIxMDAlIiBoZWlnaHQ9IjEwMCUiIGZpbGw9InVybCgjZ3JlZW5HcmFkaWVudCkiLz48cG9seWdvbiBwb2ludHM9IjAsMCAxMDAsMCA1MCw1MCIgZmlsbD0icmdiYSgyNTUsMjU1LDI1NSwwLjEpIi8+PGNpcmNsZSBjeD0iMjMwIiBjeT0iMTcwIiByPSIyNSIgZmlsbD0icmdiYSgyNTUsMjU1LDI1NSwwLjEyKSIvPjxwYXRoIGQ9Ik0yMDAgNTBMMjUwIDUwTDIyNSAxMDBaIiBmaWxsPSJyZ2JhKDI1NSwyNTUsMjU1LDAuMDgpIi8+PC9zdmc+',
  
  // 橙色渐变主题
  'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48ZGVmcz48bGluZWFyR3JhZGllbnQgaWQ9Im9yYW5nZUdyYWRpZW50IiB4MT0iMCUiIHkxPSIwJSIgeDI9IjEwMCUiIHkyPSIxMDAlIj48c3RvcCBvZmZzZXQ9IjAlIiBzdHlsZT0ic3RvcC1jb2xvcjojRkY5ODAwO3N0b3Atb3BhY2l0eToxIiAvPjxzdG9wIG9mZnNldD0iMTAwJSIgc3R5bGU9InN0b3AtY29sb3I6I0VGNkMwMDtzdG9wLW9wYWNpdHk6MSIgLz48L2xpbmVhckdyYWRpZW50PjwvZGVmcz48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSJ1cmwoI29yYW5nZUdyYWRpZW50KSIvPjxlbGxpcHNlIGN4PSI4MCIgY3k9IjYwIiByeD0iNDAiIHJ5PSIyNSIgZmlsbD0icmdiYSgyNTUsMjU1LDI1NSwwLjEpIi8+PHJlY3QgeD0iMjAwIiB5PSIzMCIgd2lkdGg9IjYwIiBoZWlnaHQ9IjYwIiByeD0iMzAiIGZpbGw9InJnYmEoMjU1LDI1NSwyNTUsMC4xMikiLz48cG9seWdvbiBwb2ludHM9IjUwLDE1MCA5MCwxNTAgNzAsMTkwIiBmaWxsPSJyZ2JhKDI1NSwyNTUsMjU1LDAuMDgpIi8+PC9zdmc+',
  
  // 紫色渐变主题
  'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48ZGVmcz48bGluZWFyR3JhZGllbnQgaWQ9InB1cnBsZUdyYWRpZW50IiB4MT0iMCUiIHkxPSIwJSIgeDI9IjEwMCUiIHkyPSIxMDAlIj48c3RvcCBvZmZzZXQ9IjAlIiBzdHlsZT0ic3RvcC1jb2xvcjojOWM0ZGNjO3N0b3Atb3BhY2l0eToxIiAvPjxzdG9wIG9mZnNldD0iMTAwJSIgc3R5bGU9InN0b3AtY29sb3I6IzVhNjc5ODtzdG9wLW9wYWNpdHk6MSIgLz48L2xpbmVhckdyYWRpZW50PjwvZGVmcz48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSJ1cmwoI3B1cnBsZUdyYWRpZW50KSIvPjxjaXJjbGUgY3g9IjYwIiBjeT0iNDAiIHI9IjE4IiBmaWxsPSJyZ2JhKDI1NSwyNTUsMjU1LDAuMTUpIi8+PHJlY3QgeD0iMTgwIiB5PSIxMjAiIHdpZHRoPSI5MCIgaGVpZ2h0PSI0MCIgcng9IjIwIiBmaWxsPSJyZ2JhKDI1NSwyNTUsMjU1LDAuMSkiLz48Y2lyY2xlIGN4PSIyNDAiIGN5PSI3MCIgcj0iMjIiIGZpbGw9InJnYmEoMjU1LDI1NSwyNTUsMC4wOCkiLz48L3N2Zz4=',
  
  // 红色渐变主题
  'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48ZGVmcz48bGluZWFyR3JhZGllbnQgaWQ9InJlZEdyYWRpZW50IiB4MT0iMCUiIHkxPSIwJSIgeDI9IjEwMCUiIHkyPSIxMDAlIj48c3RvcCBvZmZzZXQ9IjAlIiBzdHlsZT0ic3RvcC1jb2xvcjojZjU2YzZjO3N0b3Atb3BhY2l0eToxIiAvPjxzdG9wIG9mZnNldD0iMTAwJSIgc3R5bGU9InN0b3AtY29sb3I6I2UzMzY0NTtzdG9wLW9wYWNpdHk6MSIgLz48L2xpbmVhckdyYWRpZW50PjwvZGVmcz48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSJ1cmwoI3JlZEdyYWRpZW50KSIvPjxyZWN0IHg9IjIwIiB5PSIyMCIgd2lkdGg9IjUwIiBoZWlnaHQ9IjUwIiByeD0iOCIgZmlsbD0icmdiYSgyNTUsMjU1LDI1NSwwLjEyKSIvPjxjaXJjbGUgY3g9IjIyMCIgY3k9IjE0MCIgcj0iMzUiIGZpbGw9InJnYmEoMjU1LDI1NSwyNTUsMC4wOCkiLz48cG9seWdvbiBwb2ludHM9IjEyMCwzMCAxNzAsMzAgMTQ1LDgwIiBmaWxsPSJyZ2JhKDI1NSwyNTUsMjU1LDAuMSkiLz48L3N2Zz4=',
  
  // 青色渐变主题
  'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48ZGVmcz48bGluZWFyR3JhZGllbnQgaWQ9ImN5YW5HcmFkaWVudCIgeDE9IjAlIiB5MT0iMCUiIHgyPSIxMDAlIiB5Mj0iMTAwJSI+PHN0b3Agb2Zmc2V0PSIwJSIgc3R5bGU9InN0b3AtY29sb3I6IzE3YTJiODtzdG9wLW9wYWNpdHk6MSIgLz48c3RvcCBvZmZzZXQ9IjEwMCUiIHN0eWxlPSJzdG9wLWNvbG9yOiMxMzc5OGU7c3RvcC1vcGFjaXR5OjEiIC8+PC9saW5lYXJHcmFkaWVudD48L2RlZnM+PHJlY3Qgd2lkdGg9IjEwMCUiIGhlaWdodD0iMTAwJSIgZmlsbD0idXJsKCNjeWFuR3JhZGllbnQpIi8+PGVsbGlwc2UgY3g9IjE1MCIgY3k9IjQ0IiByeD0iNjAiIHJ5PSIyMCIgZmlsbD0icmdiYSgyNTUsMjU1LDI1NSwwLjEpIi8+PGNpcmNsZSBjeD0iNzAiIGN5PSIxMzAiIHI9IjI4IiBmaWxsPSJyZ2JhKDI1NSwyNTUsMjU1LDAuMTIpIi8+PHJlY3QgeD0iMjIwIiB5PSIxMzAiIHdpZHRoPSI1MCIgaGVpZ2h0PSI1MCIgcng9IjI1IiBmaWxsPSJyZ2JhKDI1NSwyNTUsMjU1LDAuMDgpIi8+PC9zdmc+',
  
  // 深灰色主题 - 原有的图片作为第7个背景
  'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjZjVmNWY1Ii8+PHBhdGggZD0iTTEwMCA5MGMwIDEyLjQxOCAxMC4wODIgMjIuNSAyMi41IDIyLjVzMjIuNS0xMC4wODIgMjIuNS0yMi41Uzg3LjQxOCA2Ny41IDc1IDY3LjUgMTAwIDc3LjU4MiAxMDAgOTB6bTc4IDkwTDE1MCAyMDBsLTMwLTMwLTQwIDQweiIgZmlsbD0iI2RkZCIvPjx0ZXh0IHg9IjE1MCIgeT0iMTgwIiBmb250LWZhbWlseT0iQXJpYWwiIGZvbnQtc2l6ZT0iMTQiIGZpbGw9IiM5OTkiIHRleHQtYW5jaG9yPSJtaWRkbGUiPjMwMCB4IDIwMDwvdGV4dD48L3N2Zz4='
]

// 获取算法卡片背景图片
const getAlgorithmCardBackground = (algorithm, repositoryId) => {
  // 如果算法已有图片，直接返回
  if (algorithm.imageUrl && algorithm.imageUrl.trim() !== '') {
    return algorithm.imageUrl
  }
  
  // 根据算法ID和仓库ID计算背景图片索引
  const algorithmIndex = algorithm.id || 0
  const repoIndex = repositoryId || 0
  const backgroundIndex = (algorithmIndex + repoIndex) % cardBackgrounds.length
  
  return cardBackgrounds[backgroundIndex]
}

// 计算属性

// 顶部菜单列表（动态生成）
const topMenus = computed(() => {
  const menus = []
  
  // 添加所有启用的算法仓库
  algorithmRepositories.value.forEach(repo => {
    if (repo.status === 1) {
      menus.push({
        key: repo.id.toString(),
        label: repo.name
      })
    }
  })
  
  // 添加算法库管理
  menus.push({
    key: 'management',
    label: '算法库管理'
  })
  
  return menus
})

// 当前页仓库列表
const currentPageRepositories = computed(() => {
  return algorithmRepositories.value
})

// 当前页算法列表
const currentPageAlgorithms = computed(() => {
  if (activeCategory.value === 'all') {
    return algorithms.value
  }
  return algorithms.value.filter(alg => alg.category === activeCategory.value)
})

// 是否显示添加按钮
const showAddButton = computed(() => {
  // 算法库管理页面不显示添加按钮
  if (activeTopMenu.value === 'management') {
    return false
  }
  
  // 找到当前选择的仓库
  const currentRepo = algorithmRepositories.value.find(repo => 
    repo.id.toString() === activeTopMenu.value
  )
  
  // 只有扩展算法库显示添加按钮
  return currentRepo
})

// 工具方法
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

// API调用方法

// 加载算法仓库列表
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
      
      // 格式化创建时间
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

// 加载指定仓库的算法列表
const loadAlgorithmsByRepository = async (repositoryId) => {
  try {
    algorithmsLoading.value = true
    const response = await getAlgorithmPage({
      current: 1,
      size: 1000, // 使用较大的页面大小来获取所有算法
      repositoryId: repositoryId
    })
    
    if (response.code === 200) {
      algorithms.value = response.data.records || []
      algorithmTotal.value = response.data.total || 0
      currentRepositoryId.value = repositoryId
    } else {
      ElMessage.error(response.message || '加载算法列表失败')
    }
  } catch (error) {
    console.error('加载算法列表失败:', error)
    ElMessage.error('加载算法列表失败')
  } finally {
    algorithmsLoading.value = false
  }
}

const loadDeviceTree = async () => {
  try {
    const response = await getDeviceTree()
    if (response.data) {
      deviceTreeData.value = response.data
    }
  } catch (error) {
    console.error('加载设备树失败:', error)
  }
}

const loadTagNameMap = async () => {
  try {
    const response = await getTagTree()
    if (response.code === 200 && response.data) {
      const tagMap = new Map()
      const traverse = (nodes) => {
        if (!Array.isArray(nodes)) return
        nodes.forEach(node => {
          if (node.id && node.tagName) {
            tagMap.set(node.id, node.tagName)
          }
          if (node.children) {
            traverse(node.children)
          }
        })
      }
      traverse(response.data)
      tagNameMap.value = tagMap
    }
  } catch (error) {
    console.warn('load tag map failed:', error)
  }
}

const loadDeviceList = async () => {
  deviceLoading.value = true
  try {
    if (tagNameMap.value.size === 0) {
      await loadTagNameMap()
    }

    const response = await getDeviceList({
      page: currentPage.value,
      size: pageSize.value
    })
    const devices = (response.data && response.data.records) ? response.data.records : []
    const startIndex = (currentPage.value - 1) * pageSize.value

    deviceTableData.value = await Promise.all(
      devices.map(async (device, idx) => {
        try {
          const detailResponse = await getDeviceById(device.id)
          if (detailResponse.code === 200 && detailResponse.data) {
            const detail = detailResponse.data
            let tags = []
            if (Array.isArray(detail.selectedTags) && detail.selectedTags.length > 0) {
              tags = detail.selectedTags.map(tagId => tagNameMap.value.get(tagId) || `tag-${tagId}`)
            }
            const displayTag = tags.length > 0 ? tags[0] : '-'
            return {
              ...device,
              ...detail,
              index: startIndex + idx + 1,
              name: device.deviceName || device.name || '',
              tag: displayTag,
              tags,
              displayTag,
              location: detail.address || detail.location || device.address || ''
            }
          }
        } catch (error) {
          console.warn(`load device detail failed: ${device.id}`, error)
        }
        return {
          ...device,
          index: startIndex + idx + 1,
          name: device.deviceName || device.name || '',
          tag: '-',
          tags: [],
          displayTag: '-',
          location: device.address || ''
        }
      })
    )

    totalDevices.value = (response.data && response.data.total) ? response.data.total : 0
  } catch (error) {
    console.error('load device list failed:', error)
    ElMessage.error('加载设备列表失败')
  } finally {
    deviceLoading.value = false
  }
}

// 初始化数据
const initData = async () => {
  await loadAlgorithmRepositories()
  await loadDeviceTree()
  // 如果有可用的仓库，默认加载第一个仓库的算法
  if (algorithmRepositories.value.length > 0) {
    const firstEnabledRepo = algorithmRepositories.value.find(repo => repo.status === 'enabled')
    if (firstEnabledRepo) {
      activeTopMenu.value = firstEnabledRepo.id.toString()
      await loadAlgorithmsByRepository(firstEnabledRepo.id)
    }
  }
}

// 方法
const setActiveTopMenu = async (menu) => {
  console.log('切换顶部菜单到:', menu)
  activeTopMenu.value = menu
  // 切换顶部菜单时，重置分类为"全部"
  activeCategory.value = 'all'
  
  if (menu === 'management') {
    // 切换到算法库管理页面
    console.log('切换到算法库管理页面')
  } else {
    // 切换到具体算法库，加载算法列表
    const repositoryId = parseInt(menu)
    if (!isNaN(repositoryId)) {
      await loadAlgorithmsByRepository(repositoryId)
    }
  }
}

const setActiveCategory = (category) => {
  activeCategory.value = category
}

// 算法相关操作
const addAlgorithm = () => {
  // 获取当前选择的算法库ID
  const repositoryId = parseInt(activeTopMenu.value)
  if (isNaN(repositoryId)) {
    ElMessage.error('无法获取算法库信息')
    return
  }
  
  // 重置表单
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

const editAlgorithm = (algorithm) => {
  // 设置编辑的算法
  editingAlgorithm.value = algorithm
  
  // 加载算法数据到表单
  algorithmEditForm.value = {
    name: algorithm.name || '',
    category: algorithm.category || '',
    type: algorithm.type || 'detect',
    ptModelFilePath: algorithm.ptModelFilePath,
    onnxModelFilePath: algorithm.onnxModelFilePath,
    isSystem: algorithm.isSystem,
    description: algorithm.description || '',
    repositoryId: algorithm.repositoryId || currentRepositoryId.value
  }
  
  // 显示编辑弹窗
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

const deployAlgorithm = async (algorithm) => {
  selectedAlgorithm.value = algorithm
  showDeviceDrawer.value = true
  selectedDeviceRows.value = []
  currentPage.value = 1
  await loadDeviceList()
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
      // 重新加载当前页算法列表
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

// 设备侧边栏相关方法
const handleDrawerClose = (done) => {
  done()
}

const handleDeviceNodeClick = (data) => {
  console.log('点击设备树节点:', data)
}

const handleDeviceTreeSearch = (searchKeyword) => {
  console.log('设备树搜索:', searchKeyword)
  // 这里可以添加搜索逻辑
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
      .map(item => item.id || item.deviceId)
      .filter(Boolean)

  if (deviceIds.length === 0) {
    ElMessage.warning('请选择要下发的设备')
    return
  }

  const deviceIdsStr = deviceIds.join(',')

  try {
    const response = await dispatchAlgorithmToDevices(selectedAlgorithm.value.id, deviceIdsStr)
    if (response.code === 200) {
      ElMessage.success('下发成功')
      showDeviceDrawer.value = false
    } else {
      ElMessage.error(response.message || '下发失败')
    }
  } catch (error) {
    console.error('下发失败:', error)
    ElMessage.error('下发失败')
  }
}

// 算法仓库分页
const handleRepositorySizeChange = async (val) => {
  repositoryPageSize.value = val
  repositoryCurrentPage.value = 1
  await loadAlgorithmRepositories()
}

const handleRepositoryCurrentChange = async (val) => {
  repositoryCurrentPage.value = val
  await loadAlgorithmRepositories()
}



// 设备相关方法
const handleSizeChange = async (val) => {
  pageSize.value = val
  currentPage.value = 1
  await loadDeviceList()
}

const handleCurrentChange = async (val) => {
  currentPage.value = val
  await loadDeviceList()
}

const toggleDeviceTree = () => {
  deviceTreeCollapsed.value = !deviceTreeCollapsed.value
}

// 算法库管理相关方法
const handleLibrarySelectionChange = (selection) => {
  selectedRepositories.value = selection
}

const addAlgorithmLibrary = () => {
  editingRepository.value = null
  addForm.value = {
    name: '',
    repositoryType: 'extended',
    status: 'enabled',
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
    const newStatus = row.status === 'enabled' ? 'disabled' : 'enabled'
    const response = await updateAlgorithmRepositoryStatus(row.id, newStatus)
    
    if (response.code === 200) {
      ElMessage.success(`${newStatus === 'enabled' ? '启用' : '禁用'}成功`)
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
      // 编辑模式
      response = await updateAlgorithmRepository(editingRepository.value.id, formData)
    } else {
      // 新增模式
      response = await createAlgorithmRepository(formData)
    }
    
    if (response.code === 200) {
      ElMessage.success(`${editingRepository.value ? '更新' : '创建'}成功`)
      showAddDialog.value = false
      await loadAlgorithmRepositories()
      
      // 重置表单
      addForm.value = {
        name: '',
        repositoryType: 'extended',
        status: 'enabled',
        remark: ''
      }
      editingRepository.value = null
    } else {
      ElMessage.error(response.message || `${editingRepository.value ? '更新' : '创建'}失败`)
    }
  } catch (error) {
    if (typeof error === 'object' && error.message) {
      // 表单验证错误
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
    status: 'enabled',
    remark: ''
  }
  if (addFormRef.value) {
    addFormRef.value.resetFields()
  }
}

// 算法添加弹窗处理方法
const handleAlgorithmAddConfirm = async () => {
  try {
    // 验证表单
    await algorithmAddFormRef.value.validate()
    
    submitting.value = true
    
    // 为新算法分配一个背景图片
    const randomBackgroundIndex = Math.floor(Math.random() * cardBackgrounds.length)
    const assignedBackground = cardBackgrounds[randomBackgroundIndex]
    
    // 创建算法数据
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
      imageUrl: assignedBackground // 自动分配背景图片
    }
    
    // 调用算法创建API
    const response = await createAlgorithm(algorithmData)
    
    if (response.code === 200) {
      ElMessage.success('算法添加成功')
      showAlgorithmAddDialog.value = false
      
      // 重新加载当前算法库的算法列表
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

// 算法编辑弹窗处理方法
const handleAlgorithmEditConfirm = async () => {
  try {
    // 验证表单
    await algorithmEditFormRef.value.validate()
    
    submitting.value = true
    
    // 创建更新数据
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
      imageUrl: editingAlgorithm.value.imageUrl // 保持原有图片
    }
    
    // 调用算法更新API
    const response = await updateAlgorithm(editingAlgorithm.value.id, updateData)
    
    if (response.code === 200) {
      ElMessage.success('算法更新成功')
      showAlgorithmEditDialog.value = false
      
      // 重新加载当前算法库的算法列表
      if (algorithmEditForm.value.repositoryId) {
        await loadAlgorithmsByRepository(algorithmEditForm.value.repositoryId)
      }
    } else {
      ElMessage.error(response.message || '算法更新失败')
    }
    
  } catch (error) {
    if (error.message) {
      console.error('算法更新失败:', error)
      ElMessage.error('算法更新失败')
    }
  } finally {
    submitting.value = false
  }
}

const handleAlgorithmEditCancel = () => {
  showAlgorithmEditDialog.value = false
  algorithmEditForm.value = {
    name: '',
    category: '',
    type: 'detect',
    version: '1.0.0',
    description: '',
    repositoryId: null
  }
  if (algorithmEditFormRef.value) {
    algorithmEditFormRef.value.resetFields()
  }
}



// 页面初始化
onMounted(() => {
  initData()
})
</script>

<style scoped>
.algorithm-management {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}

/* 顶部菜单栏 */
.top-menu-bar {
  background: white;
  border-radius: 8px 8px 0 0;
  padding: 0;
  margin-bottom: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  border-bottom: 1px solid #e4e7ed;
}

.top-menu-item {
  padding: 16px 24px;
  cursor: pointer;
  color: #606266;
  font-size: 14px;
  font-weight: 500;
  border-bottom: 3px solid transparent;
  transition: all 0.3s ease;
  position: relative;
  background: transparent;
}

.top-menu-item:first-child {
  border-radius: 8px 0 0 0;
}

.top-menu-item:hover {
  color: #409eff;
  background-color: #f8f9fa;
}

.top-menu-item.active {
  color: #409eff;
  border-bottom-color: #409eff;
  background-color: #ecf5ff;
}

/* 分类标签栏 */
.category-tabs {
  background: white;
  border-radius: 0 0 8px 8px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  justify-content: flex-start;
  align-items: center;
  border-top: none;
  gap: 20px;
  min-height: 20px;
}

.category-section {
  display: flex;
  gap: 0;
  flex: 1;
}

.add-button-section {
  flex-shrink: 0;
  order: -1;
}

.category-tab {
  padding: 12px 24px;
  cursor: pointer;
  color: #606266;
  font-size: 14px;
  font-weight: 500;
  border-bottom: 3px solid transparent;
  transition: all 0.3s ease;
  position: relative;
}

.category-tab:hover {
  color: #409eff;
  background-color: #f8f9fa;
}

.category-tab.active {
  color: #409eff;
  border-bottom-color: #409eff;
  background-color: #ecf5ff;
}

/* 算法网格 */
.algorithm-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  padding: 0;
  max-width: 1200px;
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
  transition: transform 0.3s ease;
}

.algorithm-card:hover .card-image img {
  transform: scale(1.05);
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

/* 下拉菜单样式 */
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



/* 表格操作按钮样式 */
.table-action-buttons {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

/* 确保操作按钮为纯文字样式 */
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

/* 表格分页样式 */
.table-pagination {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

/* 响应式设计 */
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
  
  .top-menu-bar {
    overflow-x: auto;
    flex-wrap: nowrap;
    white-space: nowrap;
  }
  
  .top-menu-item {
    flex-shrink: 0;
    padding: 12px 20px;
    font-size: 13px;
  }
  
  .category-tabs {
    padding: 16px;
    flex-direction: column;
    gap: 16px;
  }
  
  .category-section {
    overflow-x: auto;
    flex-wrap: nowrap;
    white-space: nowrap;
  }
  
  .category-tab {
    flex-shrink: 0;
    padding: 10px 20px;
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

/* 加载动画 */
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

/* 为不同索引的卡片添加延迟动画 */
.algorithm-card:nth-child(1) { animation-delay: 0.1s; }
.algorithm-card:nth-child(2) { animation-delay: 0.2s; }
.algorithm-card:nth-child(3) { animation-delay: 0.3s; }
.algorithm-card:nth-child(4) { animation-delay: 0.4s; }
.algorithm-card:nth-child(5) { animation-delay: 0.5s; }
.algorithm-card:nth-child(6) { animation-delay: 0.6s; }

/* 卡片悬浮效果增强 */
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

/* 设备侧边栏样式 */
.device-drawer-content {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 0;
}

/* 主内容区域 */
.main-content {
  flex: 1;
  display: flex;
  gap: 20px;
  height: calc(100vh - 120px);
  padding: 24px;
}

/* 设备树容器 */
.device-tree-container {
  width: 280px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  flex-shrink: 0;
  transition: all 0.3s ease;
}

/* 设备树折叠状态 */
.device-tree-container.collapsed {
  width: 0;
  transform: translateX(-100%);
}

/* 表格容器 */
.table-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.table-content {
  flex: 1;
  padding: 20px;
}

/* 表格上方操作区 */
.table-actions {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

/* 展开设备树按钮 */
.expand-device-tree-btn {
  margin-right: 8px;
}

/* 分页样式 */
.pagination-container {
  padding: 16px 20px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: center;
  background: #fafafa;
}

/* 算法库管理容器样式 */
.algorithm-management-container {
  margin-top: 0;
  padding: 0;
}

/* 管理工具栏样式 */
.management-toolbar {
  background: white;
  padding: 16px 20px;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
}

/* 管理表格内容样式 */
.management-table-content {
  background: white;
  padding: 20px;
}

/* 管理表格分页样式 */
.management-table-pagination {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
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

/* 表格样式优化 */
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

/* Drawer标题样式 */
:deep(.el-drawer__header) {
  margin-bottom: 0;
  padding: 20px 24px 16px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}

/* 表格操作列按钮样式 */
.table-action-buttons {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

/* 确保操作按钮为纯文字样式 */
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

/* 新增弹框样式 */
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
  font-size: 18px;
  font-weight: 600;
  color: #262626;
}

:deep(.el-drawer__body) {
  padding: 24px;
}

/* DeviceTree组件在Drawer中的样式调整 */
:deep(.device-tree-component) {
  height: 100%;
  display: flex;
  flex-direction: column;
}

:deep(.device-tree-component .tree-header) {
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}

:deep(.device-tree-component .device-tree) {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
}


</style>

