import { ref, toRefs } from 'vue'
import { ElLoading, ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import router from '@/router'
import wvpRequest, { downloadWvp } from '@/utils/wvpRequest'

const dictCache = new Map()

export function useDict(...dictTypes) {
  const state = ref({})

  dictTypes.forEach((dictType) => {
    state.value[dictType] = dictCache.get(dictType) || []
    if (dictCache.has(dictType)) return

    wvpRequest.get(`/system/dict/data/type/${encodeURIComponent(dictType)}`)
      .then((response) => {
        const rows = Array.isArray(response?.data) ? response.data : []
        const options = rows.map((item) => ({
          label: item.dictLabel,
          value: item.dictValue,
          elTagType: item.listClass,
          elTagClass: item.cssClass
        }))
        dictCache.set(dictType, options)
        state.value[dictType] = options
      })
      .catch(() => {
        state.value[dictType] = []
      })
  })

  return toRefs(state.value)
}

export function resetForm(refName) {
  this.$refs?.[refName]?.resetFields?.()
}

export function handleTree(data, id = 'id', parentId = 'parentId', children = 'children') {
  const rows = Array.isArray(data) ? data : []
  const byId = new Map(rows.map((item) => [item[id], { ...item, [children]: item[children] || [] }]))
  const tree = []
  byId.forEach((item) => {
    const parent = byId.get(item[parentId])
    if (parent) parent[children].push(item)
    else tree.push(item)
  })
  return tree
}

export function clacPXToVW(value, baseWidth = 1920) {
  return String(Math.min(value, (window.innerWidth * value) / baseWidth))
}

export function parseTime(time, pattern = '{y}-{m}-{d} {h}:{i}:{s}') {
  if (!time) return null
  const date = time instanceof Date ? time : new Date(String(time).replace(/-/g, '/').replace('T', ' '))
  if (Number.isNaN(date.getTime())) return String(time)
  const values = {
    y: date.getFullYear(),
    m: date.getMonth() + 1,
    d: date.getDate(),
    h: date.getHours(),
    i: date.getMinutes(),
    s: date.getSeconds()
  }
  return pattern.replace(/{(y|m|d|h|i|s)+}/g, (match, key) => String(values[key]).padStart(match.length > 3 ? 4 : 2, '0'))
}

const modal = {
  msg: ElMessage.info,
  msgError: ElMessage.error,
  msgSuccess: ElMessage.success,
  msgWarning: ElMessage.warning,
  notify: ElNotification.info,
  notifyError: ElNotification.error,
  notifySuccess: ElNotification.success,
  notifyWarning: ElNotification.warning,
  confirm(content) {
    return ElMessageBox.confirm(content, '系统提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  },
  loading(content) {
    this.loadingInstance = ElLoading.service({ lock: true, text: content })
  },
  closeLoading() {
    this.loadingInstance?.close()
  }
}

const tab = {
  openPage(target) {
    return router.push(target)
  },
  closeOpenPage(target) {
    return target ? router.push(target) : router.back()
  },
  closePage() {
    return router.back()
  }
}

export function installWvpCompat(app) {
  app.config.globalProperties.useDict = useDict
  app.config.globalProperties.resetForm = resetForm
  app.config.globalProperties.handleTree = handleTree
  app.config.globalProperties.parseTime = parseTime
  app.config.globalProperties.clacPXToVW = clacPXToVW
  app.config.globalProperties.download = downloadWvp
  app.config.globalProperties.$modal = modal
  app.config.globalProperties.$tab = tab
}
