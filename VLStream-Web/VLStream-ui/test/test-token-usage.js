// 测试token使用情况
console.log('🚀 测试token使用情况...\n');

// 模拟请求拦截器的token获取逻辑
function getTokenForRequest() {
  // 只使用sessionStorage中的token（以sessionStorage为准）
  const sessionToken = sessionStorage.getItem('accessToken') || sessionStorage.getItem('token')
  const localToken = localStorage.getItem('accessToken') || localStorage.getItem('token')
  
  console.log('=== Token获取逻辑 ===');
  console.log('Session Storage accessToken:', sessionStorage.getItem('accessToken'));
  console.log('Session Storage token:', sessionStorage.getItem('token'));
  console.log('Local Storage accessToken:', localStorage.getItem('accessToken'));
  console.log('Local Storage token:', localStorage.getItem('token'));
  
  if (sessionToken) {
    console.log('✅ 使用Session Storage token:', sessionToken);
    return sessionToken;
  } else if (localToken) {
    console.log('⚠️ Session Storage中没有token，使用Local Storage token:', localToken);
    return localToken;
  } else {
    console.log('❌ 没有找到任何token');
    return null;
  }
}

// 检查当前存储状态
console.log('=== 当前存储状态 ===');
const sessionUserInfo = sessionStorage.getItem('userInfo');
const localUserInfo = localStorage.getItem('userInfo');

if (sessionUserInfo) {
  const user = JSON.parse(sessionUserInfo);
  console.log('✅ Session Storage用户信息:', user.userName);
  console.log('- accessToken:', user.accessToken);
} else {
  console.log('❌ Session Storage中没有用户信息');
}

if (localUserInfo) {
  const user = JSON.parse(localUserInfo);
  console.log('✅ Local Storage用户信息:', user.userName);
  console.log('- accessToken:', user.accessToken);
} else {
  console.log('❌ Local Storage中没有用户信息');
}

// 测试token获取
console.log('\n=== 测试token获取 ===');
const currentToken = getTokenForRequest();

if (currentToken) {
  console.log('\n=== 模拟API请求头 ===');
  console.log('Headers:');
  console.log('- requesttype: app');
  console.log('- appid: 6551b0147c4649a894e86bf8de248da4');
  console.log('- secretkey: 58f9eeefc65f4b318204ba21f39a8861');
  console.log('- accesstoken:', currentToken);
  
  // 检查token来源
  const sessionToken = sessionStorage.getItem('accessToken') || sessionStorage.getItem('token');
  const localToken = localStorage.getItem('accessToken') || localStorage.getItem('token');
  
  if (currentToken === sessionToken) {
    console.log('✅ 正确使用Session Storage中的token');
  } else if (currentToken === localToken) {
    console.log('⚠️ 使用了Local Storage中的token，Session Storage中可能没有token');
  }
} else {
  console.log('❌ 没有可用的token');
}

// 检查token一致性
console.log('\n=== 检查token一致性 ===');
const sessionAccessToken = sessionStorage.getItem('accessToken');
const sessionToken = sessionStorage.getItem('token');
const localAccessToken = localStorage.getItem('accessToken');
const localToken = localStorage.getItem('token');

const allTokens = [sessionAccessToken, sessionToken, localAccessToken, localToken].filter(Boolean);
const uniqueTokens = [...new Set(allTokens)];

if (uniqueTokens.length === 1) {
  console.log('✅ 所有存储中的token都是一致的:', uniqueTokens[0]);
} else if (uniqueTokens.length > 1) {
  console.log('⚠️ 存储中有多个不同的token:');
  uniqueTokens.forEach((token, index) => {
    console.log(`  ${index + 1}. ${token}`);
  });
  console.log('💡 建议清理不一致的token');
} else {
  console.log('❌ 没有找到任何token');
}

console.log('\n📝 说明：');
console.log('- 如果看到"使用Session Storage token"，说明token获取正确');
console.log('- 如果看到"使用Local Storage token"，说明Session Storage中没有token');
console.log('- 如果看到多个不同的token，说明存储不一致，需要清理');
console.log('- 正确的token应该来自Session Storage'); 