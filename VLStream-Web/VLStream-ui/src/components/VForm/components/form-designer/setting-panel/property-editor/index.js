/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

let comps = {}

const modules = import.meta.glob('./**/*.vue', { eager: true })
for (const path in modules) {
  let cname = modules[path].default.name
  comps[cname] = modules[path].default
}

export default comps
