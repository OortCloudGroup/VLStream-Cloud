/**
 * 测试Token同步功能
 * 验证token的即时同步机制
 */

async function testTokenSync() {
    console.log('🔍 测试Token同步功能');
    
    // 获取当前token状态
    const urlToken = getTokenFromUrl();
    const sessionToken = sessionStorage.getItem('accessToken');
    const localToken = localStorage.getItem('accessToken');
    
    console.log('当前Token状态:');
    console.log('- URL Token:', urlToken ? urlToken.substring(0, 8) + '...' : 'null');
    console.log('- Session Token:', sessionToken ? sessionToken.substring(0, 8) + '...' : 'null');
    console.log('- Local Token:', localToken ? localToken.substring(0, 8) + '...' : 'null');
    console.log('');
    
    // 测试1: 模拟URL中有新token
    console.log('=== 测试1: 模拟URL中有新token ===');
    const newToken = 'test_new_token_' + Date.now();
    
    // 模拟在URL中添加新token
    const url = new URL(window.location.href);
    url.searchParams.set('accessToken', newToken);
    window.history.replaceState({}, document.title, url.toString());
    
    console.log('✅ 已在URL中添加新token:', newToken.substring(0, 8) + '...');
    console.log('请等待5秒让同步器检测到变化...');
    
    // 等待同步器检测
    setTimeout(() => {
        console.log('检查同步结果:');
        const newSessionToken = sessionStorage.getItem('accessToken');
        const newLocalToken = localStorage.getItem('accessToken');
        
        console.log('- 新Session Token:', newSessionToken ? newSessionToken.substring(0, 8) + '...' : 'null');
        console.log('- 新Local Token:', newLocalToken ? newLocalToken.substring(0, 8) + '...' : 'null');
        
        if (newSessionToken === newToken && newLocalToken === newToken) {
            console.log('✅ Token同步成功！');
        } else {
            console.log('❌ Token同步失败');
        }
        
        // 清除测试token
        url.searchParams.delete('accessToken');
        window.history.replaceState({}, document.title, url.toString());
        
    }, 6000);
    
    console.log('');
    
    // 测试2: 手动触发同步
    console.log('=== 测试2: 手动触发同步 ===');
    console.log('调用 tokenSyncManager.forceSync() 手动触发同步');
    
    // 检查tokenSyncManager是否可用
    if (window.tokenSyncManager) {
        console.log('✅ tokenSyncManager已加载');
        window.tokenSyncManager.forceSync();
    } else {
        console.log('❌ tokenSyncManager未加载');
    }
    
    console.log('');
    
    // 测试3: 验证token有效性
    console.log('=== 测试3: 验证token有效性 ===');
    const currentToken = sessionToken || localToken;
    
    if (currentToken) {
        try {
            const response = await fetch('http://oort.oortcloudsmart.com:21410/bus/apaas-sso/sso/v1/verifyToken', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'requesttype': 'app',
                    'appid': '6551b0147c4649a894e86bf8de248da4',
                    'secretkey': '58f9eeefc65f4b318204ba21f39a8861',
                    'accesstoken': currentToken
                },
                body: JSON.stringify({
                    accessToken: currentToken
                })
            });
            
            console.log('Token验证响应状态:', response.status);
            
            if (response.ok) {
                const result = await response.json();
                console.log('✅ Token验证成功:', result);
            } else {
                const errorText = await response.text();
                console.log('❌ Token验证失败:', errorText);
            }
        } catch (error) {
            console.log('❌ Token验证请求异常:', error.message);
        }
    } else {
        console.log('💡 没有可验证的token');
    }
    
    console.log('');
    
    // 测试4: 检查用户信息同步
    console.log('=== 测试4: 检查用户信息同步 ===');
    
    // 检查用户信息
    const userInfo = sessionStorage.getItem('userInfo');
    if (userInfo) {
        try {
            const parsedUserInfo = JSON.parse(userInfo);
            console.log('✅ 用户信息已同步:');
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
    
    // 测试5: 模拟页面可见性变化
    console.log('=== 测试5: 模拟页面可见性变化 ===');
    console.log('请切换到其他标签页，然后切换回来，观察控制台输出');
    console.log('应该会看到 "📱 页面重新可见，检查token同步" 的日志');
    
    console.log('');
    console.log('📝 同步机制说明:');
    console.log('1. 🔄 定时检查: 每5秒检查一次token变化');
    console.log('2. 📱 页面可见性: 页面重新可见时检查token');
    console.log('3. 🍪 跨标签页: 监听storage变化实现跨标签页同步');
    console.log('4. 🔗 URL参数: 优先使用URL中的token参数');
    console.log('5. ✅ 自动验证: 自动验证token有效性');
    console.log('6. 🎯 事件通知: 通过自定义事件通知组件更新');
    console.log('');
    console.log('🎯 预期效果:');
    console.log('- 用户在统一用户平台换用户后，VLStream-ui能即时同步新token');
    console.log('- 页面刷新后能正确显示新用户信息');
    console.log('- 跨标签页的token同步');
    console.log('- 自动清理无效token');
}

// 辅助函数：从URL获取token
function getTokenFromUrl() {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get('accessToken') || urlParams.get('token');
}

// 运行测试
testTokenSync().catch(console.error); 