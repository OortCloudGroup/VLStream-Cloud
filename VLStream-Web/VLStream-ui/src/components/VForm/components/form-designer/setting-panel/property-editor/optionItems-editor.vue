<template>
  <el-form-item label-width="0">
    <el-divider class="custom-divider-margin-top">
      {{ i18nt('designer.setting.optionsSetting') }}
    </el-divider>
    <template v-if="selectedWidget.type === 'cascader-area'">
      <area-type-setting v-model="optionModel.areaOptionItems" :designer="designer" :selected-widget="selectedWidget" />
      <area-center-setting v-model="optionModel.mapCenter" :designer="designer" :area-type="optionModel.areaOptionItems?.areaType" :selected-widget="selectedWidget" />
    </template>

    <option-items-setting v-else :designer="designer" :selected-widget="selectedWidget" />
  </el-form-item>
</template>

<script>
import i18n from '~@/utils/i18n'
import OptionItemsSetting from '~@/components/form-designer/setting-panel/option-items-setting'
import areaTypeSetting from '~@/components/form-designer/setting-panel/area-type-setting'
import areaCenterSetting from '~@/components/form-designer/setting-panel/area-center-setting'

import { ElFormItem, ElDivider } from 'element-plus'
export default {
  name: 'OptionItemsEditor',
  components: {
    ElFormItem,
    ElDivider,
    OptionItemsSetting,
    areaTypeSetting,
    areaCenterSetting
  },
  mixins: [i18n],
  props: {
    designer: Object,
    selectedWidget: Object,
    optionModel: Object
  }
}
</script>

<style scoped>

</style>
