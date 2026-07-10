/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 11:03:48
* @Last Modified by:   兰舰
* @Last Modified time: 2024-11-15 11:03:48
* @Copyright aPaaS-front-team. All rights reserved.
*/
import { defineStore } from 'pinia'
import store from '@/store/index'
import { ref } from 'vue'
export const useFlowStore = defineStore('useFlowStore', () => {
  const currentFlowForm = ref('')
  const formFiledList = ref([])

  const setCurrentFlowForm = (data: any) => {
    currentFlowForm.value = data
  }

  const setFormFiledList = (data: any) => {
    formFiledList.value = data
  }

  return {
    currentFlowForm,
    formFiledList,
    setCurrentFlowForm,
    setFormFiledList
  }
})

/** 在 setup 外使用 */
export function useFlowStoreHook() {
  return useFlowStore(store)
}

