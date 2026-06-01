import request from '@/utils/request'

// 获取系统配置
export function getConfig() {
  return request.get('/config')
}

// 更新系统配置（管理端）
export function updateConfig(data) {
  return request.put('/config', data)
}
