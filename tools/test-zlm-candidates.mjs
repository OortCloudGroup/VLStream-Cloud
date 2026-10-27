import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { tmpdir } from 'node:os'
import { pathToFileURL, fileURLToPath } from 'node:url'
import { vmBuildEnvironment } from '../VLStream-Web/VLStream-ui/scripts/build-vm.mjs'

const source = fs.readFileSync(new URL('../VLStream-Web/VLStream-ui/src/utils/zlmPlaybackUrl.js', import.meta.url), 'utf8')
  .replace(/import\.meta\.env\.VITE_ZLM_[A-Z_]+/g, 'undefined')
const { resolveZlmPlaybackUrl: resolve } = await import('data:text/javascript;base64,' + Buffer.from(source).toString('base64'))
globalThis.window = { location: { href: 'https://console.example/bus/vls-ui/onvif/cameraManage' } }
const sourceUrl = 'https://zlm.cluster:4443/index/api/webrtc?app=rtsp_preview&stream=fixture&type=play&callId=a%2Bb'
const candidate = { udp: '192.0.2.10:30017', tcp: '192.0.2.10:32742' }

assert.equal(resolve(sourceUrl), sourceUrl)
const routed = new URL(resolve(sourceUrl, '/bus/apaas-zlmediakit', candidate))
assert.equal(routed.origin, 'https://console.example')
assert.equal(routed.pathname, '/bus/apaas-zlmediakit/index/api/webrtc')
for (const key of ['app', 'stream', 'type', 'callId']) {
  assert.equal(routed.searchParams.get(key), new URL(sourceUrl).searchParams.get(key))
}
assert.equal(routed.searchParams.get('cand_udp'), candidate.udp)
assert.equal(routed.searchParams.get('cand_tcp'), candidate.tcp)
assert.equal(new URL(resolve(sourceUrl, '', candidate)).host, 'zlm.cluster:4443')
assert.equal(new URL(resolve(sourceUrl, '', { udp: candidate.udp })).searchParams.has('cand_tcp'), false)
const replaced = new URL(resolve(sourceUrl + '&cand_udp=198.51.100.1:8000', '', candidate))
assert.deepEqual(replaced.searchParams.getAll('cand_udp'), [candidate.udp])
assert.equal(resolve('https://media.example/video.mp4', '/bus/zlm', candidate), 'https://media.example/video.mp4')
assert.equal(resolve(undefined, '/bus/zlm', candidate), undefined)
assert.equal(resolve(sourceUrl, '', { udp: ' ', tcp: '' }), sourceUrl)

for (const value of ['192.0.2.10:0', '192.0.2.10:65536', '192.0.2.10:08000', '256.0.2.10:30017',
  '192.00.2.10:30017', '0.0.0.0:30017', '224.0.0.1:30017', 'node.example:30017',
  'https://192.0.2.10:30017', '192.0.2.10:30017/path', '192.0.2.10:30017&other=1', '192.0.2.10:30\r017']) {
  assert.throws(() => resolve(sourceUrl, '/bus/zlm', { udp: value }), /UDP候选配置无效/)
  assert.throws(() => resolve(sourceUrl, '/bus/zlm', { tcp: value }), /TCP候选配置无效/)
}
for (const port of [1, 65535]) {
  assert.equal(new URL(resolve(sourceUrl, '', { udp: `192.0.2.10:${port}` })).searchParams.get('cand_udp'), `192.0.2.10:${port}`)
}
console.log('ZLM candidate routing, query preservation, defaults, protocol separation and invalid configuration checks passed')

// Verify actual Vite build-time environment handling using the real resolver source.
// Synthetic addresses only; no deployment files or network requests are touched.
const uiRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../VLStream-Web/VLStream-ui')
const { build } = await import(pathToFileURL(path.join(uiRoot, 'node_modules/vite/dist/node/index.js')))
const fixture = fs.mkdtempSync(path.join(tmpdir(), 'vls-zlm-build-config-'))
const keys = ['VITE_ZLM_API_BASE_URL', 'VITE_ZLM_RTC_CAND_TCP', 'VITE_ZLM_RTC_CAND_UDP', 'VITE_ZLM_RTC_LAN_HOST']
const previous = Object.fromEntries(keys.map(key => [key, process.env[key]]))
try {
  for (const key of keys) delete process.env[key]
  fs.writeFileSync(path.join(fixture, 'entry.js'), fs.readFileSync(path.join(uiRoot, 'src/utils/zlmPlaybackUrl.js')))
  fs.writeFileSync(path.join(fixture, '.env.production'), 'VITE_ZLM_API_BASE_URL=/existing-zlm\n')
  fs.writeFileSync(path.join(fixture, '.env.test'), 'VITE_ZLM_RTC_CAND_UDP=192.0.2.20:30017\n')
  async function compiled(mode) {
    const result = await build({ root: fixture, configFile: false, mode, logLevel: 'error',
      build: { write: false, minify: false, lib: { entry: path.join(fixture, 'entry.js'), formats: ['es'] } } })
    const code = (Array.isArray(result) ? result[0] : result).output.find(item => item.type === 'chunk').code
    return import('data:text/javascript;base64,' + Buffer.from(code).toString('base64'))
  }
  let output = await compiled('production')
  let url = new URL(output.resolveZlmPlaybackUrl(sourceUrl))
  assert.equal(url.pathname, '/existing-zlm/index/api/webrtc')
  assert.equal(url.searchParams.has('cand_tcp'), false)
  assert.equal(url.searchParams.has('cand_udp'), false)
  fs.writeFileSync(path.join(fixture, '.env.production.local'), 'VITE_ZLM_RTC_CAND_TCP=192.0.2.10:32742\n')
  output = await compiled('production')
  url = new URL(output.resolveZlmPlaybackUrl(sourceUrl))
  assert.equal(url.searchParams.get('cand_tcp'), '192.0.2.10:32742')
  assert.equal(url.searchParams.has('cand_udp'), false)
  for (const key of ['app', 'stream', 'type', 'callId']) assert.equal(url.searchParams.get(key), new URL(sourceUrl).searchParams.get(key))
  output = await compiled('test')
  url = new URL(output.resolveZlmPlaybackUrl(sourceUrl))
  assert.equal(url.searchParams.has('cand_tcp'), false, 'production.local must not leak into test mode')
  assert.equal(url.searchParams.get('cand_udp'), '192.0.2.20:30017')
  assert.equal(url.host, 'zlm.cluster:4443')
  process.env.VITE_ZLM_RTC_CAND_TCP = '192.0.2.30:32742'
  output = await compiled('production')
  url = new URL(output.resolveZlmPlaybackUrl(sourceUrl))
  assert.equal(url.searchParams.get('cand_tcp'), '192.0.2.30:32742', 'deployment job env must override production.local')
  console.log('Vite production defaults, production.local, test isolation and build-job precedence checks passed')

  // Reproduce a VM snapshot inheriting a cluster-specific production default.
  // Both file and parent-process candidates must be cleared only for build:vm.
  fs.writeFileSync(path.join(fixture, '.env.production'),
    'VITE_ZLM_API_BASE_URL=/cluster-zlm\nVITE_ZLM_RTC_CAND_TCP=192.0.2.10:32742\nVITE_ZLM_RTC_CAND_UDP=192.0.2.10:30017\n')
  fs.writeFileSync(path.join(fixture, '.env.production.local'),
    'VITE_ZLM_API_BASE_URL=/bus/zlm-server\nVITE_ZLM_RTC_LAN_HOST=192.0.2.200\n')
  const clusterEnvironment = { ...process.env, VITE_ZLM_RTC_CAND_UDP: '192.0.2.30:30017' }
  const vmEnvironment = vmBuildEnvironment(clusterEnvironment)
  assert.equal(clusterEnvironment.VITE_ZLM_RTC_CAND_TCP, '192.0.2.30:32742')
  assert.equal(vmEnvironment.VITE_ZLM_API_BASE_URL, clusterEnvironment.VITE_ZLM_API_BASE_URL)
  for (const key of ['VITE_ZLM_RTC_CAND_TCP', 'VITE_ZLM_RTC_CAND_UDP']) process.env[key] = vmEnvironment[key]
  output = await compiled('production')
  url = new URL(output.resolveZlmPlaybackUrl(sourceUrl))
  assert.equal(url.pathname, '/bus/zlm-server/index/api/webrtc')
  assert.equal(url.searchParams.has('cand_tcp'), false)
  assert.equal(url.searchParams.has('cand_udp'), false)
  const answer = output.addZlmLanCandidate('a=candidate:fixture 1 udp 120 198.51.100.1 8000 typ host\r\n')
  assert.ok(answer.includes('192.0.2.200 8000 typ host'))
  assert.ok(answer.includes('198.51.100.1 8000 typ host'))
  // The normal company production build remains able to use its explicit inputs.
  for (const key of ['VITE_ZLM_RTC_CAND_TCP', 'VITE_ZLM_RTC_CAND_UDP']) process.env[key] = clusterEnvironment[key]
  output = await compiled('production')
  url = new URL(output.resolveZlmPlaybackUrl(sourceUrl))
  assert.equal(url.searchParams.get('cand_tcp'), '192.0.2.30:32742')
  assert.equal(url.searchParams.get('cand_udp'), '192.0.2.30:30017')
  console.log('VM build isolation clears file/process candidates while preserving routes, LAN ports and company production inputs')
} finally {
  for (const key of keys) {
    if (previous[key] === undefined) delete process.env[key]
    else process.env[key] = previous[key]
  }
  // Keep this diagnostic fixture in OS temp; never delete a computed repository path.
}
