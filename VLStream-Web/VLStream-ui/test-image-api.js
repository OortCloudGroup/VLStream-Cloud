// 测试后端图片API是否正常工作
const axios = require('axios');

async function testImageAPI() {
  const baseURL = 'http://localhost:18080';
  const annotationId = 1;
  const imageName = 'Xnip2024-08-20_17-03-32.png';
  
  console.log('=== 测试后端图片API ===');
  console.log(`基础URL: ${baseURL}`);
  console.log(`标注项目ID: ${annotationId}`);
  console.log(`图片名称: ${imageName}`);
  
  try {
    // 测试1: 检查API端点是否存在
    console.log('\n1. 测试API端点...');
    const apiUrl = `${baseURL}/api/annotation-images/dataset/${annotationId}/image/${imageName}`;
    console.log(`API URL: ${apiUrl}`);
    
    const response = await axios.get(apiUrl, {
      timeout: 10000,
      responseType: 'arraybuffer'
    });
    
    console.log('✅ API调用成功!');
    console.log(`状态码: ${response.status}`);
    console.log(`内容类型: ${response.headers['content-type']}`);
    console.log(`内容长度: ${response.data.length} bytes`);
    
    // 测试2: 检查图片文件是否存在
    console.log('\n2. 检查图片文件...');
    const filePath = `E:/work/vls-tr/VLStream-server/data/images/${imageName}`;
    const fs = require('fs');
    
    if (fs.existsSync(filePath)) {
      const stats = fs.statSync(filePath);
      console.log('✅ 图片文件存在!');
      console.log(`文件路径: ${filePath}`);
      console.log(`文件大小: ${stats.size} bytes`);
      console.log(`修改时间: ${stats.mtime}`);
    } else {
      console.log('❌ 图片文件不存在!');
      console.log(`期望路径: ${filePath}`);
    }
    
  } catch (error) {
    console.log('❌ API调用失败!');
    if (error.response) {
      console.log(`状态码: ${error.response.status}`);
      console.log(`错误信息: ${error.response.statusText}`);
    } else if (error.request) {
      console.log('请求错误: 无法连接到服务器');
    } else {
      console.log(`错误: ${error.message}`);
    }
  }
}

// 运行测试
testImageAPI().catch(console.error);




