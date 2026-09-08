/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import { spawn } from 'node:child_process'
import { createSvgIconsPlugin } from 'vite-plugin-svg-icons'

const vlsSrc = path.resolve(__dirname, 'src')

const requestCameraSnapshot = (streamUrl, snapshotPath) => new Promise((resolve, reject) => {
  const cameraHost = streamUrl.port === '554' ? streamUrl.hostname : streamUrl.host
  const cameraUrl = new URL(`http://${cameraHost}`)
  cameraUrl.pathname = snapshotPath

  const command = process.platform === 'win32' ? 'curl.exe' : 'curl'
  const args = [
    '--silent',
    '--show-error',
    '--fail',
    '--digest',
    '--connect-timeout', '5',
    '--max-time', '8',
    '-u', `${decodeURIComponent(streamUrl.username)}:${decodeURIComponent(streamUrl.password)}`,
    cameraUrl.toString()
  ]
  const processHandle = spawn(command, args, { windowsHide: true })
  const chunks = []
  let errorOutput = ''

  processHandle.stdout.on('data', (chunk) => chunks.push(chunk))
  processHandle.stderr.on('data', (chunk) => { errorOutput += chunk.toString() })
  processHandle.on('error', reject)
  processHandle.on('close', (code) => {
    if (code === 0 && chunks.length) {
      resolve(Buffer.concat(chunks))
    } else {
      reject(new Error(errorOutput.trim() || 'Camera snapshot request failed'))
    }
  })
})

const createCameraPreviewPlugin = () => ({
  name: 'camera-preview-fallback',
  configureServer(server) {
    server.middlewares.use('/__camera-preview', async (req, res) => {
      try {
        const requestUrl = new URL(req.url, 'http://localhost')
        const source = requestUrl.searchParams.get('stream')
        if (!source) throw new Error('Missing camera stream')

        const streamUrl = new URL(source)
        if (streamUrl.protocol !== 'rtsp:' || !streamUrl.username || !streamUrl.password) {
          throw new Error('Invalid camera stream')
        }

        const channelMatch = streamUrl.pathname.match(/\/Streaming\/Channels\/(\d+)/i)
        if (!channelMatch) throw new Error('Unsupported camera stream path')

        const channelId = channelMatch[1].length === 1 ? `${channelMatch[1]}01` : channelMatch[1]
        const snapshotPath = `/ISAPI/Streaming/channels/${channelId}/picture`
        const snapshot = await requestCameraSnapshot(streamUrl, snapshotPath)

        res.writeHead(200, {
          'Content-Type': 'image/jpeg',
          'Cache-Control': 'no-store'
        })
        res.end(snapshot)
      } catch (error) {
        res.statusCode = 502
        res.end(error.message)
      }
    })
  }
})

export default defineConfig(async ({ mode }) => {
  const { default: AutoImport } = await import('unplugin-auto-import/vite')
  const env = loadEnv(mode, process.cwd(), '')
  const backendTarget = env.VITE_DEV_PROXY_TARGET || 'http://127.0.0.1:8080'
  const ssoTarget = env.VITE_SSO_PROXY_TARGET || backendTarget
  const apaasTarget = env.VITE_APAAS_PROXY_TARGET || 'http://oort.oortcloudsmart.com:21410'
  const platformTarget = env.VITE_PLATFORM_PROXY_TARGET || (() => {
    try {
      return new URL(env.VITE_PLATFORM_LOGIN_URL).origin
    } catch (error) {
      return 'https://workup.oortcloudsmart.com:2443'
    }
  })()
  const webRtcTarget = env.VITE_WEBRTC_PROXY_TARGET || 'http://127.0.0.1:8000'
  const cameraRtcTarget = env.VITE_CAMERA_RTC_PROXY_TARGET || 'http://146.56.220.167:8082'
  const wvpTarget = env.VITE_WVP_PROXY_TARGET || 'http://127.0.0.1:9080'

  const backendProxy = {
    target: backendTarget,
    changeOrigin: true,
    secure: false
  }

  return {
    plugins: [
      vue(),
      AutoImport({
        imports: ['vue', 'vue-router'],
        dts: false
      }),
      createCameraPreviewPlugin(),
      createSvgIconsPlugin({
        iconDirs: [
          path.resolve(vlsSrc, 'assets/img/svg'),
          path.resolve(vlsSrc, 'assets/img/processui/svgs/VForm/svg'),
          path.resolve(vlsSrc, 'assets/img/processui/svgs'),
          path.resolve(vlsSrc, 'assets/img/unifi/svgs'),
          path.resolve(vlsSrc, 'assets/img/message/svgs'),
          path.resolve(vlsSrc, 'assets/wvp/icons'),
        ],
        symbolId: 'icon-[dir]-[name]'
      })
    ],
    base: '/bus/vls-ui/',
    resolve: {
      alias: [
        { find: '@/pages/events/views', replacement: path.resolve(vlsSrc, 'views/events') },
        { find: '@/config/AppConfig', replacement: path.resolve(vlsSrc, 'config/AppConfig.js') },
        { find: '@/utils/apaasApiBase', replacement: path.resolve(vlsSrc, 'utils/apaasApiBase.js') },
        { find: '@', replacement: vlsSrc },
        { find: '~@', replacement: path.resolve(vlsSrc, 'components/VForm') }
      ],
      extensions: ['.js', '.vue', '.json', '.ts', '.tsx']
    },
    server: {
      port: 3000,
      host: '0.0.0.0',
      proxy: {
        '/wvp-api': {
          target: wvpTarget,
          changeOrigin: true,
          secure: false,
          rewrite: (path) => path.replace(/^\/wvp-api/, '')
        },
        '/system': backendProxy,
        '/blade-auth': backendProxy,
        '/blade-system': backendProxy,
        '/workflow': backendProxy,
        '/wf': backendProxy,
        '/workorder': backendProxy,
        '/WorkOrder': backendProxy,
        '/model': backendProxy,
        '/work': backendProxy,
        '/rule': backendProxy,
        '/task': backendProxy,
        '/auth': backendProxy,
        '/blade-job': backendProxy,
        '/vls': backendProxy,
        '/vlsTagManagement': backendProxy,
        '/vlsAlgorithmTraining': backendProxy,
        '/vlsAlgorithmModel': backendProxy,
        '/vlsAlgorithmManagement': backendProxy,
        '/vlsDevice': backendProxy,
        '/video-record': backendProxy,
        '/api': backendProxy,
        // WebRTC-streamer service , need to .
        '/bus/webrtc-streamer-server': {
          target: webRtcTarget,
          changeOrigin: true,
          secure: false,
          rewrite: (path) => path.replace(/^\/bus\/webrtc-streamer-server/, '')
        },
        '/bus/camera-rtc': {
          target: cameraRtcTarget,
          changeOrigin: true,
          secure: false,
          ws: true,
          rewrite: (path) => path.replace(/^\/bus\/camera-rtc/, '')
        },
        '/sso': {
          target: ssoTarget,
          changeOrigin: true,
          secure: false
        },
        // Model Hub / OortToolKit SSO（workup-dev）
        '/bus/apaas-sso': {
          target: platformTarget,
          changeOrigin: true,
          secure: false
        },
        // and .
        '/bus/apaas-admin-platform': {
          target: platformTarget,
          changeOrigin: true,
          secure: false
        },
        '/bus/apaas-unified-msg': {
          target: platformTarget,
          changeOrigin: true,
          secure: false
        },
        '/bus/apaas-user': {
          target: platformTarget,
          changeOrigin: true,
          secure: false
        },
        // OortCloud New API 、 and interface
        '/bus/apaas-newapi': {
          target: platformTarget,
          changeOrigin: true,
          secure: false
        },
        // Model Hub fastdfs
        '/bus/apaas-fastdfsservice': {
          target: platformTarget,
          changeOrigin: true,
          secure: false
        },
        // AI model ( model)
        '/bus/apaas-deployment-service': {
          target: platformTarget,
          changeOrigin: true,
          secure: false
        },
        '/oort': {
          target: apaasTarget,
          changeOrigin: true,
          secure: false
        }
      }
    }
  }
})
