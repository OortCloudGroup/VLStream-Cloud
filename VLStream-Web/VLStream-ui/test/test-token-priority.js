// 测试token优先级逻辑
console.log('🚀 测试token优先级逻辑...\n');

// 模拟AuthManager的getCurrentToken方法
function getCurrentToken() {
  // 优先使用sessionStorage中的token
  const sessionToken = sessionStorage.getItem('token') || 
                      sessionStorage.getItem('accessToken') || 
                      sessionStorage.getItem('userCenterToken')
  
  if (sessionToken) {
    console.log('✅ 使用sessionStorage中的token:', sessionToken);
    return sessionToken;
  }
  
  // 如果sessionStorage中没有，才使用localStorage中的token
  const localToken = localStorage.getItem('token') || 
                    localStorage.getItem('accessToken') || 
                    localStorage.getItem('userCenterToken')
  
  if (localToken) {
    console.log('⚠️ 使用localStorage中的token:', localToken);
    return localToken;
  }
  
  console.log('❌ 没有找到任何token');
  return null;
}

// 模拟AuthManager的getCachedUserInfo方法
function getCachedUserInfo() {
  try {
    // 优先使用sessionStorage中的用户信息
    const sessionUser = sessionStorage.getItem('userInfo');
    if (sessionUser) {
      const userInfo = JSON.parse(sessionUser);
      console.log('✅ 使用sessionStorage中的用户信息:', userInfo.userName);
      return userInfo;
    }
    
    // 如果sessionStorage中没有，才使用localStorage中的用户信息
    const localUser = localStorage.getItem('userInfo');
    if (localUser) {
      const userInfo = JSON.parse(localUser);
      console.log('⚠️ 使用localStorage中的用户信息:', userInfo.userName);
      return userInfo;
    }
  } catch (error) {
    console.error('解析缓存用户信息失败:', error);
  }
  
  console.log('❌ 没有找到任何用户信息');
  return null;
}

// 检查当前存储状态
console.log('=== 当前存储状态 ===');
console.log('sessionStorage token:', sessionStorage.getItem('accessToken'));
console.log('localStorage token:', localStorage.getItem('accessToken'));

const sessionUserInfo = sessionStorage.getItem('userInfo');
const localUserInfo = localStorage.getItem('userInfo');

if (sessionUserInfo) {
  const user = JSON.parse(sessionUserInfo);
  console.log('sessionStorage 用户:', user.userName);
}

if (localUserInfo) {
  const user = JSON.parse(localUserInfo);
  console.log('localStorage 用户:', user.userName);
}

console.log('\n=== 测试优先级逻辑 ===');
const currentToken = getCurrentToken();
const currentUser = getCachedUserInfo();

console.log('\n📝 结果：');
console.log('- 当前使用的token:', currentToken);
console.log('- 当前使用的用户:', currentUser ? currentUser.userName : '无');

console.log('\n💡 说明：');
console.log('- 如果显示sessionStorage的token和用户，说明优先级正确');
console.log('- 如果显示localStorage的token和用户，说明sessionStorage中没有数据');
console.log('- 如果都显示"无"，说明存储中没有有效数据'); 