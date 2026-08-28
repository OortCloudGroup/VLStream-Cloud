/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import { ref } from 'vue'
import store from '@/store/index'
import { defineStore } from 'pinia'

type ErrorMsg = {
  id: string
  msg: string[]
}

export const useErrorMsgStore = defineStore('errorMsg', () => {
  const errorMsgList = ref([])
  const addErrorMsg = (msg) => {
    // Process interface tigase shiku full Replace to imany
    msg.interfaceName = msg.interfaceName.replace(/tigase|shiku/g, 'imany')
    errorMsgList.value.unshift(msg)
  }

  const clearAllErrorMsg = () => {
    errorMsgList.value = []
  }

  // { id: 'id', msg: ['msg1', 'msg2'] }
  const nodeError = ref<ErrorMsg[]>([])
  const addNodeError = (msg) => {
    const errIndex = nodeError.value.findIndex(item => item.id === msg.id)
    if (errIndex === -1) {
      nodeError.value.unshift(msg)
    } else {
      const errMsgIndex = nodeError.value[errIndex].msg.findIndex(item => item === msg.msg[0])
      if (errMsgIndex !== -1) return
      nodeError.value[errIndex].msg.unshift(msg.msg[0])
    }
    console.log(nodeError.value)
  }
  const clearNodeError = () => {
    nodeError.value = []
  }
  const removeNodeError = (id) => {
    nodeError.value = nodeError.value.filter((item) => item.id !== id)
  }

  const getNodeErrorMsg = (id) => {
    return nodeError.value.find((item) => item.id === id)
  }

  return {
    errorMsgList,
    addErrorMsg,
    clearAllErrorMsg,
    nodeError,
    addNodeError,
    clearNodeError,
    removeNodeError,
    getNodeErrorMsg
  }
})

/* * in setup */
export function useErrorMsgStoreHook() {
  return useErrorMsgStore(store)
}
