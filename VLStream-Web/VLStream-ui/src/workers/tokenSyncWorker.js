// SharedWorker用于跨标签页token同步
const connections = []
let lastKnownToken = null
let checkInterval = null

// 监听新的连接
self.addEventListener('connect', (event) => {
  const port = event.ports[0]
  connections.push(port)
  
  console.log('SharedWorker: 新连接已建立，当前连接数:', connections.length)
  
  port.addEventListener('message', async (e) => {
    const { type, data } = e.data
    
    switch (type) {
      case 'START_MONITORING':
        startTokenMonitoring()
        break
      case 'STOP_MONITORING':
        stopTokenMonitoring()
        break
      case 'CHECK_TOKEN':
        await checkAndBroadcastToken()
        break
      case 'UPDATE_TOKEN':
        await updateToken(data.token)
        break
    }
  })
  
  port.addEventListener('close', () => {
    const index = connections.indexOf(port)
    if (index > -1) {
      connections.splice(index, 1)
    }
    console.log('SharedWorker: 连接已关闭，当前连接数:', connections.length)
  })
  
  port.start()
  
  // 立即检查一次token
  checkAndBroadcastToken()
})

// 开始token监控
function startTokenMonitoring() {
  if (checkInterval) {
    clearInterval(checkInterval)
  }
  
  // 每2秒检查一次token变化
  checkInterval = setInterval(async () => {
    await checkAndBroadcastToken()
  }, 2000)
  
  console.log('SharedWorker: Token监控已启动')
}

// 停止token监控
function stopTokenMonitoring() {
  if (checkInterval) {
    clearInterval(checkInterval)
    checkInterval = null
  }
  console.log('SharedWorker: Token监控已停止')
}

// 检查并广播token变化
async function checkAndBroadcastToken() {
  try {
    // 通过API检查最新的用户信息
    const latestUserInfo = await getCurrentUserFromAPI()
    
    if (latestUserInfo && latestUserInfo.accessToken) {
      const newToken = latestUserInfo.accessToken
      
      // 如果token发生变化，广播给所有连接
      if (newToken !== lastKnownToken) {
        lastKnownToken = newToken
        
        broadcastToAllConnections({
          type: 'TOKEN_UPDATED',
          token: newToken,
          userInfo: latestUserInfo,
          timestamp: Date.now()
        })
        
        console.log('SharedWorker: Token已更新并广播')
      }
    } else {
      // 如果没有有效token，且之前有token，说明用户已登出
      if (lastKnownToken) {
        lastKnownToken = null
        
        broadcastToAllConnections({
          type: 'TOKEN_INVALID',
          timestamp: Date.now()
        })
        
        console.log('SharedWorker: Token已失效并广播')
      }
    }
  } catch (error) {
    console.error('SharedWorker: 检查token失败:', error)
  }
}

// 更新token
async function updateToken(newToken) {
  if (newToken && newToken !== lastKnownToken) {
    // 验证新token
    const userInfo = await verifyTokenWithAPI(newToken)
    
    if (userInfo) {
      lastKnownToken = newToken
      
      broadcastToAllConnections({
        type: 'TOKEN_UPDATED',
        token: newToken,
        userInfo: userInfo,
        timestamp: Date.now()
      })
      
      console.log('SharedWorker: 手动更新token成功')
    } else {
      console.log('SharedWorker: 新token验证失败')
    }
  }
}

// 广播消息给所有连接
function broadcastToAllConnections(message) {
  connections.forEach(port => {
    try {
      port.postMessage(message)
    } catch (error) {
      console.error('SharedWorker: 发送消息失败:', error)
    }
  })
}

// 通过API获取当前用户信息
async function getCurrentUserFromAPI() {
  try {
    const response = await fetch('http://oort.oortcloudsmart.com:21410/bus/apaas-sso/api/v1/getCurrentUser', {
      method: 'GET',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json'
      }
    })
    
    if (response.ok) {
      const data = await response.json()
      if (data.code === 200) {
        return data.data
      }
    }
  } catch (error) {
    console.error('SharedWorker: 获取用户信息失败:', error)
  }
  return null
}

// 通过API验证token
async function verifyTokenWithAPI(token) {
  try {
    const response = await fetch('http://oort.oortcloudsmart.com:21410/bus/apaas-sso/sso/v1/verifyToken', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'appid': '6551b0147c4649a894e86bf8de248da4',
        'secretkey': '58f9eeefc65f4b318204ba21f39a8861'
      },
      body: JSON.stringify({
        accessToken: token
      })
    })
    
    if (response.ok) {
      const data = await response.json()
      if (data.code === 200) {
        return data.data
      }
    }
  } catch (error) {
    console.error('SharedWorker: Token验证失败:', error)
  }
  return null
}

console.log('SharedWorker: Token同步Worker已启动')