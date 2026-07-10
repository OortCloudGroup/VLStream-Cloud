/**
 * 测试图片访问功能
 * 验证图片路径修改后的访问情况
 */

async function testImageAccess() {
    console.log('🔍 测试图片访问功能');
    
    const baseUrl = 'http://192.168.60.77:32557';
    const token = sessionStorage.getItem('accessToken') || localStorage.getItem('accessToken');
    
    console.log('当前token:', token);
    console.log('');
    
    // 测试1: 直接访问图片文件
    console.log('=== 测试1: 直接访问图片文件 ===');
    const imageUrl = `${baseUrl}/image/1753432868248_1752113041564_Xnip2024-08-20_17-03-32.png`;
    console.log('图片URL:', imageUrl);
    
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
    
    // 测试2: 检查图片文件是否存在
    console.log('=== 测试2: 检查图片文件是否存在 ===');
    const checkUrl = `${baseUrl}/image/exists?fileName=1753432868248_1752113041564_Xnip2024-08-20_17-03-32.png`;
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
    
    // 测试3: 获取标注实例数据，验证图片路径
    console.log('=== 测试3: 获取标注实例数据 ===');
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
                
                // 构建完整的图片访问URL
                const fullImageUrl = `${baseUrl}${instance.imagePath}`;
                console.log(`  - 完整URL: ${fullImageUrl}`);
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
    console.log('📝 测试总结:');
    console.log('- 如果图片直接访问成功，说明静态资源映射配置正确');
    console.log('- 如果图片文件存在但访问失败，可能是权限或路径问题');
    console.log('- 如果标注实例中的图片路径正确，说明数据库存储正常');
}

// 运行测试
testImageAccess().catch(console.error); 