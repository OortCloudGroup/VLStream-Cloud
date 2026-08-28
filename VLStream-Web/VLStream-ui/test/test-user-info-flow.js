/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

// userinfoworkflow
console.log('🚀 测试用户信息流程...\n');

// userinfo
const mockUserInfo = {
  userId: "751fc4b0-81b4-4fe2-940b-ac18d7bc3439",
  userName: "周亮",
  tenantId: "0e391fd7-1033-4f09-88c0-187582fee462",
  accessToken: "421e68dff50a4c2d8387949d482a467a",
  loginId: "zhouliang"
};

// saveUserToLocal method
function saveUserToLocal(userInfo) {
  try {
    // localStorage and sessionStorage
    localStorage.setItem('userInfo', JSON.stringify(userInfo));
    localStorage.setItem('accessToken', userInfo.accessToken);
    sessionStorage.setItem('userInfo', JSON.stringify(userInfo));
    sessionStorage.setItem('accessToken', userInfo.accessToken);

    console.log('✅ 用户信息已保存到本地，包含tenantId:', userInfo.tenantId);
    return true;
  } catch (error) {
    console.error('❌ 保存用户信息失败:', error);
    return false;
  }
}

// checkLocalToken method
function checkLocalToken() {
  // only Session Storage in userinfo
  const sessionUserInfo = sessionStorage.getItem('userInfo');
  if (sessionUserInfo) {
    try {
      const userInfo = JSON.parse(sessionUserInfo);
      const sessionToken = sessionStorage.getItem('accessToken');
      if (sessionToken) {
        console.log('✅ Session Storage中有用户信息:', userInfo.userName);
        return userInfo;
      }
    } catch (error) {
      console.error('❌ Session Storage用户信息解析失败:', error);
      sessionStorage.removeItem('userInfo');
      sessionStorage.removeItem('accessToken');
    }
  }

  console.log('❌ Session Storage中没有有效的用户信息');
  return null;
}

// loadUserInfo method
function loadUserInfo() {
  try {
    // from Session StorageGet userinfo
    let userInfo = checkLocalToken();

    // if Session Storage in , from localStorageGet
    if (!userInfo) {
      const localUserInfo = localStorage.getItem('userInfo');
      if (localUserInfo) {
        try {
          userInfo = JSON.parse(localUserInfo);
          console.log('✅ 从localStorage获取到用户信息:', userInfo.userName);
          // userinfo Session Storage
          saveUserToLocal(userInfo);
        } catch (error) {
          console.error('❌ 解析localStorage用户信息失败:', error);
        }
      }
    }

    if (userInfo) {
      const currentUser = {
        userName: userInfo.userName || '管理员',
        userId: userInfo.userId || '',
        loginId: userInfo.loginId || ''
      };
      console.log('✅ 用户信息加载成功:', currentUser.userName);
      return currentUser;
    } else {
      console.log('❌ 没有找到有效的用户信息');
      return null;
    }
  } catch (error) {
    console.error('❌ 加载用户信息失败:', error);
    return null;
  }
}

// current
console.log('=== 当前存储状态 ===');
console.log('sessionStorage userInfo:', sessionStorage.getItem('userInfo') ? '存在' : '不存在');
console.log('sessionStorage accessToken:', sessionStorage.getItem('accessToken') ? '存在' : '不存在');
console.log('localStorage userInfo:', localStorage.getItem('userInfo') ? '存在' : '不存在');
console.log('localStorage accessToken:', localStorage.getItem('accessToken') ? '存在' : '不存在');

// userinfo
console.log('\n=== 测试保存用户信息 ===');
const saveResult = saveUserToLocal(mockUserInfo);
console.log('保存结果:', saveResult ? '成功' : '失败');

// token
console.log('\n=== 测试检查本地token ===');
const checkResult = checkLocalToken();
console.log('检查结果:', checkResult ? checkResult.userName : 'null');

// Load userinfo
console.log('\n=== 测试加载用户信息 ===');
const loadResult = loadUserInfo();
console.log('加载结果:', loadResult ? loadResult.userName : 'null');

//
console.log('\n=== 最终验证 ===');
const finalSessionUser = sessionStorage.getItem('userInfo');
const finalLocalUser = localStorage.getItem('userInfo');

if (finalSessionUser && finalLocalUser) {
  console.log('✅ 用户信息已正确保存到两个存储中');
  const sessionUser = JSON.parse(finalSessionUser);
  const localUser = JSON.parse(finalLocalUser);
  console.log('- Session Storage用户:', sessionUser.userName);
  console.log('- Local Storage用户:', localUser.userName);
} else {
  console.log('❌ 用户信息保存不完整');
}

console.log('\n📝 说明：');
console.log('- 如果所有测试都通过，页面右上角应该能正确显示用户名');
console.log('- 如果保存失败，检查存储权限');
console.log('- 如果加载失败，检查用户信息格式');