<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
  Created by: ChaoQun Lei
  Updated by: ChaoQun Lei
-->

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
          <!-- level=0 - , to group item -->
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
          <!-- level=1 - whether level=2 whether -->
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
          <!-- level=2 sub - -->
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
import { getTagTree } from '@/api/eventGroupTagManagement'

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
    default: () => [0, 1, 2], // all layer
    validator: (value) => {
      return Array.isArray(value) && value.every(level => [0, 1, 2].includes(level))
    }
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

// data
const selectedValues = ref([])
const loading = ref(false)
const tagTreeData = ref([])

// method already , in in in

// property - Process item data
const tagOptions = computed(() => {
  return transformTagTreeToOptions(tagTreeData.value)
})

// property - value
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

// property - Process in value (Convert to )
const displaySelectedValues = computed(() => {
  return selectedValues.value.map(value => {
    const label = valueToLabelMap.value.get(value)
    return label || value // if , value
  })
})

// modelValue
watch(() => props.modelValue, (newValue) => {
  selectedValues.value = Array.isArray(newValue) ? newValue : []
}, { immediate: true })

// method
const loadTagTree = async () => {
  loading.value = true
  try {
    const response = await getTagTree()

    if ((!response.code || response.code === 200) && response.data) {
      tagTreeData.value = response.data
      console.log('加载标签树数据:', response.data)
    } else {
      tagTreeData.value = []
      ElMessage.error(response.msg || response.message || '加载标签树失败')
    }
  } catch (error) {
    console.error('加载标签树失败:', error)
    tagTreeData.value = []
    ElMessage.error(`加载标签树失败：${error.message || error}`)
  } finally {
    loading.value = false
  }
}

// Process
const handleVisibleChange = (visible) => {
  if (visible) {
    // , CSS
    nextTick(() => {
      addDropdownClass()
    })
  }
}

// CSS
const addDropdownClass = () => {
  setTimeout(() => {
    try {
      const dropdown = document.querySelector('.el-select-dropdown')
      if (dropdown) {
        dropdown.classList.add('tag-selector-dropdown')
        console.log('已添加下拉选项样式类')

        // info: whether correct
        const parentOptions = dropdown.querySelectorAll('.parent-option')
        const childOptions = dropdown.querySelectorAll('.child-option')
        const typeOptions = dropdown.querySelectorAll('.type-option')
        const parentSelectableOptions = dropdown.querySelectorAll('.parent-selectable-option')

        console.log('找到大类选项:', typeOptions.length)
        console.log('找到父级选项:', parentOptions.length)
        console.log('找到可选父级选项:', parentSelectableOptions.length)
        console.log('找到子级选项:', childOptions.length)

        //
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

// Convert data to item
const transformTagTreeToOptions = (treeData) => {
  if (!Array.isArray(treeData)) return []

  return treeData.map(typeNode => {
    // level=0: ( )
    const typeOption = {
      label: typeNode.tagName || typeNode.name,
      value: typeNode.id || typeNode.tagName,
      level: typeNode.level || 0,
      children: []
    }

    // only allowedLevels level=0 , to item
    if (props.allowedLevels.includes(0)) {
      typeOption.children.push({
        label: typeNode.tagName || typeNode.name,
        value: typeNode.id || typeNode.tagName,
        level: 0
      })
    }

    // all sub node, levelinfo, allowedLevels
    const flattenChildren = (nodes, result = []) => {
      if (!Array.isArray(nodes)) return result

      nodes.forEach(node => {
        const nodeLevel = node.level || 1

        // only layer
        if (props.allowedLevels.includes(nodeLevel)) {
          result.push({
            label: node.tagName || node.name,
            value: node.id || node.tagName,
            level: nodeLevel
          })
        }

        // Process sub node ( current node , sub node can need to )
        if (node.children && Array.isArray(node.children)) {
          flattenChildren(node.children, result)
        }
      })

      return result
    }

    if (typeNode.children && Array.isArray(typeNode.children)) {
      // sub node item after , only layer
      const childrenOptions = flattenChildren(typeNode.children)
      typeOption.children.push(...childrenOptions)
    }

    return typeOption
  })
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

// component Load data
onMounted(() => {
  loadTagTree()
})

// method
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

/* to after */
.tag-selector .el-select__tags .el-tag {
  margin-right: 6px !important;
  margin-bottom: 2px !important;
}

.tag-selector .el-select__tags .el-tag.el-tag--info {
  background-color: #f0f9ff !important;
  border-color: #b3d8ff !important;
  color: #409eff !important;
}

/* full - item correct */
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

/* - in also can */
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

/*  */
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
