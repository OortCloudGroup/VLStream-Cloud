// Optional deployment proxy for browser-facing ZLM SDP requests.
export function resolveZlmPlaybackUrl(address, proxyBase = import.meta.env.VITE_ZLM_API_BASE_URL, candidates = {
  udp: import.meta.env.VITE_ZLM_RTC_CAND_UDP,
  tcp: import.meta.env.VITE_ZLM_RTC_CAND_TCP
}) {
  const udp = candidates?.udp?.trim()
  const tcp = candidates?.tcp?.trim()
  if (!address || (!proxyBase && !udp && !tcp)) return address
  const url = new URL(address, window.location.href)
  if (!url.pathname.replace(/\/$/, '').endsWith('/index/api/webrtc')) return address
  let resolved = address
  if (proxyBase) {
    const base = new URL(proxyBase, window.location.href)
    if (!['http:', 'https:'].includes(base.protocol)) throw new Error('播放代理地址不可用')
    resolved = base.toString().replace(/\/$/, '') + '/index/api/webrtc' + url.search
  }
  if (!udp && !tcp) return resolved
  const target = new URL(resolved, window.location.href)
  // ZLM 1da300c accepts independent external IP:port candidates per SDP request.
  if (udp) target.searchParams.set('cand_udp', validateZlmCandidate(udp, 'UDP'))
  if (tcp) target.searchParams.set('cand_tcp', validateZlmCandidate(tcp, 'TCP'))
  return target.toString()
}

function validateZlmCandidate(value, protocol) {
  const match = /^((?:\d{1,3}\.){3}\d{1,3}):([1-9]\d{0,4})$/.exec(value)
  const octets = match?.[1].split('.') || []
  if (!match || octets.some(part => Number(part) > 255 || (part.length > 1 && part.startsWith('0')))
      || Number(octets[0]) === 0 || Number(octets[0]) >= 224 || Number(match[2]) > 65535) {
    throw new Error(`WebRTC ${protocol}候选配置无效，需使用IPv4:端口`)
  }
  return value
}

// Keep the public ICE candidate and optionally offer the same published RTC port on the LAN.
export function addZlmLanCandidate(sdp, host = import.meta.env.VITE_ZLM_RTC_LAN_HOST, address) {
  // A local browser talking to a local SDP endpoint can use the same published RTC port on loopback.
  // This also survives a LAN interface change without altering ZLM/network configuration.
  if (!host && address) {
    try {
      const endpoint = new URL(address, window.location.href)
      const local = name => ['localhost', '127.0.0.1', '[::1]'].includes(name)
      if (local(window.location.hostname) && local(endpoint.hostname)) host = '127.0.0.1'
    } catch { return sdp }
  }
  if (!host || typeof sdp !== 'string') return sdp
  if (!/^\d{1,3}(\.\d{1,3}){3}$/.test(host) || host.split('.').some(x => Number(x) > 255)) return sdp
  const newline = sdp.includes('\r\n') ? '\r\n' : '\n'
  return sdp.split(/\r?\n/).flatMap(line => {
    const match = /^(a=candidate:)(\S+) (\d+) (udp|tcp) (\d+) (\S+) (\d+) typ host(.*)$/i.exec(line)
    if (!match || match[6] === host) return [line]
    const extra = match[1] + match[2].slice(0, 30) + 'L ' + match[3] + ' ' + match[4] + ' ' + match[5] + ' ' + host + ' ' + match[7] + ' typ host' + match[8]
    return [line, extra]
  }).join(newline)
}
