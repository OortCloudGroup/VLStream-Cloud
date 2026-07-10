<template>
  <div class="algorithm-training-platform">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h1>算法训练平台</h1>
        <p>管理AI算法的训练任务和模型发布</p>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="showCreateTrainingDialog = true">
          <el-icon><Plus /></el-icon>
          新建训练任务
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-overview">
      <div class="stat-card">
        <div class="stat-icon training">
          <el-icon><Loading /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-number">{{ trainingStats.running }}</div>
          <div class="stat-label">训练中</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon completed">
          <el-icon><Check /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-number">{{ trainingStats.completed }}</div>
          <div class="stat-label">已完成</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon pending">
          <el-icon><Clock /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-number">{{ trainingStats.pending }}</div>
          <div class="stat-label">等待中</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon models">
          <el-icon><Box /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-number">{{ trainingStats.models }}</div>
          <div class="stat-label">已发布模型</div>
        </div>
      </div>
    </div>

    <!-- 标签页 -->
    <el-tabs v-model="activeTab" @tab-click="handleTabClick">
      <!-- 训练任务 -->
      <el-tab-pane label="训练任务" name="training">
        <div class="training-section">
          <!-- 搜索筛选 -->
          <div class="filter-bar">
            <el-input
              v-model="trainingSearch"
              placeholder="搜索训练任务"
              style="width: 300px"
              clearable
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            
            <el-select v-model="trainingStatusFilter" placeholder="状态" style="width: 150px" clearable>
              <el-option label="全部" value="" />
              <el-option label="训练中" value="training" />
              <el-option label="已完成" value="completed" />
              <el-option label="等待中" value="pending" />
              <el-option label="失败" value="failed" />
            </el-select>
            
            <el-select v-model="algorithmTypeFilter" placeholder="算法类型" style="width: 180px" clearable>
              <el-option label="全部" value="" />
              <el-option label="人脸识别" value="人脸识别" />
              <el-option label="车牌识别" value="车牌识别" />
              <el-option label="行为分析" value="行为分析" />
              <el-option label="物体检测" value="物体检测" />
            </el-select>
          </div>

          <!-- 训练任务列表 -->
          <div class="training-cards">
            <div 
              v-for="task in filteredTrainingTasks" 
              :key="task.id"
              class="training-card"
            >
              <div class="card-header">
                <div class="task-info">
                  <h3>{{ task.name }}</h3>
                  <el-tag :type="getTaskStatusType(task.status)" size="small">
                    {{ getTaskStatusText(task.status) }}
                  </el-tag>
                </div>
                <div class="card-actions">
                  <el-dropdown>
                    <el-icon class="more-icon"><MoreFilled /></el-icon>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item @click="viewTaskDetails(task)">
                          <el-icon><View /></el-icon>查看详情
                        </el-dropdown-item>
                        <el-dropdown-item v-if="task.status === 'training'" @click="stopTraining(task)">
                          <el-icon><VideoPause /></el-icon>停止训练
                        </el-dropdown-item>
                        <el-dropdown-item v-if="task.status === 'completed'" @click="publishModel(task)">
                          <el-icon><Upload /></el-icon>发布模型
                        </el-dropdown-item>
                        <el-dropdown-item @click="deleteTask(task)" divided>
                          <el-icon><Delete /></el-icon>删除任务
                        </el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </div>
              
              <div class="card-content">
                <div class="task-details">
                  <div class="detail-item">
                    <span class="detail-label">算法类型：</span>
                    <span>{{ task.algorithmType }}</span>
                  </div>
                  <div class="detail-item">
                    <span class="detail-label">数据集：</span>
                    <span>{{ task.datasetName }} ({{ task.datasetSize }})</span>
                  </div>
                  <div class="detail-item">
                    <span class="detail-label">训练进度：</span>
                    <div class="progress-info">
                      <el-progress :percentage="task.progress" :show-text="false" :stroke-width="6" />
                      <span class="progress-text">{{ task.progress }}% ({{ task.epoch }}/{{ task.totalEpochs }})</span>
                    </div>
                  </div>
                  <div v-if="task.status === 'training'" class="detail-item">
                    <span class="detail-label">GPU使用率：</span>
                    <span class="gpu-usage">{{ task.gpuUsage }}</span>
                  </div>
                  <div v-if="task.accuracy > 0" class="detail-item">
                    <span class="detail-label">准确率：</span>
                    <span class="accuracy">{{ task.accuracy }}%</span>
                  </div>
                </div>
                
                <div v-if="task.status === 'training'" class="training-metrics">
                  <div class="metric-item">
                    <span class="metric-label">预计剩余时间</span>
                    <span class="metric-value">{{ task.estimatedTime }}</span>
                  </div>
                  <div class="metric-item">
                    <span class="metric-label">开始时间</span>
                    <span class="metric-value">{{ task.startTime }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>
      
      <!-- 模型仓库 -->
      <el-tab-pane label="模型仓库" name="models">
        <div class="models-section">
          <!-- 搜索筛选 -->
          <div class="filter-bar">
            <el-input
              v-model="modelSearch"
              placeholder="搜索模型"
              style="width: 300px"
              clearable
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            
            <el-select v-model="modelStatusFilter" placeholder="状态" style="width: 150px" clearable>
              <el-option label="全部" value="" />
              <el-option label="已发布" value="published" />
              <el-option label="测试中" value="testing" />
              <el-option label="草稿" value="draft" />
            </el-select>
          </div>

          <!-- 模型列表 -->
          <div class="model-table">
            <el-table :data="filteredModels" style="width: 100%">
              <el-table-column prop="name" label="模型名称" width="200" />
              <el-table-column prop="algorithmType" label="算法类型" width="120">
                <template #default="{ row }">
                  <el-tag :type="getAlgorithmTagType(row.algorithmType)" size="small">
                    {{ row.algorithmType }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="version" label="版本" width="100" />
              <el-table-column prop="accuracy" label="准确率" width="100">
                <template #default="{ row }">
                  <span class="accuracy-value">{{ row.accuracy }}%</span>
                </template>
              </el-table-column>
              <el-table-column prop="size" label="模型大小" width="100" />
              <el-table-column prop="format" label="格式" width="100" />
              <el-table-column prop="downloadCount" label="下载量" width="100" />
              <el-table-column prop="deployCount" label="部署量" width="100" />
              <el-table-column prop="status" label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="getModelStatusType(row.status)" size="small">
                    {{ getModelStatusText(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="创建时间" width="160" />
              <el-table-column label="操作" width="200" fixed="right">
                <template #default="{ row }">
                  <el-button size="small" type="primary" text @click="downloadModel(row)">下载</el-button>
                  <el-button size="small" type="success" text @click="deployModel(row)">部署</el-button>
                  <el-button size="small" type="info" text @click="viewModelDetails(row)">详情</el-button>
                  <el-button size="small" type="danger" text @click="deleteModel(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </el-tab-pane>
      
      <!-- 数据标注 -->
      <el-tab-pane label="数据标注" name="annotation">
        <div class="annotation-section">
          <div class="coming-soon">
            <el-icon size="64" color="#c0c4cc"><Tools /></el-icon>
            <h3>数据标注工具</h3>
            <p>该功能正在开发中，敬请期待...</p>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 新建训练任务对话框 -->
    <el-dialog
      v-model="showCreateTrainingDialog"
      title="新建训练任务"
      width="600px"
      @close="resetTrainingForm"
    >
      <el-form :model="trainingForm" :rules="trainingRules" ref="trainingFormRef" label-width="120px">
        <el-form-item label="任务名称" prop="name">
          <el-input v-model="trainingForm.name" placeholder="请输入训练任务名称" />
        </el-form-item>
        
        <el-form-item label="算法类型" prop="algorithmType">
          <el-select v-model="trainingForm.algorithmType" placeholder="请选择算法类型" style="width: 100%">
            <el-option label="人脸识别" value="人脸识别" />
            <el-option label="车牌识别" value="车牌识别" />
            <el-option label="行为分析" value="行为分析" />
            <el-option label="物体检测" value="物体检测" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="训练数据集" prop="datasetId">
          <el-select v-model="trainingForm.datasetId" placeholder="请选择训练数据集" style="width: 100%">
            <el-option label="人脸数据集v1.0 (50000张图片)" value="dataset1" />
            <el-option label="车牌数据集v2.1 (30000张图片)" value="dataset2" />
            <el-option label="行为数据集v1.5 (80000个视频片段)" value="dataset3" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="训练轮次" prop="epochs">
          <el-input-number v-model="trainingForm.epochs" :min="1" :max="1000" style="width: 100%" />
        </el-form-item>
        
        <el-form-item label="学习率" prop="learningRate">
          <el-input-number v-model="trainingForm.learningRate" :min="0.0001" :max="1" :step="0.0001" :precision="4" style="width: 100%" />
        </el-form-item>
        
        <el-form-item label="批处理大小" prop="batchSize">
          <el-input-number v-model="trainingForm.batchSize" :min="1" :max="128" style="width: 100%" />
        </el-form-item>
        
        <el-form-item label="训练描述">
          <el-input
            v-model="trainingForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入训练任务描述"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showCreateTrainingDialog = false">取消</el-button>
        <el-button type="primary" @click="createTrainingTask">开始训练</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Plus, Search, MoreFilled, View, VideoPause, Upload, Delete, 
  Loading, Check, Clock, Box, Tools 
} from '@element-plus/icons-vue'
import { mockTrainingData, mockModelData } from '@/mock/videoData'

// 响应式数据
const activeTab = ref('training')
const trainingSearch = ref('')
const trainingStatusFilter = ref('')
const algorithmTypeFilter = ref('')
const modelSearch = ref('')
const modelStatusFilter = ref('')

// 对话框控制
const showCreateTrainingDialog = ref(false)

// 统计数据
const trainingStats = ref({
  running: 2,
  completed: 15,
  pending: 3,
  models: 8
})

// 训练任务数据
const trainingTasks = ref([])

// 模型数据
const models = ref([])

// 训练表单
const trainingForm = ref({
  name: '',
  algorithmType: '',
  datasetId: '',
  epochs: 100,
  learningRate: 0.001,
  batchSize: 32,
  description: ''
})

// 表单验证规则
const trainingRules = {
  name: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  algorithmType: [{ required: true, message: '请选择算法类型', trigger: 'change' }],
  datasetId: [{ required: true, message: '请选择训练数据集', trigger: 'change' }],
  epochs: [{ required: true, message: '请输入训练轮次', trigger: 'blur' }],
  learningRate: [{ required: true, message: '请输入学习率', trigger: 'blur' }],
  batchSize: [{ required: true, message: '请输入批处理大小', trigger: 'blur' }]
}

// 计算属性
const filteredTrainingTasks = computed(() => {
  let result = trainingTasks.value
  
  if (trainingSearch.value) {
    result = result.filter(task => 
      task.name.toLowerCase().includes(trainingSearch.value.toLowerCase())
    )
  }
  
  if (trainingStatusFilter.value) {
    result = result.filter(task => task.status === trainingStatusFilter.value)
  }
  
  if (algorithmTypeFilter.value) {
    result = result.filter(task => task.algorithmType === algorithmTypeFilter.value)
  }
  
  return result
})

const filteredModels = computed(() => {
  let result = models.value
  
  if (modelSearch.value) {
    result = result.filter(model => 
      model.name.toLowerCase().includes(modelSearch.value.toLowerCase())
    )
  }
  
  if (modelStatusFilter.value) {
    result = result.filter(model => model.status === modelStatusFilter.value)
  }
  
  return result
})

// 方法
const getTaskStatusType = (status) => {
  const typeMap = {
    'training': 'primary',
    'completed': 'success',
    'pending': 'warning',
    'failed': 'danger'
  }
  return typeMap[status] || 'info'
}

const getTaskStatusText = (status) => {
  const textMap = {
    'training': '训练中',
    'completed': '已完成',
    'pending': '等待中',
    'failed': '失败'
  }
  return textMap[status] || status
}

const getAlgorithmTagType = (type) => {
  const typeMap = {
    '人脸识别': 'primary',
    '车牌识别': 'success',
    '行为分析': 'warning',
    '物体检测': 'info'
  }
  return typeMap[type] || 'info'
}

const getModelStatusType = (status) => {
  const typeMap = {
    'published': 'success',
    'testing': 'warning',
    'draft': 'info'
  }
  return typeMap[status] || 'info'
}

const getModelStatusText = (status) => {
  const textMap = {
    'published': '已发布',
    'testing': '测试中',
    'draft': '草稿'
  }
  return textMap[status] || status
}

const handleTabClick = (tab) => {
  console.log('切换标签页:', tab.props.name)
}

const viewTaskDetails = (task) => {
  ElMessage.info(`查看训练任务详情: ${task.name}`)
}

const stopTraining = (task) => {
  ElMessageBox.confirm('确定要停止此训练任务吗？', '确认操作')
    .then(() => {
      task.status = 'stopped'
      ElMessage.success('训练任务已停止')
    })
    .catch(() => {
      ElMessage.info('操作已取消')
    })
}

const publishModel = (task) => {
  ElMessage.info(`发布模型: ${task.name}`)
}

const deleteTask = (task) => {
  ElMessageBox.confirm('确定要删除此训练任务吗？', '确认删除')
    .then(() => {
      const index = trainingTasks.value.findIndex(t => t.id === task.id)
      if (index > -1) {
        trainingTasks.value.splice(index, 1)
        ElMessage.success('删除成功')
      }
    })
    .catch(() => {
      ElMessage.info('已取消删除')
    })
}

const downloadModel = (model) => {
  ElMessage.info(`下载模型: ${model.name}`)
}

const deployModel = (model) => {
  ElMessage.info(`部署模型: ${model.name}`)
}

const viewModelDetails = (model) => {
  ElMessage.info(`查看模型详情: ${model.name}`)
}

const deleteModel = (model) => {
  ElMessageBox.confirm('确定要删除此模型吗？', '确认删除')
    .then(() => {
      const index = models.value.findIndex(m => m.id === model.id)
      if (index > -1) {
        models.value.splice(index, 1)
        ElMessage.success('删除成功')
      }
    })
    .catch(() => {
      ElMessage.info('已取消删除')
    })
}

const resetTrainingForm = () => {
  trainingForm.value = {
    name: '',
    algorithmType: '',
    datasetId: '',
    epochs: 100,
    learningRate: 0.001,
    batchSize: 32,
    description: ''
  }
}

const createTrainingTask = () => {
  const newTask = {
    id: Date.now(),
    name: trainingForm.value.name,
    algorithmType: trainingForm.value.algorithmType,
    datasetName: '选定数据集',
    datasetSize: '数据量',
    status: 'pending',
    progress: 0,
    accuracy: 0,
    epoch: 0,
    totalEpochs: trainingForm.value.epochs,
    startTime: '',
    estimatedTime: '待开始',
    gpuUsage: '0%',
    createTime: new Date().toLocaleString()
  }
  
  trainingTasks.value.unshift(newTask)
  showCreateTrainingDialog.value = false
  resetTrainingForm()
  ElMessage.success('训练任务创建成功')
}

onMounted(() => {
  // 加载mock数据
  trainingTasks.value = mockTrainingData()
  models.value = mockModelData()
})
</script>

<style scoped>
.algorithm-training-platform {
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

.stats-overview {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.stat-card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.stat-icon.training { background: #409eff; }
.stat-icon.completed { background: #67c23a; }
.stat-icon.pending { background: #e6a23c; }
.stat-icon.models { background: #909399; }

.stat-content {
  flex: 1;
}

.stat-number {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 5px;
}

.stat-label {
  color: #909399;
  font-size: 14px;
}

.filter-bar {
  display: flex;
  gap: 15px;
  align-items: center;
  margin-bottom: 20px;
  padding: 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.training-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 20px;
}

.training-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.task-info h3 {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 16px;
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

.task-details {
  margin-bottom: 16px;
}

.detail-item {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.detail-label {
  color: #909399;
  font-size: 14px;
  min-width: 80px;
}

.progress-info {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
}

.progress-text {
  font-size: 12px;
  color: #606266;
}

.gpu-usage {
  color: #409eff;
  font-weight: 500;
}

.accuracy {
  color: #67c23a;
  font-weight: 500;
}

.training-metrics {
  border-top: 1px solid #f0f0f0;
  padding-top: 16px;
  display: flex;
  gap: 20px;
}

.metric-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.metric-label {
  color: #909399;
  font-size: 12px;
}

.metric-value {
  color: #303133;
  font-size: 14px;
  font-weight: 500;
}

.model-table {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.accuracy-value {
  color: #67c23a;
  font-weight: 500;
}

.coming-soon {
  text-align: center;
  padding: 60px 20px;
  color: #909399;
}

.coming-soon h3 {
  margin: 16px 0 8px 0;
  color: #606266;
}

.coming-soon p {
  margin: 0;
}
</style> 