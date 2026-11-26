const scriptLoads = new Map()

function loadScript(path, getExport) {
  const loaded = getExport()
  if (loaded) return Promise.resolve(loaded)
  if (scriptLoads.has(path)) return scriptLoads.get(path)

  const promise = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = `${import.meta.env.BASE_URL}${path}`
    const fail = () => {
      script.remove()
      reject(new Error(`Failed to load ${path}`))
    }
    script.onload = () => {
      const exported = getExport()
      if (exported) resolve(exported)
      else fail()
    }
    script.onerror = fail
    document.head.appendChild(script)
  }).catch(error => {
    scriptLoads.delete(path)
    throw error
  })
  scriptLoads.set(path, promise)
  return promise
}

const validConstructor = value => typeof value === 'function' ? value : undefined
export const loadJessibuca = () => loadScript('jessibuca/jessibuca.js', () =>
  validConstructor(window.Jessibuca) || validConstructor(window.jessibuca))

let h265Ready
export function loadH265Web() {
  if (!h265Ready) {
    h265Ready = (async () => {
      const Player = await loadScript('h265web/h265webjs-v20221106.js', () => validConstructor(window.new265webjs))
      await loadScript('h265web/missile.js', () => validConstructor(window.Module?.cwrap))
      // A classic script load completes before the bundled Emscripten runtime finishes its WASM initialization.
      if (!window.runtimeInitialized) {
        await new Promise((resolve, reject) => {
          const deadline = Date.now() + 15_000
          const timer = setInterval(() => {
            if (!window.runtimeInitialized && Date.now() < deadline) return
            clearInterval(timer)
            if (window.runtimeInitialized) resolve()
            else reject(new Error('H265 decoder did not initialize'))
          }, 50)
        })
      }
      return Player
    })().catch(error => {
      h265Ready = undefined
      throw error
    })
  }
  return h265Ready
}
