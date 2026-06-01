import request from '@/utils/request'

// 获取销售列表（管理端）
export function getSalesList(params) {
  return request.get('/admin/sales', { params })
}

// 创建销售（管理端）
export function createSales(data) {
  return request.post('/admin/sales', data)
}

// 更新销售（管理端）
export function updateSales(id, data) {
  return request.put(`/admin/sales/${id}`, data)
}

// 切换销售状态（管理端）
export function toggleSalesStatus(id) {
  return request.put(`/admin/sales/${id}/toggle-status`)
}
