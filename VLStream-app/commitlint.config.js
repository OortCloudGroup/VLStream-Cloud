/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 11:06:41
* @Last Modified by:   兰舰
* @Last Modified time: 2024-11-15 11:06:41
* @Copyright aPaaS-front-team. All rights reserved.
*/

/**
  rule配置说明:：rule由name和配置数组组成，如：'name:[0, 'always', 72]'，
  数组中第一位为level，可选0,1,2，0为disable，1为warning，2为error，
  第二位为应用与否，可选always|never，
  第三位该rule的值

 提交格式（注意冒号后面有空格）
 */
module.exports = {
  extends: ['@commitlint/config-conventional'],
  rules: {
    'type-enum': [
      2,
      'always',
      [
        'upd', // 更新某功能（不是 feat, 不是 fix）
        'feat', // 新功能（feature）
        'fix', // 修补bug
        'perf', // 优化性能
        'docs', // 文档（documentation）
        'style', // css 样式等格式（不影响代码运行的变动）
        'refactor', // 重构（即不是新增功能，也不是修改bug的代码变动）
        'test', // 增加测试
        'revert', // 回滚
        'type', // 类型文件更改
        'chore', // 构建过程或辅助工具的变动
        'wip' // 开发中
      ]
    ],
    'type-empty': [2, 'never'],
    'subject-empty': [0],
    'subject-full-stop': [0, 'never'],
    'subject-case': [0, 'never']
  }
}
