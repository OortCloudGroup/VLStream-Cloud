// Test VLStream-server APIs
async function testVLStreamAPIs() {
  const currentToken = sessionStorage.getItem('accessToken') || sessionStorage.getItem('token')
  
  console.log('🔍 Testing VLStream-server API')
  console.log('Current token:', currentToken)
  console.log('')
  
  const testAPIs = [
    {
      name: 'VLStream device list API (direct access)',
      url: 'http://oort.oortcloudsmart.com:21410/bus/vls-server/device/page',
      method: 'GET',
      params: { current: 1, size: 10 }
    },
    {
      name: 'VLStream device list API (with api prefix)',
      url: 'http://oort.oortcloudsmart.com:21410/bus/vls-server/api/device/page',
      method: 'GET',
      params: { current: 1, size: 10 }
    },
    {
      name: 'VLStream user sync API',
      url: 'http://oort.oortcloudsmart.com:21410/bus/vls-server/api/user/sync',
      method: 'POST',
      data: {
        userName: 'Zhou Liang',
        userId: '751fc4b0-81b4-4fe2-940b-ac18d7bc3439',
        tenantId: '0e391fd7-1033-4f09-88c0-187582fee462',
        accessToken: currentToken
      }
    },
    {
      name: 'VLStream tenant list API',
      url: 'http://oort.oortcloudsmart.com:21410/bus/vls-server/auth/getTenantList',
      method: 'POST',
      data: { accessToken: currentToken }
    },
    {
      name: 'VLStream health check',
      url: 'http://oort.oortcloudsmart.com:21410/bus/vls-server/auth/health',
      method: 'GET'
    }
  ]
  
  for (const api of testAPIs) {
    console.log(`\n=== Test: ${api.name} ===`)
    console.log('URL:', api.url)
    console.log('Method:', api.method)
    
    const headers = {
      'Content-Type': 'application/json',
      'requesttype': 'app',
      'appid': '6551b0147c4649a894e86bf8de248da4',
      'secretkey': '58f9eeefc65f4b318204ba21f39a8861',
      'accesstoken': currentToken
    }
    
    console.log('Headers:', headers)
    
    try {
      let response
      
      if (api.method === 'GET') {
        const url = new URL(api.url)
        if (api.params) {
          Object.keys(api.params).forEach(key => {
            url.searchParams.append(key, api.params[key])
          })
        }
        response = await fetch(url.toString(), {
          method: 'GET',
          headers: headers
        })
      } else {
        response = await fetch(api.url, {
          method: api.method,
          headers: headers,
          body: JSON.stringify(api.data || {})
        })
      }
      
      console.log(`Response status: ${response.status}`)
      
      if (response.ok) {
        const data = await response.json()
        console.log('✅ Request successful!')
        console.log('Response data:', data)
      } else {
        const errorData = await response.json()
        console.log('❌ Request failed!')
        console.log('Error message:', errorData)
        
        // Analyze error type
        if (errorData.code === 4004) {
          console.log('🔍 Error analysis: accessToken invalid. Validation failed')
          console.log('Possible reasons:')
          console.log('1. VLStream-server has no global authentication interceptor')
          console.log('2. Need to manually verify token in each API')
          console.log('3. Path mapping issue')
        } else if (errorData.code === 503) {
          console.log('🔍 Error analysis: Service unavailable')
          console.log('Possible reasons:')
          console.log('1. VLStream-server service not started')
          console.log('2. Gateway routing configuration issue')
          console.log('3. Service registration issue')
        }
      }
      
    } catch (error) {
      console.log(`❌ Request exception: ${error.message}`)
    }
  }
  
  console.log('\n📝 Test summary:')
  console.log('- If all APIs return 503 error, VLStream-server service is not started or gateway routing has issues')
  console.log('- If some APIs return 4004 error, need to add global authentication interceptor')
  console.log('- If APIs with /api prefix fail, path mapping configuration has issues')
}

// Run test
testVLStreamAPIs().catch(console.error) 