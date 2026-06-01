import request from '@/utils/request'

// 销售登录
export function salesLogin(data) {
  return request.post('/auth/sales/login', data)
}

// 管理员登录
export function adminLogin(data) {
  return request.post('/auth/admin/login', data)
}
