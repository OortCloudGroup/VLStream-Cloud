/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import request from '@/utils/request'

export const getList = (current, size, params) => {
  return request({
    url: '/vlsSceneGovernance/list',
    method: 'get',
    params: {
      ...params,
      current,
      size,
    },
  });
};

export const remove = ids => {
  return request({
    url: '/vlsSceneGovernance/remove',
    method: 'post',
    params: {
      ids,
    },
  });
};

export const add = row => {
  return request({
    url: '/vlsSceneGovernance/submit',
    method: 'post',
    data: row,
  });
};

export const update = row => {
  return request({
    url: '/vlsSceneGovernance/submit',
    method: 'post',
    data: row,
  });
};