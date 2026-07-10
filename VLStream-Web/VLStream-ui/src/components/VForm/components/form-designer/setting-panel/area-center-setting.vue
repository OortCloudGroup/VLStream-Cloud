<template>
  <div v-if="areaType > 3" style="width: 100%;">
    <el-form-item style="width: 100%;" label="设置定位中点">
      <span class="localCenter">{{ localModelValue }}</span>
      <!-- <el-input v-model="localModelValue" placeholder="请输入定位中点" type="text">
        <template #append>
          <el-button :icon="Plus" @click="showBDMAP" />
        </template>
      </el-input> -->
      <el-button :icon="Plus" @click="showBDMAP" />
    </el-form-item>
    <el-dialog v-model="bMapVisible" title="选择定位中点" width="70%">
      <BaidumapSelectAddress :center="localCenter" :area-type="areaType" @change="changeLocationCenter" />
    </el-dialog>
  </div>
</template>

<script>
import i18n from '~@/utils/i18n'
import { ElFormItem, ElButton, ElInput, ElDialog } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import BaidumapSelectAddress from './baidumapSelectAddress.vue'

export default {
  name: 'AreaCenterSetting',
  components: {
    ElFormItem,
    ElButton,
    ElInput,
    ElDialog,
    BaidumapSelectAddress
  },
  mixins: [i18n],
  props: {
    designer: Object,
    selectedWidget: Object,
    modelValue: {
      type: Object
    },
    areaType: {
      type: Number,
      default: 1
    }
  },
  emits: ['update:modelValue'],
  data() {
    return {
      localModelValue: null,
      Plus: Plus,
      address: '',
      bMapVisible: false
    }
  },
  watch: {
    areaType() {
      if (this.areaType <= 3) {
        this.$emit('update:modelValue', '')
      } else {
        this.$emit('update:modelValue', this.localModelValue)
      }
    }
  },
  created() {
    // 如果this.modelValue 是一个对象
    if (typeof this.modelValue === 'object') {
      this.localModelValue = this.modelValue.address
    } else {
      this.localModelValue = this.modelValue
    }
    this.localCenter = this.modelValue
    if (this.areaType <= 3) {
      this.$emit('update:modelValue', '')
    } else {
      this.$emit('update:modelValue', this.modelValue)
    }
  },
  methods: {
    showBDMAP() {
      this.bMapVisible = true
    },
    changeLocationCenter(data) {
      // { address: address.value, center: centerPoint.value }
      this.localModelValue = data.address
      this.$emit('update:modelValue', data)
      this.bMapVisible = false
    }
  }
}
</script>

  <style scoped>

    .item_width {
      width: 180px;
    }

  </style>
