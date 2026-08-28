<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
-->

<template>
  <div class="device-tree-component">
    <!--  -->
    <div v-if="showSearch" class="search-section">
      <SearchInput
        v-model="searchKeyword"
        placeholder="搜索设备"
        button-text="查询"
        size="small"
        width="200px"
        @search="handleSearch"
      />
    </div>

    <!--  -->
    <div class="tree-header">
      <h4>{{ title }}</h4>
      <div class="header-controls">
        <CollapseToggle
          v-if="showCollapseBtn"
          class="collapse-btn"
          :is-expanded="!collapsed"
          @toggle="toggleCollapse"
        />
      </div>

    </div>

    <!-- device -->
    <div class="device-tree">
      <el-tree
        :data="filteredTreeData"
        :props="defaultProps"
        :expand-on-click-node="false"
        @node-click="handleNodeClick"
        default-expand-all
        node-key="id"
      >
        <template #default="{ node, data }">
          <span
            class="custom-tree-node"
            @mouseenter="showAddButton(node)"
            @mouseleave="hideAddButton(node)"
          >
            <!-- device/ -->
            <el-icon v-if="data.type === 'tag'" class="tree-icon tag-icon">
              <Collection />
            </el-icon>
            <el-icon v-else-if="data.type === 'device'" class="tree-icon device-icon">
              <VideoCamera />
            </el-icon>
            <el-icon v-else class="tree-icon">
              <Folder />
            </el-icon>

            <!-- node -->
            <span class="node-label">{{ data.label }}</span>

            <!-- device ( ) -->
            <span v-if="data.type === 'tag'" class="node-count">({{ data.children?.length || 0 }})</span>

            <!-- operationbutton -->
            <div v-if="node.showAdd" class="action-buttons">
              <!-- button -->
              <el-dropdown
                v-if="showAddActions"
                class="add-dropdown"
                @command="handleAddCommand($event, data)"
                trigger="click"
              >
                <el-button
                  size="small"
                  class="add-btn"
                  type="text"
                  @click.stop
                >
                  <el-icon><Plus /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="sibling">新增同级</el-dropdown-item>
                    <el-dropdown-item command="child" v-if="data.type === 'tag'">新增下级</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>

              <!-- Delete button -->
              <el-button
                v-if="showDeleteActions"
                size="small"
                class="delete-btn"
                type="text"
                @click.stop="handleDeleteCommand(data)"
              >
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </span>
        </template>
      </el-tree>
    </div>

    <!-- operationbutton -->
    <div v-if="showBottomActions" class="bottom-actions">
      <div class="action-buttons-container">
        <el-dropdown
          v-if="showAddActions"
          @command="handleBottomAddCommand"
          trigger="click"
          class="add-dropdown-bottom"
        >
          <el-button type="primary" size="small" class="bottom-add-btn">
            <el-icon><Plus /></el-icon>
            新增
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="group">新增分组</el-dropdown-item>
              <el-dropdown-item command="device">新增设备</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <el-button
          v-if="showDeleteActions"
          type="danger"
          size="small"
          class="bottom-delete-btn"
          @click="handleBottomDeleteCommand"
          :disabled="!selectedNode"
        >
          <el-icon><Delete /></el-icon>
          删除
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import {
  Folder,
  VideoCamera,
  Plus,
  Delete,
  Collection
} from '@element-plus/icons-vue'
import CollapseToggle from '@/components/CollapseToggle.vue'
import SearchInput from '@/components/SearchInput.vue'

// Props
const props = defineProps({
  // data
  treeData: {
    type: Array,
    default: () => []
  },
  //
  title: {
    type: String,
    default: '设备树'
  },
  // whether
  showSearch: {
    type: Boolean,
    default: true
  },
  // whether button
  showCollapseBtn: {
    type: Boolean,
    default: false
  },
  // whether operation
  showAddActions: {
    type: Boolean,
    default: true
  },
  // whether Delete operation
  showDeleteActions: {
    type: Boolean,
    default: true
  },
  // whether operationbutton
  showBottomActions: {
    type: Boolean,
    default: false
  },
  // whether
  collapsed: {
    type: Boolean,
    default: false
  }
})

// Emits
const emit = defineEmits([
  'node-click',
  'add-device',
  'delete-device',
  'bottom-add',
  'bottom-delete',
  'search',
  'toggle-collapse'
])

// data
const searchKeyword = ref('')
const selectedNode = ref(null)

// componentproperty
const defaultProps = {
  children: 'children',
  label: 'label'
}

// property - after data
const filteredTreeData = computed(() => {
  if (!searchKeyword.value) {
    return props.treeData
  }

  const filterNode = (nodes) => {
    return nodes.filter(node => {
      if (node.label.toLowerCase().includes(searchKeyword.value.toLowerCase())) {
        return true
      }
      if (node.children) {
        const filteredChildren = filterNode(node.children)
        if (filteredChildren.length > 0) {
          return true
        }
      }
      return false
    }).map(node => {
      if (node.children) {
        return {
          ...node,
          children: filterNode(node.children)
        }
      }
      return node
    })
  }

  return filterNode(props.treeData)
})

// method
const handleSearch = (searchValue) => {
  emit('search', searchValue || searchKeyword.value)
}

const handleNodeClick = (data) => {
  console.log('DeviceTree: 节点点击事件触发', data)
  selectedNode.value = data
  emit('node-click', data)
}

const toggleCollapse = () => {
  emit('toggle-collapse')
}

const showAddButton = (node) => {
  if (props.showAddActions) {
    node.showAdd = true
  }
}

const hideAddButton = (node) => {
  if (props.showAddActions) {
    setTimeout(() => {
      node.showAdd = false
    }, 100)
  }
}

const handleAddCommand = (command, data) => {
  emit('add-device', { command, data })
}

const handleDeleteCommand = (data) => {
  emit('delete-device', data)
}

const handleBottomAddCommand = (command) => {
  emit('bottom-add', { command, selectedNode: selectedNode.value })
}

const handleBottomDeleteCommand = () => {
  if (selectedNode.value) {
    emit('bottom-delete', selectedNode.value)
  }
}

//
watch(searchKeyword, (newVal) => {
  if (newVal === '') {
    emit('search', '')
  }
})
</script>

<style scoped>
.device-tree-component {
  height: 100%;
  display: flex;
  flex-direction: column;
}

/*  */
.search-section {
  padding: 15px;
  border-bottom: 1px solid #e4e7ed;
  background: #fafafa;
}



/*  */
.tree-header {
  padding: 15px;
  border-bottom: 1px solid #e4e7ed;
  background: #fafafa;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.tree-header h4 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.header-controls {
  display: flex;
  align-items: center;
  gap: 8px;
  z-index: 5;
  position: relative;
}

.collapse-btn {
  color: #606266;
  cursor: pointer;
  z-index: 10;
  position: relative;
  pointer-events: auto;
}

/* device */
.device-tree {
  flex: 1;
  padding: 10px;
  overflow-y: auto;
}

.custom-tree-node {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  padding-right: 8px;
}

.tree-icon {
  margin-right: 8px;
  color: #606266;
  font-size: 16px;
}

.device-icon {
  color: #67c23a;
}

.tag-icon {
  color: #409eff;
}



.node-label {
  flex: 1;
  cursor: pointer;
  color: #303133;
}

.node-count {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
}



/* operationbutton */
.action-buttons {
  display: flex;
  align-items: center;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.3s;
  margin-left: 8px;
}

.custom-tree-node:hover .action-buttons {
  opacity: 1;
}

.add-btn {
  padding: 2px !important;
  color: #409eff;
}

.add-btn:hover {
  background-color: #ecf5ff;
}

.delete-btn {
  padding: 2px !important;
  color: #f56c6c;
}

.delete-btn:hover {
  background-color: #fef0f0;
  color: #f56c6c;
}

/* node */
:deep(.el-tree-node__content) {
  height: 32px;
  line-height: 32px;
  padding: 0 8px;
}

:deep(.el-tree-node__content:hover) {
  background-color: #f5f7fa;
}

:deep(.el-tree-node__expand-icon) {
  color: #c0c4cc;
}

:deep(.el-tree-node__expand-icon.expanded) {
  color: #409eff;
}

:deep(.el-tree-node__label) {
  font-size: 14px;
  color: #303133;
}

/*  */
:deep(.el-tree-node__label) {
  position: relative;
}

/* operationbutton */
.bottom-actions {
  padding: 15px;
  border-top: 1px solid #e4e7ed;
  background: #fafafa;
}

.action-buttons-container {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.bottom-add-btn {
  background: #409eff;
  border-color: #409eff;
  color: white;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 4px;
}

.bottom-add-btn:hover {
  background: #66b1ff;
  border-color: #66b1ff;
}

.bottom-delete-btn {
  background: #f56c6c;
  border-color: #f56c6c;
  color: white;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 4px;
}

.bottom-delete-btn:hover:not(:disabled) {
  background: #f78989;
  border-color: #f78989;
}

.bottom-delete-btn:disabled {
  background: #c0c4cc;
  border-color: #c0c4cc;
  color: white;
  cursor: not-allowed;
}
</style>