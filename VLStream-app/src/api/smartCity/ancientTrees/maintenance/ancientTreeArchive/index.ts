import request from '@/api/smartCity/index'

// 古树名木档案信息 VO
export interface AncientTreeArchiveVO {
  id: undefined,
  treeNo: undefined,
  chineseName: undefined,
  familyGenus: undefined,
  scientificName: undefined,
  clfs: undefined,
  age: undefined,
  protectionLevel: undefined,
  height: undefined,
  dbh: undefined,
  growthStatus: undefined,
  geom: undefined,
  aliasName: undefined,
  latinName: undefined,
  gfdxm: undefined,
  gfnbm: undefined,
  crownSpread: undefined,
  locateRegion: undefined,
  address: undefined,
  longitude: undefined,
  latitude: undefined,
  zbhj: undefined,
  maintenanceMethod: undefined,
  reviewStatus: undefined,
  reviewer: undefined,
  reviewerName: undefined,
  reviewTime: undefined,
  reviewComments: undefined,
  publishStatus: undefined,
  publishTime: undefined,
  publisher: undefined,
  publisherName: undefined,
  picUrl: undefined,
  picUrls: [],
  pointsList: undefined,
  linesList: undefined,
  polygonsList: undefined,
  ghdw: undefined,
}

export interface AncientTreeArchiveStatisticVO {
  totalTrees: undefined,
  treeSpeciesCount: undefined,
  nationalFirstLevelProtectionCount: undefined,
  nationalSecondLevelProtectionCount: undefined,
  nationalThirdLevelProtectionCount: undefined,
  treesUnderYears: undefined,
  treesBetweenTears: undefined,
  treesOverYears: undefined,
}

// 古树名木档案信息 API
export const AncientTreeArchiveApi = {
  // 查询古树名木档案信息分页
  getAncientTreeArchivePage: async(params: any) => {
    return await request.get({ url: '/smartCity/ancient-tree-archive/page', params })
  },
  // 查询古树名木档案信息列表
  getAncientTreeArchiveList: async(params: any) => {
    return await request.get({ url: '/smartCity/ancient-tree-archive/list', params })
  },
  // 查询古树名木档案信息详情
  getAncientTreeArchive: async(id: number) => {
    return await request.get({ url: '/smartCity/ancient-tree-archive/get?id=' + id })
  },

  // 新增古树名木档案信息
  createAncientTreeArchive: async(data: AncientTreeArchiveVO) => {
    return await request.post({ url: '/smartCity/ancient-tree-archive/create', data })
  },

  // 修改古树名木档案信息
  updateAncientTreeArchive: async(data: AncientTreeArchiveVO) => {
    return await request.put({ url: '/smartCity/ancient-tree-archive/update', data })
  },
  // 提交审核古树名木档案信息
  submitApprovalAncientTreeArchive: async(data: AncientTreeArchiveVO) => {
    return await request.put({ url: '/smartCity/ancient-tree-archive/submitApproval', data })
  },
  // 审核古树名木档案信息
  approvalAncientTreeArchive: async(data: AncientTreeArchiveVO) => {
    return await request.put({ url: '/smartCity/ancient-tree-archive/approval', data })
  },
  // 发布古树名木档案信息
  publishAncientTreeArchive: async(data: AncientTreeArchiveVO) => {
    return await request.put({ url: '/smartCity/ancient-tree-archive/publish', data })
  },
  // 删除古树名木档案信息
  deleteAncientTreeArchive: async(id: number) => {
    return await request.delete({ url: '/smartCity/ancient-tree-archive/delete?id=' + id })
  },

  // 导出古树名木档案信息 Excel
  exportAncientTreeArchive: async(params) => {
    return await request.download({ url: '/smartCity/ancient-tree-archive/export-excel', params })
  },
  // 查询图层数据
  getAncientTreeArchiveGeoData: async(layerName: string) => {
    return await request.get({ url: `/smartCity/ancient-tree-archive/geoJson/${layerName}` })
  },
  // 查询古树统计数据
  getAncientTreeArchiveStatistic: async() => {
    return await request.get({ url: '/smartCity/ancient-tree-archive/getStatistic' })
  },
  // 根据道路古树统计
  getAncientTreeArchiveRoadStatistic: async(params) => {
    return await request.get({ url: '/smartCity/ancient-tree-archive/getRoadStatistic', params })
  }
}
