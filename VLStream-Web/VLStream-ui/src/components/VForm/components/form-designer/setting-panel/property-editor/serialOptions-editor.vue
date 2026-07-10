<template>
  <el-form-item label-width="0">
    <el-divider class="custom-divider-margin-top">
      规则设置
    </el-divider>
    <el-button type="primary" @click="showSerialDialog">
      添加/编辑规则
    </el-button>
    <br />
    <div v-for="(item,index) in serOptions" :key="index" class="rule_item" @click="showSerialDialog">
      <span>规则{{ index + 1 }}：</span>
      <span v-if="item.type ===1">自动计数</span>
      <span v-if="item.type ===2">日期</span>
      <span v-if="item.type ===3">固定字符</span>
      <span v-if="item.type ===4">表单字段</span>
    </div>
    <el-dialog v-model="dialogVisible" title="序列号规则" width="45%">
      <SerialOptionsRules v-model="optionModel.serialOptions" :designer="designer" @cancelSave="dialogVisible = false" />
    </el-dialog>
  </el-form-item>
</template>

<script>
import i18n from '~@/utils/i18n'
import { ElFormItem, ElDivider, ElButton, ElDialog } from 'element-plus'
import SerialOptionsRules from '../serial-options-rules.vue'
export default {
  name: 'SerialOptionsEditor',
  components: {
    ElFormItem,
    ElDivider,
    ElButton,
    ElDialog,
    SerialOptionsRules
  },
  mixins: [i18n],
  props: {
    designer: Object,
    selectedWidget: Object,
    optionModel: Object
  },
  data() {
    return {
      dialogVisible: false
    }
  },
  computed: {
    serOptions() {
      let tempArr = this.optionModel.serialOptions
      if (Object.prototype.toString.call(this.optionModel.serialOptions) === '[object Object]') {
        tempArr = [this.optionModel.serialOptions]
      }
      return tempArr
    }
  },
  methods: {
    showSerialDialog() {
      this.dialogVisible = true
    }
  }
}
</script>

<style lang="scss" scoped>

  .rule_item {
    display: flex;
    flex-direction: row;
    margin: 8px 0 ;
    border: 1px solid #ebeef5;
    padding: 4px;
    cursor: pointer;
    width: 100%;
    span:first-child {
     margin-right: 10ox;
    }
  }

</style>
