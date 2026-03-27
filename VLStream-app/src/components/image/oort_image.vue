<!--
 *@Created by: 兰舰
 * Email: gglanjian@qq.com
 * Phone: 16620805419
 * @Date: 2025-06-15 11:45:51
 * @Last Modified by:  兰舰
 * @Copyright aPaaS-front-team. All rights reserved.
!-->
<template>
  <img
    :src="imageSrc"
    :class="{'defaultS': defaultStyle}"
    v-bind="$attrs"
    @error="imageError"
  />
</template>
``
<script setup>
import { watch, ref } from 'vue'
import Config from '@/config/index'
import defaultImg from '@/assets/img/default_pic.png'
import deptDefaultImg from '@/assets/img/dept_default.png'
import userDefaultImg from '@/assets/img/tx.png'
import featureImage from '@/assets/img/featureDefault.png'
import dituImage from '@/assets/img/dituDefault.png'
const props = defineProps({
  'src': {
    type: String,
    default: ''
  },
  'errorImg': {
    type: String,
    default: defaultImg
  },
  'defaultType': {
    type: String,
    default: ''
  }
})
let imageSrc = ref('')
let defaultStyle = ref(false)
watch(() => props.src, (val) => {
  if (val && val.includes('http')) {
    let arr = val.split('/wj1/')
    if (arr.length === 2) {
      // this.firstTemp++
      imageSrc.value = Config.URL + Config.gateWay + 'wj1/' + arr[1]
    } else if (val.split('/oortwj1/').length === 2) {
      imageSrc.value = Config.URL + Config.gateWay + 'oortwj1/' + val.split('/oortwj1/')[1]
    }
  } else {
    imageSrc.value = val
    if (!val) imageError()
  }
}, { immediate: true })

function imageError() {
  defaultStyle.value = true
  if (props.defaultType === 'dept') {
    imageSrc.value = deptDefaultImg
  } else if (props.defaultType === 'user') {
    imageSrc.value = userDefaultImg
  } else if (props.defaultType === 'feature') {
    imageSrc.value = featureImage
  } else if (props.defaultType === 'ditu') {
    imageSrc.value = dituImage
  } else {
    imageSrc.value = defaultImg
  }
}

</script>

<style scoped>

.defaultS {
  width: 100%;
}

</style>
