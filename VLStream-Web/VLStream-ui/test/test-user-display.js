// 测试用户信息显示
console.log('🚀 测试用户信息显示...\n');

// 模拟从localStorage获取用户信息
function getLocalUserInfo() {
  const userInfoStr = localStorage.getItem('userInfo');
  if (userInfoStr) {
    try {
      return JSON.parse(userInfoStr);
    } catch (error) {
      console.error('解析用户信息失败:', error);
      return null;
    }
  }
  return null;
}

// 模拟从sessionStorage获取用户信息
function getSessionUserInfo() {
  const userInfoStr = sessionStorage.getItem('userInfo');
  if (userInfoStr) {
    try {
      return JSON.parse(userInfoStr);
    } catch (error) {
      console.error('解析用户信息失败:', error);
      return null;
    }
  }
  return null;
}

// 检查用户信息
console.log('=== 检查本地存储的用户信息 ===');
const localUserInfo = getLocalUserInfo();
const sessionUserInfo = getSessionUserInfo();

console.log('localStorage用户信息:', localUserInfo);
console.log('sessionStorage用户信息:', sessionUserInfo);

if (localUserInfo) {
  console.log('✅ localStorage中有用户信息:');
  console.log('- 用户名:', localUserInfo.userName);
  console.log('- 用户ID:', localUserInfo.userId);
  console.log('- 租户ID:', localUserInfo.tenantId);
  console.log('- accessToken:', localUserInfo.accessToken);
} else if (sessionUserInfo) {
  console.log('✅ sessionStorage中有用户信息:');
  console.log('- 用户名:', sessionUserInfo.userName);
  console.log('- 用户ID:', sessionUserInfo.userId);
  console.log('- 租户ID:', sessionUserInfo.tenantId);
  console.log('- accessToken:', sessionUserInfo.accessToken);
} else {
  console.log('❌ 本地存储中没有用户信息');
}

// 检查token
console.log('\n=== 检查token ===');
const localToken = localStorage.getItem('accessToken');
const sessionToken = sessionStorage.getItem('accessToken');

console.log('localStorage token:', localToken);
console.log('sessionStorage token:', sessionToken);

if (localToken || sessionToken) {
  console.log('✅ 找到有效token');
} else {
  console.log('❌ 没有找到有效token');
}

console.log('\n📝 说明：');
console.log('- 如果用户信息存在，页面右上角应该显示用户名');
console.log('- 如果token存在，应该能正常调用API');
console.log('- 如果都没有，说明用户信息没有正确保存'); 