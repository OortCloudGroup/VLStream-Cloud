import { build } from 'esbuild'
import { fileURLToPath } from 'node:url'
import { normalizePath } from 'vite'

// The opaque frame cannot import from the application's origin. Bundle the page bootstrap, the
// Three.js kit and the draft renderer as local source strings, including their libraries.
export function generatedPageRuntime() {
    const id = 'virtual:vls-generated-page-runtime'
    const runtimeDir = fileURLToPath(new URL('../src/generated-page/', import.meta.url))
    return {
        name: 'vls-generated-page-runtime',
        resolveId(source) { if (source === id) return '\0' + id },
        handleHotUpdate({ file, server }) {
            if (!normalizePath(file).startsWith(normalizePath(runtimeDir))) return
            const module = server.moduleGraph.getModuleById('\0' + id)
            if (module) return [module]
        },
        async load(source) {
            if (source !== '\0' + id) return
            const entries = { runtime: 'runtime', three: 'three-kit', draft: 'draft' }
            const bundles = await Promise.all(Object.entries(entries).map(async ([name, file]) => {
                const entry = runtimeDir + file + '.mjs'
                this.addWatchFile(entry)
                const result = await build({ entryPoints: [entry], bundle: true, platform: 'browser',
                    format: 'iife', target: 'es2020', minify: true, write: false, legalComments: 'eof' })
                return 'export const ' + name + 'Source=' + JSON.stringify(result.outputFiles[0].text)
            }))
            return bundles.join('\n')
        },
    }
}
