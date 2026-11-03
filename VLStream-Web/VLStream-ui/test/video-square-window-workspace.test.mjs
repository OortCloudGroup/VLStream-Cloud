import test from 'node:test'
import assert from 'node:assert/strict'
import { clampVideoWindow, uniformWindowSize, isWindowDragGesture, fitVideoPicture, packVideoWindows } from '../src/utils/videoSquareWindowWorkspace.mjs'
import { applyVideoPresentation } from '../src/utils/videoSquarePresentation.mjs'
import { resolveLayoutSpec, VIDEO_LAYOUT_OPTIONS } from '../src/utils/videoSquareLayout.mjs'
test('bottom keeps every mounted camera and grid restores without replacing identities', () => {
    const cameras = [{id:1}, {id:2}, null, {id:4}]
    const dialog = {cameras, layoutCount:4, activeWindowIndex:1}
    applyVideoPresentation(dialog, 'bottom')
    assert.equal(dialog.cameras, cameras)
    assert.equal(dialog.layoutCount, 4)
    applyVideoPresentation(dialog, 'grid')
    assert.equal(dialog.cameras, cameras)
})
test('drag stays reachable after every edge and narrow resize', () => {
    for (const bounds of [{width:900,height:600},{width:118,height:220},{width:320,height:180}]) {
        const size = {width:100,height:150}
        for (const position of [{x:-1000,y:-1000},{x:10000,y:10000}]) {
            const fitted = clampVideoWindow(position,size,bounds)
            assert.ok(fitted.x >= 0 && fitted.y >= 0)
            assert.ok(fitted.x + size.width <= bounds.width)
            assert.ok(fitted.y + size.height <= bounds.height)
        }
    }
})
test('a click jitter is distinct from dragging', () => {
    assert.equal(isWindowDragGesture({x:10,y:20},{clientX:12,clientY:22}), false)
    assert.equal(isWindowDragGesture({x:10,y:20},{clientX:16,clientY:20}), true)
})
test('picture fits its decoded ratio without crop or a forced tall canvas', () => {
    for (const ratio of [704 / 480,16 / 9,9 / 16]) {
        const picture = fitVideoPicture(300,600,ratio)
        assert.ok(Math.abs(picture.width / picture.height - ratio) < 1e-9)
        assert.ok(picture.width <= 300 && picture.height <= 600)
    }
    assert.ok(Math.abs(fitVideoPicture(300,600,704 / 480).height - 300 * 480 / 704) < 1e-9)
})

const near=(actual,expected)=>assert.ok(Math.abs(actual-expected)<1e-6,`${actual} != ${expected}`)
test('manual layouts keep every window inside the 16:9 canvas with the documented order', () => {
    for (const option of VIDEO_LAYOUT_OPTIONS) {
        const spec = resolveLayoutSpec(option.count, option.variant)
        assert.equal(spec.cells.length, option.count)
        const result = packVideoWindows({width:1600, height:900, ...spec})
        near(result.width, 1600)
        near(result.height, 900)
        for (const box of result.boxes) {
            assert.ok(box.x >= 0 && box.y >= 0)
            assert.ok(box.x + box.width <= result.width + 1e-6)
            assert.ok(box.y + box.height <= result.height + 1e-6)
            assert.ok(box.width > 0 && box.height > 0)
        }
    }
})
test('three-window defaults center the single top window and split the bottom row', () => {
    for (const variant of ['grid', 'three-top']) {
        const result = packVideoWindows({width:1600, height:900, ...resolveLayoutSpec(3, variant)})
        near(result.boxes[0].x, 400)
        near(result.boxes[0].width, 800)
        near(result.boxes[0].height, 450)
        near(result.boxes[1].y, 450)
        near(result.boxes[1].width, 800)
        near(result.boxes[2].x, 800)
    }
    const left = packVideoWindows({width:1600, height:900, ...resolveLayoutSpec(3, 'three-left')})
    near(left.boxes[0].y, 225)
    near(left.boxes[0].width, 800)
    near(left.boxes[1].x, 800)
})
test('the one-big-one-small variant overlays the second window at the bottom right', () => {
    const result = packVideoWindows({width:1600, height:900, ...resolveLayoutSpec(2, 'two-overlay')})
    near(result.boxes[0].width, 1600)
    near(result.boxes[0].height, 900)
    near(result.boxes[1].x, 1600 * 2 / 3)
    near(result.boxes[1].y, 900 * 2 / 3)
    near(result.boxes[1].width, 1600 / 3)
})
test('five, six and eight keep the big window and the documented bottom row order', () => {
    const five = packVideoWindows({width:1600, height:900, ...resolveLayoutSpec(5)})
    near(five.boxes[0].x, 200)
    near(five.boxes[0].width, 1200)
    near(five.boxes[0].height, 675)
    near(five.boxes[1].y, 675)
    near(five.boxes[1].x, 0)
    near(five.boxes[4].x, 1200)
    const six = packVideoWindows({width:1600, height:900, ...resolveLayoutSpec(6)})
    near(six.boxes[5].x, 0)
    near(six.boxes[4].x, 1600 / 3)
    near(six.boxes[3].x, 1600 * 2 / 3)
    near(six.boxes[5].y, 600)
    const eight = packVideoWindows({width:1600, height:900, ...resolveLayoutSpec(8)})
    near(eight.boxes[7].x, 0)
    near(eight.boxes[4].x, 1200)
    near(eight.boxes[4].y, 675)
})
test('twenty-three follows the design glyph: two big left blocks, a 3x5 right block and a bottom row', () => {
    const spec = resolveLayoutSpec(23)
    assert.equal(spec.cells.length, 23)
    const result = packVideoWindows({width:1600, height:900, ...spec})
    near(result.boxes[0].width, 800)
    near(result.boxes[0].height, 450)
    near(result.boxes[10].y, 450)
    near(result.boxes[10].width, 800)
    near(result.boxes[10].height, 300)
    near(result.boxes[1].x, 800)
    near(result.boxes[3].x, result.boxes[1].x + result.boxes[1].width * 2)
    near(result.boxes[17].y, 750)
    near(result.boxes[22].x + result.boxes[22].width, 1600)
    for (const box of result.boxes) {
        assert.ok(box.width > 0 && box.height > 0)
        assert.ok(box.x + box.width <= result.width + 1e-6 && box.y + box.height <= result.height + 1e-6)
    }
    for (let i = 0; i < 23; i++) for (let j = i + 1; j < 23; j++) {
        const a = result.boxes[i], b = result.boxes[j]
        const x = Math.min(a.x + a.width, b.x + b.width) - Math.max(a.x, b.x)
        const y = Math.min(a.y + a.height, b.y + b.height) - Math.max(a.y, b.y)
        assert.ok(x < 1e-6 || y < 1e-6, `windows ${i}/${j} overlap`)
    }
})
test('every count from one to twenty-five has a defined layout spec', () => {
    for (let count = 1; count <= 25; count++) {
        assert.equal(resolveLayoutSpec(count).cells.length, count)
    }
    assert.equal(VIDEO_LAYOUT_OPTIONS.length, 27)
    assert.deepEqual(VIDEO_LAYOUT_OPTIONS.slice(-4).map(option => option.label), ['二十二画面', '二十三画面', '二十四画面', '二十五画面'])
})
test('bottom windows keep one 4:3 cell size so fewer than four stay left aligned',()=>{
    const size=uniformWindowSize(1600,900)
    near(size.width,400)
    near(size.height,300*1.15)
    assert.ok(size.width*4<=1600+1e-6)
    const short=uniformWindowSize(1600,140)
    assert.ok(short.height<=140)
    near(short.width/short.height,4/3/1.15)
    assert.ok(short.width<=1600)
    const narrow=uniformWindowSize(300,900)
    near(narrow.width,220)
})
