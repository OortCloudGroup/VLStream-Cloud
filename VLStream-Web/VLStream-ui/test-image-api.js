/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

// after APIwhether
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
    // 1: API whether in
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

    // 2: whether in
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

//
testImageAPI().catch(console.error);




