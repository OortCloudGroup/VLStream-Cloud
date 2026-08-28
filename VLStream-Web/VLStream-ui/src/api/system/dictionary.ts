/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

import request from '@/utils/request'

const success = (list: any[], count = list.length) => ({
  code: 200,
  success: true,
  data: { list, count, total: count }
})

/* * RuoYi dict item Convert to form data . */
export async function dictListByTag(data: Record<string, any>) {
  const dictType = data.dict_tag || data.dictType
  const response: any = await request({
    url: `/system/dict/data/type/${encodeURIComponent(dictType)}`,
    method: 'get'
  })
  const rows = response?.data || response?.rows || []
  return success(rows.map((item: any) => ({
    ...item,
    name: item.dictLabel,
    description: item.dictValue,
    dict_tag: item.dictType,
    sort: item.dictSort
  })))
}

/* * RuoYi dict type Convert to form data . */
export async function dictList(data: Record<string, any>) {
  const response: any = await request({
    url: '/system/dict/type/list',
    method: 'get',
    params: {
      pageNum: data.page || data.pageNum || 1,
      pageSize: data.pagesize || data.pageSize || 10
    }
  })
  const rows = response?.rows || response?.data || []
  return success(rows.map((item: any) => ({
    ...item,
    name: item.dictName,
    dict_tag: item.dictType,
    description: item.remark || '',
    sort: item.dictId
  })), Number(response?.total || rows.length))
}
