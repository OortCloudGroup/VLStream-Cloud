/**
 * 真实Token获取测试
 * 测试从统一用户平台获取最新token的功能
 */

async function testRealTokenFetch() {
    console.log('🎯 真实Token获取测试');
    console.log('测试从统一用户平台获取最新token的功能');
    
    // 等待一下让获取器初始化
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
    console.log('=== 测试统一用户平台Token获取器 ===');
    
    if (window.unifiedPlatformTokenFetcher) {
        console.log('✅ unifiedPlatformTokenFetcher已加载');
        
        // 测试获取最新token
        console.log('🔄 开始获取统一用户平台最新token...');
        const latestToken = await window.unifiedPlatformTokenFetcher.getLatestToken()
        
        if (latestToken) {
            console.log('✅ 成功获取到统一用户平台最新token');
            console.log('最新Token:', latestToken.substring(0, 8) + '...');
            
            // 验证token有效性
            console.log('🔍 验证最新token有效性...');
            const isValid = await window.unifiedPlatformTokenFetcher.validateToken(latestToken)
            
            if (isValid) {
                console.log('✅ 最新token验证成功');
                
                // 同步到当前系统
                console.log('🔄 同步最新token到当前系统...');
                await syncTokenToCurrentSystem(latestToken)
                
            } else {
                console.log('❌ 最新token验证失败');
            }
            
        } else {
            console.log('❌ 未能获取到统一用户平台最新token');
            console.log('💡 可能需要手动获取token');
            
            // 提供手动获取选项
            console.log('');
            console.log('=== 手动获取Token选项 ===');
            console.log('1. 打开统一用户平台获取新token');
            console.log('2. 手动输入token');
            console.log('3. 查看获取token的详细说明');
            
            const choice = prompt('请选择操作 (1-3):')
            
            switch (choice) {
                case '1':
                    window.unifiedPlatformTokenFetcher.openUnifiedPlatform()
                    break
                case '2':
                    const manualToken = await window.unifiedPlatformTokenFetcher.manualInputToken()
                    if (manualToken) {
                        await syncTokenToCurrentSystem(manualToken)
                    }
                    break
                case '3':
                    window.unifiedPlatformTokenFetcher.showTokenInstructions()
                    break
                default:
                    console.log('未选择任何操作')
            }
        }
        
    } else {
        console.log('❌ unifiedPlatformTokenFetcher未加载');
    }
    
    console.log('');
    console.log('=== 真实场景测试指南 ===');
    console.log('🎯 为了验证真实的token获取，请按以下步骤操作:');
    console.log('');
    console.log('1. 打开统一用户平台:');
    console.log('   http://oort.oortcloudsmart.com:21410/bus/apaas-web/desktopHome/index.html#/home');
    console.log('');
    console.log('2. 使用不同用户登录，获取新token');
    console.log('');
    console.log('3. 在统一用户平台的控制台中运行:');
    console.log('   console.log("accessToken:", localStorage.getItem("accessToken"))');
    console.log('');
    console.log('4. 复制输出的token值');
    console.log('');
    console.log('5. 在VLStream-ui的控制台中运行:');
    console.log('   window.unifiedPlatformTokenFetcher.manualInputToken()');
    console.log('');
    console.log('6. 粘贴token值，观察是否自动同步');
    
    console.log('');
    console.log('=== Token获取机制说明 ===');
    console.log('🔄 获取策略:');
    console.log('- iframe方式: 通过隐藏iframe访问统一用户平台');
    console.log('- API方式: 通过API获取用户信息');
    console.log('- localStorage方式: 从本地存储获取');
    console.log('- 手动输入: 用户手动输入token');
    console.log('');
    console.log('✅ 验证机制:');
    console.log('- 自动验证token在统一用户平台的有效性');
    console.log('- 验证成功后同步到当前系统');
    console.log('- 更新用户信息和租户信息');
    console.log('- 触发组件更新事件');
    
    console.log('');
    console.log('🎯 预期效果:');
    console.log('- 能够获取到统一用户平台的最新有效token');
    console.log('- 自动同步到VLStream-ui系统');
    console.log('- 用户信息正确更新');
    console.log('- 两个系统的token保持一致');
}

// 同步token到当前系统
async function syncTokenToCurrentSystem(token) {
    try {
        console.log('🔄 开始同步token到当前系统')
        
        // 更新token存储
        sessionStorage.setItem('accessToken', token)
        localStorage.setItem('accessToken', token)
        sessionStorage.setItem('token', token)
        localStorage.setItem('token', token)
        
        // 获取并更新用户信息
        const userInfo = await getUserInfo(token)
        if (userInfo) {
            sessionStorage.setItem('userInfo', JSON.stringify(userInfo))
            localStorage.setItem('userInfo', JSON.stringify(userInfo))
            console.log('✅ 用户信息更新成功:', userInfo.userName)
        }
        
        // 触发token更新事件
        const event = new CustomEvent('tokenUpdated', {
            detail: { token, source: 'unified-platform-fetcher' }
        })
        window.dispatchEvent(event)
        
        console.log('✅ token同步到当前系统成功')
        
    } catch (error) {
        console.error('❌ token同步失败:', error)
    }
}

// 获取用户信息
async function getUserInfo(token) {
    try {
        const response = await fetch('http://oort.oortcloudsmart.com:21410/bus/apaas-sso/sso/v1/getUserInfo', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'requesttype': 'app',
                'appid': '6551b0147c4649a894e86bf8de248da4',
                'secretkey': '58f9eeefc65f4b318204ba21f39a8861',
                'accesstoken': token
            },
            body: JSON.stringify({
                accessToken: token
            })
        })
        
        if (response.ok) {
            const result = await response.json()
            return result.data
        }
        
        return null
    } catch (error) {
        console.error('获取用户信息失败:', error)
        return null
    }
}

// 获取当前系统token
function getCurrentSystemToken() {
    return sessionStorage.getItem('accessToken') || 
           localStorage.getItem('accessToken') ||
           sessionStorage.getItem('token') ||
           localStorage.getItem('token')
}

// 运行测试
testRealTokenFetch().catch(console.error); 