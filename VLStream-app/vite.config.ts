/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 11:07:04
 * @Last Modified by: 兰舰
 * @Last Modified time: 2025-05-09 11:48:30
* @Copyright aPaaS-front-team. All rights reserved.
*/
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import VueSetupExtend from 'vite-plugin-vue-setup-extend'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'

import { createSvgIconsPlugin } from 'vite-plugin-svg-icons'
import vueJsx from '@vitejs/plugin-vue-jsx'
// @ts-ignore
import path, { resolve } from 'path'
import postcsspxtoviewport from 'postcss-px-to-viewport'
import { createHtmlPlugin } from 'vite-plugin-html'
import { visualizer } from 'rollup-plugin-visualizer'
// import OptimizationPersist from 'vite-plugin-optimize-persist'
// import PkgConfig from 'vite-plugin-package-config'
import pageConfig from './src/config/pageConfig.ts'
import copy from 'rollup-plugin-copy'
import legacy from '@vitejs/plugin-legacy'

export default defineConfig(({ command, mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  let proDir = env.VITE_PROJECT_DIR
  // 如果是编译生产
  if (env.NODE_BUILDTARGET) {
    proDir = env.NODE_BUILDTARGET
  }
  console.log('---------------------------------------------------------------')
  console.log('--------------------当前运行项目---', path.resolve(__dirname, './src/' + proDir))
  console.log('---------------------------------------------------------------')
  const projectPath = 'pages/' + proDir

  if (command === 'serve') {
    // dev 独有配置
  } else {
    // command === 'build'
    // build 独有配置
  }

  // 扩展的js
  const extendsJS = {
    ...{
      extendJSLib1: '',
      extendJSLib2: '',
      extendJSLib3: '',
      extendJSLib4: '',
      extendJSLib5: '',
      lib: []
    },
    ...pageConfig[proDir]
  }
  console.log('扩展的库', extendsJS)

  // 替换 'src' 和 'dist' 路径与实际路径一致
  const minJsFilePath = 'lib/'
  const distPath = 'lib'

  let copyTarget = []

  if (extendsJS.lib.length > 0) {
    console.log('复制的本地lib', extendsJS)
    extendsJS.lib.map(item => {
      copyTarget.push({ src: minJsFilePath + item, dest: distPath })
    })
  }
  console.log(copyTarget)

  console.log('---------------', '/src/pages/' + proDir + '/main.ts')

  return {
    define: {
      // enable hydration mismatch details in production build
      __VUE_PROD_HYDRATION_MISMATCH_DETAILS__: 'true'
    },
    public: '/',
    base: './',
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src'),
        '@@': path.resolve(__dirname, './src/' + projectPath),
        '~@': path.resolve(__dirname, './src/components/VForm'), // 表单组件库
        'vant/es': path.resolve(__dirname, './node_modules/vant/es'),
        'vant/lib': path.resolve(__dirname, './node_modules/vant/lib')
      },
      extensions: ['.js', '.vue', '.json', '.ts'] // 使用路径别名时想要省略的后缀名，可以自己 增减
    },
    plugins: [
      vue(),
      // 添加jsx/tsx支持
      vueJsx({}),
      // 图标
      createSvgIconsPlugin({
        // Specify the icon folder to be cached
        iconDirs: [
          resolve(process.cwd(), 'src/assets/img/svg')
        ],
        // Specify symbolId format
        symbolId: 'icon-[dir]-[name]'
      }),
      VueSetupExtend(),
      AutoImport({
        imports: ['vue', 'vue-router']
      }),
      Components({
        dts: false
      }),
      createHtmlPlugin({
        // 配置选项
        // 比如要注入环境变量等
        inject: {
          data: {
            injectScript: '/src/pages/' + proDir + '/main.ts',
            buildTime: new Date().toLocaleString(),
            ...extendsJS
          }
        },
        minify: true // 是否压缩 HTML
      }),
      visualizer(),
      // PkgConfig(),
      // OptimizationPersist(),
      copy({
        targets: copyTarget
      }),
      legacy({
        targets: ['defaults', 'not IE 11']
      })
    ],
    assetsInclude: [
      '**/*.ttf',
      '**/*.otf',
      '**/*.woff',
      '**/*.woff2',
      '**/*.eot'
    ],
    build: {
      target: ['es2020', 'chrome91', 'safari14'] // 确保包含支持 BigInt 的环境
    },
    optimizeDeps: {
      include: [
        'vue',
        'pinia',
        'echarts',
        'sass',
        'vue-router',
        'vant'
      ]

    },
    css: {
      preprocessorOptions: {
        // 全局scss变量
        // scss: {
        //   charset: false,
        //   additionalData: `
        //   @import "./src/assets/style/vars.scss";
        //   `
        // }
      },
      postcss: {
        plugins: [
          postcsspxtoviewport({
            unitToConvert: 'px', // 默认值`px`，需要转换的单位
            viewportWidth: 360, // 视窗的宽度,对应设计稿宽度
            // viewportHeight: 1080, // 视窗的高度, 根据375设备的宽度来指定，一般是667，也可不配置
            unitPrecision: 2, // 指定`px`转换为视窗单位值的小数位数
            propList: ['*'], // 转化为vw的属性列表
            viewportUnit: 'vw', // 指定需要转换成视窗单位
            fontViewportUnit: 'vw', // 字体使用的视窗单位
            selectorBlaskList: ['.ignore-'], // 指定不需要转换为视窗单位的类
            mediaQuery: false, // 允许在媒体查询中转换`px`
            minPixelValue: 1, // 小于或等于`1px`时不转换为视窗单位
            replace: true, // 是否直接更换属性值而不添加备用属性
            exclude: [], // 忽略某些文件夹下的文件或特定文件
            landscape: false, // 是否添加根据landscapeWidth生成的媒体查询条件 @media (orientation: landscape)
            landscapeUnit: 'vw', // 横屏时使用的单位
            landscapeWidth: 640 // 横屏时使用的视窗宽度
          })
        ]
      }
    },
    test: {
      // ...
    },
    server: {
      host: '0.0.0.0',
      port: 8080, // 将开发服务器端口设置为 8080
      proxy: {
        // '/workflow': {
        //   target: 'http://192.168.88.54:8080',
        //   changeOrigin: true,
        //   rewrite: path => path.replace(/^\/workflow/, '')
        // }
      }
    }
  }
})
