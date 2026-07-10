// 测试API请求头
console.log('🚀 测试API请求头...\n');

// 模拟请求拦截器的逻辑
function getRequestHeaders() {
  const headers = {
    'Content-Type': 'application/json',
    'requesttype': 'app',
      'appid': '6551b0147c4649a894e86bf8de248da4',
  'secretkey': '58f9eeefc65f4b318204ba21f39a8861'
  };
  
  // 获取token
  const token = sessionStorage.getItem('accessToken') || 
                sessionStorage.getItem('token') || 
                localStorage.getItem('accessToken') || 
                localStorage.getItem('token');
  
  if (token) {
    headers.accesstoken = token;
    console.log('✅ 找到token:', token);
  } else {
    console.log('❌ 没有找到token');
  }
  
  return headers;
}

// 检查当前存储的token
console.log('=== 当前存储的token ===');
console.log('sessionStorage accessToken:', sessionStorage.getItem('accessToken'));
console.log('sessionStorage token:', sessionStorage.getItem('token'));
console.log('localStorage accessToken:', localStorage.getItem('accessToken'));
console.log('localStorage token:', localStorage.getItem('token'));

// 生成请求头
console.log('\n=== 生成的请求头 ===');
const headers = getRequestHeaders();
console.log('请求头:', headers);

// 验证关键字段
console.log('\n=== 验证关键字段 ===');
console.log('requesttype:', headers.requesttype);
console.log('appid:', headers.appid);
console.log('secretkey:', headers.secretkey);
console.log('accesstoken:', headers.accesstoken);

if (headers.requesttype === 'app' && 
    headers.appid && 
    headers.secretkey && 
    headers.accesstoken) {
  console.log('✅ 所有必需的认证头都已设置');
} else {
  console.log('❌ 缺少必需的认证头');
}

console.log('\n📝 说明：');
console.log('- 如果所有认证头都已设置，后端API应该能正常访问');
console.log('- 如果缺少accesstoken，说明token没有正确保存或获取');
console.log('- 如果其他头缺失，说明配置有问题'); 