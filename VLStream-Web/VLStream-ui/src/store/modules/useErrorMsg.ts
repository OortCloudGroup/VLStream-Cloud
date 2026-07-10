/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 11:03:53
 * @Last Modified by: 兰舰
 * @Last Modified time: 2025-05-08 09:39:28
* @Copyright aPaaS-front-team. All rights reserved.
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
    // 处理下接口里面的关键路径  包含 tigase  shiku 的字符 全部替换为 imany
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

/** 在 setup 外使用 */
export function useErrorMsgStoreHook() {
  return useErrorMsgStore(store)
}
