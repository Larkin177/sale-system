import request from '@/utils/request'

// 获取系统配置
export function getConfig() {
  return request.get('/config')
}

// 更新系统配置（管理端）
export function updateConfig(data) {
  return request.put('/config', data)
}

// Site settings
export function getSiteSettings() {
  return request.get('/site-settings')
}
export function getAdminSiteSettings() {
  return request.get('/admin/site-settings')
}
export function updateSiteSettings(data) {
  return request.put('/admin/site-settings', data)
}

// 根据手机号查询客户最近订单
export function getCustomerLastOrder(phone) {
  return request.get('/customer/last-order', { params: { phone } })
}

// 发送短信验证码
export function sendVerificationCode(phone) {
  return request.post('/verification/send', { phone })
}

// 验证短信验证码
export function verifyCode(phone, code) {
  return request.post('/verification/verify', { phone, code })
}

// 图形验证码
export function generateCaptcha() {
  return request.get('/captcha/generate')
}
export function verifyCaptcha(id, answer) {
  return request.post('/captcha/verify', { id, answer })
}

// 获取支付二维码
export function getPaymentQrCode(method) {
  return request.get('/payment/qrcode', { params: { method } })
}

// 获取客户记住的价格
export function getCustomerPrice(phone) {
  return request.get('/customer/price', { params: { phone } })
}
