// Optional deployment proxy for browser-facing ZLM SDP requests.
export function resolveZlmPlaybackUrl(address, proxyBase = import.meta.env.VITE_ZLM_API_BASE_URL) {
  if (!address || !proxyBase) return address
  const url = new URL(address, window.location.href)
  if (!url.pathname.replace(/\/$/, '').endsWith('/index/api/webrtc')) return address
  const base = new URL(proxyBase, window.location.href)
  if (!['http:', 'https:'].includes(base.protocol)) throw new Error('播放代理地址不可用')
  return base.toString().replace(/\/$/, '') + '/index/api/webrtc' + url.search
}

// Keep the public ICE candidate and optionally offer the same published UDP port on the LAN.
export function addZlmLanCandidate(sdp, host = import.meta.env.VITE_ZLM_RTC_LAN_HOST) {
  if (!host || typeof sdp !== 'string') return sdp
  if (!/^\d{1,3}(\.\d{1,3}){3}$/.test(host) || host.split('.').some(x => Number(x) > 255)) return sdp
  const newline = sdp.includes('\r\n') ? '\r\n' : '\n'
  return sdp.split(/\r?\n/).flatMap(line => {
    const match = /^(a=candidate:)(\S+) (\d+) (udp) (\d+) (\S+) (\d+) typ host(.*)$/i.exec(line)
    if (!match || match[6] === host) return [line]
    const extra = match[1] + match[2].slice(0, 30) + 'L ' + match[3] + ' ' + match[4] + ' ' + match[5] + ' ' + host + ' ' + match[7] + ' typ host' + match[8]
    return [line, extra]
  }).join(newline)
}
