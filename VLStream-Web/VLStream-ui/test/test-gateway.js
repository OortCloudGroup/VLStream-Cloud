/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

// configuration
const axios = require('axios');

// configuration
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

// API
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
      // OPTIONS
      const optionsResponse = await axios.options(fullUrl, {
        timeout: 5000,
        validateStatus: () => true //
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

  // configuration
  await testConfig(configs.dev, 'dev');

  // configuration
  await testConfig(configs.prod, 'prod');

  console.log('\n✅ 测试完成！');
  console.log('\n📝 说明：');
  console.log('- 开发环境使用相对路径，通过Vite代理转发');
  console.log('- 生产环境使用网关地址，通过Nginx代理转发');
  console.log('- 如果OPTIONS请求返回200或204，说明CORS配置正确');
}

//
runTests().catch(console.error);