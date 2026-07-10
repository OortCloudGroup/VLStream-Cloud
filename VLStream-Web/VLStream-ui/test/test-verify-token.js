// 测试verifyToken API
const axios = require('axios');

// 测试配置
const testConfigs = [
  {
    name: '用户中心API (开发环境)',
    baseURL: '/user-center',
    path: '/v1/verifyToken',
    data: { accessToken: '7d76ac73faaa427b9df1129f60b74284' }
  },
  {
    name: 'SSO认证API (生产环境)',
    baseURL: 'http://oort.oortcloudsmart.com:21410/bus/apaas-sso',
    path: '/sso/v1/verifyToken',
    data: { accessToken: '7d76ac73faaa427b9df1129f60b74284' }
  },
  {
    name: '网关API (生产环境)',
    baseURL: 'http://oort.oortcloudsmart.com:21410/bus/vls-server',
    path: '/auth/verifyToken',
    data: { accessToken: '7d76ac73faaa427b9df1129f60b74284' }
  }
];

async function testVerifyToken(config) {
  console.log(`\n=== 测试: ${config.name} ===`);
  console.log(`BaseURL: ${config.baseURL}`);
  console.log(`路径: ${config.path}`);
  console.log(`请求数据:`, config.data);
  
  const fullUrl = config.baseURL + config.path;
  console.log(`完整URL: ${fullUrl}`);
  
  try {
    // 测试OPTIONS预检请求
    console.log('\n1. 测试OPTIONS预检请求...');
    const optionsResponse = await axios.options(fullUrl, {
      timeout: 5000,
      validateStatus: () => true
    });
    
    console.log(`OPTIONS响应状态: ${optionsResponse.status}`);
    console.log(`CORS头:`, optionsResponse.headers);
    
    // 测试POST请求
    console.log('\n2. 测试POST请求...');
    const postResponse = await axios.post(fullUrl, config.data, {
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json',
        'requestType': 'app',
        'appID': '6551b0147c4649a894e86bf8de248da4',
        'secretKey': '58f9eeefc65f4b318204ba21f39a8861'
      },
      validateStatus: () => true
    });
    
    console.log(`POST响应状态: ${postResponse.status}`);
    console.log(`响应数据:`, postResponse.data);
    
    if (postResponse.status === 200) {
      console.log('✅ 请求成功！');
    } else {
      console.log('❌ 请求失败！');
    }
    
  } catch (error) {
    console.log(`❌ 请求异常: ${error.message}`);
    if (error.response) {
      console.log(`响应状态: ${error.response.status}`);
      console.log(`响应数据:`, error.response.data);
    }
  }
}

async function runTests() {
  console.log('🚀 开始测试verifyToken API...\n');
  
  for (const config of testConfigs) {
    await testVerifyToken(config);
  }
  
  console.log('\n✅ 测试完成！');
  console.log('\n📝 说明：');
  console.log('- 用户中心API路径: /v1/verifyToken');
  console.log('- SSO认证API路径: /sso/v1/verifyToken');
  console.log('- 网关API路径: /auth/verifyToken');
  console.log('- 需要添加用户中心认证头: requestType, appID, secretKey');
}

// 运行测试
runTests().catch(console.error); 