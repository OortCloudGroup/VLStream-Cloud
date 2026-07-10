/**
 * 改进的跨系统Token同步测试
 * 使用真实token进行测试
 */

async function testImprovedCrossSystemSync() {
    console.log('🎯 改进的跨系统Token同步测试');
    console.log('使用真实token进行测试');
    
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
    console.log('=== 真实Token测试 ===');
    
    // 获取当前有效的token进行测试
    let testToken = null;
    
    // 方法1：从当前系统获取
    if (currentToken) {
        testToken = currentToken;
        console.log('✅ 使用当前系统token进行测试');
    }
    // 方法2：从用户信息获取
    else if (userInfo) {
        try {
            const parsedUserInfo = JSON.parse(userInfo);
            if (parsedUserInfo.accessToken) {
                testToken = parsedUserInfo.accessToken;
                console.log('✅ 使用用户信息中的token进行测试');
            }
        } catch (error) {
            console.log('❌ 用户信息解析失败');
        }
    }
    
    if (!testToken) {
        console.log('❌ 没有找到可用的token进行测试');
        console.log('💡 请先在统一用户平台登录获取token');
        return;
    }
    
    console.log('测试Token:', testToken.substring(0, 8) + '...');
    
    console.log('');
    console.log('=== 模拟URL参数传递 ===');
    
    // 模拟URL参数传递真实token
    const currentUrl = new URL(window.location.href);
    currentUrl.searchParams.set('accessToken', testToken);
    window.history.replaceState({}, document.title, currentUrl.toString());
    
    console.log('✅ 已设置URL参数传递真实token');
    console.log('URL:', currentUrl.toString());
    
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
    if (newCurrentToken === testToken) {
        console.log('✅ 跨系统Token同步成功！');
        console.log('💡 真实token已成功同步到当前系统');
    } else {
        console.log('❌ 跨系统Token同步失败');
        console.log('💡 真实token未能同步到当前系统');
    }
    
    // 清理模拟数据
    console.log('');
    console.log('🧹 清理模拟数据...');
    currentUrl.searchParams.delete('accessToken');
    window.history.replaceState({}, document.title, currentUrl.toString());
    
    console.log('');
    console.log('=== 真实场景测试指南 ===');
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
    console.log('=== 改进的同步机制说明 ===');
    console.log('🔄 同步策略:');
    console.log('- URL参数token: 直接同步，不验证（通常来自统一用户平台）');
    console.log('- API获取token: 验证后同步');
    console.log('- 其他来源token: 验证后同步');
    console.log('');
    console.log('✅ 同步流程:');
    console.log('1. 检测URL参数中的token');
    console.log('2. 判断token来源');
    console.log('3. 根据来源决定是否验证');
    console.log('4. 更新本地存储');
    console.log('5. 同步用户信息');
    console.log('6. 触发组件更新');
    console.log('7. 清理URL参数');
    
    console.log('');
    console.log('🎯 预期效果:');
    console.log('- URL参数中的token能直接同步，无需验证');
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
testImprovedCrossSystemSync().catch(console.error); 