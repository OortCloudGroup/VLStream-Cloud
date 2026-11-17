import assert from 'node:assert/strict'
import fs from 'node:fs/promises'
import { parse, compileScript, compileTemplate } from '@vue/compiler-sfc'
const file = 'src/components/FeaturedPublishActions.vue'
const source = (await fs.readFile(file, 'utf8')).replace(/\r\n/g, '\n')
const { descriptor, errors } = parse(source)
assert.deepEqual(errors, [])
compileScript(descriptor, { id: file })
assert.deepEqual(compileTemplate({ source: descriptor.template.content, filename: file, id: file }).errors, [])
const template = descriptor.template.content
assert(template.includes("@click=\"setFeatured(state?.featured !== true)\""))
assert(template.includes("@click=\"open\">{{ $tp(listLabel) }}"))
assert(template.includes('StarFilled : Star'))
assert(template.includes('发布到开发广场'))
assert(!template.includes('精选与发布'))
assert(!template.includes('<el-switch'))
assert(!source.includes('state.destination'))
assert(!source.includes('state.value?.destination'))
assert(source.includes('const canPublish = computed(() => uploadAllowed(state.value))'))
assert(!template.includes('127.0.0.1'))
assert(!template.includes('Model Hub'))
const {harness}=await import('./publication-component-harness.mjs')
for(const kind of ['model','dataset']){
 const h=await harness();h.props.kind=kind
 await h.setFeatured(true);assert.equal(h.state.value.featured,true)
 await h.setFeatured(false);assert.equal(h.state.value.featured,false)
 assert.equal(h.calls.star,2);assert.equal(h.calls.publish,0);assert.equal(h.calls.reconcile,0)
 h.destroy()
}
for(const name of ['AlgorithmModel.vue','AlgorithmStandard.vue','DataManagement.vue']){
 const page=await fs.readFile('src/views/AlgorithmTraining/'+name,'utf8')
 assert(!page.includes(":label=\"$tp('Model Hub')\""));assert(page.includes('<FeaturedPublishActions'))
}
assert(source.includes('watch([() => props.kind, () => props.id]'))
assert(source.includes('{ immediate: true }'))
console.log('Featured/publication separation, local-only toggles, no internal address, Vue compilation passed')
