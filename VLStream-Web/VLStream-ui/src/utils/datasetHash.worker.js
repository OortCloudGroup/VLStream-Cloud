import CryptoJS from 'crypto-js'

self.onmessage = async ({ data: file }) => {
  try {
    const hash = CryptoJS.algo.SHA256.create()
    const chunkSize = 8 * 1024 * 1024
    for (let offset = 0; offset < file.size; offset += chunkSize) {
      const bytes = new Uint8Array(await file.slice(offset, offset + chunkSize).arrayBuffer())
      hash.update(CryptoJS.lib.WordArray.create(bytes))
      self.postMessage({ progress: Math.min(file.size, offset + chunkSize) / file.size })
    }
    self.postMessage({ sha256: hash.finalize().toString() })
  } catch (error) { self.postMessage({ error: error.message || '文件校验失败' }) }
}
