// 测试两个网关配置
const axios = require('axios');

// 测试配置
const testConfigs = [
  {
    name: '用户平台网关 (apaas-sso)',
    baseURL: 'http://oort.oortcloudsmart.com:21410/bus/apaas-sso',
    apis: [
      {
        path: '/sso/v1/verifyToken',
        method: 'POST',
        data: { accessToken: '7d76ac73faaa427b9df1129f60b74284' },
        headers: {
          'Content-Type': 'application/json',
          'requesttype': 'app',
            'appid': '6551b0147c4649a894e86bf8de248da4',
  'secretkey': '58f9eeefc65f4b318204ba21f39a8861',
          'accesstoken': '421e68dff50a4c2d8387949d482a467a'
        }
      }
    ]
  },
  {
    name: '后端网关 (vls-server)',
    baseURL: 'http://oort.oortcloudsmart.com:21410/bus/vls-server',
    apis: [
      {
        path: '/api/algorithm/list',
        method: 'GET',
        data: null,
        headers: {
          'Content-Type': 'application/json'
        }
      },
      {
        path: '/api/scene/list',
        method: 'GET',
        data: null,
        headers: {
          'Content-Type': 'application/json'
        }
      }
    ]
  }
];

async function testGateway(gatewayConfig) {
  console.log(`\n=== 测试: ${gatewayConfig.name} ===`);
  console.log(`BaseURL: ${gatewayConfig.baseURL}`);
  
  for (const api of gatewayConfig.apis) {
    console.log(`\n--- 测试API: ${api.method} ${api.path} ---`);
    const fullUrl = gatewayConfig.baseURL + api.path;
    console.log(`完整URL: ${fullUrl}`);
    
    try {
      // 测试OPTIONS预检请求
      console.log('1. 测试OPTIONS预检请求...');
      const optionsResponse = await axios.options(fullUrl, {
        timeout: 5000,
        validateStatus: () => true
      });
      
      console.log(`OPTIONS响应状态: ${optionsResponse.status}`);
      console.log(`CORS头:`, {
        'Access-Control-Allow-Origin': optionsResponse.headers['access-control-allow-origin'],
        'Access-Control-Allow-Methods': optionsResponse.headers['access-control-allow-methods'],
        'Access-Control-Allow-Headers': optionsResponse.headers['access-control-allow-headers']
      });
      
      // 测试实际请求
      console.log('2. 测试实际请求...');
      const requestConfig = {
        timeout: 10000,
        headers: api.headers,
        validateStatus: () => true
      };
      
      let response;
      if (api.method === 'GET') {
        response = await axios.get(fullUrl, requestConfig);
      } else if (api.method === 'POST') {
        response = await axios.post(fullUrl, api.data, requestConfig);
      }
      
      console.log(`${api.method}响应状态: ${response.status}`);
      if (response.status === 200) {
        console.log('✅ 请求成功！');
        console.log(`响应数据:`, response.data);
      } else {
        console.log('❌ 请求失败！');
        console.log(`响应数据:`, response.data);
      }
      
    } catch (error) {
      console.log(`❌ 请求异常: ${error.message}`);
      if (error.response) {
        console.log(`响应状态: ${error.response.status}`);
        console.log(`响应数据:`, error.response.data);
      }
    }
  }
}

async function runTests() {
  console.log('🚀 开始测试两个网关配置...\n');
  
  for (const config of testConfigs) {
    await testGateway(config);
  }
  
  console.log('\n✅ 测试完成！');
  console.log('\n📝 网关分工说明：');
  console.log('- apaas-sso：用户平台网关，处理用户认证相关API');
  console.log('- vls-server：后端网关，处理业务相关API');
  console.log('\n🔧 配置要点：');
  console.log('- 用户认证API需要添加认证头：requestType, appID, secretKey');
  console.log('- 两个网关都需要配置CORS头');
  console.log('- Nginx需要代理两个网关路径');
}

// 运行测试
runTests().catch(console.error); 