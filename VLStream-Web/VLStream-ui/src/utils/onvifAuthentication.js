export function prepareOnvifAuthentication(data, account) {
  let streams = data?.streamUris
  if (typeof streams === 'string') {
    try { streams = JSON.parse(streams) } catch { streams = null }
  }
  const entries = streams && !Array.isArray(streams) && typeof streams === 'object' ? Object.entries(streams) : []
  if (!entries.length || entries.some(([token, uri]) => !token.trim() || typeof uri !== 'string' || !/^rtsps?:\/\//i.test(uri))) {
    const error = new Error('设备未返回可用视频通道，请检查 ONVIF 配置与账号')
    error.name = 'OnvifAuthenticationResultError'
    throw error
  }
  return {
    options: entries.map(([token]) => ({ label: token, value: token })),
    result: { ...data, streamUris: streams, ip: data.ip || account.ip, username: account.username,
      password: account.password, playType: '1', channel: null, url: null }
  }
}
