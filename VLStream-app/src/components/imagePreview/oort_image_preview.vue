<template>
  <van-image-preview
    v-model:show="popupShow"
    v-bind="$attrs"
    :images="imgList"
  >
    <slot />
  </van-image-preview>
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount, onDeactivated } from 'vue'
import bus from '@/utils/bus'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  images: {
    type: Array,
    default: null
  },
  flag: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['update:modelValue'])

const imgList = ref([])
const popupShow = ref(false)

watch(() => props.images, () => {
  init()
})

watch(() => props.modelValue, (newVal) => {
  popupShow.value = newVal
  handlerPopupEvent()
})

const init = () => {
  imgList.value = []
  if (props.images?.length !== 0) {
    let imgUrl
    let data
    props.images.forEach(res => {
      if (res?.url) {
        data = res?.url
      } else {
        data = res
      }
      if (props.flag === 1) {
        let index = data.lastIndexOf('visappoint/v1/showImage/')
        imgUrl = data.substring(index, data.length)
      } else {
        // let index1 = data.lastIndexOf('oort/oortwj1/')
        // imgUrl = data.substring(index1, data.length)
        imgUrl = data
      }
      imgList.value.push(imgUrl)
    })
  }
}

const popShow = () => {
  popupShow.value = false
}

const handlerPopupEvent = () => {
  bus.$emit('popShow', popupShow.value)
}

onMounted(() => {
  init()
  popupShow.value = props.modelValue
  bus.$on('popupHide', popShow)
})

onBeforeUnmount(() => {
  bus.$off('popupHide', popShow)
})

onDeactivated(() => {
  bus.$off('popupHide', popShow)
})

// 当 popupShow 变化时，通知父组件更新 modelValue
watch(() => popupShow.value, (newVal) => {
  emit('update:modelValue', newVal)
})
</script>

<style scoped>

</style>
