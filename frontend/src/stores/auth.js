import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const role = ref(localStorage.getItem('role') || '')
  const userInfo = ref(null)

  // 页面加载时恢复 userInfo（密码字段已为null，安全）
  const savedUser = localStorage.getItem('userInfo')
  if (savedUser) {
    try { userInfo.value = JSON.parse(savedUser) } catch (e) { /* ignore */ }
  }

  function setAuth(newToken, newRole, user) {
    token.value = newToken
    role.value = newRole
    userInfo.value = user
    localStorage.setItem('token', newToken)
    localStorage.setItem('role', newRole)
    // 持久化用户信息（不含密码）
    if (user) {
      localStorage.setItem('userInfo', JSON.stringify(user))
    }
  }

  function logout() {
    token.value = ''
    role.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('role')
    localStorage.removeItem('userInfo')
  }

  return { token, role, userInfo, setAuth, logout }
})
