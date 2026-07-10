<template>
  <div>
    <el-divider class="custom-divider-margin-top">
      基础数据
    </el-divider>
    <DictTagSetting v-model="optionModel.basicOptions" />
  </div>
</template>

<script>
import i18n from '~@/utils/i18n'
import { ElDivider } from 'element-plus'
import DictTagSetting from '~@/components/form-designer/setting-panel/dict-tag-setting'
export default {
  name: 'BasicOptionsEditor',
  components: {
    ElDivider,
    DictTagSetting
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
