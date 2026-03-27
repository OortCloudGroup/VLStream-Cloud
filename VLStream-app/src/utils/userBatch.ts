/*
* 批量拉取用户信息，合并同一渲染帧内的多个请求，降低并发
*/
import { getUserList } from '@/api/unifiedUsert/sso'
import { useUserStore } from '@/store/modules/useraPaas'

type ResolveFn = (value: any) => void
type RejectFn = (reason?: any) => void

interface QueueItem {
  userId: string
  resolve: ResolveFn
  reject: RejectFn
}

let pendingQueue: QueueItem[] = []
let scheduled = false

function scheduleFlush() {
  if (scheduled) return
  scheduled = true
  // 使用微任务尽快合并同一轮更新
  Promise.resolve().then(flushQueue)
}

async function flushQueue() {
  scheduled = false
  const queue = pendingQueue
  pendingQueue = []
  if (queue.length === 0) return

  const store: any = useUserStore()
  const uniqueIds = Array.from(new Set(queue.map(item => item.userId)))

  // 先命中缓存，减少请求体
  const uncachedIds: string[] = []
  const cachedMap: Record<string, any> = {}
  uniqueIds.forEach(id => {
    const cached = store.userListStore[id]
    if (cached) {
      cachedMap[id] = cached
    } else if (id) {
      uncachedIds.push(id)
    }
  })

  // 把缓存命中的先 resolve
  if (Object.keys(cachedMap).length) {
    queue.forEach(item => {
      const cached = cachedMap[item.userId]
      if (cached) item.resolve(cached)
    })
  }

  if (uncachedIds.length === 0) {
    // 全部命中缓存
    return
  }

  const payload: any = {
    accessToken: store.userInfo?.accessToken || store.token,
    user_id: uncachedIds,
    tenant_id: store.tenantId,
    hideLoading: true
  }

  try {
    const res: any = await getUserList(payload)
    if (res?.code === 200 && Array.isArray(res.data?.list)) {
      const list: any[] = res.data.list
      // 写入缓存
      list.forEach(user => {
        if (user && user.user_id) {
          store.addUser(user)
        }
      })
      const map: Record<string, any> = {}
      list.forEach(user => {
        map[user.user_id] = user
      })
      // resolve 未被缓存命中的项
      queue.forEach(item => {
        if (cachedMap[item.userId]) return
        const user = map[item.userId]
        if (user) item.resolve(user)
      })
      // 对于仍未返回的 id，进行 reject，避免悬挂
      queue.forEach(item => {
        if (cachedMap[item.userId]) return
        const user = map[item.userId]
        if (!user) item.reject(new Error('User not found: ' + item.userId))
      })
    } else {
      const err = new Error('getUserList failed')
      queue.forEach(item => {
        if (!cachedMap[item.userId]) item.reject(err)
      })
    }
  } catch (error) {
    queue.forEach(item => {
      if (!cachedMap[item.userId]) item.reject(error)
    })
  }
}

export function batchFetchUser(userId: string) {
  return new Promise<any>((resolve, reject) => {
    if (!userId) {
      reject(new Error('Invalid userId'))
      return
    }
    pendingQueue.push({ userId, resolve, reject })
    scheduleFlush()
  })
}

