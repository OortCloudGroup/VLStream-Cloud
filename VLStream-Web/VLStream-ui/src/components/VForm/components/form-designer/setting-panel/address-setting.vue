<template>
  <div style="width: 100%;">
    <el-form-item style="width: 100%;" label="选择模式">
      <el-select v-model="selectMod" placeholder="请选择模式" @change="updateData">
        <el-option
          v-for="item in selectModOptions"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>
    </el-form-item>
    <el-form-item label="选择范围">
      <el-select v-model="selectScope" placeholder="请选择范围" @change="updateData">
        <el-option
          v-for="item in selectScopeOptions"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>
    </el-form-item>
    <el-form-item v-if="selectScope === 3" label="自定义人员或部门" style="flex-direction: column;">
      <address-setting-selectScope v-model="scopeValue" :select-mod="selectMod" :is-multi="multiple" @change="updateData" />
    </el-form-item>
    <el-form-item label="默认值类型">
      <el-select v-model="defaultValueType" placeholder="请选择默认值类型" @change="updateData">
        <el-option
          v-for="item in defaultValueTypeOptions"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>
    </el-form-item>
    <el-form-item v-if="defaultValueType === 2" label="选择固定值" style="flex-direction: column;">
      <address-setting-selectScope v-model="defaultValue" :select-mod="selectMod" :is-multi="false" @change="updateData" />
    </el-form-item>
  </div>
</template>

<script setup lang="ts">
import { ElSelect, ElOption, ElFormItem } from 'element-plus'
import { ref, onMounted } from 'vue'
import AddressSettingSelectScope from './address-setting-selectScope.vue'

const props = defineProps({
  modelValue: Object,
  multiple: Boolean
})

const selectMod = ref(props.modelValue?.selectMod || 1)
const selectModOptions = ref([
  { label: '选择用户和组织', value: 1 },
  { label: '只选择组织', value: 2 },
  { label: '只选择用户', value: 3 }
])

const selectScope = ref(props.modelValue?.selectScope || 1)
const selectScopeOptions = ref([
  { label: '全部', value: 1 },
  { label: '本部门', value: 2 },
  { label: '自定义', value: 3 }
])

const defaultValueType = ref(props.modelValue?.defaultValueType || 1)
// 1，无， 2 固定值， 3 提交人， 4 提交部门， 5 创建人， 6 创建人部门, 7拥有者 ，8 修改者
const defaultValueTypeOptions = ref([
  { label: '无', value: 1 },
  { label: '固定值', value: 2 },
  { label: '提交人', value: 3 },
  { label: '提交部门', value: 4 },
  { label: '创建人', value: 5 },
  { label: '创建人部门', value: 6 },
  { label: '拥有者', value: 7 },
  { label: '修改者', value: 8 }
])

const scopeValue = ref(props.modelValue?.scopeValue || {})
const defaultValue = ref(props.modelValue?.defaultValue || {})

const emits = defineEmits(['update:modelValue'])
const updateData = () => {
  emits('update:modelValue', {
    selectMod: selectMod.value,
    selectScope: selectScope.value,
    defaultValueType: defaultValueType.value,
    scopeValue: scopeValue.value,
    defaultValue: defaultValue.value
  })
}

onMounted(() => {
})

</script>

  <style scoped>

    .item_width {
      width: 180px;
    }

  </style>
