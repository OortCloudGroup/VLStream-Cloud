// A local expiry deadline stops media even when a renewal request hangs.
export function createSharedVideoLease({ api, onChange, allowCameraRtc = false, setTimer = setTimeout, clearTimer = clearTimeout }) {
  let revision = 0, lease = null, renewTimer, expiryTimer
  const publish = (status, error = '') => onChange({ status, error, lease })
  const close = value => { if (value?.sessionId) Promise.resolve().then(() => api.close(value.deviceRef, value.sessionId)).catch(() => {}) }
  const clearTimers = () => { clearTimer(renewTimer); clearTimer(expiryTimer) }
  const fail = error => {
    revision++; clearTimers(); const old = lease; lease = null; close(old)
    publish('error', error?.response?.data?.msg || error?.message || '共享查看权限已失效，播放已停止')
  }
  const arm = current => {
    const seconds = Number(lease.expiresInSeconds)
    if (!Number.isFinite(seconds) || seconds <= 0 || seconds > 300) throw new Error('共享预览有效期无效')
    clearTimers()
    expiryTimer = setTimer(() => { if (current === revision) fail(new Error('共享预览已到期，播放已停止')) }, seconds * 1000)
    renewTimer = setTimer(async () => {
      const active = lease
      try {
        const result = (await api.renew(active.deviceRef, active.sessionId))?.data
        if (current !== revision) return
        if (result?.sessionId !== active.sessionId) throw new Error('共享预览续期响应无效')
        lease = { ...active, expiresInSeconds: result.expiresInSeconds }
        arm(current)
      } catch (error) { if (current === revision) fail(error) }
    }, seconds * 500)
  }
  return {
    async start(deviceRef, streamId) {
      const current = ++revision
      clearTimers(); close(lease); lease = null; publish('loading')
      try {
        const result = (await api.start(deviceRef, streamId))?.data
        const next = { ...result, deviceRef }
        if (current !== revision) { close(next); return }
        lease = next
        if (!lease.sessionId || !['ZLM','cameraRTC'].includes(lease.playMode)) throw new Error('共享预览响应无效')
        if(lease.playMode==='cameraRTC'&&!allowCameraRtc)throw new Error('共享CameraRTC尚未完成网关安全配置，暂不可用')
        arm(current); publish('active')
      } catch (error) { if (current === revision) fail(error) }
    },
    stop() { revision++; clearTimers(); const old = lease; lease = null; close(old); publish('idle') },
  }
}
