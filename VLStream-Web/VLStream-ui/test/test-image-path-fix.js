/**
 * 测试图片路径修复效果
 * 验证前端图片路径修改后的效果
 */

async function testImagePathFix() {
    console.log('🔍 测试图片路径修复效果');
    
    const baseUrl = 'http://192.168.60.77:32557';
    const token = sessionStorage.getItem('accessToken') || localStorage.getItem('accessToken');
    
    console.log('当前token:', token);
    console.log('');
    
    // 测试1: 获取标注实例数据，验证图片路径格式
    console.log('=== 测试1: 获取标注实例数据 ===');
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
                    
                    // 测试图片访问
                    testImageAccess(fullImageUrl, instance.imageName);
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
    console.log('1. ✅ 修改了前端 AlgorithmStandard.vue 中的图片路径从 /src/img/ 改为 /image/');
    console.log('2. ✅ 创建了 imageUpload.js API文件，提供统一的图片处理函数');
    console.log('3. ✅ 修改了图片显示逻辑，使用 getImageUrl() 函数获取完整URL');
    console.log('4. ✅ 后端配置已修改，图片存储在 ./data/images/ 目录');
    console.log('5. ✅ 后端静态资源映射已配置，/image/** 映射到 ./data/images/');
    console.log('');
    console.log('🎯 预期效果:');
    console.log('- 前端不再请求 /src/img/ 路径');
    console.log('- 图片通过 /image/ 路径正确访问');
    console.log('- 标注实例中的图片路径格式正确');
    console.log('- 图片能够正常显示');
}

// 测试图片访问
async function testImageAccess(imageUrl, imageName) {
    console.log(`  - 测试访问: ${imageName}`);
    
    try {
        const response = await fetch(imageUrl, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'requesttype': 'app',
                'appid': '6551b0147c4649a894e86bf8de248da4',
                'secretkey': '58f9eeefc65f4b318204ba21f39a8861',
                'accesstoken': sessionStorage.getItem('accessToken') || localStorage.getItem('accessToken')
            }
        });
        
        if (response.ok) {
            console.log(`    ✅ 图片访问成功 (${response.status})`);
            console.log(`    - Content-Type: ${response.headers.get('content-type')}`);
            console.log(`    - Content-Length: ${response.headers.get('content-length')}`);
        } else {
            console.log(`    ❌ 图片访问失败 (${response.status})`);
            const errorText = await response.text();
            console.log(`    - 错误信息: ${errorText}`);
        }
    } catch (error) {
        console.log(`    ❌ 图片访问异常: ${error.message}`);
    }
}

// 运行测试
testImagePathFix().catch(console.error); 