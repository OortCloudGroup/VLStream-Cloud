import assert from 'node:assert/strict'
import { test } from 'node:test'
import { createEventVideoLoader, eventVideoSources, hasEventVideoReferences, releaseEventVideo } from './eventVideos.mjs'

const deferred = () => {
    let resolve, reject
    const promise = new Promise((yes, no) => { resolve = yes; reject = no })
    return { promise, resolve, reject }
}
const detail = (id, url) => ({ code: 200, data: { id, video: [url], videoUrl: url, videoMediaId: `media-${id}` } })

test('multiple media preserve order and only the actual first source owns the exposed media ID', () => {
    const first = 'https://storage.test/a.mp4?signature=one'
    const second = 'https://storage.test/b.mp4?signature=two'
    const videos = eventVideoSources({ video: [first, second, first], videoUrl: first, videoMediaId: 'first' })
    assert.deepEqual(videos.map(video => video.url), [first, second])
    assert.equal(videos[0].videoMediaId, 'first')
    assert.equal(videos[1].videoMediaId, undefined)
    assert.deepEqual(eventVideoSources({ video: [], videoUrl: first }), [])
    assert.equal(hasEventVideoReferences({ video: [], videoUrl: first }), false)
})

test('invalid/unresolved URLs cannot shift the first video ID onto a later video', () => {
    const url = 'https://storage.test/b.mp4'
    const videos = eventVideoSources({ video: ['vls-media:unresolved', url, 'javascript:alert(1)', 'https://user:pass@storage.test/a.mp4'], videoUrl: null, videoMediaId: 'first' })
    assert.equal(videos.length, 1)
    assert.equal(videos[0].url, url)
    assert.equal(videos[0].videoMediaId, undefined)
    assert.deepEqual(eventVideoSources(null), [])
    assert.equal(hasEventVideoReferences({ video: ['vls-media:unresolved'] }), true)
    assert.equal(hasEventVideoReferences({ videoMediaId: 'refreshable' }), true)
    assert.equal(hasEventVideoReferences(null), false)
})

test('switching events ignores a late success from the previous event', async () => {
    const a = deferred(), b = deferred(), states = []
    const loader = createEventVideoLoader(({ id }) => id === 'a' ? a.promise : b.promise, () => 'tenant:user', state => states.push(state))
    const loadingA = loader.load('a'), loadingB = loader.load('b')
    b.resolve(detail('b', 'https://storage.test/b.mp4'))
    await loadingB
    a.resolve(detail('a', 'https://storage.test/a.mp4'))
    await loadingA
    assert.equal(states.at(-1).eventId, 'b')
    assert.equal(states.at(-1).videos[0].url, 'https://storage.test/b.mp4')
    assert.equal(states.filter(state => state.status === 'ready').length, 1)
})

test('closing and identity changes invalidate late responses', async () => {
    for (const changeIdentity of [false, true]) {
        const pending = deferred(), states = []
        let identity = 'tenant-a:user'
        const loader = createEventVideoLoader(() => pending.promise, () => identity, state => states.push(state))
        const loading = loader.load('a')
        if (changeIdentity) identity = 'tenant-b:user'
        loader.invalidate()
        pending.resolve(detail('a', 'https://storage.test/a.mp4'))
        await loading
        assert.deepEqual(states.at(-1), { status: 'idle', eventId: '', videos: [] })
        assert.equal(states.filter(state => state.status === 'ready').length, 0)
    }
})

test('each reopen renews signed URLs and rejected refresh clears the old media', async () => {
    const responses = [detail('a', 'https://storage.test/a.mp4?sig=old'), detail('a', 'https://storage.test/a.mp4?sig=new'), { code: 403 }]
    const states = [], ids = []
    const loader = createEventVideoLoader(({ id }) => { ids.push(id); return Promise.resolve(responses.shift()) }, () => 'identity', state => states.push(state))
    await loader.load('a')
    loader.invalidate()
    await loader.load('a')
    assert.equal(states.at(-1).videos[0].url, 'https://storage.test/a.mp4?sig=new')
    await loader.load('a')
    assert.deepEqual(states.at(-1), { status: 'error', eventId: 'a', videos: [] })
    assert.deepEqual(ids, ['a', 'a', 'a'])
})

test('mismatched detail identity and network failure never reveal media', async () => {
    for (const fetch of [() => Promise.resolve(detail('other', 'https://storage.test/other.mp4')), () => Promise.reject(new Error('network'))]) {
        const states = []
        const loader = createEventVideoLoader(fetch, () => 'identity', state => states.push(state))
        await loader.load('a')
        assert.deepEqual(states.at(-1), { status: 'error', eventId: 'a', videos: [] })
    }
})

test('an unresolved stored video is retryable and is not reported as an empty event', async () => {
    const states = []
    const loader = createEventVideoLoader(() => Promise.resolve({ code: 200, data: { id: 'a', video: ['vls-media:unavailable'], videoMediaId: 'unavailable' } }), () => 'identity', state => states.push(state))
    await loader.load('a')
    assert.deepEqual(states.at(-1), { status: 'error', eventId: 'a', videos: [] })
})

test('release pauses before removing the source and resets the media element', () => {
    const actions = []
    const video = {
        srcObject: { old: true },
        pause: () => actions.push('pause'),
        removeAttribute: name => actions.push(`remove:${name}`),
        load: () => actions.push('load')
    }
    releaseEventVideo(video)
    assert.deepEqual(actions, ['pause', 'remove:src', 'load'])
    assert.equal(video.srcObject, null)
    releaseEventVideo(null)
})
