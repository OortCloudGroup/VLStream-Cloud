/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import mitt from 'mitt'

const instance = mitt()
const eventBus = {}
eventBus.$on = instance.on
eventBus.$off = instance.off
eventBus.$emit = instance.emit

export default eventBus
