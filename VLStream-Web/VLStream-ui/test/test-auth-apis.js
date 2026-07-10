// 测试认证相关API
const axios = require('axios');

async function testAuthAPIs() {
  console.log('🚀 测试认证相关API...\n');
  
  const testToken = '421e68dff50a4c2d8387949d482a467a';
  const baseURL = 'http://oort.oortcloudsmart.com:21410/bus/apaas-sso';
  
  const testConfigs = [
    {
      name: 'verifyToken API',
      url: `${baseURL}/sso/v1/verifyToken`,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'requesttype': 'app',
        'appid': '6551b0147c4649a894e86bf8de248da4',
        'secretkey': '58f9eeefc65f4b318204ba21f39a8861',
        'accesstoken': testToken
      },
      data: {
        accessToken: testToken
      }
    },
    {
      name: 'getTenant API',
      url: `${baseURL}/sso/v1/getTenant`,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'requesttype': 'app',
        'appid': '6551b0147c4649a894e86bf8de248da4',
        'secretkey': '58f9eeefc65f4b318204ba21f39a8861',
        'accesstoken': testToken,
        'tenantid': '0e391fd7-1033-4f09-88c0-187582fee462'
      },
      data: {
        accessToken: testToken,
        tenant_id: '0e391fd7-1033-4f09-88c0-187582fee462'
      }
    },
    {
      name: 'getUserTenants API',
      url: `${baseURL}/sso/v1/getUserTenants`,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'requesttype': 'app',
        'appid': '6551b0147c4649a894e86bf8de248da4',
        'secretkey': '58f9eeefc65f4b318204ba21f39a8861',
        'accesstoken': testToken
      },
      data: {
        accessToken: testToken
      }
    }
  ];
  
  for (const config of testConfigs) {
    console.log(`\n=== 测试: ${config.name} ===`);
    console.log('URL:', config.url);
    console.log('方法:', config.method);
    console.log('请求头:', config.headers);
    console.log('请求数据:', config.data);
    console.log('');
    
    try {
      // 测试OPTIONS预检请求
      console.log('1. 测试OPTIONS预检请求...');
      const optionsResponse = await axios.options(config.url, {
        timeout: 5000,
        validateStatus: () => true
      });
      
      console.log(`OPTIONS响应状态: ${optionsResponse.status}`);
      console.log('CORS头:', {
        'Access-Control-Allow-Origin': optionsResponse.headers['access-control-allow-origin'],
        'Access-Control-Allow-Methods': optionsResponse.headers['access-control-allow-methods'],
        'Access-Control-Allow-Headers': optionsResponse.headers['access-control-allow-headers']
      });
      console.log('');
      
      // 测试实际请求
      console.log('2. 测试实际请求...');
      const response = await axios.post(config.url, config.data, {
        headers: config.headers,
        timeout: 10000,
        validateStatus: () => true
      });
      
      console.log(`${config.method}响应状态: ${response.status}`);
      if (response.status === 200) {
        console.log('✅ 请求成功！');
        console.log('响应数据:', response.data);
      } else {
        console.log('❌ 请求失败！');
        console.log('响应数据:', response.data);
      }
      
    } catch (error) {
      console.log(`❌ 请求异常: ${error.message}`);
      if (error.response) {
        console.log(`响应状态: ${error.response.status}`);
        console.log(`响应数据:`, error.response.data);
      }
    }
  }
  
  console.log('\n📝 配置说明：');
  console.log('- verifyToken API需要token作为请求头和请求体');
  console.log('- getTenantId API需要用户中心认证头');
  console.log('- 所有API都需要正确的appid和secretkey');
}

// 运行测试
testAuthAPIs().catch(console.error); 