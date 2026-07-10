<template>
  <div class="tag-selector">
    <el-select
      v-model="selectedValues"
      multiple
      filterable
      clearable
      placeholder="请选择标签"
      style="width: 100%"
      @change="handleChange"
      @clear="handleClear"
      @visible-change="handleVisibleChange"
    >
      <el-option-group
        v-for="typeGroup in tagOptions"
        :key="typeGroup.value"
        :label="typeGroup.label"
      >
        <template v-for="item in typeGroup.children" :key="item.value">
          <!-- level=0 大类标签 - 可选，显示为分组第一项 -->
          <el-option
            v-if="item.level === 0 && props.allowedLevels.includes(0)"
            :label="item.label"
            :value="item.value"
            :disabled="false"
            class="type-option"
            :style="{ padding: '0 !important' }"
          >
            <div :style="{ display: 'block', padding: '6px 12px 6px 30px', color: '#1A53FF', fontSize: '14px', fontWeight: '600', position: 'relative', width: '100%', boxSizing: 'border-box' }">
              <span :style="{ position: 'absolute', left: '18px', top: '50%', color: '#d9d9d9', fontSize: '10px', transform: 'translateY(-50%)', lineHeight: '1' }">—</span>
              {{ item.label }}
            </div>
          </el-option>
          <!-- level=1 父级标签 - 根据是否包含level=2决定是否可选 -->
          <el-option
            v-else-if="item.level === 1 && props.allowedLevels.includes(1)"
            :label="item.label"
            :value="item.value"
            :disabled="props.allowedLevels.includes(2)"
            :class="props.allowedLevels.includes(2) ? 'parent-option' : 'parent-selectable-option'"
            :style="{ padding: '0 !important', cursor: props.allowedLevels.includes(2) ? 'not-allowed !important' : 'pointer !important', backgroundColor: props.allowedLevels.includes(2) ? '#f0f2f5 !important' : '#ffffff !important' }"
          >
            <div :style="{ 
              display: 'block', 
              padding: '2px 12px 2px 35px', 
              color: props.allowedLevels.includes(2) ? '#3F63F3' : '#1A53FF', 
              fontSize: '12px', 
              fontWeight: props.allowedLevels.includes(2) ? '500' : '600', 
              backgroundColor: props.allowedLevels.includes(2) ? '#f0f2f5' : '#ffffff', 
              borderBottom: props.allowedLevels.includes(2) ? '1px solid #e4e7ed' : 'none', 
              cursor: props.allowedLevels.includes(2) ? 'not-allowed' : 'pointer', 
              position: 'relative', 
              width: '100%', 
              boxSizing: 'border-box' 
            }">
              <span :style="{ position: 'absolute', left: '18px', top: '50%', color: '#d9d9d9', fontSize: '10px', transform: 'translateY(-50%)', lineHeight: '1' }">—</span>
              {{ item.label }}
            </div>
          </el-option>
          <!-- level=2 子级标签 - 可选 -->
          <el-option
            v-else-if="item.level === 2 && props.allowedLevels.includes(2)"
            :label="item.label"
            :value="item.value"
            :disabled="false"
            class="child-option"
            :style="{ padding: '0 !important' }"
          >
            <div :style="{ display: 'block', padding: '2px 25px 2px 50px', color: '#333', position: 'relative', width: '100%', boxSizing: 'border-box' }">
              <span :style="{ position: 'absolute', left: '30px', top: '50%', color: '#d9d9d9', fontSize: '10px', transform: 'translateY(-50%)', lineHeight: '1' }">—</span>
              {{ item.label }}
            </div>
          </el-option>
        </template>
      </el-option-group>
    </el-select>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { getTagTree } from '@/api/tagManagement'

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => []
  },
  placeholder: {
    type: String,
    default: '请选择标签'
  },
  disabled: {
    type: Boolean,
    default: false
  },
  allowedLevels: {
    type: Array,
    default: () => [0, 1, 2], // 默认显示所有层级
    validator: (value) => {
      return Array.isArray(value) && value.every(level => [0, 1, 2].includes(level))
    }
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

// 响应式数据
const selectedValues = ref([])
const loading = ref(false)
const tagTreeData = ref([])

// 内联样式方法已移除，现在直接在模板中使用内联样式

// 计算属性 - 处理标签选项数据
const tagOptions = computed(() => {
  return transformTagTreeToOptions(tagTreeData.value)
})

// 计算属性 - 创建值到标签名称的映射
const valueToLabelMap = computed(() => {
  const map = new Map()
  
  const addToMap = (options) => {
    if (!Array.isArray(options)) return
    
    options.forEach(option => {
      map.set(option.value, option.label)
      if (option.children && Array.isArray(option.children)) {
        addToMap(option.children)
      }
    })
  }
  
  addToMap(tagOptions.value)
  return map
})

// 计算属性 - 处理显示的选中值（转换为标签名称）
const displaySelectedValues = computed(() => {
  return selectedValues.value.map(value => {
    const label = valueToLabelMap.value.get(value)
    return label || value // 如果找不到对应的标签名称，则使用原值
  })
})

// 监听 modelValue 变化
watch(() => props.modelValue, (newValue) => {
  selectedValues.value = Array.isArray(newValue) ? newValue : []
}, { immediate: true })

// 方法
const loadTagTree = async () => {
  loading.value = true
  try {
    const response = await getTagTree()
    
    if (response.code === 200 && response.data) {
      tagTreeData.value = response.data
      console.log('加载标签树数据:', response.data)
    } else {
      // 如果API返回失败，使用模拟数据
      tagTreeData.value = getMockTagData()
    }
  } catch (error) {
    console.error('加载标签树失败:', error)
    // 出错时使用模拟数据
    tagTreeData.value = getMockTagData()
  } finally {
    loading.value = false
  }
}

// 处理下拉框显示状态变化
const handleVisibleChange = (visible) => {
  if (visible) {
    // 下拉框打开时，通过添加CSS类来确保样式生效
    nextTick(() => {
      addDropdownClass()
    })
  }
}

// 添加CSS类来确保样式生效
const addDropdownClass = () => {
  setTimeout(() => {
    try {
      const dropdown = document.querySelector('.el-select-dropdown')
      if (dropdown) {
        dropdown.classList.add('tag-selector-dropdown')
        console.log('已添加下拉选项样式类')
        
        // 调试信息：检查样式是否正确应用
        const parentOptions = dropdown.querySelectorAll('.parent-option')
        const childOptions = dropdown.querySelectorAll('.child-option')
        const typeOptions = dropdown.querySelectorAll('.type-option')
        const parentSelectableOptions = dropdown.querySelectorAll('.parent-selectable-option')
        
        console.log('找到大类选项:', typeOptions.length)
        console.log('找到父级选项:', parentOptions.length)
        console.log('找到可选父级选项:', parentSelectableOptions.length)
        console.log('找到子级选项:', childOptions.length)
        
        // 强制应用样式
        typeOptions.forEach((option, index) => {
          const div = option.querySelector('div')
          if (div) {
            div.style.setProperty('padding', '6px 12px 6px 30px', 'important')
            div.style.setProperty('color', '#1A53FF', 'important')
            div.style.setProperty('font-weight', '600', 'important')
            console.log(`大类选项${index}样式已应用`)
          }
          const span = option.querySelector('span')
          if (span) {
            span.style.setProperty('left', '18px', 'important')
          }
        })
        
        parentOptions.forEach((option, index) => {
          const div = option.querySelector('div')
          if (div) {
            div.style.setProperty('padding', '5px 12px 5px 35px', 'important')
            console.log(`父级选项${index}样式已应用`)
          }
          const span = option.querySelector('span')
          if (span) {
            span.style.setProperty('left', '18px', 'important')
          }
        })
        
        parentSelectableOptions.forEach((option, index) => {
          const div = option.querySelector('div')
          if (div) {
            div.style.setProperty('padding', '2px 12px 2px 35px', 'important')
            div.style.setProperty('color', '#1A53FF', 'important')
            div.style.setProperty('font-weight', '600', 'important')
            div.style.setProperty('background-color', '#ffffff', 'important')
            div.style.setProperty('cursor', 'pointer', 'important')
            console.log(`可选父级选项${index}样式已应用`)
          }
          const span = option.querySelector('span')
          if (span) {
            span.style.setProperty('left', '18px', 'important')
          }
        })
        
        childOptions.forEach((option, index) => {
          const div = option.querySelector('div')
          if (div) {
            div.style.setProperty('padding', '5px 25px 5px 50px', 'important')
            console.log(`子级选项${index}样式已应用`)
          }
          const span = option.querySelector('span')
          if (span) {
            span.style.setProperty('left', '30px', 'important')
          }
        })
      }
    } catch (error) {
      console.error('添加样式类失败:', error)
    }
  }, 50)
}

// 转换标签树数据为选项格式
const transformTagTreeToOptions = (treeData) => {
  if (!Array.isArray(treeData)) return []
  
  return treeData.map(typeNode => {
    // level=0: 类型级（大类）
    const typeOption = {
      label: typeNode.tagName || typeNode.name,
      value: typeNode.id || typeNode.tagName,
      level: typeNode.level || 0,
      children: []
    }
    
    // 只有当allowedLevels包含level=0时，才添加大类本身作为可选项
    if (props.allowedLevels.includes(0)) {
      typeOption.children.push({
        label: typeNode.tagName || typeNode.name,
        value: typeNode.id || typeNode.tagName,
        level: 0
      })
    }
    
    // 扁平化所有子节点，保持level信息，并根据allowedLevels过滤
    const flattenChildren = (nodes, result = []) => {
      if (!Array.isArray(nodes)) return result
      
      nodes.forEach(node => {
        const nodeLevel = node.level || 1
        
        // 只添加允许的层级
        if (props.allowedLevels.includes(nodeLevel)) {
          result.push({
            label: node.tagName || node.name,
            value: node.id || node.tagName,
            level: nodeLevel
          })
        }
        
        // 递归处理子节点（即使当前节点不显示，子节点可能需要显示）
        if (node.children && Array.isArray(node.children)) {
          flattenChildren(node.children, result)
        }
      })
      
      return result
    }
    
    if (typeNode.children && Array.isArray(typeNode.children)) {
      // 将子节点添加到大类选项后面，只包含允许的层级
      const childrenOptions = flattenChildren(typeNode.children)
      typeOption.children.push(...childrenOptions)
    }
    
    return typeOption
  })
}

// 获取模拟数据
const getMockTagData = () => {
  return [
    {
      id: 'own',
      tagName: '自有',
      level: 0,
      children: [
        {
          id: 'own_device',
          tagName: '设备类型',
          level: 1,
          children: [
            { id: 'camera', tagName: '摄像头', level: 2 },
            { id: 'ptz', tagName: '云台', level: 2 },
            { id: 'dome', tagName: '球机', level: 2 },
            { id: 'bullet', tagName: '枪机', level: 2 },
            { id: 'dome_mini', tagName: '半球', level: 2 }
          ]
        },
        {
          id: 'own_location',
          tagName: '位置类型',
          level: 1,
          children: [
            { id: 'indoor', tagName: '室内', level: 2 },
            { id: 'outdoor', tagName: '室外', level: 2 },
            { id: 'entrance', tagName: '出入口', level: 2 },
            { id: 'parking', tagName: '停车场', level: 2 }
          ]
        }
      ]
    },
    {
      id: 'public',
      tagName: '公共',
      level: 0,
      children: [
        {
          id: 'public_function',
          tagName: '功能类型',
          level: 1,
          children: [
            { id: 'monitor', tagName: '监控', level: 2 },
            { id: 'alarm', tagName: '报警', level: 2 },
            { id: 'record', tagName: '录像', level: 2 },
            { id: 'analysis', tagName: '分析', level: 2 }
          ]
        },
        {
          id: 'public_brand',
          tagName: '品牌类型',
          level: 1,
          children: [
            { id: 'hikvision', tagName: '海康威视', level: 2 },
            { id: 'dahua', tagName: '大华', level: 2 },
            { id: 'uniview', tagName: '宇视', level: 2 },
            { id: 'other', tagName: '其他', level: 2 }
          ]
        }
      ]
    }
  ]
}

const handleChange = (values) => {
  console.log('TagSelector change:', values)
  selectedValues.value = Array.isArray(values) ? values : []
  emit('update:modelValue', selectedValues.value)
  emit('change', selectedValues.value)
}

const handleClear = () => {
  selectedValues.value = []
  emit('update:modelValue', [])
  emit('change', [])
}

// 组件挂载时加载数据
onMounted(() => {
  loadTagTree()
})

// 暴露方法
defineExpose({
  loadTagTree
})
</script>

<style>
.tag-selector {
  width: 100%;
}

.tag-selector .el-select {
  width: 100%;
}

/* 基础样式作为后备 */
.tag-selector .el-select__tags .el-tag {
  margin-right: 6px !important;
  margin-bottom: 2px !important;
}

.tag-selector .el-select__tags .el-tag.el-tag--info {
  background-color: #f0f9ff !important;
  border-color: #b3d8ff !important;
  color: #409eff !important;
}

/* 全局样式 - 确保下拉选项样式正确 */
:global(.tag-selector-dropdown .type-option) {
  padding: 0 !important;
  background-color: #ffffff !important;
}

:global(.tag-selector-dropdown .type-option:hover) {
  background-color: #f5f7fa !important;
}

:global(.tag-selector-dropdown .type-option div) {
  padding: 6px 12px 6px 30px !important;
  color: #1A53FF !important;
  font-size: 14px !important;
  font-weight: 600 !important;
  position: relative !important;
  width: 100% !important;
  box-sizing: border-box !important;
}

:global(.tag-selector-dropdown .type-option div span) {
  position: absolute !important;
  left: 18px !important;
  top: 50% !important;
  color: #d9d9d9 !important;
  font-size: 10px !important;
  transform: translateY(-50%) !important;
  line-height: 1 !important;
}

:global(.tag-selector-dropdown .type-option:hover div) {
  color: #3d70ff !important;
}

:global(.tag-selector-dropdown .type-option.selected) {
  font-weight: 700 !important;
  background-color: #f0f9ff !important;
}

:global(.tag-selector-dropdown .type-option.selected div) {
  color: #1A53FF !important;
}

:global(.tag-selector-dropdown .parent-option) {
  padding: 0 !important;
  cursor: not-allowed !important;
  background-color: #f0f2f5 !important;
}

:global(.tag-selector-dropdown .parent-option:hover) {
  background-color: #f0f2f5 !important;
}

:global(.tag-selector-dropdown .parent-option div) {
  padding: 5px 12px 5px 35px !important;
  color: #909399 !important;
  background-color: #f0f2f5 !important;
  border-bottom: 1px solid #e4e7ed !important;
  cursor: not-allowed !important;
  position: relative !important;
  width: 100% !important;
  box-sizing: border-box !important;
}

:global(.tag-selector-dropdown .parent-option div span) {
  position: absolute !important;
  left: 18px !important;
  top: 50% !important;
  color: #d9d9d9 !important;
  font-size: 10px !important;
  transform: translateY(-50%) !important;
  line-height: 1 !important;
}

:global(.tag-selector-dropdown .child-option) {
  padding: 0 !important;
}

:global(.tag-selector-dropdown .child-option:hover) {
  background-color: #f5f7fa !important;
}

:global(.tag-selector-dropdown .child-option div) {
  padding: 5px 25px 5px 50px !important;
  color: #333 !important;
  position: relative !important;
  width: 100% !important;
  box-sizing: border-box !important;
}

:global(.tag-selector-dropdown .child-option div span) {
  position: absolute !important;
  left: 30px !important;
  top: 50% !important;
  color: #d9d9d9 !important;
  font-size: 10px !important;
  transform: translateY(-50%) !important;
  line-height: 1 !important;
}

:global(.tag-selector-dropdown .child-option:hover div) {
  color: #409eff !important;
}

:global(.tag-selector-dropdown .child-option.selected) {
  font-weight: 600 !important;
  background-color: #f0f9ff !important;
}

:global(.tag-selector-dropdown .child-option.selected div) {
  color: #409eff !important;
}

/* 备用样式 - 确保在没有添加类的情况下也能正常显示 */
:global(.el-select-dropdown .type-option) {
  padding: 0 !important;
  background-color: #ffffff !important;
}

:global(.el-select-dropdown .type-option:hover) {
  background-color: #f5f7fa !important;
}

:global(.el-select-dropdown .type-option div) {
  padding: 6px 12px 6px 30px !important;
  color: #1A53FF !important;
  font-size: 14px !important;
  font-weight: 600 !important;
  position: relative !important;
  width: 100% !important;
  box-sizing: border-box !important;
}

:global(.el-select-dropdown .type-option div span) {
  position: absolute !important;
  left: 18px !important;
  top: 50% !important;
  color: #d9d9d9 !important;
  font-size: 10px !important;
  transform: translateY(-50%) !important;
  line-height: 1 !important;
}

:global(.el-select-dropdown .parent-option) {
  padding: 0 !important;
  cursor: not-allowed !important;
  background-color: #f0f2f5 !important;
}

:global(.el-select-dropdown .parent-option div) {
  padding: 5px 12px 5px 35px !important;
  color: #909399 !important;
  background-color: #f0f2f5 !important;
  border-bottom: 1px solid #e4e7ed !important;
  cursor: not-allowed !important;
  position: relative !important;
  width: 100% !important;
  box-sizing: border-box !important;
}

:global(.el-select-dropdown .parent-option div span) {
  position: absolute !important;
  left: 18px !important;
  top: 50% !important;
  color: #d9d9d9 !important;
  font-size: 10px !important;
  transform: translateY(-50%) !important;
  line-height: 1 !important;
}

:global(.el-select-dropdown .child-option) {
  padding: 0 !important;
}

:global(.el-select-dropdown .child-option div) {
  padding: 5px 25px 5px 50px !important;
  color: #333 !important;
  position: relative !important;
  width: 100% !important;
  box-sizing: border-box !important;
}

:global(.el-select-dropdown .child-option div span) {
  position: absolute !important;
  left: 30px !important;
  top: 50% !important;
  color: #d9d9d9 !important;
  font-size: 10px !important;
  transform: translateY(-50%) !important;
  line-height: 1 !important;
}

/* 可选的父级标签样式 */
:global(.tag-selector-dropdown .parent-selectable-option) {
  padding: 0 !important;
  background-color: #ffffff !important;
}

:global(.tag-selector-dropdown .parent-selectable-option:hover) {
  background-color: #f5f7fa !important;
}

:global(.tag-selector-dropdown .parent-selectable-option div) {
  padding: 2px 12px 2px 35px !important;
  color: #1A53FF !important;
  font-size: 12px !important;
  font-weight: 600 !important;
  background-color: #ffffff !important;
  cursor: pointer !important;
  position: relative !important;
  width: 100% !important;
  box-sizing: border-box !important;
}

:global(.tag-selector-dropdown .parent-selectable-option div span) {
  position: absolute !important;
  left: 18px !important;
  top: 50% !important;
  color: #d9d9d9 !important;
  font-size: 10px !important;
  transform: translateY(-50%) !important;
  line-height: 1 !important;
}

:global(.tag-selector-dropdown .parent-selectable-option:hover div) {
  color: #3d70ff !important;
}

:global(.tag-selector-dropdown .parent-selectable-option.selected) {
  font-weight: 700 !important;
  background-color: #f0f9ff !important;
}

:global(.tag-selector-dropdown .parent-selectable-option.selected div) {
  color: #1A53FF !important;
}
</style>
