import test from 'node:test'
import assert from 'node:assert/strict'
import { carouselWindowSize, clampVideoWindow, isWindowDragGesture, fitVideoPicture } from '../src/utils/videoSquareWindowWorkspace.mjs'
import { applyVideoPresentation } from '../src/utils/videoSquarePresentation.mjs'
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
        const size = carouselWindowSize(bounds.width,bounds.height)
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
