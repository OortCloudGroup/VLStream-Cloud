// 测试用户信息API调用
console.log('🚀 测试用户信息API调用...\n');

// 模拟用户信息获取流程
async function testUserInfoFlow() {
  console.log('=== 当前存储的用户信息 ===');
  
  // 检查存储中的用户信息
  const sessionUserInfo = sessionStorage.getItem('userInfo');
  const localUserInfo = localStorage.getItem('userInfo');
  
  if (sessionUserInfo) {
    const user = JSON.parse(sessionUserInfo);
    console.log('✅ Session Storage用户信息:', user.userName);
    console.log('- userId:', user.userId);
    console.log('- tenantId:', user.tenantId);
    console.log('- accessToken:', user.accessToken);
  } else {
    console.log('❌ Session Storage中没有用户信息');
  }
  
  if (localUserInfo) {
    const user = JSON.parse(localUserInfo);
    console.log('✅ Local Storage用户信息:', user.userName);
    console.log('- userId:', user.userId);
    console.log('- tenantId:', user.tenantId);
    console.log('- accessToken:', user.accessToken);
  } else {
    console.log('❌ Local Storage中没有用户信息');
  }
  
  // 检查token
  console.log('\n=== 当前存储的token ===');
  const sessionToken = sessionStorage.getItem('accessToken');
  const localToken = localStorage.getItem('accessToken');
  
  console.log('Session Storage token:', sessionToken);
  console.log('Local Storage token:', localToken);
  
  // 模拟API调用
  console.log('\n=== 模拟API调用 ===');
  
  if (sessionToken) {
    console.log('🔍 模拟verifyToken API调用:');
    console.log('- URL: /sso/v1/verifyToken');
    console.log('- Method: POST');
    console.log('- Headers:');
    console.log('  - requesttype: app');
      console.log('  - appid: 6551b0147c4649a894e86bf8de248da4');
  console.log('  - secretkey: 58f9eeefc65f4b318204ba21f39a8861');
    console.log('  - accesstoken:', sessionToken);
    console.log('- Body: { accessToken: "' + sessionToken + '" }');
  }
  
  // 检查是否有getUserInfo调用
  console.log('\n=== 检查getUserInfo调用 ===');
  console.log('📝 说明：');
  console.log('- 我们的应用主要通过verifyToken API获取用户信息');
  console.log('- getUserInfo API主要用于获取额外的用户详细信息');
  console.log('- 如果看到/sso/v1/getUserInfo调用，可能来自其他应用');
  
  // 验证用户信息完整性
  console.log('\n=== 验证用户信息完整性 ===');
  const userInfo = sessionUserInfo ? JSON.parse(sessionUserInfo) : 
                   localUserInfo ? JSON.parse(localUserInfo) : null;
  
  if (userInfo) {
    const requiredFields = ['userId', 'userName', 'tenantId', 'accessToken'];
    const missingFields = requiredFields.filter(field => !userInfo[field]);
    
    if (missingFields.length === 0) {
      console.log('✅ 用户信息完整，包含所有必需字段');
    } else {
      console.log('❌ 用户信息不完整，缺少字段:', missingFields);
    }
    
    console.log('用户信息详情:');
    console.log('- 用户名:', userInfo.userName);
    console.log('- 用户ID:', userInfo.userId);
    console.log('- 租户ID:', userInfo.tenantId);
    console.log('- 登录ID:', userInfo.loginId || '未设置');
    console.log('- 登录时间:', userInfo.loginTime || '未设置');
    console.log('- 登录IP:', userInfo.loginIP || '未设置');
  } else {
    console.log('❌ 没有找到用户信息');
  }
}

// 运行测试
testUserInfoFlow().catch(console.error);

console.log('\n📝 总结：');
console.log('- 我们的应用使用verifyToken API获取用户信息');
console.log('- 用户信息存储在Session Storage和Local Storage中');
console.log('- 页面右上角显示的用户名来自本地存储的用户信息');
console.log('- 如果看到getUserInfo API调用，可能来自其他应用或页面'); 