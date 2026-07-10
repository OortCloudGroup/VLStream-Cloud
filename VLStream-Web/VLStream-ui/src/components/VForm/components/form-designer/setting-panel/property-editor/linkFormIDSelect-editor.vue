<template>
  <el-form-item label="选择表单">
    <el-select v-model="optionModel.linkFormID">
      <el-option
        v-for="item in formList"
        :key="item.formId"
        :label="item.formName"
        :value="item.formId"
      />
    </el-select>
  </el-form-item>
</template>

<script>
import { ElFormItem, ElSelect, ElOption } from 'element-plus'
import { listForm } from '@/api/processui'
export default {
  name: 'LinkFormIDSelectEditor',
  components: {
    ElFormItem,
    ElSelect,
    ElOption
  },
  props: {
    designer: Object,
    selectedWidget: Object,
    optionModel: Object
  },
  data() {
    return {
      formList: []
    }
  },
  created() {
    this.getFormList()
  },
  methods: {
    getFormList() {
      const params = {
        pageNum: 1,
        pageSize: 999,
        isFormComponents: 0
      }
      listForm(params).then(res => {
        this.formList = res.rows || []
      })
    }
  }
}
</script>

<style scoped>

</style>
