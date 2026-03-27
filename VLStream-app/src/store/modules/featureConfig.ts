import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useFeatureConfigStore = defineStore('featureConfig', () => {
  const featureCategoryList = ref([])
  const setFeatureCategoryList = (list) => {
    featureCategoryList.value = list
  }

  return {
    featureCategoryList,
    setFeatureCategoryList
  }
})
