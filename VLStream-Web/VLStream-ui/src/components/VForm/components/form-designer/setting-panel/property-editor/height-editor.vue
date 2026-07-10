<template>
  <el-form-item label="组件高度">
    <el-input
      v-model="compHeight"
      type="number"
      class="hide-spin-button"
      min="0"
      style="width: 100%"
      @input="inputNumberHandler"
    />
  </el-form-item>
</template>

<script>
import i18n from '~@/utils/i18n'
import propertyMixin from '~@/components/form-designer/setting-panel/property-editor/propertyMixin'
import { ElFormItem, ElInput } from 'element-plus'

export default {
  name: 'HeightEditor',
  components: {
    ElFormItem,
    ElInput
  },
  mixins: [i18n, propertyMixin],
  props: {
    designer: Object,
    selectedWidget: Object,
    optionModel: Object
  },
  computed: {
    compHeight: {
      get() {
        return this.optionModel['height']
      },

      set(newValue) {
        if (!newValue || isNaN(newValue)) {
          // eslint-disable-next-line vue/no-mutating-props
          this.optionModel.height = null
        } else {
          // eslint-disable-next-line vue/no-mutating-props
          this.optionModel.height = Number(newValue)
        }
      }
    }
  }
}
</script>

<style scoped>

</style>
