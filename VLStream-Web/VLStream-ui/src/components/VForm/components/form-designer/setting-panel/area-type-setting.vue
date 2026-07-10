<template>
  <el-form-item label="地区精度" style="width: 100%;">
    <el-select v-model="type" class="item_width" placeholder="请选择" @change="changeAreaType">
      <el-option
        v-for="item in areaTypeList"
        :key="item.value"
        :label="item.label"
        :value="item.value"
      />
    </el-select>
  </el-form-item>
</template>

<script>
import i18n from '~@/utils/i18n'
import { ElFormItem, ElSelect, ElOption } from 'element-plus'
import { provinceCityArea, province, provinceCity } from '@/assets/json/proviceCityArea'

export default {
  name: 'AreaTypeSetting',
  components: {
    ElFormItem,
    ElSelect,
    ElOption
  },
  mixins: [i18n],
  props: {
    designer: Object,
    selectedWidget: Object,
    modelValue: {
      type: Object
    }
  },
  emits: ['update:modelValue'],
  data() {
    return {
      type: 1,
      areaTypeList: [
        { label: '省', value: 1 },
        { label: '省-市', value: 2 },
        { label: '省-市-区', value: 3 },
        { label: '省-市-区-详细地址', value: 4 },
        { label: '详细地址', value: 5 }
      ],
      localModleValue: this.modelValue
    }
  },
  created() {
    this.type = this.modelValue?.areaType || 1
    this.changeAreaType()
  },
  methods: {
    changeAreaType() {
      let tempArr = []
      if (this.type === 1) {
        tempArr = province()
      }
      if (this.type === 2) {
        console.log('provinceCity', provinceCity())
        tempArr = provinceCity()
      }
      if (this.type === 3) {
        tempArr = provinceCityArea()
      }
      let obj = { ...this.modelValue, ...{ optionItems: tempArr, areaType: this.type }}
      this.$emit('update:modelValue', obj)
    }
  }
}
</script>

  <style scoped>

    .item_width {
      width: 180px;
    }

  </style>
