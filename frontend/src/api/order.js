import request from '@/utils/request'

// 获取我的订单（销售端）
export function getMyOrders(params) {
  return request.get('/orders/my', { params })
}

// 订单认领（销售端）
export function claimOrder(data) {
  return request.post('/orders/claim', data)
}

// 获取未认领订单（销售端）
export function getUnclaimedOrders() {
  return request.get('/orders/unclaimed')
}

// 获取我的统计（销售端）
export function getMyStats() {
  return request.get('/orders/stats')
}

// 获取所有订单（管理端）
export function getAllOrders(params) {
  return request.get('/admin/orders', { params })
}

// 更新订单状态（管理端）
export function updateOrderStatus(id, status) {
  return request.put(`/admin/orders/${id}/status`, { status })
}

// 模拟支付（沙箱测试）
export function simulatePayment(id) {
  return request.post(`/admin/orders/${id}/simulate-pay`)
}

// 获取发货消息预览
export function getDeliveryPreview(id) {
  return request.get(`/admin/orders/${id}/delivery-preview`)
}

// 发送发货消息
export function sendDelivery(id) {
  return request.post(`/admin/orders/${id}/send-delivery`)
}
