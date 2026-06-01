import request from '@/utils/request'

// 获取仪表盘数据（管理端）
export function getDashboardData() {
  return request.get('/admin/dashboard')
}

// 获取分润列表（管理端）
export function getCommissions(params) {
  return request.get('/admin/commissions', { params })
}

// 结算分润（管理端）
export function settleCommission(id) {
  return request.put(`/admin/commissions/${id}/settle`)
}

// 批量结算（管理端）
export function batchSettle(ids) {
  return request.post('/admin/commissions/batch-settle', { ids })
}

// 获取排行榜数据（销售端）
export function getLeaderboard() {
  return request.get('/leaderboard')
}
