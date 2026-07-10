import request from '@/utils/request'

export const getList = (current, size, params) => {
  return request({
    url: '/blade-job/job-info/list',
    method: 'get',
    params: {
      ...params,
      current,
      size,
    },
  });
};

export const getDetail = id => {
  return request({
    url: '/blade-job/job-info/detail',
    method: 'get',
    params: {
      id,
    },
  });
};

export const remove = ids => {
  return request({
    url: '/blade-job/job-info/remove',
    method: 'post',
    params: {
      ids,
    },
  });
};

export const add = row => {
  return request({
    url: '/blade-job/job-info/submit',
    method: 'post',
    data: row,
  });
};

export const update = row => {
  return request({
    url: '/blade-job/job-info/submit',
    method: 'post',
    data: row,
  });
};

export const change = row => {
  return request({
    url: '/blade-job/job-info/change',
    method: 'post',
    params: {
      id: row.id,
      enable: row.enable,
    },
  });
};

export const run = row => {
  return request({
    url: '/blade-job/job-info/run',
    method: 'post',
    params: {
      id: row.id,
    },
  });
};

export const sync = row => {
  return request({
    url: '/blade-job/job-info/sync',
    method: 'post',
    data: row,
  });
};
