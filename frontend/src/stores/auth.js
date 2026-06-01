import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const role = ref(localStorage.getItem('role') || '')
  const userInfo = ref(null)

  function setAuth(newToken, newRole, user) {
    token.value = newToken
    role.value = newRole
    userInfo.value = user
    localStorage.setItem('token', newToken)
    localStorage.setItem('role', newRole)
  }

  function logout() {
    token.value = ''
    role.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('role')
  }

  return { token, role, userInfo, setAuth, logout }
})
