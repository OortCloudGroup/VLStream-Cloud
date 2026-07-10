<template>
  <div>
    <el-form-item v-show="!!subFormChildWidgetFlag" :label="i18nt('designer.setting.widgetColumnWidth')">
      <el-input v-model="optionModel.columnWidth" type="text" />
    </el-form-item>
  </div>
</template>

<script>
import i18n from '~@/utils/i18n'
import { ElFormItem, ElInput } from 'element-plus'

export default {
  name: 'ColumnWidthEditor',
  components: {
    ElFormItem,
    ElInput
  },
  mixins: [i18n],
  props: {
    designer: Object,
    selectedWidget: Object,
    optionModel: Object
  },
  data() {
    return {
      // subFormChildWidgetFlag: false,
      subFormChildWidgetFlag: true
    }
  },
  created() {

  },
  mounted() {
    this.designer.handleEvent('field-selected', (parentWidget) => {
      this.subFormChildWidgetFlag = !!parentWidget && (parentWidget.type === 'sub-form')
      // console.log('subFormChildWidgetFlag', this.subFormChildWidgetFlag)
    })
  }
}
</script>

<style scoped>

</style>
