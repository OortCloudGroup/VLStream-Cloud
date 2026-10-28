import test from 'node:test'
import assert from 'node:assert/strict'
import { clampVideoWindow, uniformWindowSize, isWindowDragGesture, fitVideoPicture, packVideoWindows } from '../src/utils/videoSquareWindowWorkspace.mjs'
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
test('mixed-aspect rows pack actual picture edges together and scale both dimensions when height is limited', () => {
    const result=packVideoWindows({width:1300,height:711,count:4,aspects:[704/480,16/9]})
    near(result.boxes[0].x+result.boxes[0].width,result.boxes[1].x)
    near(result.boxes[0].y+result.boxes[0].height,result.boxes[2].y)
    assert.ok(result.width<1300)
    near(result.height,711)
    near(result.boxes[0].width/result.boxes[0].height,704/480)
    near(result.boxes[1].width/result.boxes[1].height,16/9)
})
test('every layout preserves complete picture ratios, reachable boxes and zero adjacent seams across sizes', () => {
    for (const width of [118,320,910,1600]) for (const height of [180,600,1200]) {
        for (const count of [1,2,3,4,5,6,8,9,16,25]) for(const variant of ['grid',...(count===2?['two-overlay']:count===3?['three-top','three-left']:[])]) {
            const aspects=Array.from({length:count},(_,i)=>[704/480,16/9,9/16][i%3])
            const insets=Array.from({length:count},()=>count===1?32:0)
            const result=packVideoWindows({width,height,count,variant,aspects,insets})
            assert.equal(result.boxes.length,count)
            assert.ok(result.width<=width+1e-6 && result.height<=height+1e-6)
            for(const [index,box]of result.boxes.entries()) {
                assert.ok(box.x>=-1e-6 && box.y>=-1e-6 && box.width>0 && box.height>0)
                assert.ok(box.x+box.width<=result.width+1e-6 && box.y+box.height<=result.height+1e-6)
                near(box.width/(box.height-insets[index]),aspects[index])
            }
            if(variant!=='two-overlay')for(let i=0;i<count;i++)for(let j=i+1;j<count;j++) {
                const a=result.boxes[i],b=result.boxes[j]
                const x=Math.min(a.x+a.width,b.x+b.width)-Math.max(a.x,b.x)
                const y=Math.min(a.y+a.height,b.y+b.height)-Math.max(a.y,b.y)
                assert.ok(x<1e-6 || y<1e-6,'automatic slots overlap')
            }
        }
    }
})
test('large-main six/eight and the three-window variants keep touching side and bottom blocks',()=>{
    for(const count of [6,8]) {
        const result=packVideoWindows({width:1200,height:700,count,aspects:[704/480,16/9]})
        near(result.boxes[0].width,result.boxes[1].x)
        near(result.boxes[1].height,result.boxes[2].y)
        const bottom=count===6?4:5
        near(result.boxes[0].height,result.boxes[bottom].y)
        near(result.boxes[bottom].x+result.boxes[bottom].width,result.boxes[bottom+1].x)
    }
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
