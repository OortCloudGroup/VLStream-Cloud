/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import emitter from '~@/utils/emitter'
import eventBus from '~@/utils/event-bus'

export default {
  mixins: [emitter],
  created() {},
  methods: {
    editEventHandler(eventName, eventParams) {
      this.dispatch('SettingPanel', 'editEventHandler', [eventName, [...eventParams]])
    }

  }
}
