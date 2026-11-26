import assert from 'node:assert/strict'
import test from 'node:test'
import { authorizedAliases, draftDocument, excerpt, liveDocument, publicCameras, sampleBindings, sourceLine, usesThree, visibleSlot }
    from '../src/generated-page/host.mjs'

const page = '<!doctype html>\n<html lang="zh">\n<head>\n<title>园区</title>\n</head>\n<body>\n<canvas id="c"></canvas>\n<script>\nnew THREE.Scene()\n</script>\n</body>\n</html>'

test('the live page gets the CSP and host scripts before any generated code', () => {
    const built = liveDocument(page, { runtime: 'const end = "</script>"\nrun()', three: 'window.THREE={}', cameras: [{ id: 'c1', name: '</script><b>' }], theme: 'dark' })
    const csp = built.html.indexOf('Content-Security-Policy'), title = built.html.indexOf('<title>')
    assert.ok(csp > built.html.indexOf('<head>') && csp < title, 'policy must precede generated head content')
    for (const rule of ["connect-src 'none'", "frame-src 'none'", "form-action 'none'", "base-uri 'none'", "default-src 'none'"]) assert.ok(built.html.includes(rule), rule)
    assert.ok(built.html.includes('window.THREE={}'), 'pages that use THREE receive the bundled library')
    assert.equal((built.html.match(/<\/script>/gi) || []).length, 4, 'injected script text cannot close its element early')
    assert.ok(built.html.includes('\\u003c/script>\\u003cb>'), 'camera names are inert JSON')
    assert.ok(built.html.includes('"theme":"dark"'))
    assert.equal(liveDocument('<html><head></head><body><p>静态</p></body></html>', { runtime: 'r()', three: 'THREE_LIB' }).html.includes('THREE_LIB'), false)
    assert.equal(usesThree('const THREEx = 1'), false)
})

test('a page cannot redirect itself with a refresh meta tag', () => {
    const built = liveDocument('<html><head><meta http-equiv="refresh" content="0;url=https://example.com"></head><body>x</body></html>', { runtime: '' })
    assert.equal(/http-equiv="refresh"/i.test(built.html), false)
})

test('reported lines map back to the saved source the model edits', () => {
    const built = liveDocument(page, { runtime: 'a()\nb()\nc()', three: 'three()\nmore()' })
    const scriptLine = page.split('\n').indexOf('new THREE.Scene()') + 1
    assert.equal(sourceLine(scriptLine + built.lines.shift, built.lines), scriptLine)
    assert.equal(sourceLine(2, built.lines), 2)
    assert.equal(sourceLine(built.lines.at + 1, built.lines), 0, 'host script lines are not attributed to the page')
    assert.equal(excerpt(page, scriptLine), '<script>\nnew THREE.Scene()\n</script>')
    assert.equal(built.html.split('\n')[scriptLine - 1 + built.lines.shift], 'new THREE.Scene()')
})

test('drafts only run the nonced bootstrap', () => {
    const html = draftDocument('const x = "</script>"', 'abc123', 'dark')
    assert.match(html, /script-src 'nonce-abc123'/)
    assert.doesNotMatch(html, /unsafe-inline'[^;]*script|script-src[^;]*unsafe/)
    assert.match(html, /data-vls-theme="dark"/)
    assert.equal(Array.from(html.matchAll(/<\/script>/g)).length, 1)
})

test('pages only receive names, statuses and aliases still present in the viewer catalog', () => {
    const bindings = { c1: 'front-door', c2: 'removed', c3: 'garage', unknown: 'garage' }
    const aliases = authorizedAliases(bindings, [
        { id: 'front-door', deviceName: '入口', protocols: ['RTSP'], status: 'ONLINE' },
        { id: 'garage', deviceName: 'garage', status: 'weird' },
    ])
    assert.deepEqual(publicCameras(aliases), [{ id: 'c1', name: '入口', status: 'ONLINE' }, { id: 'c3', name: 'c3', status: 'UNKNOWN' }])
    assert.equal(aliases.get('c1').id, 'front-door')
    assert.equal(aliases.has('c2'), false)
})

test('built-in pages bind online cameras first', () => {
    const devices = [{ id: 'a', status: 'OFFLINE' }, { id: 'b', status: 'ONLINE' }, { id: 'c' }, { id: 'd', status: 'ONLINE' }]
    assert.deepEqual(sampleBindings(devices, 3), { c1: 'b', c2: 'd', c3: 'c' })
    assert.deepEqual(sampleBindings([]), {})
})

test('the real player stays inside the visible page and has a fallback for unusable slots', () => {
    assert.deepEqual(visibleSlot({ x: -20, y: 70, width: 240, height: 280 }, 420, 300), {
        left: '0px', top: '70px', width: '220px', height: '230px',
    })
    assert.equal(visibleSlot({ x: 405, y: 20, width: 240, height: 120 }, 420, 300), null)
    assert.equal(visibleSlot({ x: NaN, y: 20, width: 240, height: 120 }, 420, 300), null)
    assert.equal(visibleSlot(null, 420, 300), null)
})
