// 测试网关地址配置
const axios = require('axios');

// 测试配置
const configs = {
  dev: {
    baseURL: '',
    description: '开发环境（相对路径）'
  },
  prod: {
    baseURL: 'http://oort.oortcloudsmart.com:21410/bus/vls-server',
    description: '生产环境（网关地址）'
  }
};

// 测试API路径
const testApis = [
  '/auth/verifyToken',
  '/auth/login',
  '/api/algorithm/list',
  '/api/scene/list'
];

async function testConfig(config, env) {
  console.log(`\n=== 测试${config.description} ===`);
  console.log(`BaseURL: ${config.baseURL || '(相对路径)'}`);
  
  for (const api of testApis) {
    const fullUrl = config.baseURL + api;
    console.log(`\n测试API: ${api}`);
    console.log(`完整URL: ${fullUrl}`);
    
    try {
      // 测试OPTIONS预检请求
      const optionsResponse = await axios.options(fullUrl, {
        timeout: 5000,
        validateStatus: () => true // 接受任何状态码
      });
      
      console.log(`OPTIONS响应状态: ${optionsResponse.status}`);
      console.log(`CORS头: ${JSON.stringify(optionsResponse.headers, null, 2)}`);
      
    } catch (error) {
      console.log(`请求失败: ${error.message}`);
      if (error.response) {
        console.log(`响应状态: ${error.response.status}`);
        console.log(`响应头: ${JSON.stringify(error.response.headers, null, 2)}`);
      }
    }
  }
}

async function runTests() {
  console.log('🚀 开始测试网关地址配置...\n');
  
  // 测试开发环境配置
  await testConfig(configs.dev, 'dev');
  
  // 测试生产环境配置
  await testConfig(configs.prod, 'prod');
  
  console.log('\n✅ 测试完成！');
  console.log('\n📝 说明：');
  console.log('- 开发环境使用相对路径，通过Vite代理转发');
  console.log('- 生产环境使用网关地址，通过Nginx代理转发');
  console.log('- 如果OPTIONS请求返回200或204，说明CORS配置正确');
}

// 运行测试
runTests().catch(console.error); 