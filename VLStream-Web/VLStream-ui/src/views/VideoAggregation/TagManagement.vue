<template>
  <div class="tag-management">
    <!-- 标签页切换 - 顶部 -->
    <div class="tab-nav">
      <div class="tab-item" :class="{ active: activeTab === 'maintenance' }" @click="activeTab = 'maintenance'">标签维护</div>
      <div class="tab-item" :class="{ active: activeTab === 'device' }" @click="activeTab = 'device'">标签设备</div>
    </div>
    
    <!-- 主要内容区域 -->
    <div class="main-content-container">
      <!-- 标签维护页面 -->
      <div v-if="activeTab === 'maintenance'" class="maintenance-content">
        <!-- 左侧树形结构 -->
        <div class="device-tree" v-show="!treeCollapsed">
          <!-- 树形搜索框 -->
          <div class="tree-search">
            <SearchInput
              v-model="treeSearchKeyword"
              placeholder="搜索"
              button-text="查询"
              size="small"
              width="200px"
              @search="handleTreeSearch"
            />
          </div>
          
          <div class="tree-header">
            <span class="tree-title">自有</span>
            <div class="tree-actions">
              <CollapseToggle 
                :is-expanded="!treeCollapsed" 
                @toggle="handleTreeToggle" 
              />
            </div>
          </div>

          <div class="tree-content" v-loading="treeLoading">
            <el-tree
              :data="treeData"
              :props="treeProps"
              :expand-on-click-node="false"
              :default-expand-all="false"
              node-key="id"
              @node-click="handleNodeClick"
            >
              <template #default="{ node, data }">
                <div class="tree-node" :class="[`node-level-${node.level}`, { 'is-leaf': node.isLeaf }]">
                  <span class="node-label">{{ node.label }}</span>
                  <div class="node-actions" v-if="data.type !== 'root' && node.level === 2">
                    <el-button size="small" text @click.stop="addChild(data)" class="add-btn">
                      <el-icon><Plus /></el-icon>
                    </el-button>
                    <el-button size="small" text @click.stop="deleteNode(data)" class="delete-btn">
                      <el-icon><Delete /></el-icon>
                    </el-button>
                  </div>
                </div>
              </template>
            </el-tree>
            
            <!-- 公共部分 -->
            <div class="public-section">
              <div class="public-title">公共</div>
              <el-tree
                :data="publicTreeData"
                :props="treeProps"
                :expand-on-click-node="false"
                :default-expand-all="false"
                node-key="id"
                @node-click="handleNodeClick"
              >
                <template #default="{ node, data }">
                  <div class="tree-node public-node">
                    <el-icon class="node-icon"><Key /></el-icon>
                    <span class="node-label">{{ node.label }}</span>
                  </div>
                </template>
              </el-tree>
            </div>
          </div>
        </div>

        <!-- 右侧内容区域 -->
        <div class="content-area" :class="{ 'full-width': treeCollapsed }">
          <!-- 操作按钮栏 -->
          <div class="toolbar">
            <div class="toolbar-left">
              <!-- 展开树形区域按钮 -->
              <div class="expand-tree-btn-inline" v-if="treeCollapsed">
                <CollapseToggle 
                  :is-expanded="false" 
                  @toggle="handleTreeToggle" 
                />
              </div>
              <ActionButtonGroup 
                :selected-count="selectedRows.length"
                @add="handleToolbarAdd"
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
              :data="currentPageTableData" 
              stripe
              v-loading="loading"
              @selection-change="handleSelectionChange"
              style="width: 100%"
            >
              <el-table-column type="selection" width="55" />
              <el-table-column type="index" label="序号" width="80" align="center" />
              <el-table-column prop="tagName" label="标签名称" min-width="150" />
              <el-table-column prop="description" label="描述" min-width="200" />
              <el-table-column prop="isPublic" label="是否公开" width="120" align="center">
                <template #default="scope">
                  <el-switch 
                    v-model="scope.row.isPublic" 
                    @change="handlePublicChange(scope.row)"
                  />
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="创建时间" min-width="160" />
              <el-table-column label="操作" width="180" fixed="right" align="right">
                <template #default="scope">
                  <div class="operation-buttons">
                    <el-button 
                      class="operation-btn edit-btn" 
                      @click="handleRowEdit(scope.row)">
                      编辑
                    </el-button>
                    <el-button 
                      class="operation-btn delete-btn"
                      @click="handleRowDelete(scope.row)">
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

      <!-- 标签设备页面 -->
      <div v-if="activeTab === 'device'" class="device-content">
        <!-- 左侧设备树形结构 -->
        <div class="device-tree" v-show="!deviceTreeCollapsed">
          <!-- 设备树形搜索框 -->
          <div class="tree-search">
            <SearchInput
              v-model="deviceTreeSearchKeyword"
              placeholder="搜索"
              button-text="查询"
              size="small"
              width="200px"
              @search="handleDeviceTreeSearch"
            />
          </div>
          
          <div class="tree-header">
            <span class="tree-title">设备树</span>
            <div class="tree-actions">
              <CollapseToggle 
                :is-expanded="!deviceTreeCollapsed" 
                @toggle="handleDeviceTreeToggle" 
              />
            </div>
          </div>

          <div class="tree-content">
            <el-tree
              :data="deviceTreeData"
              :props="treeProps"
              :expand-on-click-node="false"
              :default-expand-all="false"
              node-key="id"
              @node-click="handleDeviceNodeClick"
            >
              <template #default="{ node, data }">
                <div class="tree-node" :class="[`node-level-${node.level}`, { 'is-leaf': node.isLeaf }]">
                  <span class="node-label">{{ node.label }}</span>
                </div>
              </template>
            </el-tree>
          </div>
        </div>

        <!-- 右侧设备表格区域 -->
        <div class="content-area" :class="{ 'full-width': deviceTreeCollapsed }">
          <!-- 设备操作按钮栏 -->
          <div class="toolbar">
            <div class="toolbar-left">
              <!-- 展开设备树区域按钮 -->
              <div class="expand-tree-btn-inline" v-if="deviceTreeCollapsed">
                <CollapseToggle 
                  :is-expanded="false" 
                  @toggle="handleDeviceTreeToggle" 
                />
              </div>
            </div>
            
            <div class="toolbar-right">
              <AdvancedSearch 
                @search="handleDeviceAdvancedSearch"
                @reset="handleDeviceAdvancedSearchReset"
                @export="handleExport"
                @upload="handleUpload"
                @template="handleDownloadTemplate"
                @batch="handleBatchOperation"
              />
            </div>
          </div>
          
          <!-- 设备表格内容 -->
          <div class="table-content">
            <el-table 
              :data="deviceTableData" 
              stripe
              @selection-change="handleDeviceSelectionChange"
              style="width: 100%"
            >
              <el-table-column type="selection" width="55" />
              <el-table-column type="index" label="序号" width="80" align="center" />
              <el-table-column prop="deviceName" label="设备名称" min-width="150" />
              <el-table-column prop="tags" label="标签" min-width="200">
                <template #default="scope">
                  <el-tag
                    v-for="tag in scope.row.tags"
                    :key="tag"
                    size="small"
                    style="margin-right: 8px;"
                  >
                    {{ tag }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="deviceId" label="设备ID" min-width="120" />
              <el-table-column prop="streamUrl" label="视频流地址" min-width="250" />
              <el-table-column prop="status" label="状态" min-width="100" align="center">
                <template #default="scope">
                  <el-tag
                      :type="scope.row.status === 1 ? 'success' : 'danger'"
                      size="small">
                    {{ scope.row.status === 1 ? '在线' : '离线' }}
                  </el-tag>
                </template>
              </el-table-column>
<!--              <el-table-column label="操作" width="120" fixed="right" align="right">-->
<!--                <template #default="scope">-->
<!--                  <div class="operation-buttons">-->
<!--                    <el-button -->
<!--                      class="operation-btn view-btn" -->
<!--                      @click="handleViewDevice(scope.row)">-->
<!--                      查看-->
<!--                    </el-button>-->
<!--                  </div>-->
<!--                </template>-->
<!--              </el-table-column>-->
            </el-table>
            
            <!-- 设备分页 - 紧贴表格数据 -->
            <div class="table-pagination">
              <el-pagination
                v-model:current-page="deviceCurrentPage"
                v-model:page-size="devicePageSize"
                :page-sizes="[10, 20, 50, 100]"
                :total="deviceTotal"
                layout="total, sizes, prev, pager, next, jumper"
                @size-change="handleDeviceSizeChange"
                @current-change="handleDeviceCurrentChange"
              />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 新增/编辑标签对话框 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="dialogTitle" 
      width="400px"
      @close="handleDialogClose"
    >
      <el-form :model="tagForm" :rules="tagRules" ref="tagFormRef" label-width="80px">
        <el-form-item label="上级" prop="parentId">
          <TagSelector
            v-model="selectedParentTag"
            placeholder="请选择上级标签"
            :allowed-levels="[0, 1]"
            @change="handleParentTagChange"
          />
        </el-form-item>
        <el-form-item label="标签名称" prop="tagName">
          <el-input v-model="tagForm.tagName" placeholder="请输入" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import {
  Search,
  Plus,
  Edit,
  Delete,
  Operation,
  Refresh,
  Key
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import CollapseToggle from '@/components/CollapseToggle.vue'
import DateRangePicker from '@/components/DateRangePicker.vue'
import SearchInput from '@/components/SearchInput.vue'
import ActionButtonGroup from '@/components/ActionButtonGroup.vue'
import TagSelector from '@/components/TagSelector.vue'
import AdvancedSearch from '@/components/AdvancedSearch.vue'
import { 
  getTagTree, 
  getTagManagementPage,
  createTag, 
  updateTag, 
  deleteTag, 
  batchDeleteTags,
  getTagDevices 
} from '@/api/tagManagement'
import { getDeviceById, getDeviceList, getDeviceTree } from '@/api/device'
import { defaultDeviceTreeData } from './constants.js'

// 当前激活的标签页
const activeTab = ref('maintenance')

// 加载状态
const loading = ref(false)
const treeLoading = ref(false)

// 搜索表单
const searchForm = reactive({
  tagName: '',
  dateRange: []
})

// 设备搜索表单
const deviceSearchForm = reactive({
  deviceName: '',
  relatedTag: ''
})

// 树形搜索关键词
const treeSearchKeyword = ref('')
const deviceTreeSearchKeyword = ref('')

// 树形折叠状态
const treeCollapsed = ref(false)
const deviceTreeCollapsed = ref(false)

// 当前选中的标签节点
const selectedTreeNode = ref(null)
const selectedTagType = ref('own') // own 或 public
const selectedDeviceNode = ref(null)

// 树形数据 - 从API加载
const treeData = ref([])
const publicTreeData = ref([])
const allTagsData = ref([]) // 保存完整的标签数据
const tagNameMap = ref(new Map())

// 转换后端数据为前端树形结构
const convertToTreeData = (apiData) => {
  if (!apiData || !Array.isArray(apiData)) return []
  
  console.log('开始转换树形数据:', apiData.length, '个节点')
  
  // 递归转换树形数据为前端组件所需格式
  const transformTreeData = (nodes) => {
    return nodes.map(node => ({
      id: node.id,
      label: node.tagName,
      type: node.level === 0 ? 'root' : node.level === 1 ? 'category' : 'subcategory',
      tagType: node.categoryType, // 修正：使用categoryType
      level: node.level,
      parentId: node.parentId,
      children: node.children && node.children.length > 0 ? transformTreeData(node.children) : [],
      // 保存原始数据
      original: node
    }))
  }
  
  const result = transformTreeData(apiData)
  console.log('树形数据转换完成:', result.length, '个根节点')
  
  return result
}

// 设备树形数据
const buildTagNameMap = (tags) => {
  const tagMap = new Map()
  if (!Array.isArray(tags)) return tagMap
  tags.forEach((tag) => {
    if (!tag) return
    if (tag.id !== undefined && tag.id !== null && tag.tagName) {
      tagMap.set(tag.id, tag.tagName)
    }
  })
  return tagMap
}

const ensureTagNameMap = async () => {
  if (tagNameMap.value.size > 0) return
  if (allTagsData.value.length > 0) {
    tagNameMap.value = buildTagNameMap(allTagsData.value)
    return
  }
  try {
    const response = await getTagTree()
    const tagData = Array.isArray(response) ? response : (response?.data || [])
    if (Array.isArray(tagData) && tagData.length > 0) {
      const flatten = (nodes) => {
        let result = []
        if (!Array.isArray(nodes)) return result
        nodes.forEach((node) => {
          result.push(node)
          if (Array.isArray(node.children) && node.children.length > 0) {
            result = result.concat(flatten(node.children))
          }
        })
        return result
      }
      tagNameMap.value = buildTagNameMap(flatten(tagData))
    }
  } catch (error) {
    console.warn('Failed to load tag map:', error)
  }
}

const deviceTreeData = ref([...defaultDeviceTreeData])

const treeProps = {
  children: 'children',
  label: 'label'
}

// 表格数据 - 从分页API加载
const tableData = ref([])
const flatTagsList = ref([]) // 展平的标签列表，用于表格显示（保留用于兼容）

// 分页相关
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 当前页表格数据（直接使用tableData，不再使用计算属性）
const currentPageTableData = ref([])

// 查询参数
const queryParams = ref({
  keyword: '',
  categoryType: '',
  level: null,
  parentId: null
})

// 选中行
const selectedRows = ref([])

// 设备表格数据
const deviceTableData = ref([])

const normalizeDeviceRow = (device, deviceDetail) => {
  const merged = { ...(device || {}), ...(deviceDetail || {}) }

  let tags = []
  if (Array.isArray(merged.selectedTags) && merged.selectedTags.length > 0) {
    tags = merged.selectedTags.map((tagId) => tagNameMap.value.get(tagId) || `Tag_${tagId}`)
  } else if (Array.isArray(merged.tags) && merged.tags.length > 0) {
    tags = merged.tags
  }

  return {
    ...merged,
    deviceId: merged.deviceId ?? merged.id,
    deviceName: merged.deviceName ?? merged.name ?? merged.deviceTitle ?? '-',
    streamUrl: merged.streamUrl ?? merged.originalRtspUrl ?? merged.rtspUrl ?? merged.webrtcUrl ?? '-',
    tags,
    status: merged.statusDesc ?? merged.status ?? merged.deviceStatus ?? '-'
  }
}

const loadDeviceTree = async () => {
  try {
    const response = await getDeviceTree()
    if (response?.data) {
      deviceTreeData.value = response.data
    }
  } catch (error) {
    console.error('Failed to load device tree:', error)
  }
}

const loadDeviceList = async () => {
  try {
    await ensureTagNameMap()
    const params = {
      page: deviceCurrentPage.value,
      size: devicePageSize.value,
      deviceName: deviceSearchForm.deviceName
    }
    if (deviceSearchForm.relatedTag) {
      params.tagName = deviceSearchForm.relatedTag
    }

    const response = await getDeviceList(params)
    const records = response?.data?.records || response?.records || []
    deviceTableData.value = await Promise.all(
      records.map(async (device) => {
        let detailData = null
        const deviceId = device?.id ?? device?.deviceId
        if (deviceId !== undefined && deviceId !== null) {
          try {
            const detailResponse = await getDeviceById(deviceId)
            if (detailResponse?.code === 200 && detailResponse.data) {
              detailData = detailResponse.data
            }
          } catch (error) {
            console.warn(`Failed to load device detail: ${deviceId}`, error)
          }
        }
        return normalizeDeviceRow(device, detailData)
      })
    )
    deviceTotal.value = response?.data?.total || response?.total || deviceTableData.value.length
  } catch (error) {
    console.error('Failed to load devices:', error)
    ElMessage.error('Failed to load devices')
  }
}


const deviceCurrentPage = ref(1)
const devicePageSize = ref(10)
const deviceTotal = ref(0)

// 设备选中行
const selectedDeviceRows = ref([])

// 对话框相关
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEditing = ref(false)
const tagFormRef = ref()

// 表单数据
const tagForm = reactive({
  id: null,
  tagName: '',
  parentId: null,
  parentCategory: '',
  categoryId: null,
  categoryType: 'own',
  description: '',
  isPublic: false,
  level: 1,
  position: 0
})

// 分类选项
const categoryOptions = ref([
  { id: 1, name: '一级标签' },
  { id: 2, name: '公共' },
  { id: 3, name: '所有标签' }
])

// 父级选项
const parentOptions = ref([])

// TagSelector相关
const selectedParentTag = ref([])

// 表单验证规则
const tagRules = {
  tagName: [
    { required: true, message: '请输入标签名称', trigger: 'blur' }
  ],
  parentId: [
    { required: true, message: '请选择上级', trigger: 'change' }
  ]
}

// 方法
const handleTreeAction = (action) => {
  console.log('树操作:', action)
}

// 根据ID查找标签名称
const findTagNameById = (id) => {
  if (!id || !allTagsData.value.length) return ''
  
  // 递归查找标签
  const findInTree = (nodes) => {
    for (const node of nodes) {
      if (node.id === id) {
        return node.tagName
      }
      if (node.children && node.children.length > 0) {
        const found = findInTree(node.children)
        if (found) return found
      }
    }
    return null
  }
  
  return findInTree(allTagsData.value) || ''
}

// 生成父级选项
const generateParentOptions = () => {
  const options = []
  
  console.log('生成新增父级选项，allTagsData长度:', allTagsData.value.length)
  console.log('当前选中树节点:', selectedTreeNode.value)
  
  if (!allTagsData.value.length) {
    console.warn('allTagsData为空，返回默认选项')
    // 如果数据还没加载完成，提供默认选项
    return [
      { id: 'own', name: '自有', level: 0, categoryType: 'own' },
      { id: 'public', name: '公共', level: 0, categoryType: 'public' }
    ]
  }
  
  // 根据当前选中的树节点或表格数据的上下文确定显示哪些父级选项
  if (selectedTreeNode.value) {
    const node = selectedTreeNode.value
    console.log('根据选中节点生成选项，节点:', node)
    
    if (node.level === 1) {
      // 选中的是标签类型（level=1），新增的标签应该是level=2，父级就是这个标签类型
      options.push({
        id: node.id,
        name: node.label,
        level: node.level,
        categoryType: node.tagType
      })
    } else if (node.level === 2) {
      // 选中的是具体标签（level=2），新增的标签应该与其平级，父级应该是其父级
      if (node.parentId) {
        const parentTag = findTagInAllData(node.parentId)
        if (parentTag) {
          options.push({
            id: parentTag.id,
            name: parentTag.tagName,
            level: parentTag.level,
            categoryType: parentTag.categoryType
          })
        }
      }
    } else if (node.tagType) {
      // 选中的是大类（自有/公共），新增的标签应该是level=1，父级应该是大类
      options.push({
        id: node.tagType,
        name: node.tagType === 'own' ? '自有' : '公共',
        level: 0,
        categoryType: node.tagType
      })
    }
  } else {
    console.log('没有选中节点，生成通用选项')
    // 没有选中特定节点，显示所有可能的父级选项
    
    // 添加根级选项（自有/公共）
    options.push(
      { id: 'own', name: '自有', level: 0, categoryType: 'own' },
      { id: 'public', name: '公共', level: 0, categoryType: 'public' }
    )
    
    // 添加所有level=1的标签类型作为可选父级
    const level1Tags = allTagsData.value.filter(tag => tag.level === 1)
    console.log('找到的level=1标签:', level1Tags)
    
    // 使用Set来去重，防止重复添加
    const addedIds = new Set(['own', 'public']) // 已经添加的根级选项
    
    level1Tags.forEach(tag => {
      if (!addedIds.has(tag.id)) {
        addedIds.add(tag.id)
        options.push({
          id: tag.id,
          name: tag.tagName,
          level: tag.level,
          categoryType: tag.categoryType
        })
      }
    })
  }
  
  console.log('生成的父级选项:', options)
  return options
}

// 在所有数据中查找标签
const findTagInAllData = (id) => {
  if (!id || !allTagsData.value.length) return null
  
  // 递归查找标签
  const findInTree = (nodes) => {
    for (const node of nodes) {
      if (node.id === id) {
        return node
      }
      if (node.children && node.children.length > 0) {
        const found = findInTree(node.children)
        if (found) return found
      }
    }
    return null
  }
  
  return findInTree(allTagsData.value)
}

// 根据父级标签建议子标签名称
const getSuggestedTagNames = (parentName, categoryType) => {
  const suggestions = []
  
  // 根据父级名称和分类类型建议合适的子标签名称
  if (parentName === '设备类型') {
    suggestions.push('网络摄像机', '模拟摄像机', '云台摄像机', '红外摄像机', '鱼眼摄像机')
  } else if (parentName === '设备位置') {
    suggestions.push('室内区域', '室外区域', '入口区域', '通道区域', '重点区域')
  } else if (parentName === '设备品牌') {
    suggestions.push('海康威视', '大华技术', '宇视科技', '华为技术', '天地伟业')
  } else if (parentName === '设备状态') {
    suggestions.push('正常运行', '离线状态', '故障状态', '维护状态', '停用状态')
  } else if (parentName === '通用分类') {
    suggestions.push('基础设备', '高级设备', '特殊设备', '临时设备', '备用设备')
  } else if (parentName === '功能分类') {
    suggestions.push('监控录像', '实时预览', '报警联动', '智能分析', '远程控制')
  } else {
    // 通用建议
    if (categoryType === 'own') {
      suggestions.push('新建标签', '自定义标签', '专用标签')
    } else if (categoryType === 'public') {
      suggestions.push('公共标签', '通用标签', '共享标签')
    } else {
      suggestions.push('新标签')
    }
  }
  
  // 检查建议的名称是否已存在，如果存在则添加数字后缀
  const existingNames = new Set()
  if (allTagsData.value) {
    allTagsData.value.forEach(tag => {
      if (tag.tagName) {
        existingNames.add(tag.tagName)
      }
    })
  }
  
  const uniqueSuggestions = suggestions.map(name => {
    let uniqueName = name
    let counter = 1
    while (existingNames.has(uniqueName)) {
      uniqueName = `${name}${counter}`
      counter++
    }
    return uniqueName
  })
  
  return uniqueSuggestions
}

// 建议标签类型名称（level=1）
const getSuggestedCategoryNames = (categoryType) => {
  const suggestions = []
  
  if (categoryType === 'own') {
    suggestions.push('设备类型', '设备位置', '设备品牌', '设备状态', '使用场景', '重要等级')
  } else if (categoryType === 'public') {
    suggestions.push('通用分类', '功能分类', '技术规格', '标准类型', '应用场景', '管理分类')
  }
  
  // 检查建议的名称是否已存在
  const existingNames = new Set()
  if (allTagsData.value) {
    allTagsData.value.forEach(tag => {
      if (tag.tagName) {
        existingNames.add(tag.tagName)
      }
    })
  }
  
  const uniqueSuggestions = suggestions.map(name => {
    let uniqueName = name
    let counter = 1
    while (existingNames.has(uniqueName)) {
      uniqueName = `${name}${counter}`
      counter++
    }
    return uniqueName
  })
  
  return uniqueSuggestions
}

// 处理父级选择变更
const handleParentChange = (parentId) => {
  console.log('父级选择变更:', parentId)
  
  // 根据选中的父级更新相关字段
  const selectedParent = parentOptions.value.find(option => option.id === parentId)
  if (selectedParent) {
    // 设置标签的层级和分类类型
    tagForm.level = selectedParent.level + 1
    tagForm.categoryType = selectedParent.categoryType
    
    console.log('更新标签层级和分类:', {
      level: tagForm.level,
      categoryType: tagForm.categoryType
    })
  }
}

// 处理TagSelector父级标签选择变更
const handleParentTagChange = (selectedTags) => {
  console.log('TagSelector父级选择变更:', selectedTags)
  
  // 实现单选逻辑：如果选择了新的标签，则只保留最后一个
  if (selectedTags && selectedTags.length > 1) {
    // 保留最后选择的标签
    const lastSelected = selectedTags[selectedTags.length - 1]
    selectedParentTag.value = [lastSelected]
    tagForm.parentId = lastSelected
    
    // 处理选择的标签类型
    handleSelectedTag(lastSelected)
  } else if (selectedTags && selectedTags.length === 1) {
    const selectedTag = selectedTags[0]
    tagForm.parentId = selectedTag
    
    // 处理选择的标签类型
    handleSelectedTag(selectedTag)
  } else {
    tagForm.parentId = null
    // 重置相关字段
    tagForm.categoryType = 'own'
    tagForm.level = 1
  }
}

// 处理选择的标签，设置相关字段
const handleSelectedTag = (selectedValue) => {
  console.log('处理选择的标签:', selectedValue)
  
  // 如果选择的是大类（"own"/"public"）
  if (selectedValue === 'own' || selectedValue === 'public') {
    tagForm.categoryType = selectedValue
    tagForm.level = 1  // 大类下的子标签是level=1
    console.log('选择了大类:', selectedValue, '设置categoryType:', tagForm.categoryType, 'level:', tagForm.level)
  } else {
    // 如果选择的是具体的标签ID，需要查找该标签的信息
    const parentTag = findTagById(selectedValue)
    if (parentTag) {
      tagForm.categoryType = parentTag.categoryType
      tagForm.level = parentTag.level + 1  // 子标签比父标签高一层
      console.log('选择了具体标签:', selectedValue, '父标签信息:', parentTag, '设置categoryType:', tagForm.categoryType, 'level:', tagForm.level)
    } else {
      // 触发原有的逻辑作为备用
      handleParentChange(selectedValue)
    }
  }
}

// 查找标签信息的辅助方法
const findTagById = (tagId) => {
  // 在allTagsData中查找对应的标签
  return allTagsData.value.find(tag => tag.id === tagId || tag.id === parseInt(tagId))
}

// 生成编辑模式的父级选项
const generateEditParentOptions = (currentRow) => {
  const options = []
  
  console.log('生成编辑父级选项，当前行数据:', currentRow)
  console.log('allTagsData:', allTagsData.value)
  
  if (!allTagsData.value.length) {
    console.warn('allTagsData为空，无法生成父级选项')
    return options
  }
  
  // 根据当前标签的层级确定可选的父级
  if (currentRow.level === 1) {
    // 编辑标签类型（level=1），父级应该是根级（自有/公共）
    options.push(
      { id: 'own', name: '自有', level: 0, categoryType: 'own' },
      { id: 'public', name: '公共', level: 0, categoryType: 'public' }
    )
    
    // 自动设置当前的父级ID为对应的分类类型
    setTimeout(() => {
      tagForm.parentId = currentRow.categoryType
      selectedParentTag.value = [currentRow.categoryType]
      console.log('自动设置父级ID为:', currentRow.categoryType)
    }, 0)
    
  } else if (currentRow.level === 2) {
    // 编辑具体标签（level=2），父级应该是同类型下的所有标签类型（level=1）
    console.log('查找level=1的标签，categoryType:', currentRow.categoryType)
    
    // 直接从展平的数据中查找level=1的标签，避免重复
    const level1Tags = allTagsData.value.filter(tag => 
      tag.level === 1 && tag.categoryType === currentRow.categoryType
    )
    
    console.log('找到的level=1标签:', level1Tags)
    
    // 使用Set来去重，防止重复添加
    const addedIds = new Set()
    
    level1Tags.forEach(tag => {
      if (!addedIds.has(tag.id)) {
        addedIds.add(tag.id)
        options.push({
          id: tag.id,
          name: tag.tagName,
          level: tag.level,
          categoryType: tag.categoryType
        })
      }
    })
    
    // 自动设置当前的父级ID
    setTimeout(() => {
      tagForm.parentId = currentRow.parentId
      selectedParentTag.value = currentRow.parentId ? [currentRow.parentId] : []
      console.log('自动设置父级ID为:', currentRow.parentId)
    }, 0)
  }
  
  console.log('生成的父级选项:', options)
  parentOptions.value = options
}

const handleTreeToggle = () => {
  console.log('点击标签树折叠按钮，当前状态:', treeCollapsed.value)
  treeCollapsed.value = !treeCollapsed.value
  console.log('标签树折叠状态变更为:', treeCollapsed.value)
  console.log('isExpanded 应该为:', !treeCollapsed.value)
}

const handleDeviceTreeToggle = () => {
  console.log('点击设备树折叠按钮，当前状态:', deviceTreeCollapsed.value)
  deviceTreeCollapsed.value = !deviceTreeCollapsed.value
  console.log('设备树折叠状态变更为:', deviceTreeCollapsed.value)
  console.log('isExpanded 应该为:', !deviceTreeCollapsed.value)
}

const handleDeviceNodeClick = (data) => {
  selectedDeviceNode.value = data
}

const handleNodeClick = (data) => {
  console.log('节点点击:', data)
  selectedTreeNode.value = data
  selectedTagType.value = data.tagType || 'own'
  
  // 清空搜索条件，避免干扰树节点过滤
  queryParams.value = {
    keyword: '',
    categoryType: '',
    level: null,
    parentId: null
  }
  
  // 同时清空搜索表单
  searchForm.tagName = ''
  
  // 根据选中的节点过滤表格数据
  currentPage.value = 1
  loadTagData()
}

const addChild = (data) => {
  console.log('添加子节点:', data)
  
  // 设置选中的树节点，这样generateParentOptions会生成正确的父级选项
  selectedTreeNode.value = data
  
  // 打开新增对话框，允许自动填充
  handleAdd(true)
}

const deleteNode = async (data) => {
  console.log('删除节点:', data)
  try {
    await ElMessageBox.confirm(
      `确定要删除标签 "${data.label}" 吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    loading.value = true
    await deleteTag(data.id)
    ElMessage.success('删除成功')
    
    // 重新加载树形结构和表格数据
    await loadTagTree()
    await loadTagData()
  } catch (error) {
    if (error === 'cancel') {
      ElMessage.info('已取消删除')
    } else {
      console.error('删除失败:', error)
      ElMessage.error('删除失败: ' + (error.message || '未知错误'))
    }
  } finally {
    loading.value = false
  }
}

const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

// 工具栏新增按钮（不自动填充）
const handleToolbarAdd = () => {
  console.log('工具栏新增按钮点击')
  // 清空选中的树节点，确保不会自动填充
  selectedTreeNode.value = null
  handleAdd(false) // 传递false表示不自动填充
}

// 通用新增函数
const handleAdd = (autoFill = true) => {
  dialogTitle.value = '新增标签'
  isEditing.value = false
  resetForm()
  
  // 生成父级选项
  parentOptions.value = generateParentOptions()
  
  // 根据上下文自动填充信息（仅当autoFill为true且有选中节点时）
  if (autoFill && selectedTreeNode.value) {
    const node = selectedTreeNode.value
    console.log('根据选中节点自动填充信息:', node)
    
    // 自动设置父级
    if (node.level === 1) {
      // 选中的是标签类型（level=1），新增的标签父级是这个类型
      tagForm.parentId = node.id
      selectedParentTag.value = [node.id]
      tagForm.level = 2
      tagForm.categoryType = node.tagType
      
      // 根据父级类型建议标签名称
      const suggestions = getSuggestedTagNames(node.label, node.tagType)
      if (suggestions.length > 0) {
        tagForm.tagName = suggestions[0]
      }
    } else if (node.tagType && node.level === 0) {
      // 选中的是根级（自有/公共），新增的标签应该是level=1
      tagForm.parentId = node.id
      selectedParentTag.value = [node.id]
      tagForm.level = 1
      tagForm.categoryType = node.tagType
      
      // 建议标签类型名称
      const suggestions = getSuggestedCategoryNames(node.tagType)
      if (suggestions.length > 0) {
        tagForm.tagName = suggestions[0]
      }
    } else if (node.level === 2) {
      // 选中的是具体标签，新增与其平级的标签
      if (node.parentId) {
        tagForm.parentId = node.parentId
        selectedParentTag.value = [node.parentId]
        tagForm.level = 2
        tagForm.categoryType = node.tagType
        
        // 根据同级标签建议名称
        const parentTag = findTagInAllData(node.parentId)
        if (parentTag) {
          const suggestions = getSuggestedTagNames(parentTag.tagName, node.tagType)
          if (suggestions.length > 0) {
            tagForm.tagName = suggestions[0]
          }
        }
      }
    }
    
    // 确保父级选项包含我们设置的父级ID
    handleParentChange(tagForm.parentId)
  } else {
    // 没有选中特定节点或不自动填充时，设置默认父级
    if (parentOptions.value.length > 0) {
      tagForm.parentId = parentOptions.value[0].id
      selectedParentTag.value = [parentOptions.value[0].id]
      handleParentChange(tagForm.parentId)
    }
  }
  
  console.log('新增标签表单初始化完成:', tagForm)
  dialogVisible.value = true
}

const handleEdit = () => {
  if (selectedRows.value.length !== 1) {
    ElMessage.warning('请选择一条记录进行编辑')
    return
  }
  
  dialogTitle.value = '编辑标签'
  isEditing.value = true
  const row = selectedRows.value[0]
  
  // 生成父级选项 - 编辑时需要更灵活的选项
  generateEditParentOptions(row)
  
  Object.assign(tagForm, {
    id: row.id,
    tagName: row.tagName,
    parentId: row.parentId,
    categoryId: row.categoryId || 1,
    description: row.description,
    isPublic: row.isPublic,
    level: row.level,
    position: row.position || 0
  })
  
  // 设置TagSelector的选中值
  selectedParentTag.value = row.parentId ? [row.parentId] : []
  
  dialogVisible.value = true
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
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    loading.value = true
    
    // 批量删除
    if (selectedRows.value.length === 1) {
      await deleteTag(selectedRows.value[0].id)
    } else {
      const tagIds = selectedRows.value.map(row => row.id)
      await batchDeleteTags(tagIds)
    }
    
    selectedRows.value = []
    ElMessage.success('删除成功')
    
    // 重新加载树形结构和表格数据
    await loadTagTree()
    await loadTagData()
  } catch (error) {
    if (error === 'cancel') {
      ElMessage.info('已取消删除')
    } else {
      console.error('删除失败:', error)
      ElMessage.error('删除失败: ' + (error.message || '未知错误'))
    }
  } finally {
    loading.value = false
  }
}

const handleRowEdit = (row) => {
  dialogTitle.value = '编辑标签'
  isEditing.value = true

  // 生成父级选项 - 编辑时需要更灵活的选项
  generateEditParentOptions(row)

  Object.assign(tagForm, {
    id: row.id,
    tagName: row.tagName,
    parentId: row.parentId,
    categoryId: row.categoryId || 1,
    description: row.description,
    isPublic: row.isPublic,
    level: row.level,
    position: row.position || 0
  })
  
  // 设置TagSelector的选中值
  selectedParentTag.value = row.parentId ? [row.parentId] : []
  
  dialogVisible.value = true
}

const handleRowDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除标签 "${row.tagName}" 吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    loading.value = true
    await deleteTag(row.id)
    ElMessage.success('删除成功')
    
    // 重新加载树形结构和表格数据
    await loadTagTree()
    await loadTagData()
  } catch (error) {
    if (error === 'cancel') {
      ElMessage.info('已取消删除')
    } else {
      console.error('删除失败:', error)
      ElMessage.error('删除失败: ' + (error.message || '未知错误'))
    }
  } finally {
    loading.value = false
  }
}

const handlePublicChange = async (row) => {
  try {
    loading.value = true
    
    const tagData = {
      tagName: row.tagName,
      description: row.description,
      tagType: row.isPublic ? 'public' : 'own',
      parentId: row.parentId,
      level: row.level,
      position: row.position || 0
    }
    
    await updateTag(row.id, tagData)
    ElMessage.success(`已${row.isPublic ? '公开' : '取消公开'}标签`)
    
    // 重新加载树形结构和表格数据
    await loadTagTree()
    await loadTagData()
  } catch (error) {
    // 恢复原状态
    row.isPublic = !row.isPublic
    console.error('状态切换失败:', error)
    ElMessage.error('状态切换失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
  loadTagData()
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  loadTagData()
}

// 设备相关方法
const handleDeviceSelectionChange = (selection) => {
  selectedDeviceRows.value = selection
}

const handleDeviceSizeChange = (val) => {
  devicePageSize.value = val
  deviceCurrentPage.value = 1
  loadDeviceList()
}

const handleDeviceCurrentChange = (val) => {
  deviceCurrentPage.value = val
  loadDeviceList()
}

const handleViewDevice = (row) => {
  console.log('查看设备:', row)
  // 这里可以添加查看设备的具体逻辑
}

const handleDialogClose = () => {
  resetForm()
}

const handleSave = async () => {
  if (!tagFormRef.value) return
  
  try {
    await tagFormRef.value.validate()
    loading.value = true
    
    // 处理parentId的类型转换
    let processedParentId = null
    
    if (tagForm.parentId) {
      // 如果选择的是大类（"own"/"public"），则parentId应该为null
      if (tagForm.parentId === 'own' || tagForm.parentId === 'public') {
        processedParentId = null
        // 同时更新categoryType
        if (!tagForm.categoryType) {
          tagForm.categoryType = tagForm.parentId
        }
      } else {
        // 如果是数字ID，转换为Long类型
        const parentIdNum = parseInt(tagForm.parentId)
        processedParentId = isNaN(parentIdNum) ? null : parentIdNum
      }
    }
    
    const tagData = {
      tagName: tagForm.tagName,
      description: tagForm.description,
      tagType: tagForm.categoryType || 'own',
      parentId: processedParentId,
      level: tagForm.level || 1,
      position: tagForm.position || 0
    }
    
    console.log('保存标签数据:', tagData)
    console.log('原始parentId:', tagForm.parentId, '处理后parentId:', processedParentId)
    
    if (isEditing.value) {
      // 编辑逻辑
      await updateTag(tagForm.id, tagData)
      ElMessage.success('编辑成功')
    } else {
      // 新增逻辑
      await createTag(tagData)
      ElMessage.success('新增成功')
    }
    
    dialogVisible.value = false
    selectedRows.value = []
    // 重新加载树形结构和表格数据
    await loadTagTree()
    await loadTagData()
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  Object.assign(tagForm, {
    id: null,
    tagName: '',
    parentId: null,
    parentCategory: '根级',
    categoryId: null,
    categoryType: 'own',
    description: '',
    isPublic: false,
    level: 1,
    position: 0
  })
  
  // 重置TagSelector选择
  selectedParentTag.value = []
  
  if (tagFormRef.value) {
    tagFormRef.value.resetFields()
  }
}

// 分页查询标签数据
const loadTagData = async () => {
  try {
    loading.value = true
    
    // 构建查询参数
    const params = {
      current: currentPage.value,
      size: pageSize.value,
      ...queryParams.value
    }
    
    // 根据选中的树节点设置过滤条件
    if (selectedTreeNode.value) {
      console.log('选中的树节点:', selectedTreeNode.value)
      
      // 判断节点类型
      if (selectedTreeNode.value.level === 1) {
        // 选中的是标签类型（一级节点），显示该类型下的所有子标签
        params.parentId = selectedTreeNode.value.id
        params.level = 2
        console.log('过滤条件 - 标签类型:', { parentId: params.parentId, level: params.level })
      } else if (selectedTreeNode.value.level === 2) {
        // 选中的是具体标签（二级节点），只显示该具体标签
        params.tagId = selectedTreeNode.value.id // 使用标签ID进行精确查询
        console.log('过滤条件 - 具体标签:', { tagId: params.tagId })
      } else if (selectedTreeNode.value.tagType) {
        // 选中的是大类（自有/公共），显示该大类下的所有标签
        params.categoryType = selectedTreeNode.value.tagType
        console.log('过滤条件 - 大类:', { categoryType: params.categoryType })
      }
    }
    
    console.log('分页查询参数:', params)
    
    const response = await getTagManagementPage(params)
    console.log('分页查询响应:', response)
    
    // 处理响应数据 - 可能直接是分页数据，也可能包装在success/data中
    let pageData = null
    
    if (response && response.records !== undefined) {
      // 直接的分页数据格式
      pageData = response
    } else if (response && response.success && response.data) {
      // 包装的成功响应格式
      pageData = response.data
    } else if (response && response.data && response.data.records !== undefined) {
      // 其他包装格式
      pageData = response.data
    }
    
    if (pageData && pageData.records !== undefined) {
      // 处理返回的分页数据
      const records = pageData.records || []
      
      // 转换数据格式
      const formattedData = records.map(tag => ({
        id: tag.id,
        tagName: tag.tagName,
        description: tag.description || '-',
        isPublic: tag.categoryType === 'public',
        createTime: tag.createTime ? new Date(tag.createTime).toLocaleString() : '-',
        level: tag.level,
        parentId: tag.parentId,
        categoryType: tag.categoryType,
        position: tag.sortOrder || 0,
        tagColor: tag.tagColor,
        isActive: tag.isActive
      }))
      
      // 更新表格数据
      tableData.value = formattedData
      currentPageTableData.value = formattedData
      total.value = pageData.total || 0
      
      console.log(`分页查询成功：当前页 ${pageData.current}/${pageData.pages || Math.ceil(pageData.total / pageData.size)}，共 ${pageData.total} 条记录`)
    } else {
      console.error('分页查询失败或数据格式不正确:', response)
      ElMessage.error('查询标签数据失败')
    }
  } catch (error) {
    console.error('分页查询出错:', error)
    ElMessage.error(`查询失败: ${error.message}`)
  } finally {
    loading.value = false
  }
}

// 树形结构加载函数（用于左侧导航）
const loadTagTree = async () => {
  try {
    treeLoading.value = true
    const response = await getTagTree()
    
    console.log('树形API响应数据:', response)
    
    // 响应处理 - 根据axios拦截器的处理逻辑
    let tagData = []
    if (Array.isArray(response)) {
      tagData = response
    } else if (response && response.data) {
      tagData = response.data
    } else if (response && response.success && response.data) {
      tagData = response.data
    }
    
    console.log('处理后的标签数据:', tagData)
    
    if (tagData && tagData.length > 0) {
      // 后端返回的数据格式：
      // [
      //   {id: "own", tagName: "自有标签", categoryType: "own", children: [...]},
      //   {id: "public", tagName: "公共标签", categoryType: "public", children: [...]}
      // ]
      
      // 查找自有标签和公共标签根节点
      const ownRoot = tagData.find(item => item.categoryType === 'own')
      const publicRoot = tagData.find(item => item.categoryType === 'public')
      
      console.log('自有标签根节点:', ownRoot)
      console.log('公共标签根节点:', publicRoot)
      
      // 转换为前端组件需要的格式
      if (ownRoot && ownRoot.children) {
        treeData.value = convertToTreeData(ownRoot.children)
        console.log('自有标签树形数据:', treeData.value)
      } else {
        treeData.value = []
        console.log('没有自有标签数据')
      }
      
      if (publicRoot && publicRoot.children) {
        publicTreeData.value = convertToTreeData(publicRoot.children)
        console.log('公共标签树形数据:', publicTreeData.value)
      } else {
        publicTreeData.value = []
        console.log('没有公共标签数据')
      }
      
      console.log('树形结构加载完成')
      
      // 保存完整的标签数据用于查找功能 - 需要展平树形数据
      const flattenAllTags = (nodes) => {
        let result = []
        if (!nodes || !Array.isArray(nodes)) return result
        
        for (const node of nodes) {
          result.push(node)
          if (node.children && node.children.length > 0) {
            result = result.concat(flattenAllTags(node.children))
          }
        }
        return result
      }
      
      allTagsData.value = flattenAllTags(tagData)
      console.log('保存的完整标签数据:', allTagsData.value)
      tagNameMap.value = buildTagNameMap(allTagsData.value)
      
      // 加载表格数据
      await loadTagData()
    } else {
      console.warn('没有获取到标签数据')
      ElMessage.warning('没有获取到标签数据')
    }
  } catch (error) {
    console.error('加载标签树失败:', error)
    ElMessage.error(`加载标签树失败: ${error.message}`)
  } finally {
    treeLoading.value = false
  }
}

// 计算树形数据中的总标签数量
const countAllTags = (nodes) => {
  let count = 0
  nodes.forEach(node => {
    count += 1
    if (node.children && node.children.length > 0) {
      count += countAllTags(node.children)
    }
  })
  return count
}

// 展平标签数据
const flattenTags = (tags) => {
  console.log('开始展平标签数据:', tags.length, '个根节点')
  
  // 递归展平树形数据
  const flattenRecursive = (nodes) => {
    let result = []
    
    nodes.forEach(node => {
      // 添加当前节点
      result.push({
        id: node.id,
        tagName: node.tagName,
        description: node.description || '-',
        isPublic: node.tagType === 'public',
        createTime: node.createTime ? new Date(node.createTime).toLocaleString() : '-',
        level: node.level,
        parentId: node.parentId,
        tagType: node.tagType,
        position: node.sortOrder || 0,
        tagColor: node.tagColor,
        isActive: node.isActive
      })
      
      // 递归处理子节点
      if (node.children && node.children.length > 0) {
        result = result.concat(flattenRecursive(node.children))
      }
    })
    
    return result
  }
  
  const flattenedData = flattenRecursive(tags)
  
  console.log('展平后的数据:', flattenedData.length, '个标签')
  
  // 保存完整数据用于过滤
  allTagsData.value = flattenedData
  flatTagsList.value = [...allTagsData.value]
  total.value = flatTagsList.value.length
  
  console.log('表格数据已更新，总数:', total.value)
}

// 筛选标签数据
const filterTagsList = () => {
  let filtered = allTagsData.value
  
  // 根据当前选中的节点过滤
  if (selectedTreeNode.value) {
    if (selectedTreeNode.value.type === 'category') {
      // 选中分类，显示该分类下的所有标签
      filtered = filtered.filter(tag => 
        tag.parentId === selectedTreeNode.value.id || 
        tag.id === selectedTreeNode.value.id
      )
    } else if (selectedTreeNode.value.type === 'public') {
      // 选中公共标签
      filtered = filtered.filter(tag => tag.tagType === 'public')
    }
  }
  
  // 根据搜索条件过滤
  if (searchForm.tagName) {
    filtered = filtered.filter(tag => 
      tag.tagName.toLowerCase().includes(searchForm.tagName.toLowerCase())
    )
  }
  
  flatTagsList.value = filtered
  total.value = filtered.length
  currentPage.value = 1
}

// 搜索处理方法
const handleSearch = () => {
  console.log('执行搜索:', searchForm)
  
  // 清除树节点选择，使用搜索条件
  selectedTreeNode.value = null
  selectedTagType.value = 'own'
  
  queryParams.value.keyword = searchForm.tagName
  currentPage.value = 1
  loadTagData()
}

const handleReset = () => {
  console.log('重置搜索条件')
  Object.assign(searchForm, {
    tagName: '',
    dateRange: []
  })
  selectedTreeNode.value = null
  queryParams.value = {
    keyword: '',
    categoryType: '',
    level: null,
    parentId: null
  }
  currentPage.value = 1
  loadTagData()
}

// 树形搜索处理方法
const handleTreeSearch = (searchValue) => {
  console.log('树形搜索:', searchValue)
  // 这里可以添加树形结构搜索逻辑
}

const handleDeviceTreeSearch = (searchValue) => {
  console.log('设备树搜索:', searchValue)
  // 这里可以添加设备树搜索逻辑
}

// 高级搜索相关方法
const handleAdvancedSearch = (searchData) => {
  console.log('高级搜索:', searchData)
  
  // 更新搜索表单
  if (searchData.keyword) {
    searchForm.tagName = searchData.keyword
  }
  if (searchData.deviceName) {
    searchForm.tagName = searchData.deviceName
  }
  if (searchData.deviceId) {
    searchForm.tagName = searchData.deviceId
  }
  if (searchData.selectedTags && searchData.selectedTags.length > 0) {
    searchForm.tagName = searchData.selectedTags[0]
  }
  if (searchData.dateRange && searchData.dateRange.length > 0) {
    searchForm.dateRange = searchData.dateRange
  }
  
  handleSearch()
}

const handleAdvancedSearchReset = () => {
  console.log('重置高级搜索')
  searchForm.tagName = ''
  searchForm.dateRange = []
  handleReset()
}

const handleDeviceAdvancedSearch = (searchData) => {
  console.log('设备高级搜索:', searchData)
  
  // 更新设备搜索表单
  if (searchData.keyword) {
    deviceSearchForm.deviceName = searchData.keyword
  }
  if (searchData.deviceName) {
    deviceSearchForm.deviceName = searchData.deviceName
  }
  if (searchData.deviceId) {
    deviceSearchForm.deviceName = searchData.deviceId
  }
  if (searchData.selectedTags && searchData.selectedTags.length > 0) {
    deviceSearchForm.relatedTag = searchData.selectedTags[0]
  }
  deviceCurrentPage.value = 1
  loadDeviceList()
  ElMessage.success('设备高级搜索完成')
}

const handleDeviceAdvancedSearchReset = () => {
  console.log('重置设备高级搜索')
  deviceSearchForm.deviceName = ''
  deviceSearchForm.relatedTag = ''
  deviceCurrentPage.value = 1
  loadDeviceList()
  ElMessage.info('已重置设备高级搜索条件')
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

onMounted(() => {
  // 初始化树形结构和表格数据
  loadTagTree()
  loadDeviceTree()
  loadDeviceList()
})
</script>

<style scoped>
.tag-management {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
  padding: 20px;
  overflow: hidden;
}

/* 标签页切换 - 顶部 */
.tab-nav {
  display: flex;
  background: #F0F2F5;
  border-radius: 8px 8px 0 0;
  padding: 20px 20px 0 20px;
  border-bottom: 1px solid rgba(26, 83, 255, 0.2);
  margin-bottom: 0;
}

.tab-item {
  padding: 12px 24px;
  cursor: pointer;
  border-bottom: 3px solid transparent;
  color: #606266;
  transition: all 0.3s;
  background: transparent;
}

.tab-item.active {
  color: #1A53FF;
  border-bottom-color: #1A53FF;
  font-weight: 600;
}

.tab-item:hover:not(.active) {
  color: #1A53FF;
}



/* 主要内容区域 */
.main-content-container {
  flex: 1;
  display: flex;
  gap: 0;
  min-height: 0;
  overflow: hidden;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border-radius: 0 0 8px 8px;
}



.tab-item {
  padding: 12px 24px;
  cursor: pointer;
  border-bottom: 3px solid transparent;
  color: #606266;
  transition: all 0.3s;
  background: transparent;
}

.tab-item.active {
  color: #1A53FF;
  border-bottom-color: #1A53FF;
  font-weight: 600;
}

.tab-item:hover:not(.active) {
  color: #1A53FF;
}

/* 标签维护内容 */
.maintenance-content {
  flex: 1;
  display: flex;
  gap: 0;
  min-height: 0;
  overflow: hidden;
}

/* 标签设备内容 */
.device-content {
  flex: 1;
  display: flex;
  gap: 0;
  min-height: 0;
  overflow: hidden;
}

/* 搜索区域 */
.search-section {
  flex: 1;
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
}

/* 左侧树形结构 */
.device-tree {
  width: 280px;
  min-width: 280px;
  max-width: 280px;
  flex-shrink: 0;
  background: white;
  border-right: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
}

/* 设备树区域圆角 */
.device-tree {
  border-radius: 0 0 0 8px;
}

.tree-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.tree-title {
  font-size: 16px;
  font-weight: 600;
  color: #1A53FF;
}

.tree-actions {
  display: flex;
  gap: 8px;
}

.tree-search {
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.tree-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

.tree-node {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  position: relative;
}

.node-label {
  flex: 1;
  font-size: 14px;
}

.node-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.3s;
}

.tree-node:hover .node-actions {
  opacity: 1;
}

/* 不同层级的节点样式 */
.node-level-1 .node-label {
  font-weight: 600;
  color: #303133;
}

.node-level-2 .node-label {
  color: #606266;
}

.node-level-3 .node-label {
  color: #909399;
}

/* 操作按钮样式 */
.add-btn {
  color: #67c23a !important;
}

.delete-btn {
  color: #f56c6c !important;
}

/* 公共部分样式 */
.public-section {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.public-title {
  font-size: 16px;
  font-weight: 600;
  color: #1A53FF;
  margin-bottom: 12px;
  padding: 0;
}

.public-node {
  padding-left: 8px;
}

.node-icon {
  margin-right: 8px;
  color: #909399;
}

/* 展开树形区域按钮 - 内联样式 */
.expand-tree-btn-inline {
  display: flex;
  align-items: center;
  margin-right: 12px;
  background: white;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 4px;
}

/* 右侧内容区域 */
.content-area {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  position: relative;
  gap: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  overflow: hidden;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
  border-radius: 8px 8px 0 0;
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toolbar-right {
  display: flex;
  align-items: center;
}

/* 操作列按钮样式 */
.operation-buttons {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.operation-btn {
  padding: 0;
  font-size: 14px;
  border: none;
  background: transparent;
  color: #1A53FF;
  text-decoration: none;
  transition: all 0.3s;
  cursor: pointer;
}

.operation-btn.edit-btn {
  color: #1A53FF;
}

.operation-btn.delete-btn {
  color: #f56c6c;
}

.operation-btn.view-btn {
  color: #1A53FF;
}

.operation-btn:hover {
  opacity: 0.7;
}

/* 表格容器样式 */
.table-content {
  flex: 1;
  background: white;
  border-radius: 0 0 8px 8px;
  padding: 20px;
  overflow: hidden;
}

/* 树形区域展开时的右侧圆角 */
.content-area:not(.full-width) .toolbar {
  border-radius: 0 8px 0 0;
}

.content-area:not(.full-width) .table-content {
  border-radius: 0 0 8px 0;
}

/* 树形区域折叠时的完整圆角 */
.content-area.full-width .toolbar {
  border-radius: 8px 8px 0 0;
}

.content-area.full-width .table-content {
  border-radius: 0 0 8px 8px;
}

/* 分页容器样式 */
.table-pagination {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}



/* 表格样式调整 */
:deep(.el-table) {
  font-size: 14px;
}

:deep(.el-table th) {
  background-color: #fafafa;
  font-weight: 600;
}

/* 主色调替换 */
:deep(.el-button--primary) {
  background-color: #1A53FF;
  border-color: #1A53FF;
}

:deep(.el-button--primary:hover) {
  background-color: #4A72FF;
  border-color: #4A72FF;
}

:deep(.el-switch.is-checked .el-switch__core) {
  background-color: #1A53FF;
  border-color: #1A53FF;
}

:deep(.el-pagination .el-pagination__sizes .el-select .el-input .el-input__inner:focus),
:deep(.el-pagination .el-pagination__jump .el-input .el-input__inner:focus) {
  border-color: #1A53FF;
}

:deep(.el-pagination .btn-next:hover),
:deep(.el-pagination .btn-prev:hover),
:deep(.el-pagination .el-pager li:hover) {
  color: #1A53FF;
}

:deep(.el-pagination .el-pager li.active) {
  color: #1A53FF;
  font-weight: 700;
}

/* 树形组件样式调整 */
:deep(.el-tree-node__content) {
  height: 36px;
}

:deep(.el-tree-node__label) {
  font-size: 14px;
}

:deep(.el-tree-node__content:hover) {
  background-color: rgba(26, 83, 255, 0.1);
}

:deep(.el-tree--highlight-current .el-tree-node.is-current > .el-tree-node__content) {
  background-color: rgba(26, 83, 255, 0.2);
  color: #1A53FF;
}

/* 对话框样式 */
:deep(.el-dialog) {
  border-radius: 8px;
}

:deep(.el-dialog__header) {
  padding: 20px 24px 16px 24px;
  border-bottom: 1px solid #f0f0f0;
}

:deep(.el-dialog__body) {
  padding: 24px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
}

/* 上级分类显示样式 */
.parent-category {
  color: #606266;
  font-size: 14px;
  line-height: 32px;
}

</style>
