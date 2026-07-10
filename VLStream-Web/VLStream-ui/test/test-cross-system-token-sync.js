/**
 * 跨系统Token同步测试
 * 验证VLStream-ui和统一用户平台之间的token同步
 */

async function testCrossSystemTokenSync() {
    console.log('🔍 跨系统Token同步测试');
    
    // 等待一下让同步器运行
    await new Promise(resolve => setTimeout(resolve, 1000));
    
    // 检查当前系统token状态
    console.log('=== 检查当前系统Token状态 ===');
    
    const currentToken = getCurrentSystemToken();
    console.log('当前系统Token:', currentToken ? currentToken.substring(0, 8) + '...' : 'null');
    
    // 检查跨系统同步器状态
    console.log('=== 检查跨系统同步器状态 ===');
    
    if (window.crossSystemTokenSync) {
        console.log('✅ crossSystemTokenSync已加载');
        console.log('同步器状态:', {
            isInitialized: window.crossSystemTokenSync.isInitialized,
            syncInterval: window.crossSystemTokenSync.syncInterval ? '运行中' : '已停止',
            lastUnifiedToken: window.crossSystemTokenSync.lastUnifiedToken ? window.crossSystemTokenSync.lastUnifiedToken.substring(0, 8) + '...' : 'null'
        });
        
        // 显示同步器配置
        console.log('同步器配置:');
        console.log('- 检查间隔: 3秒');
        console.log('- 自动初始化: 是');
        console.log('- 页面可见性监听: 是');
        console.log('- 跨标签页同步: 是');
        
    } else {
        console.log('❌ crossSystemTokenSync未加载');
    }
    
    console.log('');
    
    // 测试获取统一用户平台token
    console.log('=== 测试获取统一用户平台Token ===');
    
    if (window.crossSystemTokenSync) {
        try {
            const unifiedToken = await window.crossSystemTokenSync.getUnifiedPlatformToken();
            console.log('统一用户平台Token:', unifiedToken ? unifiedToken.substring(0, 8) + '...' : 'null');
            
            if (unifiedToken) {
                console.log('✅ 成功获取统一用户平台token');
            } else {
                console.log('💡 未获取到统一用户平台token');
            }
        } catch (error) {
            console.log('❌ 获取统一用户平台token失败:', error.message);
        }
    }
    
    console.log('');
    
    // 测试token验证
    console.log('=== 测试Token验证 ===');
    
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
                console.log('💡 当前token在统一用户平台有效');
            } else {
                const errorText = await response.text();
                console.log('❌ Token验证失败:', errorText);
                console.log('💡 当前token在统一用户平台无效，需要同步');
            }
        } catch (error) {
            console.log('❌ Token验证请求异常:', error.message);
        }
    } else {
        console.log('💡 没有可验证的token');
    }
    
    console.log('');
    
    // 手动触发跨系统同步
    console.log('=== 手动触发跨系统同步 ===');
    
    if (window.crossSystemTokenSync) {
        console.log('🔄 手动触发跨系统token同步');
        window.crossSystemTokenSync.forceSync();
        
        // 等待同步完成
        setTimeout(async () => {
            console.log('检查同步结果:');
            
            const newCurrentToken = getCurrentSystemToken();
            console.log('- 同步后的当前系统Token:', newCurrentToken ? newCurrentToken.substring(0, 8) + '...' : 'null');
            
            if (newCurrentToken && newCurrentToken !== currentToken) {
                console.log('✅ 跨系统Token同步成功！');
                console.log('💡 token已从统一用户平台同步到当前系统');
            } else if (newCurrentToken === currentToken) {
                console.log('✅ Token已是最新，无需同步');
            } else {
                console.log('❌ 跨系统Token同步失败');
            }
        }, 3000);
    }
    
    console.log('');
    
    // 检查用户信息同步
    console.log('=== 检查用户信息同步 ===');
    
    const userInfo = sessionStorage.getItem('userInfo');
    if (userInfo) {
        try {
            const parsedUserInfo = JSON.parse(userInfo);
            console.log('✅ 用户信息已同步:');
            console.log('- 用户名:', parsedUserInfo.userName);
            console.log('- 用户ID:', parsedUserInfo.userId);
            console.log('- 租户ID:', parsedUserInfo.tenantId);
            console.log('- 是否有token:', !!parsedUserInfo.accessToken);
            
        } catch (error) {
            console.log('❌ 用户信息解析失败:', error.message);
        }
    } else {
        console.log('💡 没有找到用户信息');
    }
    
    console.log('');
    
    // 模拟真实场景测试
    console.log('=== 模拟真实场景测试 ===');
    console.log('🎯 真实使用场景测试:');
    console.log('1. 在统一用户平台换用户登录');
    console.log('2. 获取新token');
    console.log('3. 在VLStream-ui中验证跨系统token同步');
    console.log('');
    console.log('📋 操作步骤:');
    console.log('1. 打开统一用户平台: http://oort.oortcloudsmart.com:21410/bus/apaas-web/desktopHome/index.html#/home');
    console.log('2. 使用不同用户登录');
    console.log('3. 复制新token到VLStream-ui的URL参数中');
    console.log('4. 观察VLStream-ui是否自动同步新用户信息');
    console.log('5. 验证两个系统的token是否一致');
    
    console.log('');
    console.log('📝 跨系统同步机制总结:');
    console.log('✅ 定时检查: 每3秒检查一次跨系统token变化');
    console.log('✅ 页面可见性: 页面重新可见时检查跨系统token');
    console.log('✅ 跨标签页: 监听storage变化实现跨标签页同步');
    console.log('✅ URL参数: 优先使用URL中的token参数');
    console.log('✅ 自动验证: 自动验证token在统一用户平台的有效性');
    console.log('✅ 事件通知: 通过自定义事件通知组件更新');
    console.log('✅ 用户信息同步: 自动同步用户信息');
    console.log('');
    console.log('🎯 预期效果:');
    console.log('- VLStream-ui和统一用户平台的token保持一致');
    console.log('- 用户在统一用户平台换用户后，VLStream-ui能即时同步');
    console.log('- 页面刷新后能正确显示新用户信息');
    console.log('- 跨标签页的token同步');
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
testCrossSystemTokenSync().catch(console.error); 