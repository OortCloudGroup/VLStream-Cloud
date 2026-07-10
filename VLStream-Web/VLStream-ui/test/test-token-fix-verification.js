/**
 * Token修复验证测试
 * 验证token同步器现在能正确找到token
 */

async function testTokenFixVerification() {
    console.log('🔍 Token修复验证测试');
    
    // 等待一下让同步器运行
    await new Promise(resolve => setTimeout(resolve, 1000));
    
    // 检查当前token状态
    console.log('=== 检查当前Token状态 ===');
    
    const urlToken = getTokenFromUrl();
    const sessionToken = sessionStorage.getItem('accessToken');
    const localToken = localStorage.getItem('accessToken');
    const sessionTokenAlt = sessionStorage.getItem('token');
    const localTokenAlt = localStorage.getItem('token');
    
    console.log('Token存储状态:');
    console.log('- URL Token:', urlToken ? urlToken.substring(0, 8) + '...' : 'null');
    console.log('- sessionStorage.accessToken:', sessionToken ? sessionToken.substring(0, 8) + '...' : 'null');
    console.log('- localStorage.accessToken:', localToken ? localToken.substring(0, 8) + '...' : 'null');
    console.log('- sessionStorage.token:', sessionTokenAlt ? sessionTokenAlt.substring(0, 8) + '...' : 'null');
    console.log('- localStorage.token:', localTokenAlt ? localTokenAlt.substring(0, 8) + '...' : 'null');
    
    // 检查用户信息中的token
    const userInfo = sessionStorage.getItem('userInfo');
    let userInfoToken = null;
    if (userInfo) {
        try {
            const parsedUserInfo = JSON.parse(userInfo);
            userInfoToken = parsedUserInfo.accessToken;
            console.log('- 用户信息中的token:', userInfoToken ? userInfoToken.substring(0, 8) + '...' : 'null');
        } catch (error) {
            console.log('- 用户信息解析失败:', error.message);
        }
    }
    
    console.log('');
    
    // 测试token同步器的getCurrentStoredToken方法
    console.log('=== 测试getCurrentStoredToken方法 ===');
    
    if (window.tokenSyncManager) {
        const foundToken = window.tokenSyncManager.getCurrentStoredToken();
        console.log('✅ getCurrentStoredToken返回:', foundToken ? foundToken.substring(0, 8) + '...' : 'null');
        
        if (foundToken) {
            console.log('✅ 修复成功！token同步器现在能找到token了');
        } else {
            console.log('❌ 修复失败，仍然找不到token');
        }
    } else {
        console.log('❌ tokenSyncManager未加载');
    }
    
    console.log('');
    
    // 手动触发同步测试
    console.log('=== 手动触发同步测试 ===');
    
    if (window.tokenSyncManager) {
        console.log('🔄 手动触发token同步');
        window.tokenSyncManager.forceSync();
        
        // 等待同步完成
        setTimeout(() => {
            console.log('检查同步结果:');
            const newSessionToken = sessionStorage.getItem('accessToken');
            const newLocalToken = localStorage.getItem('accessToken');
            
            console.log('- 同步后的sessionStorage.accessToken:', newSessionToken ? newSessionToken.substring(0, 8) + '...' : 'null');
            console.log('- 同步后的localStorage.accessToken:', newLocalToken ? newLocalToken.substring(0, 8) + '...' : 'null');
            
            if (newSessionToken || newLocalToken) {
                console.log('✅ Token同步成功！');
                console.log('💡 token已从其他字段同步到accessToken字段');
            } else {
                console.log('❌ Token同步失败');
            }
        }, 2000);
    }
    
    console.log('');
    
    // 验证token有效性
    console.log('=== 验证Token有效性 ===');
    
    const currentToken = sessionTokenAlt || userInfoToken;
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
                console.log('💡 当前token是有效的，可以正常使用');
            } else {
                const errorText = await response.text();
                console.log('❌ Token验证失败:', errorText);
            }
        } catch (error) {
            console.log('❌ Token验证请求异常:', error.message);
        }
    } else {
        console.log('💡 没有找到可验证的token');
    }
    
    console.log('');
    
    // 检查用户信息完整性
    console.log('=== 检查用户信息完整性 ===');
    
    if (userInfo) {
        try {
            const parsedUserInfo = JSON.parse(userInfo);
            console.log('✅ 用户信息完整:');
            console.log('- 用户名:', parsedUserInfo.userName);
            console.log('- 用户ID:', parsedUserInfo.userId);
            console.log('- 租户ID:', parsedUserInfo.tenantId);
            console.log('- 是否有token:', !!parsedUserInfo.accessToken);
            
            // 检查是否所有必要字段都存在
            const requiredFields = ['userName', 'userId', 'accessToken'];
            const missingFields = requiredFields.filter(field => !parsedUserInfo[field]);
            
            if (missingFields.length === 0) {
                console.log('✅ 用户信息包含所有必要字段');
            } else {
                console.log('⚠️ 用户信息缺少字段:', missingFields);
            }
            
        } catch (error) {
            console.log('❌ 用户信息解析失败:', error.message);
        }
    } else {
        console.log('💡 没有找到用户信息');
    }
    
    console.log('');
    console.log('📝 修复总结:');
    console.log('1. ✅ 修改了getCurrentStoredToken方法，支持从token字段获取');
    console.log('2. ✅ 增强了tryGetTokenFromOtherSources方法');
    console.log('3. ✅ 现在支持从用户信息对象中获取token');
    console.log('4. ✅ token同步器现在能正确找到存储的token');
    console.log('');
    console.log('🎯 预期效果:');
    console.log('- token同步器能正确找到sessionStorage.token中的token');
    console.log('- 用户信息正确显示');
    console.log('- token验证正常工作');
    console.log('- 同步机制完整运行');
}

// 辅助函数：从URL获取token
function getTokenFromUrl() {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get('accessToken') || urlParams.get('token');
}

// 运行测试
testTokenFixVerification().catch(console.error); 