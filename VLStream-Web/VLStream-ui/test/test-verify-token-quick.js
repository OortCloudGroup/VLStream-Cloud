// 快速测试verifyToken API配置
const axios = require('axios');

async function testVerifyToken() {
  console.log('🚀 测试verifyToken API配置...\n');
  
  const testConfig = {
    url: 'http://oort.oortcloudsmart.com:21410/bus/apaas-sso/sso/v1/verifyToken',
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'requesttype': 'app',
        'appid': '6551b0147c4649a894e86bf8de248da4',
  'secretkey': '58f9eeefc65f4b318204ba21f39a8861',
      'accesstoken': '421e68dff50a4c2d8387949d482a467a'
    },
    data: {
      accessToken: '421e68dff50a4c2d8387949d482a467a'
    }
  };
  
  console.log('请求配置:');
  console.log('URL:', testConfig.url);
  console.log('方法:', testConfig.method);
  console.log('请求头:', testConfig.headers);
  console.log('请求数据:', testConfig.data);
  console.log('');
  
  try {
    // 测试OPTIONS预检请求
    console.log('1. 测试OPTIONS预检请求...');
    const optionsResponse = await axios.options(testConfig.url, {
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
    
    // 测试POST请求
    console.log('2. 测试POST请求...');
    const postResponse = await axios.post(testConfig.url, testConfig.data, {
      headers: testConfig.headers,
      timeout: 10000,
      validateStatus: () => true
    });
    
    console.log(`POST响应状态: ${postResponse.status}`);
    if (postResponse.status === 200) {
      console.log('✅ 请求成功！');
      console.log('响应数据:', postResponse.data);
    } else {
      console.log('❌ 请求失败！');
      console.log('响应数据:', postResponse.data);
    }
    
  } catch (error) {
    console.log(`❌ 请求异常: ${error.message}`);
    if (error.response) {
      console.log(`响应状态: ${error.response.status}`);
      console.log(`响应数据:`, error.response.data);
    }
  }
  
  console.log('\n📝 配置说明：');
  console.log('- 使用正确的appid和secretkey');
  console.log('- 请求头名称使用小写');
  console.log('- token同时作为请求头和请求体传递');
}

// 运行测试
testVerifyToken().catch(console.error); 