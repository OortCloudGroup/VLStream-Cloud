<template>
  <el-table :data="filedList">
    <el-table-column prop="name" label="字段" />
    <el-table-column v-if="showReadonly" prop="readonly">
      <template #header>
        <el-checkbox v-model="allReadonly" label="只读" />
      </template>
      <template #default="scope">
        <el-checkbox v-model="scope.row.readonly" @change="changeReadonly(scope.row)" />
      </template>
    </el-table-column>
    <el-table-column v-if="showRequired" prop="required">
      <template #header>
        <el-checkbox v-model="allRequired" label="必填" />
      </template>
      <template #default="scope">
        <el-checkbox v-model="scope.row.required" @change="changeRequired(scope.row)" />
      </template>
    </el-table-column>
    <el-table-column v-if="showHidden" prop="hidden">
      <template #header>
        <el-checkbox v-model="allHidden" label="隐藏" />
      </template>
      <template #default="scope">
        <el-checkbox v-model="scope.row.hidden" @change="changeHidden(scope.row)" />
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup>

import { computed, watchEffect } from 'vue'
import { ElTable, ElCheckbox, ElTableColumn } from 'element-plus'

const props = defineProps({
  filedList: {
    type: Array,
    default: () => []
  },
  showReadonly: {
    type: Boolean,
    default: true
  },
  showRequired: {
    type: Boolean,
    default: true
  },
  showHidden: {
    type: Boolean,
    default: true
  }
})

const allReadonly = computed({
  get() {
    return props.filedList.every((e) => e.readonly)
  },
  set(val) {
    props.filedList.forEach((e) => (e.readonly = val))
    if (val) {
      allHidden.value = false
      allRequired.value = false
    }
  }
})
const allHidden = computed({
  get() {
    return props.filedList.every((e) => e.hidden)
  },
  set(val) {
    props.filedList.forEach((e) => (e.hidden = val))
    if (val) {
      allRequired.value = false
      allReadonly.value = false
    }
  }
})
const allRequired = computed({
  get() {
    return props.filedList.every((e) => e.required)
  },
  set(val) {
    props.filedList.forEach((e) => (e.required = val))
    if (val) {
      allReadonly.value = false
      allHidden.value = false
    }
  }
})

const changeReadonly = (row) => {
  if (row.readonly) {
    row.required = false
    row.hidden = false
  }
}
const changeRequired = (row) => {
  if (row.required) {
    row.readonly = false
    row.hidden = false
  }
}
const changeHidden = (row) => {
  if (row.hidden) {
    row.readonly = false
    row.required = false
  }
}

watchEffect(() => {
  // let formProperties = props.formProperties
  // const fields = props.fields
  // formProperties = fields.value.map((field) => ({
  //   id: field.id,
  //   name: field.label,
  //   readonly: field.readonly || false,
  //   hidden: field.hidden || false,
  //   required: field.required || false
  // }))
  // props.formProperties.forEach((item) => {
  //   const properties = formProperties.find((f) => f.id === item.id)
  //   if (properties) {
  //     item.readonly = properties.readonly
  //     item.hidden = properties.hidden
  //     item.required = properties.required
  //   }
  // })
})

</script>

<style scoped lang="scss">

</style>
