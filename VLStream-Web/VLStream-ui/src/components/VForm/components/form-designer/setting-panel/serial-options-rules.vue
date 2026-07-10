<template>
  <div class="serial_dialog">
    <el-button type="primary" @click="addOption">
      添加规则
    </el-button>
    <div v-for="(item,index) in options" :key="index" class="rule_item">
      <el-form-item style="width: 100%;" label="规则类型">
        <el-select v-model="item.type" placeholder="请选择规则类型" style="width: 100%;">
          <el-option label=" 自动计数" :value="1" />
          <el-option label=" 提交日期" :value="2" />
          <el-option label=" 固定字符" :value="3" />
          <el-option label=" 表单字段" :value="4" />
        </el-select>
      </el-form-item>
      <template v-if="item.type === 1">
        <el-form-item style="width: 100%;" label="重置周期">
          <el-select v-model="item.countCycle" style="width:120px">
            <el-option label="不自动周期重置" :value="0" />
            <el-option label="每日重置" :value="1" />
            <el-option label="每周重置" :value="2" />
            <el-option label="每月重置" :value="3" />
            <el-option label="每年重置" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="初始数值">
          <el-input-number v-model="item.initNumber" style="width:120px" />
        </el-form-item>
      </template>
      <template v-if="item.type === 2">
        <el-form-item style="width: 100%;" label="提交日期格式">
          <el-select v-model="item.dateFormat" style="width:120px">
            <el-option label="2025" value="YYYY" />
            <el-option label="202501" value="YYYYMM" />
            <el-option label="20250101" value="YYYYMMDD" />
            <el-option label="2025-01" value="YYYY-MM" />
            <el-option label="2025-01-01" value="YYYY-MM-DD" />
            <el-option label="2025/01" value="YYYY/MM" />
            <el-option label="2025/01/01" value="YYYY/MM/DD" />
          </el-select>
        </el-form-item>
      </template>
      <template v-if="item.type === 3">
        <el-form-item label="固定字符">
          <el-input v-model="item.content" style="width:120px" />
        </el-form-item>
      </template>
      <template v-if="item.type === 4">
        <el-form-item style="width: 100%;" label="表单字段">
          <el-select v-model="item.formFiled" style="width:120px">
            <el-option v-for="(itd, inds) in widgetList" :key="inds" :label="itd.label" :value="itd.id" />
          </el-select>
        </el-form-item>
      </template>
      <el-icon class="remove_item" @click="removeOption">
        <Delete color="red" size="24" />
      </el-icon>
    </div>
    <span class="dialog-footer">
      <el-button size="large" @click="cancelSave">取 消</el-button>
      <el-button size="large" type="primary" @click="confirmSave">确 定</el-button>
    </span>
  </div>
</template>

<script setup lang="ts">
import { ElInput, ElFormItem, ElSelect, ElOption, ElInputNumber, ElIcon, ElButton } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { ref, computed } from 'vue'

const props = defineProps({
  modelValue: Object,
  designer: Object
})
const options = ref([])

// 如果props.modelValue 是否是个对象，是对象转换成数组成为第一个值
if (Object.prototype.toString.call(props.modelValue) === '[object Object]') {
  options.value = [props.modelValue]
} else {
  options.value = props.modelValue
}

const widgetList = computed(() => {
  return props.designer.widgetList.map((item) => { return { ...item, ...item.options } })
})

const addOption = () => {
  options.value.push({
    type: 1,
    initNumber: 0,
    countCycle: 0,
    dateFormat: 'YYYY',
    content: '000001111',
    formFiled: ''
  })
}

const removeOption = (index) => {
  options.value.splice(index, 1)
}
const emits = defineEmits(['update:modelValue', 'cancelSave'])
const confirmSave = () => {
  emits('update:modelValue', options.value)
  emits('cancelSave')
}

const cancelSave = () => {
  emits('cancelSave')
}

</script>

<style lang="scss" scoped>

  .rule_item {
    position: relative;
    padding: 10px;
    border: 1px solid #b3b3b3;
    display: flex;
    flex-direction: column;
    border-radius: 2px;
    margin-bottom: 10px;
    margin: 8px auto;
    width: 92%;
  }

  .remove_item {
    position: absolute;
    right: -5px;
    top: -5px;
    cursor: pointer;
  }

  .dialog-footer {
    display: flex;
    justify-content: flex-end;
    margin:20px 0;
  }

</style>
