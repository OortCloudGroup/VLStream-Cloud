/**
 * 获取 SpringBlade 统一接口 R<T> 的业务有效数据负载 (Payload)
 * @param {Object} response 后端响应对象
 * @returns {*} 返回 response.data 或 response 本身
 */
export function getPayload(response) {
  if (!response) return null
  if (Object.prototype.hasOwnProperty.call(response, 'data')) return response.data
  return response
}

/**
 * 从 SpringBlade 分页响应中获取数据记录数组列表 (records)
 * @param {Object} response 后端响应对象，支持 R<IPage> 或直接返回 IPage 实体
 * @returns {Array} 返回记录数组，默认空数组
 */
export function getRecords(response) {
  const payload = getPayload(response)
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.records)) return payload.records
  if (Array.isArray(response?.records)) return response.records
  return []
}

/**
 * 从 SpringBlade 分页响应中获取总条数 (total)
 * @param {Object} response 后端响应对象
 * @returns {Number} 返回总条数
 */
export function getTotal(response) {
  const payload = getPayload(response)
  return Number(payload?.total ?? response?.total ?? 0)
}

/**
 * 将扁平的 parentId 列表数据结构重构成树形嵌套结构
 * @param {Array} list 扁平列表数据
 * @param {Array} rootParentIds 定义为根节点的 parentId 标识符集合
 * @returns {Array} 重构后的树形结构
 */
export function buildTree(list, rootParentIds = [0, '0', null, undefined]) {
  const source = Array.isArray(list) ? list : []
  const nodeMap = new Map()
  
  // 第一步：映射所有节点并创建 children 空数组
  source.forEach((item) => {
    nodeMap.set(String(item.id), { ...item, children: Array.isArray(item.children) ? item.children : [] })
  })

  const roots = []
  
  // 第二步：将节点归属到 parent 或 roots
  nodeMap.forEach((node) => {
    const parentId = node.parentId
    const parent = nodeMap.get(String(parentId))
    if (parent && parent.id !== node.id) {
      parent.children.push(node)
    } else if (rootParentIds.includes(parentId)) {
      roots.push(node)
    } else {
      roots.push(node)
    }
  })
  return roots
}

/**
 * 递归规范化树节点，将后端多样的文本属性映射为 Element Plus 树或表格所需的统一属性 label/children
 * @param {Array} nodes 树节点数组
 * @returns {Array} 规范化后的树节点数组
 */
export function normalizeTree(nodes) {
  const list = Array.isArray(nodes) ? nodes : []
  return list.map((node) => {
    const label = node.label || node.title || node.name || node.roleName || node.deptName || node.scopeName || node.menuName || node.fullName || node.postName || node.tenantName || node.code || node.id
    return {
      ...node,
      label,
      children: normalizeTree(node.children)
    }
  })
}

/**
 * 提取被选中行的 ID 集合，并以逗号拼装，以适配后端 SpringBlade 的 comma-separated 参数接收格式
 * @param {Array} rows 选中的行数据数组
 * @returns {String} 逗号拼接的 ID 字符串
 */
export function joinIds(rows) {
  return (Array.isArray(rows) ? rows : [])
    .map((row) => row?.id)
    .filter((id) => id !== undefined && id !== null && id !== '')
    .join(',')
}

/**
 * 校验 SpringBlade 后端提交/删除/修改操作响应是否成功
 * @param {Object} response 响应对象
 * @returns {Boolean} true 代表操作成功
 */
export function isSuccess(response) {
  if (!response) return false
  if (response.success === true) return true
  if (response.code === 200) return true
  return response.data === true
}
