/**
 * 真实跨系统Token同步测试
 * 模拟用户在统一用户平台换用户登录的场景
 */

async function testRealCrossSystemSync() {
    console.log('🎯 真实跨系统Token同步测试');
    console.log('模拟用户在统一用户平台换用户登录的场景');
    
    // 等待一下让同步器运行
    await new Promise(resolve => setTimeout(resolve, 1000));
    
    console.log('');
    console.log('=== 当前状态检查 ===');
    
    // 检查当前token状态
    const currentToken = getCurrentSystemToken();
    console.log('当前系统Token:', currentToken ? currentToken.substring(0, 8) + '...' : 'null');
    
    // 检查用户信息
    const userInfo = sessionStorage.getItem('userInfo');
    if (userInfo) {
        try {
            const parsedUserInfo = JSON.parse(userInfo);
            console.log('当前用户:', parsedUserInfo.userName);
        } catch (error) {
            console.log('用户信息解析失败');
        }
    }
    
    console.log('');
    console.log('=== 模拟跨系统Token同步场景 ===');
    
    // 模拟从统一用户平台获取新token
    console.log('📋 模拟场景1: 用户在统一用户平台换用户登录');
    console.log('1. 用户在统一用户平台登录新用户');
    console.log('2. 获取新token');
    console.log('3. 通过URL参数传递到VLStream-ui');
    
    // 模拟URL参数中的新token
    const mockNewToken = 'new_token_from_unified_platform_' + Date.now();
    const mockNewUserInfo = {
        userName: '新用户_' + Date.now(),
        userId: 'new_user_id_' + Date.now(),
        tenantId: 'new_tenant_id_' + Date.now(),
        accessToken: mockNewToken
    };
    
    console.log('');
    console.log('🔄 模拟新token同步过程:');
    console.log('- 新Token:', mockNewToken.substring(0, 8) + '...');
    console.log('- 新用户:', mockNewUserInfo.userName);
    
    // 模拟URL参数
    const currentUrl = new URL(window.location.href);
    currentUrl.searchParams.set('accessToken', mockNewToken);
    window.history.replaceState({}, document.title, currentUrl.toString());
    
    console.log('✅ 已设置URL参数模拟新token');
    
    // 等待同步器检测到变化
    console.log('⏳ 等待跨系统同步器检测到变化...');
    await new Promise(resolve => setTimeout(resolve, 4000));
    
    // 检查同步结果
    console.log('');
    console.log('=== 检查同步结果 ===');
    
    const newCurrentToken = getCurrentSystemToken();
    console.log('同步后的系统Token:', newCurrentToken ? newCurrentToken.substring(0, 8) + '...' : 'null');
    
    const newUserInfo = sessionStorage.getItem('userInfo');
    if (newUserInfo) {
        try {
            const parsedNewUserInfo = JSON.parse(newUserInfo);
            console.log('同步后的用户:', parsedNewUserInfo.userName);
        } catch (error) {
            console.log('新用户信息解析失败');
        }
    }
    
    // 验证同步是否成功
    if (newCurrentToken === mockNewToken) {
        console.log('✅ 跨系统Token同步成功！');
        console.log('💡 新token已成功同步到当前系统');
    } else {
        console.log('❌ 跨系统Token同步失败');
        console.log('💡 新token未能同步到当前系统');
    }
    
    // 清理模拟数据
    console.log('');
    console.log('🧹 清理模拟数据...');
    currentUrl.searchParams.delete('accessToken');
    window.history.replaceState({}, document.title, currentUrl.toString());
    
    console.log('');
    console.log('=== 真实测试建议 ===');
    console.log('🎯 为了验证真实的跨系统同步，请按以下步骤操作:');
    console.log('');
    console.log('1. 打开统一用户平台:');
    console.log('   http://oort.oortcloudsmart.com:21410/bus/apaas-web/desktopHome/index.html#/home');
    console.log('');
    console.log('2. 使用不同用户登录，获取新token');
    console.log('');
    console.log('3. 将新token添加到VLStream-ui的URL中:');
    console.log('   http://oort.oortcloudsmart.com:21410/bus/vls-ui?accessToken=新token值');
    console.log('');
    console.log('4. 观察VLStream-ui是否自动同步新用户信息');
    console.log('');
    console.log('5. 验证两个系统的token是否一致');
    
    console.log('');
    console.log('=== 跨系统同步机制说明 ===');
    console.log('🔄 同步触发条件:');
    console.log('- URL参数中有新token');
    console.log('- 定时检查发现token变化');
    console.log('- 页面重新可见时检查');
    console.log('- 跨标签页storage变化');
    console.log('');
    console.log('✅ 同步验证机制:');
    console.log('- 自动验证token在统一用户平台的有效性');
    console.log('- 验证成功后更新本地存储');
    console.log('- 同步用户信息');
    console.log('- 触发组件更新事件');
    console.log('- 清理URL参数');
    
    console.log('');
    console.log('🎯 预期效果:');
    console.log('- 用户在统一用户平台换用户后，VLStream-ui能即时同步');
    console.log('- 页面刷新后能正确显示新用户信息');
    console.log('- 两个系统的token保持一致');
    console.log('- 自动清理无效token');
}

// 获取当前系统token
function getCurrentSystemToken() {
    return sessionStorage.getItem('accessToken') || 
           localStorage.getItem('accessToken') ||
           sessionStorage.getItem('token') ||
           localStorage.getItem('token')
}

// 运行测试
testRealCrossSystemSync().catch(console.error); 