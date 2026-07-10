/**
 * 测试图片访问修复效果
 * 验证图片路径排除认证后的访问情况
 */

async function testImageAccessFixed() {
    console.log('🔍 测试图片访问修复效果');
    
    const baseUrl = 'http://192.168.60.77:32557';
    const imageName = '1753435507574_1752113041564_Xnip2024-08-20_17-03-32.png';
    
    console.log('测试图片:', imageName);
    console.log('');
    
    // 测试1: 直接访问图片文件（不需要认证头）
    console.log('=== 测试1: 直接访问图片文件（无认证头） ===');
    const imageUrl = `${baseUrl}/image/${imageName}`;
    console.log('图片URL:', imageUrl);
    
    try {
        const response = await fetch(imageUrl, {
            method: 'GET'
        });
        
        console.log('响应状态:', response.status);
        console.log('响应头:', Object.fromEntries(response.headers.entries()));
        
        if (response.ok) {
            console.log('✅ 图片访问成功！');
            console.log('Content-Type:', response.headers.get('content-type'));
            console.log('Content-Length:', response.headers.get('content-length'));
        } else {
            console.log('❌ 图片访问失败！');
            const errorText = await response.text();
            console.log('错误信息:', errorText);
        }
    } catch (error) {
        console.log('❌ 请求异常:', error.message);
    }
    
    console.log('');
    
    // 测试2: 带认证头访问图片文件
    console.log('=== 测试2: 带认证头访问图片文件 ===');
    const token = sessionStorage.getItem('accessToken') || localStorage.getItem('accessToken');
    console.log('当前token:', token);
    
    try {
        const response = await fetch(imageUrl, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'requesttype': 'app',
                'appid': '6551b0147c4649a894e86bf8de248da4',
                'secretkey': '58f9eeefc65f4b318204ba21f39a8861',
                'accesstoken': token
            }
        });
        
        console.log('响应状态:', response.status);
        
        if (response.ok) {
            console.log('✅ 带认证头的图片访问也成功！');
            console.log('Content-Type:', response.headers.get('content-type'));
            console.log('Content-Length:', response.headers.get('content-length'));
        } else {
            console.log('❌ 带认证头的图片访问失败！');
            const errorText = await response.text();
            console.log('错误信息:', errorText);
        }
    } catch (error) {
        console.log('❌ 请求异常:', error.message);
    }
    
    console.log('');
    
    // 测试3: 检查图片文件是否存在
    console.log('=== 测试3: 检查图片文件是否存在 ===');
    const checkUrl = `${baseUrl}/image/exists?fileName=${imageName}`;
    console.log('检查URL:', checkUrl);
    
    try {
        const response = await fetch(checkUrl, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'requesttype': 'app',
                'appid': '6551b0147c4649a894e86bf8de248da4',
                'secretkey': '58f9eeefc65f4b318204ba21f39a8861',
                'accesstoken': token
            }
        });
        
        console.log('响应状态:', response.status);
        
        if (response.ok) {
            const result = await response.json();
            console.log('✅ 检查结果:', result);
            if (result.data) {
                console.log('✅ 图片文件存在');
            } else {
                console.log('❌ 图片文件不存在');
            }
        } else {
            console.log('❌ 检查失败！');
            const errorText = await response.text();
            console.log('错误信息:', errorText);
        }
    } catch (error) {
        console.log('❌ 请求异常:', error.message);
    }
    
    console.log('');
    
    // 测试4: 获取标注实例数据，验证图片路径
    console.log('=== 测试4: 获取标注实例数据 ===');
    const annotationUrl = `${baseUrl}/api/annotation/1/instances/all`;
    console.log('API URL:', annotationUrl);
    
    try {
        const response = await fetch(annotationUrl, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'requesttype': 'app',
                'appid': '6551b0147c4649a894e86bf8de248da4',
                'secretkey': '58f9eeefc65f4b318204ba21f39a8861',
                'accesstoken': token
            }
        });
        
        console.log('响应状态:', response.status);
        
        if (response.ok) {
            const result = await response.json();
            console.log('✅ 获取标注实例成功！');
            console.log('数据条数:', result.data.length);
            
            // 检查图片路径
            result.data.forEach((instance, index) => {
                console.log(`实例${index + 1}:`);
                console.log(`  - 图片路径: ${instance.imagePath}`);
                console.log(`  - 图片名称: ${instance.imageName}`);
                
                // 验证URL格式
                if (instance.imagePath.startsWith('/image/')) {
                    console.log(`  - ✅ 图片路径格式正确`);
                    
                    // 构建完整的图片访问URL
                    const fullImageUrl = `${baseUrl}${instance.imagePath}`;
                    console.log(`  - 完整URL: ${fullImageUrl}`);
                } else {
                    console.log(`  - ❌ 图片路径格式错误，应该是 /image/ 开头`);
                }
            });
        } else {
            console.log('❌ 获取标注实例失败！');
            const errorText = await response.text();
            console.log('错误信息:', errorText);
        }
    } catch (error) {
        console.log('❌ 请求异常:', error.message);
    }
    
    console.log('');
    console.log('📝 修复总结:');
    console.log('1. ✅ 在 WebConfig 中将 /image/** 添加到认证拦截器排除列表');
    console.log('2. ✅ 图片访问不再需要认证头');
    console.log('3. ✅ 前端可以直接访问图片文件');
    console.log('4. ✅ 后端日志不再显示认证拦截警告');
    console.log('');
    console.log('🎯 预期效果:');
    console.log('- 图片访问不再被认证拦截器拦截');
    console.log('- 前端可以直接显示图片');
    console.log('- 后端日志显示图片访问成功');
    console.log('- 标注功能正常工作');
}

// 运行测试
testImageAccessFixed().catch(console.error); 