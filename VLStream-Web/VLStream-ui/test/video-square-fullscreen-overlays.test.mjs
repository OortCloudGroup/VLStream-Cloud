import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { parse } from '@vue/compiler-sfc'
import { baseParse } from '@vue/compiler-dom'

const source = readFileSync(new URL('../src/views/VideoAggregation/VideoSquareRefactored.vue', import.meta.url), 'utf8')
const { descriptor, errors } = parse(source)
assert.deepEqual(errors, [])
const root = baseParse(descriptor.template.content)
const paths = []
function visit(node, ancestors = []) {
  if (node.type === 1) paths.push({ node, ancestors })
  for (const child of node.children || []) visit(child, [...ancestors, node])
}
visit(root)
const hasClass = (node, name) => node.props?.some(p => p.name === 'class' && p.value?.content.split(/\s+/).includes(name))

for (const className of ['extended-layout-overlay', 'square-custom-layout']) {
  test(`${className} stays in the native fullscreen subtree, outside the hidden map content`, () => {
    const matches = paths.filter(({ node }) => hasClass(node, className))
    assert.equal(matches.length, 1)
    assert.ok(matches[0].ancestors.some(node => hasClass(node, 'main-content')))
    assert.ok(!matches[0].ancestors.some(node => hasClass(node, 'map-area')))
  })
}
test('custom layout does not teleport to body outside the fullscreen top layer', () => {
  const { node } = paths.find(({ node }) => hasClass(node, 'square-custom-layout'))
  assert.ok(node.props.some(p => p.name === 'bind' && p.arg?.content === 'append-to-body' && p.exp?.content === 'false'))
})
