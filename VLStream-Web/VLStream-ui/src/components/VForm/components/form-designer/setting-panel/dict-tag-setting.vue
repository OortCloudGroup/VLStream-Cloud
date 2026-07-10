<template>
  <div>
    <el-form-item style="width: 100%;" label="数据类标识">
      <el-input v-model="dataTag" placeholder="请输入数据类标识" type="text">
        <template #append>
          <el-button :icon="Plus" @click="addDictTag" />
        </template>
      </el-input>
    </el-form-item>
    <el-form-item style="width: 100%;" label="显示方式">
      <el-select v-model="showType" placeholder="请选择显示方式" type="text" @change="showTypeChange">
        <el-option label="单选" :value="1" />
        <el-option label="多选" :value="2" />
        <el-option label="下拉单选" :value="3" />
        <el-option label="下拉多选" :value="4" />
      </el-select>
    </el-form-item>
    <el-dialog v-model="dialogVisible" title="选择数据类标识" width="30%">
      <div class="dict-tag-table">
        <el-table
          header-cell-class-name="header_tenant_cell"
          stripe
          style="width: 100%"
          :data="tableData"
          @current-change="handleSelectChoose"
        >
          <el-table-column type="index" label="序号" />
          <el-table-column prop="name" label="类型名称" />
          <el-table-column prop="dict_tag" label="类型标记" />
          <el-table-column prop="description" label="描述" />
          <el-table-column prop="sort" label="排序" />
        </el-table>
        <div class="paginationBox flexRowAC">
          <el-pagination
            background
            :page-sizes="[10, 20, 50, 100]"
            :page-size="pagesize"
            layout="total, prev, pager, next, sizes"
            :total="count"
            class="justifyAlign"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取 消</el-button>
          <el-button type="primary" @click="saveDataTag">确 定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElInput, ElButton, ElDialog, ElTable, ElTableColumn, ElPagination, ElSelect, ElOption, ElFormItem } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

import { ref, onMounted } from 'vue'
import { dictList } from '@/api/login/indexapaas'

const props = defineProps({
  modelValue: Object
})

const count = ref<any>(0)
const page = ref<Number>(1)
const pagesize = ref<Number>(10)
const tableData = ref<any>([])
const dialogVisible = ref(false)

const addDictTag = () => {
  dialogVisible.value = true
}

const getList = async() => {
  const params = {
    page: page.value,
    pagesize: pagesize.value
  }
  dictList(params).then(res => {
    if (res.code === 200) {
      tableData.value = res.data.list || []
      count.value = res.data.count || 0
    }
  })
}

const handleSizeChange = (val: number) => {
  pagesize.value = val
  getList()
}
const handleCurrentChange = (val: number) => {
  page.value = val
  getList()
}

const emits = defineEmits(['update:modelValue'])
const dataTag = ref(props.modelValue.dataTag || '')
const showType = ref(props.modelValue.showType || 1)
const handleSelectChoose = (data) => {
  dialogVisible.value = false
  dataTag.value = data.dict_tag
  emits('update:modelValue', { dataTag: dataTag.value, showType: showType.value })
}

const showTypeChange = () => {
  emits('update:modelValue', { dataTag: dataTag.value, showType: showType.value })
}

onMounted(() => {
  getList()
})

</script>

  <style scoped>

    .item_width {
      width: 180px;
    }

  </style>
