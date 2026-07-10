// 测试verifyToken响应处理
const axios = require('axios');

async function testVerifyTokenResponse() {
  console.log('🚀 测试verifyToken响应处理...\n');
  
  const testToken = '421e68dff50a4c2d8387949d482a467a';
  const baseURL = 'http://oort.oortcloudsmart.com:21410/bus/apaas-sso';
  
  // 模拟我们的verifyToken方法
  async function verifyToken(data) {
    const request = axios.create({
      baseURL: baseURL,
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json',
        'requesttype': 'app',
          'appid': '6551b0147c4649a894e86bf8de248da4',
  'secretkey': '58f9eeefc65f4b318204ba21f39a8861',
        'accesstoken': data.accessToken
      }
    });
    
    // 添加响应拦截器，返回处理后的数据
    request.interceptors.response.use(
      response => {
        console.log('响应拦截器处理前:', response);
        console.log('响应拦截器处理后:', response.data);
        return response.data;
      },
      error => {
        console.error('verifyToken请求失败:', error);
        return Promise.reject(error);
      }
    );
    
    const requestData = {
      accessToken: data.accessToken
    };
    
    return request.post('/sso/v1/verifyToken', requestData);
  }
  
  try {
    console.log('调用verifyToken...');
    const response = await verifyToken({
      accessToken: testToken
    });
    
    console.log('\n=== 最终响应结果 ===');
    console.log('响应类型:', typeof response);
    console.log('响应内容:', response);
    
    if (response && response.code === 200) {
      console.log('✅ Token验证成功！');
      console.log('用户信息:', response.data);
    } else {
      console.log('❌ Token验证失败！');
      console.log('错误信息:', response);
    }
    
  } catch (error) {
    console.log('❌ 请求异常:', error.message);
    if (error.response) {
      console.log('响应状态:', error.response.status);
      console.log('响应数据:', error.response.data);
    }
  }
}

// 运行测试
testVerifyTokenResponse().catch(console.error); 