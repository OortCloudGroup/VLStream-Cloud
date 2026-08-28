/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
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

/* * in setup */
export function useFlowStoreHook() {
  return useFlowStore(store)
}

