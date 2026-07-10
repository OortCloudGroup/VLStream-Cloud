<!--
 *@Created by: 兰舰
 * Email: gglanjian@qq.com
 * Phone: 16620805419
 * @Date: 2024-11-15 11:45:51
 * @Last Modified by:  兰舰
 * @Copyright aPaaS-front-team. All rights reserved.
!-->
<template>
  <oort-svg-icon v-if="defaultType === 'dept' && defaultStyle" :class="{'defaultS': defaultStyle}" round name="dept" width="48" height="48" />
  <img
    v-else
    :src="imageSrc"
    v-bind="$attrs"
    :class="{'defaultS': defaultStyle}"
    @error="imageError"
  />
</template>

<script setup>
import { watch, ref } from 'vue'
import Config from '@/config/index'
import defaultImg from '@/assets/img/default_pic.png'
import deptDefaultImg from '@/assets/img/dept_default.png'
import userDefaultImg from '@/assets/img/tx.png'
import featureDefaultImg from '@/assets/img/smart_graden/location/ditu/default.png'
import geoserver_basemap_default from '@/assets/img/geoserver_basemap_default.png'
import smart_graden_ditu_default from '@/assets/img/smart_graden/location/ditu/default.png'
import altmanImg from '@/assets/img/xorkweb/agent_altman.png'

import companylogo from '@/assets/img/oortlogo.png'
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
      imageSrc.value = Config.URL + Config.gateWay + 'wj1/' + val.split('/oortwj1/')[1]
    }
  } else {
    imageSrc.value = val
  }
  if (!imageSrc.value) imageError()
}, { immediate: true })

function imageError() {
  defaultStyle.value = true
  if (props.defaultType === 'dept') {
    imageSrc.value = deptDefaultImg
  } else if (props.defaultType === 'user') {
    imageSrc.value = userDefaultImg
  } else if (props.defaultType === 'feature') {
    imageSrc.value = featureDefaultImg
  } else if (props.defaultType === 'geoserver_basemap_default') {
    imageSrc.value = geoserver_basemap_default
  } else if (props.defaultType === 'smart_graden_ditu_default') {
    imageSrc.value = smart_graden_ditu_default
  } else if (props.defaultType === 'companylogo') {
    imageSrc.value = companylogo
  } else if (props.defaultType === 'xork_agent') {
    imageSrc.value = altmanImg
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

