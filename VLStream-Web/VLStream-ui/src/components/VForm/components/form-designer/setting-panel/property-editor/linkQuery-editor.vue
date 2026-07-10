<template>
  <div>
    <el-form-item style="width: 100%;" label="关联查询">
      <link-search-setting v-model="optionModel.linkQuery" :designer="designer" />
    </el-form-item>
  </div>
</template>

<script>
import i18n from '~@/utils/i18n'
import { ElFormItem } from 'element-plus'
import LinkSearchSetting from '~@/components/form-designer/setting-panel/link-search-setting.vue'
export default {
  name: 'LinkQueryEditor',
  components: {
    ElFormItem,
    LinkSearchSetting
  },
  mixins: [i18n],
  props: {
    designer: Object,
    selectedWidget: Object,
    optionModel: Object
  },
  data() {
    return {
    }
  },
  created() {
  }
}
</script>

<style scoped>

</style>
