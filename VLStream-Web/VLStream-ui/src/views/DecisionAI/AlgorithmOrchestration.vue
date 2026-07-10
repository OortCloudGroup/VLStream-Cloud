<template>
  <div class="algorithm-orchestration">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h1>算法编排</h1>
        <p>配置和管理AI算法的组合编排</p>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="createOrchestration">
          <el-icon><Plus /></el-icon>
          新建编排
        </el-button>
      </div>
    </div>

    <!-- 编排列表 -->
    <div class="orchestration-cards">
      <div 
        v-for="orchestration in orchestrations" 
        :key="orchestration.id"
        class="orchestration-card"
        @click="editOrchestration(orchestration)"
      >
        <div class="card-header">
          <div class="orchestration-info">
            <h3>{{ orchestration.name }}</h3>
            <p>{{ orchestration.description }}</p>
          </div>
          <div class="card-actions" @click.stop>
            <el-dropdown>
              <el-icon class="more-icon"><MoreFilled /></el-icon>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="editOrchestration(orchestration)">
                    <el-icon><Edit /></el-icon>编辑
                  </el-dropdown-item>
                  <el-dropdown-item @click="duplicateOrchestration(orchestration)">
                    <el-icon><CopyDocument /></el-icon>复制
                  </el-dropdown-item>
                  <el-dropdown-item @click="deleteOrchestration(orchestration)" divided>
                    <el-icon><Delete /></el-icon>删除
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
        
        <div class="card-content">
          <!-- 算法流程图 -->
          <div class="flow-diagram">
            <div 
              v-for="(step, index) in orchestration.steps" 
              :key="index" 
              class="flow-step"
            >
              <div class="step-node" :class="getStepClass(step.type)">
                <el-icon><component :is="getStepIcon(step.type)" /></el-icon>
                <span class="step-name">{{ step.name }}</span>
              </div>
              <div v-if="index < orchestration.steps.length - 1" class="flow-arrow">
                <el-icon><ArrowRight /></el-icon>
              </div>
            </div>
          </div>
          
          <!-- 统计信息 -->
          <div class="card-stats">
            <div class="stat-item">
              <span class="stat-label">算法数量</span>
              <span class="stat-value">{{ orchestration.algorithmCount }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">关联设备</span>
              <span class="stat-value">{{ orchestration.deviceCount }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">运行次数</span>
              <span class="stat-value">{{ orchestration.runCount }}</span>
            </div>
          </div>
        </div>
        
        <div class="card-footer">
          <div class="orchestration-status">
            <el-tag :type="orchestration.status === 'active' ? 'success' : 'info'" size="small">
              {{ orchestration.status === 'active' ? '运行中' : '已停止' }}
            </el-tag>
          </div>
          <div class="orchestration-time">
            <span>更新时间：{{ orchestration.updateTime }}</span>
          </div>
        </div>
      </div>
      
      <!-- 新建编排卡片 -->
      <div class="orchestration-card create-card" @click="createOrchestration">
        <div class="create-content">
          <el-icon size="48" color="#c0c4cc"><Plus /></el-icon>
          <h3>新建算法编排</h3>
          <p>创建新的算法组合流程</p>
        </div>
      </div>
    </div>

    <!-- 编排编辑器对话框 -->
    <el-dialog
      v-model="showEditor"
      title="算法编排编辑器"
      width="90%"
      :before-close="handleEditorClose"
      class="orchestration-editor-dialog"
    >
      <div class="editor-container">
        <!-- 左侧算法库 -->
        <div class="algorithm-library">
          <h4>算法库</h4>
          <div class="library-search">
            <el-input
              v-model="algorithmSearch"
              placeholder="搜索算法"
              size="small"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </div>
          <div class="algorithm-list">
            <div 
              v-for="algorithm in filteredAlgorithms" 
              :key="algorithm.id"
              class="algorithm-item"
              draggable="true"
              @dragstart="handleDragStart(algorithm)"
            >
              <div class="algorithm-icon" :class="getAlgorithmTypeClass(algorithm.type)">
                <el-icon><component :is="getAlgorithmIcon(algorithm.type)" /></el-icon>
              </div>
              <div class="algorithm-info">
                <div class="algorithm-name">{{ algorithm.name }}</div>
                <div class="algorithm-type">{{ algorithm.type }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 中间编排画布 -->
        <div class="orchestration-canvas">
          <div class="canvas-header">
            <h4>编排画布</h4>
            <div class="canvas-tools">
              <el-button size="small" @click="clearCanvas">清空</el-button>
              <el-button size="small" type="primary" @click="saveOrchestration">保存</el-button>
            </div>
          </div>
          <div 
            class="canvas-area"
            @drop="handleDrop"
            @dragover.prevent
            @dragenter.prevent
          >
            <div v-if="currentSteps.length === 0" class="empty-canvas">
              <el-icon size="64" color="#c0c4cc"><Connection /></el-icon>
              <p>从左侧拖拽算法到此处开始编排</p>
            </div>
            
            <div v-else class="canvas-flow">
              <div 
                v-for="(step, index) in currentSteps" 
                :key="step.id || index"
                class="canvas-step"
              >
                <div class="step-container">
                  <div class="step-node" :class="getStepClass(step.type)">
                    <el-icon><component :is="getStepIcon(step.type)" /></el-icon>
                    <span class="step-name">{{ step.name }}</span>
                    <el-button 
                      class="remove-step" 
                      size="small" 
                      type="danger" 
                      circle
                      @click="removeStep(index)"
                    >
                      <el-icon><Close /></el-icon>
                    </el-button>
                  </div>
                  <div class="step-config">
                    <el-button size="small" @click="configureStep(step, index)">配置参数</el-button>
                  </div>
                </div>
                <div v-if="index < currentSteps.length - 1" class="canvas-arrow">
                  <el-icon><ArrowRight /></el-icon>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧属性面板 -->
        <div class="property-panel">
          <h4>编排配置</h4>
          <el-form :model="orchestrationForm" label-width="80px" size="small">
            <el-form-item label="编排名称">
              <el-input v-model="orchestrationForm.name" placeholder="请输入编排名称" />
            </el-form-item>
            <el-form-item label="描述">
              <el-input 
                v-model="orchestrationForm.description" 
                type="textarea" 
                :rows="3"
                placeholder="请输入编排描述" 
              />
            </el-form-item>
            <el-form-item label="触发条件">
              <el-select v-model="orchestrationForm.trigger" placeholder="选择触发条件">
                <el-option label="实时触发" value="realtime" />
                <el-option label="定时触发" value="scheduled" />
                <el-option label="事件触发" value="event" />
              </el-select>
            </el-form-item>
            <el-form-item label="执行模式">
              <el-radio-group v-model="orchestrationForm.mode">
                <el-radio label="serial">串行</el-radio>
                <el-radio label="parallel">并行</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  MoreFilled,
  Edit,
  CopyDocument,
  Delete,
  ArrowRight,
  Search,
  Connection,
  Close
} from '@element-plus/icons-vue'

// 响应式数据
const showEditor = ref(false)
const algorithmSearch = ref('')
const currentSteps = ref([])
const editingOrchestration = ref(null)

// 编排表单数据
const orchestrationForm = ref({
  name: '',
  description: '',
  trigger: 'realtime',
  mode: 'serial'
})

// 编排数据
const orchestrations = ref([
  {
    id: 1,
    name: '人员行为检测编排',
    description: '检测人员异常行为的完整算法链',
    steps: [
      { name: '人脸检测', type: '人脸识别' },
      { name: '人员追踪', type: '目标追踪' },
      { name: '行为分析', type: '行为分析' }
    ],
    algorithmCount: 3,
    deviceCount: 8,
    runCount: 1247,
    status: 'active',
    updateTime: '2024-01-15 14:30:00'
  },
  {
    id: 2,
    name: '车辆违章检测',
    description: '检测车辆违停、逆行等违章行为',
    steps: [
      { name: '车辆检测', type: '目标检测' },
      { name: '车牌识别', type: '车牌识别' },
      { name: '违章判断', type: '行为分析' }
    ],
    algorithmCount: 3,
    deviceCount: 12,
    runCount: 856,
    status: 'active',
    updateTime: '2024-01-14 16:20:00'
  },
  {
    id: 3,
    name: '周界入侵检测',
    description: '检测周界围栏入侵行为',
    steps: [
      { name: '移动检测', type: '目标检测' },
      { name: '区域判断', type: '行为分析' }
    ],
    algorithmCount: 2,
    deviceCount: 6,
    runCount: 423,
    status: 'inactive',
    updateTime: '2024-01-12 10:15:00'
  }
])

// 算法库数据
const algorithms = ref([
  { id: 1, name: '人脸检测', type: '人脸识别' },
  { id: 2, name: '人脸识别', type: '人脸识别' },
  { id: 3, name: '车辆检测', type: '目标检测' },
  { id: 4, name: '车牌识别', type: '车牌识别' },
  { id: 5, name: '行为分析', type: '行为分析' },
  { id: 6, name: '人员追踪', type: '目标追踪' },
  { id: 7, name: '物体检测', type: '目标检测' },
  { id: 8, name: '移动检测', type: '目标检测' }
])

// 计算属性
const filteredAlgorithms = computed(() => {
  if (!algorithmSearch.value) return algorithms.value
  return algorithms.value.filter(alg => 
    alg.name.toLowerCase().includes(algorithmSearch.value.toLowerCase())
  )
})

// 方法
const getStepClass = (type) => {
  const classMap = {
    '人脸识别': 'face-detection',
    '目标检测': 'object-detection',
    '车牌识别': 'license-plate',
    '行为分析': 'behavior-analysis',
    '目标追踪': 'object-tracking'
  }
  return classMap[type] || 'default'
}

const getStepIcon = (type) => {
  const iconMap = {
    '人脸识别': 'User',
    '目标检测': 'Camera',
    '车牌识别': 'Car',
    '行为分析': 'Warning',
    '目标追踪': 'Connection'
  }
  return iconMap[type] || 'Camera'
}

const getAlgorithmTypeClass = (type) => {
  return getStepClass(type)
}

const getAlgorithmIcon = (type) => {
  return getStepIcon(type)
}

const createOrchestration = () => {
  editingOrchestration.value = null
  currentSteps.value = []
  orchestrationForm.value = {
    name: '',
    description: '',
    trigger: 'realtime',
    mode: 'serial'
  }
  showEditor.value = true
}

const editOrchestration = (orchestration) => {
  editingOrchestration.value = orchestration
  currentSteps.value = [...orchestration.steps]
  orchestrationForm.value = {
    name: orchestration.name,
    description: orchestration.description,
    trigger: 'realtime',
    mode: 'serial'
  }
  showEditor.value = true
}

const duplicateOrchestration = (orchestration) => {
  const newOrchestration = {
    ...orchestration,
    id: Date.now(),
    name: orchestration.name + ' (副本)',
    updateTime: new Date().toLocaleString()
  }
  orchestrations.value.push(newOrchestration)
  ElMessage.success('编排复制成功')
}

const deleteOrchestration = (orchestration) => {
  ElMessageBox.confirm(
    `确定要删除编排"${orchestration.name}"吗？`,
    '确认删除',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    const index = orchestrations.value.findIndex(o => o.id === orchestration.id)
    if (index > -1) {
      orchestrations.value.splice(index, 1)
      ElMessage.success('删除成功')
    }
  }).catch(() => {
    ElMessage.info('已取消删除')
  })
}

const handleDragStart = (algorithm) => {
  // 存储拖拽的算法信息
  window.draggedAlgorithm = algorithm
}

const handleDrop = (event) => {
  event.preventDefault()
  const algorithm = window.draggedAlgorithm
  if (algorithm) {
    currentSteps.value.push({
      id: Date.now(),
      name: algorithm.name,
      type: algorithm.type
    })
    window.draggedAlgorithm = null
  }
}

const removeStep = (index) => {
  currentSteps.value.splice(index, 1)
}

const configureStep = (step, index) => {
  ElMessage.info(`配置步骤: ${step.name}`)
  // 这里应该打开参数配置对话框
}

const clearCanvas = () => {
  currentSteps.value = []
}

const saveOrchestration = () => {
  if (!orchestrationForm.value.name) {
    ElMessage.warning('请输入编排名称')
    return
  }
  
  if (currentSteps.value.length === 0) {
    ElMessage.warning('请至少添加一个算法步骤')
    return
  }

  const orchestrationData = {
    ...orchestrationForm.value,
    steps: currentSteps.value,
    algorithmCount: currentSteps.value.length,
    deviceCount: 0,
    runCount: 0,
    status: 'inactive',
    updateTime: new Date().toLocaleString()
  }

  if (editingOrchestration.value) {
    // 编辑模式
    const index = orchestrations.value.findIndex(o => o.id === editingOrchestration.value.id)
    if (index > -1) {
      orchestrations.value[index] = {
        ...orchestrations.value[index],
        ...orchestrationData
      }
      ElMessage.success('编排更新成功')
    }
  } else {
    // 新增模式
    orchestrations.value.push({
      id: Date.now(),
      ...orchestrationData
    })
    ElMessage.success('编排创建成功')
  }

  showEditor.value = false
}

const handleEditorClose = (done) => {
  ElMessageBox.confirm('确定要关闭编辑器吗？未保存的更改将丢失。')
    .then(() => {
      done()
    })
    .catch(() => {
      // 取消关闭
    })
}

onMounted(() => {
  // 页面初始化
})
</script>

<style scoped>
.algorithm-orchestration {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.header-left h1 {
  margin: 0 0 5px 0;
  color: #303133;
  font-size: 24px;
}

.header-left p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.orchestration-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 20px;
}

.orchestration-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: all 0.3s;
}

.orchestration-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.orchestration-info h3 {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 16px;
}

.orchestration-info p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.more-icon {
  color: #c0c4cc;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
}

.more-icon:hover {
  color: #409eff;
  background: #f0f9ff;
}

.flow-diagram {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  overflow-x: auto;
}

.flow-step {
  display: flex;
  align-items: center;
  gap: 8px;
}

.step-node {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 8px;
  border-radius: 6px;
  min-width: 60px;
  color: white;
}

.step-node.face-detection { background: #409eff; }
.step-node.object-detection { background: #67c23a; }
.step-node.license-plate { background: #e6a23c; }
.step-node.behavior-analysis { background: #f56c6c; }
.step-node.object-tracking { background: #909399; }
.step-node.default { background: #c0c4cc; }

.step-name {
  font-size: 12px;
  text-align: center;
}

.flow-arrow {
  color: #c0c4cc;
}

.card-stats {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-label {
  font-size: 12px;
  color: #909399;
}

.stat-value {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #909399;
}

.create-card {
  border: 2px dashed #c0c4cc;
  display: flex;
  align-items: center;
  justify-content: center;
}

.create-content {
  text-align: center;
}

.create-content h3 {
  margin: 16px 0 8px 0;
  color: #606266;
}

.create-content p {
  margin: 0;
  color: #909399;
}

/* 编辑器样式 */
.orchestration-editor-dialog {
  position: relative;
}

.editor-container {
  display: flex;
  height: 600px;
  gap: 16px;
}

.algorithm-library {
  width: 250px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 16px;
  background: #fafafa;
}

.algorithm-library h4 {
  margin: 0 0 16px 0;
  color: #303133;
}

.library-search {
  margin-bottom: 16px;
}

.algorithm-list {
  max-height: 400px;
  overflow-y: auto;
}

.algorithm-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border-radius: 4px;
  cursor: grab;
  margin-bottom: 8px;
  background: white;
  border: 1px solid #e4e7ed;
}

.algorithm-item:hover {
  background: #f0f9ff;
  border-color: #409eff;
}

.algorithm-icon {
  width: 32px;
  height: 32px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 14px;
}

.algorithm-info {
  flex: 1;
}

.algorithm-name {
  font-size: 14px;
  color: #303133;
  margin-bottom: 2px;
}

.algorithm-type {
  font-size: 12px;
  color: #909399;
}

.orchestration-canvas {
  flex: 1;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 16px;
  background: white;
}

.canvas-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.canvas-header h4 {
  margin: 0;
  color: #303133;
}

.canvas-area {
  height: 400px;
  border: 2px dashed #e4e7ed;
  border-radius: 6px;
  padding: 20px;
  background: #fafbfc;
}

.empty-canvas {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
}

.canvas-flow {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.canvas-step {
  display: flex;
  align-items: center;
  gap: 16px;
}

.step-container {
  position: relative;
}

.remove-step {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 20px;
  height: 20px;
  font-size: 12px;
}

.step-config {
  margin-top: 8px;
  text-align: center;
}

.canvas-arrow {
  color: #409eff;
  font-size: 20px;
}

.property-panel {
  width: 250px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 16px;
  background: #fafafa;
}

.property-panel h4 {
  margin: 0 0 16px 0;
  color: #303133;
}
</style> 