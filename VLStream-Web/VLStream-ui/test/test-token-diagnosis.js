/**
 * Token诊断脚本
 * 找出token存储位置和用户信息来源
 */

function diagnoseTokenStorage() {
    console.log('🔍 Token存储诊断');
    
    // 检查所有可能的token存储位置
    console.log('=== 检查所有Token存储位置 ===');
    
    // 1. 检查URL参数
    const urlParams = new URLSearchParams(window.location.search);
    const urlToken = urlParams.get('accessToken') || urlParams.get('token');
    console.log('1. URL参数中的token:', urlToken ? urlToken.substring(0, 8) + '...' : 'null');
    
    // 2. 检查sessionStorage
    const sessionToken = sessionStorage.getItem('accessToken');
    console.log('2. sessionStorage中的accessToken:', sessionToken ? sessionToken.substring(0, 8) + '...' : 'null');
    
    // 3. 检查localStorage
    const localToken = localStorage.getItem('accessToken');
    console.log('3. localStorage中的accessToken:', localToken ? localToken.substring(0, 8) + '...' : 'null');
    
    // 4. 检查其他可能的token字段
    const otherTokens = {
        'localStorage.token': localStorage.getItem('token'),
        'localStorage.userToken': localStorage.getItem('userToken'),
        'localStorage.authToken': localStorage.getItem('authToken'),
        'sessionStorage.token': sessionStorage.getItem('token'),
        'sessionStorage.userToken': sessionStorage.getItem('userToken'),
        'sessionStorage.authToken': sessionStorage.getItem('authToken')
    };
    
    console.log('4. 其他可能的token字段:');
    Object.entries(otherTokens).forEach(([key, value]) => {
        if (value) {
            console.log(`   ${key}:`, value.substring(0, 8) + '...');
        }
    });
    
    // 5. 检查cookie
    const cookies = document.cookie.split(';');
    const cookieTokens = {};
    cookies.forEach(cookie => {
        const [name, value] = cookie.trim().split('=');
        if (name.includes('token') || name.includes('Token')) {
            cookieTokens[name] = value;
        }
    });
    
    console.log('5. Cookie中的token:');
    if (Object.keys(cookieTokens).length > 0) {
        Object.entries(cookieTokens).forEach(([name, value]) => {
            console.log(`   ${name}:`, value.substring(0, 8) + '...');
        });
    } else {
        console.log('   没有找到token相关的cookie');
    }
    
    console.log('');
    
    // 检查用户信息存储
    console.log('=== 检查用户信息存储 ===');
    
    // 1. 检查sessionStorage中的用户信息
    const sessionUserInfo = sessionStorage.getItem('userInfo');
    console.log('1. sessionStorage中的userInfo:', sessionUserInfo ? '存在' : 'null');
    
    // 2. 检查localStorage中的用户信息
    const localUserInfo = localStorage.getItem('userInfo');
    console.log('2. localStorage中的userInfo:', localUserInfo ? '存在' : 'null');
    
    // 3. 解析用户信息
    if (sessionUserInfo) {
        try {
            const parsedUserInfo = JSON.parse(sessionUserInfo);
            console.log('3. sessionStorage用户信息详情:');
            console.log('   - 用户名:', parsedUserInfo.userName);
            console.log('   - 用户ID:', parsedUserInfo.userId);
            console.log('   - 登录ID:', parsedUserInfo.loginId);
            console.log('   - 租户ID:', parsedUserInfo.tenantId);
            console.log('   - 是否有token字段:', 'accessToken' in parsedUserInfo);
            if (parsedUserInfo.accessToken) {
                console.log('   - 用户信息中的token:', parsedUserInfo.accessToken.substring(0, 8) + '...');
            }
        } catch (error) {
            console.log('3. sessionStorage用户信息解析失败:', error.message);
        }
    }
    
    if (localUserInfo) {
        try {
            const parsedUserInfo = JSON.parse(localUserInfo);
            console.log('4. localStorage用户信息详情:');
            console.log('   - 用户名:', parsedUserInfo.userName);
            console.log('   - 用户ID:', parsedUserInfo.userId);
            console.log('   - 登录ID:', parsedUserInfo.loginId);
            console.log('   - 租户ID:', parsedUserInfo.tenantId);
            console.log('   - 是否有token字段:', 'accessToken' in parsedUserInfo);
            if (parsedUserInfo.accessToken) {
                console.log('   - 用户信息中的token:', parsedUserInfo.accessToken.substring(0, 8) + '...');
            }
        } catch (error) {
            console.log('4. localStorage用户信息解析失败:', error.message);
        }
    }
    
    console.log('');
    
    // 检查租户信息存储
    console.log('=== 检查租户信息存储 ===');
    
    const sessionTenantInfo = sessionStorage.getItem('tenantInfo');
    const localTenantInfo = localStorage.getItem('tenantInfo');
    
    console.log('1. sessionStorage中的tenantInfo:', sessionTenantInfo ? '存在' : 'null');
    console.log('2. localStorage中的tenantInfo:', localTenantInfo ? '存在' : 'null');
    
    if (sessionTenantInfo) {
        try {
            const parsedTenantInfo = JSON.parse(sessionTenantInfo);
            console.log('3. sessionStorage租户信息详情:');
            console.log('   - 租户名称:', parsedTenantInfo.tenantName);
            console.log('   - 租户ID:', parsedTenantInfo.tenantId);
            console.log('   - 租户状态:', parsedTenantInfo.status);
        } catch (error) {
            console.log('3. sessionStorage租户信息解析失败:', error.message);
        }
    }
    
    if (localTenantInfo) {
        try {
            const parsedTenantInfo = JSON.parse(localTenantInfo);
            console.log('4. localStorage租户信息详情:');
            console.log('   - 租户名称:', parsedTenantInfo.tenantName);
            console.log('   - 租户ID:', parsedTenantInfo.tenantId);
            console.log('   - 租户状态:', parsedTenantInfo.status);
        } catch (error) {
            console.log('4. localStorage租户信息解析失败:', error.message);
        }
    }
    
    console.log('');
    
    // 检查authManager状态
    console.log('=== 检查AuthManager状态 ===');
    
    if (window.authManager) {
        console.log('1. authManager已加载');
        console.log('2. 尝试调用authManager.checkLocalToken()');
        
        // 这里我们不能直接调用async函数，但可以检查authManager的方法
        console.log('3. authManager方法列表:', Object.getOwnPropertyNames(window.authManager));
    } else {
        console.log('1. authManager未加载');
    }
    
    console.log('');
    
    // 检查tokenSyncManager状态
    console.log('=== 检查TokenSyncManager状态 ===');
    
    if (window.tokenSyncManager) {
        console.log('1. tokenSyncManager已加载');
        console.log('2. 同步器状态:', {
            isInitialized: window.tokenSyncManager.isInitialized,
            syncInterval: window.tokenSyncManager.syncInterval ? '运行中' : '已停止',
            lastToken: window.tokenSyncManager.lastToken ? window.tokenSyncManager.lastToken.substring(0, 8) + '...' : 'null'
        });
    } else {
        console.log('1. tokenSyncManager未加载');
    }
    
    console.log('');
    
    // 分析结果
    console.log('=== 分析结果 ===');
    
    const allTokens = [
        { source: 'URL', token: urlToken },
        { source: 'sessionStorage.accessToken', token: sessionToken },
        { source: 'localStorage.accessToken', token: localToken },
        ...Object.entries(otherTokens).map(([key, value]) => ({ source: key, token: value }))
    ].filter(item => item.token);
    
    if (allTokens.length > 0) {
        console.log('✅ 找到的token:');
        allTokens.forEach(item => {
            console.log(`   ${item.source}: ${item.token.substring(0, 8)}...`);
        });
    } else {
        console.log('❌ 没有找到任何token');
    }
    
    if (sessionUserInfo || localUserInfo) {
        console.log('✅ 找到用户信息');
        console.log('💡 用户信息存在但token可能存储在其他地方');
        console.log('💡 建议检查用户信息对象中是否包含token字段');
    } else {
        console.log('❌ 没有找到用户信息');
    }
    
    console.log('');
    console.log('🎯 建议:');
    console.log('1. 检查用户信息对象中是否包含accessToken字段');
    console.log('2. 检查是否有其他token字段名');
    console.log('3. 检查token是否存储在全局变量中');
    console.log('4. 检查是否有加密或编码的token');
}

// 运行诊断
diagnoseTokenStorage(); 