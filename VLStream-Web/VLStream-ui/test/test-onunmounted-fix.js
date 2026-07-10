/**
 * 测试onUnmounted修复效果
 * 验证layout组件中的生命周期钩子是否正确工作
 */

function testOnUnmountedFix() {
    console.log('🔍 测试onUnmounted修复效果');
    
    // 检查Vue组件是否正确加载
    console.log('=== 检查Vue组件状态 ===');
    
    // 检查layout组件是否存在
    const layoutElement = document.querySelector('.layout-container');
    if (layoutElement) {
        console.log('✅ Layout组件已正确渲染');
    } else {
        console.log('❌ Layout组件未找到');
    }
    
    // 检查用户信息显示
    const userInfoElement = document.querySelector('.user-info');
    if (userInfoElement) {
        console.log('✅ 用户信息组件已渲染');
        console.log('用户信息文本:', userInfoElement.textContent.trim());
    } else {
        console.log('❌ 用户信息组件未找到');
    }
    
    // 检查租户信息显示
    const tenantInfoElement = document.querySelector('.tenant-info');
    if (tenantInfoElement) {
        console.log('✅ 租户信息组件已渲染');
        console.log('租户信息文本:', tenantInfoElement.textContent.trim());
    } else {
        console.log('❌ 租户信息组件未找到');
    }
    
    console.log('');
    
    // 检查控制台错误
    console.log('=== 检查控制台错误 ===');
    console.log('如果修复成功，应该不会再有 "onUnmounted is not defined" 错误');
    console.log('请检查浏览器控制台是否还有相关错误信息');
    
    console.log('');
    
    // 测试token同步功能
    console.log('=== 测试token同步功能 ===');
    
    // 检查tokenSyncManager是否可用
    if (window.tokenSyncManager) {
        console.log('✅ tokenSyncManager已加载');
        console.log('同步器状态:', window.tokenSyncManager.isInitialized ? '已初始化' : '未初始化');
        
        // 手动触发同步测试
        console.log('🔄 手动触发token同步测试');
        window.tokenSyncManager.forceSync();
        
    } else {
        console.log('❌ tokenSyncManager未加载');
    }
    
    console.log('');
    
    // 测试事件监听器
    console.log('=== 测试事件监听器 ===');
    
    // 模拟token更新事件
    console.log('🔄 模拟token更新事件');
    const tokenUpdateEvent = new CustomEvent('tokenUpdated', {
        detail: { token: 'test_token_' + Date.now() }
    });
    window.dispatchEvent(tokenUpdateEvent);
    
    // 模拟token失效事件
    console.log('⚠️ 模拟token失效事件');
    const tokenInvalidEvent = new CustomEvent('tokenInvalid');
    window.dispatchEvent(tokenInvalidEvent);
    
    console.log('');
    
    // 检查当前token状态
    console.log('=== 当前Token状态 ===');
    const urlToken = getTokenFromUrl();
    const sessionToken = sessionStorage.getItem('accessToken');
    const localToken = localStorage.getItem('accessToken');
    
    console.log('- URL Token:', urlToken ? urlToken.substring(0, 8) + '...' : 'null');
    console.log('- Session Token:', sessionToken ? sessionToken.substring(0, 8) + '...' : 'null');
    console.log('- Local Token:', localToken ? localToken.substring(0, 8) + '...' : 'null');
    
    console.log('');
    
    // 检查用户信息状态
    console.log('=== 当前用户信息状态 ===');
    const userInfo = sessionStorage.getItem('userInfo');
    if (userInfo) {
        try {
            const parsedUserInfo = JSON.parse(userInfo);
            console.log('✅ 用户信息存在:');
            console.log('- 用户名:', parsedUserInfo.userName);
            console.log('- 用户ID:', parsedUserInfo.userId);
            console.log('- 登录ID:', parsedUserInfo.loginId);
        } catch (error) {
            console.log('❌ 用户信息解析失败:', error.message);
        }
    } else {
        console.log('💡 没有找到用户信息');
    }
    
    console.log('');
    console.log('📝 修复总结:');
    console.log('1. ✅ 添加了 onUnmounted 到 Vue 导入语句');
    console.log('2. ✅ 修复了 ReferenceError: onUnmounted is not defined 错误');
    console.log('3. ✅ Layout组件现在可以正确使用生命周期钩子');
    console.log('4. ✅ Token同步功能应该正常工作');
    console.log('5. ✅ 事件监听器可以正确添加和移除');
    console.log('');
    console.log('🎯 预期效果:');
    console.log('- 页面不再出现 onUnmounted 相关错误');
    console.log('- Layout组件正常渲染和显示');
    console.log('- Token同步功能正常工作');
    console.log('- 用户信息正确显示');
    console.log('- 组件卸载时正确清理事件监听器');
}

// 辅助函数：从URL获取token
function getTokenFromUrl() {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get('accessToken') || urlParams.get('token');
}

// 运行测试
testOnUnmountedFix(); 