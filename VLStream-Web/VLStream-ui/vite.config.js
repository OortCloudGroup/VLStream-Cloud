import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import { createSvgIconsPlugin } from 'vite-plugin-svg-icons'

const vlsSrc = path.resolve(__dirname, 'src')

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const backendTarget = env.VITE_DEV_PROXY_TARGET || 'http://127.0.0.1:8080'
  const ssoTarget = env.VITE_SSO_PROXY_TARGET || backendTarget
  const apaasTarget = env.VITE_APAAS_PROXY_TARGET || 'http://oort.oortcloudsmart.com:21410'

  const backendProxy = {
    target: backendTarget,
    changeOrigin: true,
    secure: false
  }

  return {
    plugins: [
      vue(),
      createSvgIconsPlugin({
        iconDirs: [
          path.resolve(vlsSrc, 'assets/img/svg'),
          path.resolve(vlsSrc, 'assets/img/processui/svgs/VForm/svg'),
          path.resolve(vlsSrc, 'assets/img/processui/svgs'),
          path.resolve(vlsSrc, 'assets/img/unifi/svgs'),
          path.resolve(vlsSrc, 'assets/img/message/svgs'),
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
        '/sso': {
          target: ssoTarget,
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
