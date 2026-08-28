/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

/*
 * main full apaas configuration ( VLStream main full item )
 */
const AppConfig = {
  // event / main full (secure.vue、sysSecure2.vue appID)
  events: {
    appID: '818301f0e77f4cd8a117414cbeb32d9e',
    secretKey: '5f0de11687d744bc95e84e207d319493',
    requestType: 'app'
  },
  // work order (work order 、work orderSet )
  processui: {
    appID: '3b0ffd250acd400ea57fe23bd211c316',
    secretKey: '4555e6a5b2ec4e209932965ad6cdc9ae',
    requestType: 'app'
  },
  // task in (work order )
  task_center: {
    appID: '9afe9150e9a2475cae79ac0f6c21c834',
    secretKey: '85c3fee8e6fd4b57a3d091550b3376c5',
    requestType: 'app'
  },
  // can approval (work order workflow)
  Intelligent_approval: {
    appID: '228b506eb7484485ae9f7647dfdbbb40',
    secretKey: '07e0729633414df1b6f91b2ec3f9aff1',
    requestType: 'app'
  }
}

export default AppConfig
