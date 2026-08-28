<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
-->

<template>
  <div class="search-input-component">
    <div class="search-row">
      <el-input
        v-model="searchValue"
        :placeholder="placeholder"
        clearable
        :size="size"
        class="search-input"
        @keyup.enter="handleSearch"
      />
      <el-button
        type="primary"
        :size="size"
        class="search-btn"
        @click="handleSearch"
      >
        {{ buttonText }}
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

// Props
const props = defineProps({
  // value
  modelValue: {
    type: String,
    default: ''
  },
  //
  placeholder: {
    type: String,
    default: '搜索'
  },
  // button
  buttonText: {
    type: String,
    default: '查询'
  },
  // component
  size: {
    type: String,
    default: 'small',
    validator: (value) => ['large', 'default', 'small'].includes(value)
  },
  //
  width: {
    type: String,
    default: '200px'
  }
})

// Emits
const emit = defineEmits(['update:modelValue', 'search', 'clear'])

// data
const searchValue = ref(props.modelValue)

// method
const handleSearch = () => {
  emit('search', searchValue.value)
  emit('update:modelValue', searchValue.value)
}

const handleClear = () => {
  searchValue.value = ''
  emit('clear')
  emit('update:modelValue', '')
}

// value
watch(() => props.modelValue, (newVal) => {
  searchValue.value = newVal
})

// value
watch(searchValue, (newVal) => {
  emit('update:modelValue', newVal)
  // if null / empty ,
  if (newVal === '') {
    emit('search', '')
  }
})
</script>

<style scoped>
.search-input-component {
  display: flex;
  align-items: center;
}

.search-row {
  display: flex;
  gap: 0;
  align-items: center;
  width: 100%;
}

.search-input {
  width: v-bind(width);
  height: 40px;
}

.search-input :deep(.el-input__wrapper) {
  border-top-right-radius: 0;
  border-bottom-right-radius: 0;
}

.search-btn {
  flex-shrink: 0;
  font-weight: 500;
  height: 40px;
  border-left: none;
  border-top-left-radius: 0;
  border-bottom-left-radius: 0;
}

/*  */
.search-input.el-input--large,
.search-btn.el-button--large {
  height: 48px;
}

.search-input.el-input--default,
.search-btn.el-button--default {
  height: 36px;
}

.search-input.el-input--small,
.search-btn.el-button--small {
  height: 32px;
}
</style>