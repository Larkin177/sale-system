<template>
  <div class="login-page">
    <el-card class="login-card">
      <h2>销售登录</h2>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="0">
        <el-form-item prop="phone">
          <el-input v-model="form.phone" placeholder="手机号" prefix-icon="User" />
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
      <div class="register-link">
        还没有账号？<router-link to="/s/register">立即注册</router-link>
      </div>
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
  phone: '',
  password: ''
})

const rules = {
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

onMounted(() => {
  const savedAccount = localStorage.getItem('sales_login_account')
  if (savedAccount) {
    form.value.phone = savedAccount
    rememberMe.value = true
  }
})

const handleLogin = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await request.post('/auth/sales/login', form.value)
    // 只记住手机号，不存储密码
    if (rememberMe.value) {
      localStorage.setItem('sales_login_account', form.value.phone)
    } else {
      localStorage.removeItem('sales_login_account')
    }
    authStore.setAuth(res.data.token, 'sales', res.data.sales)
    ElMessage.success('登录成功')
    router.push('/s/dashboard')
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
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
.register-link {
  text-align: center;
  margin-top: 20px;
  color: #666;
}
.register-link a {
  color: #409eff;
  text-decoration: none;
}
.register-link a:hover {
  text-decoration: underline;
}
</style>
