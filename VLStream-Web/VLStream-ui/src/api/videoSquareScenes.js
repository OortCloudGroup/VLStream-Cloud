import request, { applyAuthHeaders, applyPlatformGatewayHeaders, getBaseURL, handleBusinessError } from '@/utils/request'
import { readSceneUpdates } from '@/utils/videoSquareScenes.mjs'

const path = mode => `/vls/video-square/scenes/${mode}`

export function getVideoSquareScene(mode, options) {
    return request({ ...options, url: path(mode), method: 'get', silentError: true })
}

export function getVideoSquareSceneModels(options) {
    return request({ ...options, url: path('models'), method: 'get', silentError: true })
}

export function undoVideoSquareScene(mode, data, options) {
    return request({ ...options, url: path(mode) + '/undo', method: 'post', data, silentError: true })
}

export async function generateVideoSquareScene(mode, data, options, onPreview) {
    const config = { ...options, baseURL: getBaseURL(), url: path(mode), headers: { 'Content-Type': 'application/json' } }
    applyPlatformGatewayHeaders(config)
    applyAuthHeaders(config)
    const response = await fetch(config.baseURL.replace(/\/$/, '') + config.url, {
        method: 'POST', headers: config.headers, body: JSON.stringify(data), signal: config.signal,
    })
    if (!response.ok || !response.headers.get('content-type')?.includes('application/x-ndjson')) {
        const body = await response.json().catch(() => ({}))
        await handleBusinessError(body, { config, status: response.status, data: body }, '视图生成失败', false)
        throw new Error(body.message || body.msg || '视图生成服务未返回有效数据')
    }
    return readSceneUpdates(response.body, onPreview)
}
