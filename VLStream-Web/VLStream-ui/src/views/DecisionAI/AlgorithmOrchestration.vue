<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
  Created by: ChaoQun Lei
  Updated by: ChaoQun Lei
-->

<template>
  <div class="algorithm-orchestration">
    <!-- page -->
    <div class="page-header">
      <div class="header-left">
        <h1>{{ $tp('算法编排') }}</h1>
        <p>{{ $tp('配置和管理AI算法的组合编排') }}</p>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="createOrchestration">
          <el-icon><Plus /></el-icon> {{ $tp('新建编排') }} </el-button>
      </div>
    </div>

    <!--  -->
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
                    <el-icon><Edit /></el-icon>{{ $tp('编辑') }} </el-dropdown-item>
                  <el-dropdown-item @click="duplicateOrchestration(orchestration)">
                    <el-icon><CopyDocument /></el-icon>{{ $tp('复制') }} </el-dropdown-item>
                  <el-dropdown-item @click="deleteOrchestration(orchestration)" divided>
                    <el-icon><Delete /></el-icon>{{ $tp('删除') }} </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>

        <div class="card-content">
          <!-- algorithmworkflow -->
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

          <!-- info -->
          <div class="card-stats">
            <div class="stat-item">
              <span class="stat-label">{{ $tp('算法数量') }}</span>
              <span class="stat-value">{{ orchestration.algorithmCount }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">{{ $tp('关联设备') }}</span>
              <span class="stat-value">{{ orchestration.deviceCount }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">{{ $tp('运行次数') }}</span>
              <span class="stat-value">{{ orchestration.runCount }}</span>
            </div>
          </div>
        </div>

        <div class="card-footer">
          <div class="orchestration-status">
            <el-tag :type="orchestration.status === 'active' ? 'success' : 'info'" size="small">
              {{ orchestration.status === 'active' ? $tp('运行中') : $tp('已停止') }}
            </el-tag>
          </div>
          <div class="orchestration-time">
            <span>{{ $tp('更新时间：') }}{{ orchestration.updateTime }}</span>
          </div>
        </div>
      </div>

      <!-- new -->
      <div class="orchestration-card create-card" @click="createOrchestration">
        <div class="create-content">
          <el-icon size="48" color="#c0c4cc"><Plus /></el-icon>
          <h3>{{ $tp('新建算法编排') }}</h3>
          <p>{{ $tp('创建新的算法组合流程') }}</p>
        </div>
      </div>
    </div>

    <!--  -->
    <el-dialog
      v-model="showEditor"
      :title="$tp('算法编排编辑器')"
      width="90%"
      :before-close="handleEditorClose"
      class="orchestration-editor-dialog"
    >
      <div class="editor-container">
        <!-- algorithm -->
        <div class="algorithm-library">
          <h4>{{ $tp('算法库') }}</h4>
          <div class="library-search">
            <el-input
              v-model="algorithmSearch"
              :placeholder="$tp('搜索算法')"
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

        <!-- in -->
        <div class="orchestration-canvas">
          <div class="canvas-header">
            <h4>{{ $tp('编排画布') }}</h4>
            <div class="canvas-tools">
              <el-button size="small" @click="clearCanvas">{{ $tp('清空') }}</el-button>
              <el-button size="small" type="primary" @click="saveOrchestration">{{ $tp('保存') }}</el-button>
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
              <p>{{ $tp('从左侧拖拽算法到此处开始编排') }}</p>
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
                    <el-button size="small" @click="configureStep(step, index)">{{ $tp('配置参数') }}</el-button>
                  </div>
                </div>
                <div v-if="index < currentSteps.length - 1" class="canvas-arrow">
                  <el-icon><ArrowRight /></el-icon>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- property -->
        <div class="property-panel">
          <h4>{{ $tp('编排配置') }}</h4>
          <el-form :model="orchestrationForm" label-width="80px" size="small">
            <el-form-item :label="$tp('编排名称')">
              <el-input v-model="orchestrationForm.name" :placeholder="$tp('请输入编排名称')" />
            </el-form-item>
            <el-form-item :label="$tp('描述')">
              <el-input
                v-model="orchestrationForm.description"
                type="textarea"
                :rows="3"
                :placeholder="$tp('请输入编排描述')"
              />
            </el-form-item>
            <el-form-item :label="$tp('触发条件')">
              <el-select v-model="orchestrationForm.trigger" :placeholder="$tp('选择触发条件')">
                <el-option :label="$tp('实时触发')" value="realtime" />
                <el-option :label="$tp('定时触发')" value="scheduled" />
                <el-option :label="$tp('事件触发')" value="event" />
              </el-select>
            </el-form-item>
            <el-form-item :label="$tp('执行模式')">
              <el-radio-group v-model="orchestrationForm.mode">
                <el-radio label="serial">{{ $tp('串行') }}</el-radio>
                <el-radio label="parallel">{{ $tp('并行') }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { translatePhrase as uiText } from '@/i18n'

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
import { getAlgorithmPage } from '@/api/algorithmManagement'
import { getOrchestrationPage, saveOrchestrationRecord, updateOrchestrationRecord, removeOrchestrationRecord } from '@/api/algorithmOrchestration'

// data
const showEditor = ref(false)
const algorithmSearch = ref('')
const currentSteps = ref([])
const editingOrchestration = ref(null)

// formdata
const orchestrationForm = ref({
  name: '',
  description: '',
  trigger: 'realtime',
  mode: 'serial'
})

// data
const orchestrations = ref([])

// algorithm data
const algorithms = ref([])

// property
const filteredAlgorithms = computed(() => {
  if (!algorithmSearch.value) return algorithms.value
  return algorithms.value.filter(alg =>
    alg.name.toLowerCase().includes(algorithmSearch.value.toLowerCase())
  )
})

// method
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

const duplicateOrchestration = async (orchestration) => {
  try {
    await saveOrchestrationRecord({
      orchestrationName: `${orchestration.name} (副本)`,
      orchestrationDesc: orchestration.description,
      triggerType: orchestration.trigger,
      executeMode: orchestration.mode,
      algorithmSteps: JSON.stringify(orchestration.steps || []),
      orchestrationStatus: 'inactive'
    })
    await loadOrchestrations()
    ElMessage.success(uiText('编排副本已真实入库'))
  } catch (error) {
    ElMessage.error(uiText('编排复制失败：{value0}', { value0: error.message || error }))
  }
}

const deleteOrchestration = (orchestration) => {
  ElMessageBox.confirm(
    uiText('确定要删除编排"{value0}"吗？', { value0: orchestration.name }),
    uiText('确认删除'),
    {
      confirmButtonText: uiText('确定'),
      cancelButtonText: uiText('取消'),
      type: 'warning'
    }
  ).then(async () => {
    await removeOrchestrationRecord(orchestration.id)
    await loadOrchestrations()
    ElMessage.success(uiText('删除成功'))
  }).catch((error) => {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(uiText('删除编排失败：{value0}', { value0: error.message || error }))
  })
}

const handleDragStart = (algorithm) => {
  // algorithminfo
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
  ElMessage.error(uiText('步骤参数编辑器尚未接入，未修改“{value0}”', { value0: step.name }))
}

const clearCanvas = () => {
  currentSteps.value = []
}

const saveOrchestration = async () => {
  if (!orchestrationForm.value.name) {
    ElMessage.warning(uiText('请输入编排名称'))
    return
  }

  if (currentSteps.value.length === 0) {
    ElMessage.warning(uiText('请至少添加一个算法步骤'))
    return
  }

  const orchestrationData = {
    id: editingOrchestration.value?.id,
    orchestrationName: orchestrationForm.value.name,
    orchestrationDesc: orchestrationForm.value.description,
    triggerType: orchestrationForm.value.trigger,
    executeMode: orchestrationForm.value.mode,
    algorithmSteps: JSON.stringify(currentSteps.value),
    deviceCount: 0,
    runCount: 0,
    orchestrationStatus: editingOrchestration.value?.status || 'inactive'
  }
  try {
    if (editingOrchestration.value) await updateOrchestrationRecord(orchestrationData)
    else await saveOrchestrationRecord(orchestrationData)
    await loadOrchestrations()
    showEditor.value = false
    ElMessage.success(editingOrchestration.value ? uiText('编排更新成功') : uiText('编排创建成功'))
  } catch (error) {
    ElMessage.error(uiText('保存编排失败：{value0}', { value0: error.message || error }))
  }
}

const handleEditorClose = (done) => {
  ElMessageBox.confirm(uiText('确定要关闭编辑器吗？未保存的更改将丢失。'))
    .then(() => {
      done()
    })
    .catch(() => {
      //
    })
}

const loadOrchestrations = async () => {
  try {
    const response = await getOrchestrationPage({ current: 1, size: 200 })
    if (response?.code !== 200) throw new Error(response?.msg || response?.message || '加载失败')
    orchestrations.value = (response?.data?.records || []).map(item => {
      let steps = []
      try { steps = JSON.parse(item.algorithmSteps || '[]') } catch (error) { steps = [] }
      return {
        ...item,
        name: item.orchestrationName,
        description: item.orchestrationDesc,
        trigger: item.triggerType,
        mode: item.executeMode,
        steps,
        algorithmCount: steps.length,
        status: item.orchestrationStatus,
        updateTime: item.updateTime
      }
    })
  } catch (error) {
    orchestrations.value = []
    ElMessage.error(uiText('加载编排失败：{value0}', { value0: error.message || error }))
  }
}

const loadAlgorithms = async () => {
  try {
    const response = await getAlgorithmPage({ current: 1, size: 500 })
    algorithms.value = (response?.data?.records || []).map(item => ({ id: item.id, name: item.name, type: item.categoryName || item.category }))
  } catch (error) {
    algorithms.value = []
    ElMessage.error(uiText('加载算法列表失败：{value0}', { value0: error.message || error }))
  }
}

onMounted(() => Promise.all([loadOrchestrations(), loadAlgorithms()]))
</script>

<style scoped>
.algorithm-orchestration {
  height: 100%;
  margin: 0;
  padding: 20px;
  background: #fff;
  border-radius: var(--common-border-radius) var(--common-border-radius) 0 0;
  overflow: auto;
  box-sizing: border-box;
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

/*  */
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
