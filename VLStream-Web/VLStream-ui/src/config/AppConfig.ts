/*
 * 主动安全模块 apaas 应用配置（仅保留 VLStream 主动安全所需项）
 */
const AppConfig = {
  // 智慧园林事件 / 主动安全（secure.vue、sysSecure2.vue 使用 appID）
  events: {
    appID: '818301f0e77f4cd8a117414cbeb32d9e',
    secretKey: '5f0de11687d744bc95e84e207d319493',
    requestType: 'app'
  },
  // 统一工单（工单管理、工单设置）
  processui: {
    appID: '3b0ffd250acd400ea57fe23bd211c316',
    secretKey: '4555e6a5b2ec4e209932965ad6cdc9ae',
    requestType: 'app'
  },
  // 任务中心（工单详情）
  task_center: {
    appID: '9afe9150e9a2475cae79ac0f6c21c834',
    secretKey: '85c3fee8e6fd4b57a3d091550b3376c5',
    requestType: 'app'
  },
  // 智能审批（工单创建流程）
  Intelligent_approval: {
    appID: '228b506eb7484485ae9f7647dfdbbb40',
    secretKey: '07e0729633414df1b6f91b2ec3f9aff1',
    requestType: 'app'
  }
}

export default AppConfig
