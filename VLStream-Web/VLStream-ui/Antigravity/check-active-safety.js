const fs = require('fs');
const path = require('path');

// 辅助方法：判断文件是否存在并读取内容
function getFileContent(filePath) {
  const absolutePath = path.resolve(__dirname, '..', filePath);
  if (!fs.existsSync(absolutePath)) {
    console.error(`❌ 错误：文件不存在 ${filePath}`);
    process.exit(1);
  }
  return fs.readFileSync(absolutePath, 'utf-8');
}

console.log('🔍 开始主动安全模块静态规范校验...\n');

// 1. 校验 src/layout/index.vue
const layoutContent = getFileContent('src/layout/index.vue');

// 1.1 校验顶部菜单中是否在“决策式AI”后添加了“主动安全”
const topMenusRegex = /topMenus\s*=\s*\[([\s\S]*?)\]/;
const matchTopMenus = layoutContent.match(topMenusRegex);
if (!matchTopMenus) {
  console.error('❌ 错误：无法在 layout/index.vue 中定位 topMenus');
  process.exit(1);
}
const topMenusStr = matchTopMenus[1];
const decisionAiIdx = topMenusStr.indexOf('decision-ai');
const activeSafetyIdx = topMenusStr.indexOf('active-safety');

if (activeSafetyIdx === -1) {
  console.error('❌ 错误：layout/index.vue 中 topMenus 未配置 active-safety (主动安全)');
  process.exit(1);
}
if (decisionAiIdx === -1) {
  console.error('❌ 错误：layout/index.vue 中 topMenus 未配置 decision-ai (决策式AI)');
  process.exit(1);
}
if (activeSafetyIdx < decisionAiIdx) {
  console.error('❌ 错误：layout/index.vue 中“主动安全”未配置在“决策式AI”之后');
  process.exit(1);
}
console.log('✅ Layout：顶部菜单“主动安全”正确配置在“决策式AI”之后');

// 1.2 校验侧边栏没有包含排除项（概览、事件拍传、事件类型）
const activeSafetySectionRegex = /'active-safety'\s*:\s*\[([\s\S]*?)\]\s*,\s*'(?:algorithm-warehouse|workspace|video-aggregation|decision-ai)'/i;
const matchSection = layoutContent.match(activeSafetySectionRegex) || [null, layoutContent];
const sectionStr = matchSection[1] || '';

const exclusions = ['概览', '事件拍传', '事件类型'];
exclusions.forEach(ex => {
  if (sectionStr.includes(ex)) {
    console.error(`❌ 错误：layout/index.vue 中主动安全分组包含排除项“${ex}”`);
    process.exit(1);
  }
});
console.log('✅ Layout：未检测到任何排除项（概览、事件拍传、事件类型）');


// 2. 校验 src/router/index.js
const routerContent = getFileContent('src/router/index.js');
const requiredRoutes = [
  '/active-safety/events/secure',
  '/active-safety/work-orders/my',
  '/active-safety/work-orders/pending',
  '/active-safety/work-orders/completed',
  '/active-safety/work-orders/claimable',
  '/active-safety/settings/secure',
  '/active-safety/settings/work-orders'
];

requiredRoutes.forEach(route => {
  if (!routerContent.includes(route)) {
    console.error(`❌ 错误：router/index.js 中缺少路由“${route}”`);
    process.exit(1);
  }
});
console.log('✅ Router：所有7个主动安全模块子路由均已注册完毕');


// 3. 校验 SecureEvents.vue 表头
const eventsContent = getFileContent('src/views/ActiveSafety/SecureEvents.vue');
const requiredEventHeaders = [
  '序号', '事件时间', '状态', '事件位置', '事件类型',
  '抓拍照片', '录制视频', '设备名称', '设备编号',
  '事件描述', '标签', '完成时间', '操作'
];

requiredEventHeaders.forEach(header => {
  if (!eventsContent.includes(header) && !eventsContent.includes(`label="${header}"`) && !eventsContent.includes(`label: '${header}'`)) {
    console.error(`❌ 错误：SecureEvents.vue 缺少要求的表头“${header}”`);
    process.exit(1);
  }
});
console.log('✅ SecureEvents.vue：主动安全事件表格全部13个表头配置验证通过');


// 4. 校验 WorkOrderList.vue 表头
const workOrderContent = getFileContent('src/views/ActiveSafety/WorkOrderList.vue');
const requiredWorkOrderHeaders = [
  '序号', '工单编号', '工单标题', '所属分类', '工单来源', '流程版本', '发起人', '工单状态', '提交时间', '接收时间', '操作'
];

requiredWorkOrderHeaders.forEach(header => {
  if (!workOrderContent.includes(header)) {
    console.error(`❌ 错误：WorkOrderList.vue 缺少要求的表头列配置“${header}”`);
    process.exit(1);
  }
});
console.log('✅ WorkOrderList.vue：所有四个工单页面表头支持度验证通过');


// 5. 校验系统设置页面配置
const sysSecureContent = getFileContent('src/views/ActiveSafety/SecureSettings.vue');
const requiredSecureLabels = ['区域 自动派单', '分组 自动派单', '标签 自动派单'];
requiredSecureLabels.forEach(label => {
  if (!sysSecureContent.includes(label)) {
    console.error(`❌ 错误：SecureSettings.vue 缺少主动安全设置项“${label}”`);
    process.exit(1);
  }
});
console.log('✅ SecureSettings.vue：主动安全设置项（区域/分组/标签 自动派单）均已声明');

const sysWorkContent = getFileContent('src/views/ActiveSafety/WorkOrderSettings.vue');
const requiredWorkLabels = ['新建', '环保模型'];
requiredWorkLabels.forEach(label => {
  if (!sysWorkContent.includes(label)) {
    console.error(`❌ 错误：WorkOrderSettings.vue 缺少工单设置项或动作“${label}”`);
    process.exit(1);
  }
});
console.log('✅ WorkOrderSettings.vue：包含工单设置项“环保模型”和“新建”动作');

console.log('\n🎉 所有主动安全模块静态合规性校验通过！符合设计规格。');
