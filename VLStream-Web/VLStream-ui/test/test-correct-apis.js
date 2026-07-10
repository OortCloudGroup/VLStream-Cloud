// 测试正确的API配置
const axios = require('axios');

async function testCorrectAPIs() {
  console.log('🚀 测试正确的API配置...\n');
  
  const testToken = '421e68dff50a4c2d8387949d482a467a';
  const tenantId = '0e391fd7-1033-4f09-88c0-187582fee462';
  const baseURL = 'http://oort.oortcloudsmart.com:21410/bus/vls-server';
  
  const testConfigs = [
    {
      name: 'verifyToken API',
      url: `${baseURL}/auth/verifyToken`,
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
      name: 'loginUser API',
      url: `${baseURL}/auth/login`,
      headers: {
        'Content-Type': 'application/json',
        'requesttype': 'app',
        'appid': '6551b0147c4649a894e86bf8de248da4',
        'secretkey': '58f9eeefc65f4b318204ba21f39a8861',
        'accesstoken': testToken,
        'tenantid': tenantId
      },
      data: {
        accessToken: testToken,
        tenant_id: tenantId
      }
    },
    {
      name: 'getUserTenants API',
      url: `${baseURL}/auth/getUserTenants`,
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
    console.log('请求头:', config.headers);
    console.log('请求数据:', config.data);
    console.log('');
    
    try {
      const response = await axios.post(config.url, config.data, {
        headers: config.headers,
        timeout: 10000,
        validateStatus: () => true
      });
      
      console.log(`响应状态: ${response.status}`);
      if (response.status === 200) {
        console.log('✅ 请求成功！');
        console.log('响应数据:', JSON.stringify(response.data, null, 2));
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
  console.log('- 所有API都使用apaas-sso网关');
  console.log('- 请求头名称使用小写');
  console.log('- token同时作为请求头和请求体传递');
  console.log('- getTenant需要额外的tenantid请求头');
}

// 运行测试
testCorrectAPIs().catch(console.error); 