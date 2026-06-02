<template>
  <div class="login-page">
    <el-card class="login-card">
      <h2>管理员登录</h2>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="0">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item>
          <div class="login-options">
            <el-checkbox v-model="rememberMe">记住密码</el-checkbox>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="login-btn" @click="handleLogin" :loading="loading">
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref(null)
const loading = ref(false)
const rememberMe = ref(false)

const form = ref({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

onMounted(() => {
  const savedAccount = localStorage.getItem('admin_login_account')
  const savedPwd = localStorage.getItem('admin_login_password')
  if (savedAccount) form.value.username = savedAccount
  if (savedPwd) {
    form.value.password = savedPwd
    rememberMe.value = true
  }
})

const handleLogin = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await request.post('/auth/admin/login', form.value)
    if (rememberMe.value) {
      localStorage.setItem('admin_login_account', form.value.username)
      localStorage.setItem('admin_login_password', form.value.password)
    } else {
      localStorage.setItem('admin_login_account', form.value.username)
      localStorage.removeItem('admin_login_password')
    }
    authStore.setAuth(res.data.token, 'admin', res.data.admin)
    ElMessage.success('登录成功')
    router.push('/admin/dashboard')
  } catch (e) {
    console.error('登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #30cfd3 0%, #330867 100%);
}
.login-card {
  width: 400px;
  padding: 20px;
}
h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}
.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.login-btn {
  width: 100%;
}
</style>
