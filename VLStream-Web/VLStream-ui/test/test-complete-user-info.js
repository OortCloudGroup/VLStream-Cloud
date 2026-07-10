// 测试完整的用户信息获取流程
console.log('🚀 测试完整的用户信息获取流程...\n');

// 模拟用户信息获取流程
async function testCompleteUserInfoFlow() {
  console.log('=== 当前存储状态 ===');
  
  // 检查当前token
  const sessionToken = sessionStorage.getItem('accessToken');
  const localToken = localStorage.getItem('accessToken');
  
  console.log('Session Storage token:', sessionToken);
  console.log('Local Storage token:', localToken);
  
  if (!sessionToken && !localToken) {
    console.log('❌ 没有找到有效的token');
    return;
  }
  
  const currentToken = sessionToken || localToken;
  console.log('✅ 使用token:', currentToken);
  
  // 模拟API调用流程
  console.log('\n=== 模拟API调用流程 ===');
  
  // 1. verifyToken API调用
  console.log('1️⃣ 调用verifyToken API:');
  console.log('- URL: /sso/v1/verifyToken');
  console.log('- Method: POST');
  console.log('- Headers:');
  console.log('  - requesttype: app');
  console.log('  - appid: 6551b0147c4649a894e86bf8de248da4');
  console.log('  - secretkey: 58f9eeefc65f4b318204ba21f39a8861');
  console.log('  - accesstoken:', currentToken);
  console.log('- Body: { accessToken: "' + currentToken + '" }');
  
  // 2. getUserInfo API调用
  console.log('\n2️⃣ 调用getUserInfo API:');
  console.log('- URL: /sso/v1/getUserInfo');
  console.log('- Method: POST');
  console.log('- Headers:');
  console.log('  - requesttype: app');
  console.log('  - appid: 6551b0147c4649a894e86bf8de248da4');
  console.log('  - secretkey: 58f9eeefc65f4b318204ba21f39a8861');
  console.log('  - accesstoken:', currentToken);
  console.log('- Body: { accessToken: "' + currentToken + '" }');
  
  // 检查当前用户信息
  console.log('\n=== 当前用户信息 ===');
  const sessionUserInfo = sessionStorage.getItem('userInfo');
  const localUserInfo = localStorage.getItem('userInfo');
  
  if (sessionUserInfo) {
    const user = JSON.parse(sessionUserInfo);
    console.log('✅ Session Storage用户信息:');
    console.log('- 用户名:', user.userName);
    console.log('- 用户ID:', user.userId);
    console.log('- 租户ID:', user.tenantId);
    console.log('- 登录ID:', user.loginId || '未设置');
    console.log('- 登录时间:', user.loginTime || '未设置');
    console.log('- 登录IP:', user.loginIP || '未设置');
    console.log('- 客户端:', user.client || '未设置');
    console.log('- 登录类型:', user.loginType || '未设置');
  }
  
  if (localUserInfo) {
    const user = JSON.parse(localUserInfo);
    console.log('✅ Local Storage用户信息:');
    console.log('- 用户名:', user.userName);
    console.log('- 用户ID:', user.userId);
    console.log('- 租户ID:', user.tenantId);
    console.log('- 登录ID:', user.loginId || '未设置');
    console.log('- 登录时间:', user.loginTime || '未设置');
    console.log('- 登录IP:', user.loginIP || '未设置');
    console.log('- 客户端:', user.client || '未设置');
    console.log('- 登录类型:', user.loginType || '未设置');
  }
  
  if (!sessionUserInfo && !localUserInfo) {
    console.log('❌ 没有找到用户信息');
  }
  
  // 验证用户信息完整性
  console.log('\n=== 验证用户信息完整性 ===');
  const userInfo = sessionUserInfo ? JSON.parse(sessionUserInfo) : 
                   localUserInfo ? JSON.parse(localUserInfo) : null;
  
  if (userInfo) {
    const basicFields = ['userId', 'userName', 'tenantId', 'accessToken'];
    const extendedFields = ['loginId', 'loginTime', 'loginIP', 'client', 'loginType'];
    
    const missingBasicFields = basicFields.filter(field => !userInfo[field]);
    const missingExtendedFields = extendedFields.filter(field => !userInfo[field]);
    
    if (missingBasicFields.length === 0) {
      console.log('✅ 基础用户信息完整');
    } else {
      console.log('❌ 缺少基础字段:', missingBasicFields);
    }
    
    if (missingExtendedFields.length === 0) {
      console.log('✅ 扩展用户信息完整');
    } else {
      console.log('⚠️ 缺少扩展字段:', missingExtendedFields);
      console.log('💡 这些字段需要通过getUserInfo API获取');
    }
    
    // 检查是否包含getUserInfo的字段
    const hasExtendedInfo = extendedFields.some(field => userInfo[field]);
    if (hasExtendedInfo) {
      console.log('✅ 用户信息包含扩展字段，可能已经调用了getUserInfo API');
    } else {
      console.log('💡 用户信息只有基础字段，需要调用getUserInfo API获取完整信息');
    }
  }
}

// 运行测试
testCompleteUserInfoFlow().catch(console.error);

console.log('\n📝 说明：');
console.log('- verifyToken API: 获取基础用户信息（userId, userName, tenantId等）');
console.log('- getUserInfo API: 获取完整用户信息（loginId, loginTime, loginIP等）');
console.log('- 新的流程会先调用verifyToken，再调用getUserInfo获取完整信息');
console.log('- 如果看到两个API都调用成功，说明用户信息获取流程完整'); 