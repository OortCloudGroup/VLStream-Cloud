// 测试服务器配置
console.log('🔧 测试服务器配置...')

// 检查静态文件是否正确部署
function checkStaticFiles() {
  console.log('🧪 检查静态文件...')
  
  // 检查index.html是否存在
  fetch('/bus/vls-ui/index.html')
    .then(response => {
      if (response.ok) {
        console.log('✅ index.html文件存在')
        return response.text()
      } else {
        console.log('❌ index.html文件不存在，状态:', response.status)
        throw new Error('index.html not found')
      }
    })
    .then(html => {
      console.log('📋 index.html内容预览:', html.substring(0, 200) + '...')
      
      // 检查是否包含Vue应用
      if (html.includes('id="app"')) {
        console.log('✅ index.html包含Vue应用挂载点')
      } else {
        console.log('❌ index.html不包含Vue应用挂载点')
      }
      
      if (html.includes('main.js')) {
        console.log('✅ index.html包含main.js引用')
      } else {
        console.log('❌ index.html不包含main.js引用')
      }
    })
    .catch(error => {
      console.error('❌ 检查index.html失败:', error)
    })
  
  // 检查main.js是否存在
  fetch('/bus/vls-ui/src/main.js')
    .then(response => {
      if (response.ok) {
        console.log('✅ main.js文件存在')
      } else {
        console.log('❌ main.js文件不存在，状态:', response.status)
      }
    })
    .catch(error => {
      console.error('❌ 检查main.js失败:', error)
    })
}

// 检查路由重定向
function checkRouteRedirect() {
  console.log('🧪 检查路由重定向...')
  
  // 测试一个不存在的路由，看是否重定向到index.html
  fetch('/bus/vls-ui/non-existent-route')
    .then(response => {
      console.log('📋 不存在的路由响应状态:', response.status)
      
      if (response.status === 200) {
        console.log('✅ 路由重定向工作正常')
        return response.text()
      } else {
        console.log('❌ 路由重定向不工作，状态:', response.status)
        throw new Error('Route redirect not working')
      }
    })
    .then(html => {
      if (html.includes('id="app"')) {
        console.log('✅ 重定向返回了正确的index.html')
      } else {
        console.log('❌ 重定向没有返回正确的index.html')
      }
    })
    .catch(error => {
      console.error('❌ 检查路由重定向失败:', error)
    })
}

// 检查当前页面状态
function checkCurrentPage() {
  console.log('🧪 检查当前页面状态...')
  
  console.log('📋 当前页面信息:')
  console.log('- URL:', window.location.href)
  console.log('- 路径:', window.location.pathname)
  console.log('- 标题:', document.title)
  
  // 检查页面内容
  const bodyContent = document.body.innerHTML
  console.log('📋 页面内容长度:', bodyContent.length)
  
  if (bodyContent.includes('id="app"')) {
    console.log('✅ 页面包含Vue应用挂载点')
  } else {
    console.log('❌ 页面不包含Vue应用挂载点')
  }
  
  if (bodyContent.includes('layout-container')) {
    console.log('✅ 页面包含layout组件')
  } else {
    console.log('❌ 页面不包含layout组件')
  }
}

// 主检查函数
function mainCheck() {
  console.log('📋 开始服务器配置检查')
  
  checkStaticFiles()
  checkRouteRedirect()
  checkCurrentPage()
  
  console.log('\n📋 检查结果:')
  console.log('1. 如果index.html不存在，说明静态文件没有正确部署')
  console.log('2. 如果路由重定向不工作，说明nginx配置有问题')
  console.log('3. 如果页面不包含Vue应用，说明应用没有正确加载')
}

// 延迟执行
setTimeout(() => {
  mainCheck()
}, 1000)

console.log('🔧 服务器配置检查工具已加载，1秒后开始检查...') 